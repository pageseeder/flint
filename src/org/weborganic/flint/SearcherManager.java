/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.AlreadyClosedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager for searches.
 *
 * <p>Usage:
 * <pre>
 * IndexSearcher searcher = searcherManager.get();
 * try {
 *    <do searching & rendering here>
 *  } finally {
 *    searcherManager.release(searcher);
 *  }
 * </pre>
 *
 * @author Jean-Baptiste Reure
 * @version 26 February 2010
 */
public class SearcherManager {

  /**
   * Logger
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(SearcherManager.class);

  /**
   * The current searcher used to run searches on the index
   */
  private IndexSearcher currentSearcher;

  /**
   * Create a new SearcherManager using the given writer.
   *
   * @param writer the IndexWriter used to load the real-time reader.
   * @throws IOException If thrown while trying to get the reader.
   */
  public SearcherManager(IndexWriter writer) throws IOException {
    IndexReader reader = writer.getReader();
    // Lucene 3.1+: IndexReader.open(writer, true);
    this.currentSearcher = new IndexSearcher(reader);
  }

  /**
   * Create a new SearcherManager using the given writer.
   *
   * @param reader the IndexWriter used to load the real-time reader.
   * @throws IOException If thrown while trying to get the reader.
   */
  public SearcherManager(IndexReader reader) throws IOException {
    this.currentSearcher = new IndexSearcher(reader);
  }

  // ------------------ Index re-opening methods -----------------------------

  /**
   * flag used to set current state.
   */
  private boolean reopening = false;

  /**
   * Start re-open
   */
  private synchronized void startReopen() throws InterruptedException {
    LOGGER.debug("Starting open");
    while (this.reopening) {
      wait();
    }
    this.reopening = true;
  }

  /**
   * Conclude re-open.
   */
  private synchronized void doneReopen() {
    LOGGER.debug("Finishing open");
    this.reopening = false;
    notifyAll();
  }

  /**
   * Trigger a check to reopen the reader.
   *
   * @throws InterruptedException
   * @throws IOException
   */
  public void maybeReopen() throws InterruptedException, IOException {
    startReopen();
    try {
      final IndexSearcher searcher = get();
      try {
        if (!this.currentSearcher.getIndexReader().isCurrent()) {
          // if not current, we need to re-open it
          IndexReader newReader = this.currentSearcher.getIndexReader().reopen();
          IndexSearcher newSearcher = new IndexSearcher(newReader);
          swapSearcher(newSearcher);
        } else {
          LOGGER.debug("Reader is still current so no need to re-open it");
        }
      } finally {
        release(searcher);
      }
    } finally {
      doneReopen();
    }
  }

  /**
   * Perform a swap between current searcher and given searcher.
   *
   * @param newSearcher
   * @throws IOException
   */
  private synchronized void swapSearcher(IndexSearcher newSearcher) throws IOException {
    LOGGER.debug("Swapping reader from {} to {}",
        this.currentSearcher.getIndexReader().hashCode(), newSearcher.getIndexReader().hashCode());
    release(this.currentSearcher);
    this.currentSearcher = newSearcher;
  }
  /**
   * Check if the reader provided is not current and not used anymore in which case it is closed.
   *
   * @param reader the reader to check
   *
   * @throws IOException if closing failed
   */
  private void closeIfDirty(IndexReader reader) throws IOException {
    // check if we should close an old one
    if (this.currentSearcher.getIndexReader() != reader) {
      if (reader.getRefCount() == 0) {
        LOGGER.debug("Closing reader {}", reader.hashCode());
        try {
          reader.close();
        } catch (AlreadyClosedException e) {
          // good then, no need to worry
        }
      } else {
        LOGGER.debug("Cannot close reader {} as there are still references ({})", reader.hashCode(), reader.getRefCount());
      }
    }
  }

  // ------------------ public methods -----------------------------

  /**
   * Close this searcher by closing the current searcher and its current reader.
   *
   * @throws IOException If closing this searcher failed
   */
  protected final void close() throws IOException {
    // close reader
    this.currentSearcher.getIndexReader().close();
    // then searcher
    this.currentSearcher.close();
  }

  /**
   * Return the current IndexSearcher. Important: call release() when finished with the searcher.
   *
   * @return the current IndexSearcher
   * @throws InterruptedException
   */
  protected synchronized IndexSearcher get() {
    this.currentSearcher.getIndexReader().incRef();
    LOGGER.debug("Getting reader {}", this.currentSearcher.getIndexReader().hashCode());
    return this.currentSearcher;
  }

  /**
   * Release the given searcher.
   *
   * @param searcher
   * @throws IOException
   */
  protected synchronized void release(IndexSearcher searcher) throws IOException {
    LOGGER.debug("Releasing reader {}", searcher.getIndexReader().hashCode());
    searcher.getIndexReader().decRef();
    // check if we should close an old one
    closeIfDirty(searcher.getIndexReader());
  }

  /**
   * Return the current IndexReader. Important: call releaseReader() when finished with the Index Reader.
   *
   * @return the current IndexReader
   *
   * @throws InterruptedException
   */
  protected synchronized IndexReader getReader() {
    LOGGER.debug("Getting reader {}", this.currentSearcher.getIndexReader().hashCode());
    this.currentSearcher.getIndexReader().incRef();
    return this.currentSearcher.getIndexReader();
  }

  /**
   * Release the given reader.
   *
   * @param reader the reader to release
   *
   * @throws IOException If thrown when attempting to close the reader, when reader is no longer in use.
   */
  protected synchronized void releaseReader(IndexReader reader) throws IOException {
    LOGGER.debug("Releasing reader {}", reader.hashCode());
    reader.decRef();
    // check if we should close an old one
    closeIfDirty(reader);
  }
}

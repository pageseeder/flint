/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint;

import java.io.IOException;

import org.apache.lucene.index.FilterIndexReader;
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
final class SearcherManager {

  /**
   * Logger
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(SearcherManager.class);

  /**
   * The current near-real time reader used to run searches on the index
   */
  private SafeIndexReader _reader = null;

  /**
   * The current searcher used to run searches on the index
   */
  private IndexSearcher _searcher = null;

  /**
   * Flag used to set current state.
   */
  private volatile boolean reopening = false;

  /**
   * Create a new SearcherManager using the given writer.
   *
   * @param writer the IndexWriter used to load the real-time reader.
   * @throws IOException If thrown while trying to get the reader.
   */
  public SearcherManager(IndexWriter writer) throws IOException {
    // Lucene 3.1+: IndexReader.open(writer, true);
    IndexReader reader = writer.getReader();
    set(reader);
  }

  /**
   * Create a new SearcherManager using the given writer.
   *
   * @param reader the IndexWriter used to load the real-time reader.
   * @throws IOException If thrown while trying to get the reader.
   */
  public SearcherManager(IndexReader reader) throws IOException {
    set(reader);
  }

  // Getting and releasing index readers and searchers
  // ----------------------------------------------------------------------------------------------

  /**
   * Return the current IndexSearcher.
   *
   * <p>Important: call release() when finished with the searcher.
   *
   * @return the current IndexSearcher
   * @throws InterruptedException
   */
  protected synchronized IndexSearcher get() {
    this._reader.incRef();
    LOGGER.debug("Getting reader {}", this._reader.hashCode());
    return this._searcher;
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
    LOGGER.debug("Getting reader {}", this._reader.hashCode());
    this._reader.incRef();
    return this._reader;
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

  protected int getRefCount() {
    return this._reader.getRefCount() - 1;
  }

  // Index re-opening methods
  // ----------------------------------------------------------------------------------------------

  /**
   * Trigger a check to reopen the reader.
   *
   * @throws InterruptedException
   * @throws IOException
   */
  public void maybeReopen() throws InterruptedException, IOException {
    startReopen();
    try {
      if (!this._reader.isCurrent()) {
        // if not current, we need to re-open it
        IndexReader newReader = this._reader.reopen();
        LOGGER.debug("Swapping reader from {} to {}", this._reader.hashCode(), newReader.hashCode());
        set(newReader);
      } else {
        LOGGER.debug("Reader is still current so no need to re-open it");
      }
    } finally {
      doneReopen();
    }
  }

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
   * Set the real time reader and searchers to use.
   *
   * @param reader The reader to use.
   */
  private synchronized void set(IndexReader reader) {
    this._reader = new SafeIndexReader(reader);
    this._searcher = new IndexSearcher(reader);
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
    if (this._reader != reader && reader instanceof SafeIndexReader) {
      if (reader.getRefCount() == 1) {
        LOGGER.debug("Closing reader {}", reader.hashCode());
        try {
          ((SafeIndexReader)reader).original().close();
        } catch (AlreadyClosedException ex) {
          // good then, no need to worry
        }
      } else {
        LOGGER.debug("Cannot close reader {} as there are still references ({})", reader.hashCode(), reader.getRefCount());
      }
    }
  }

  /**
   * Close this searcher by closing the current searcher and its current reader.
   *
   * @throws IOException If closing this searcher failed
   */
  protected final void close() throws IOException {
    // close reader
    this._reader.close();
    // then searcher
    this._searcher.close();
  }

  /**
   * An index reader implementation which overrides some methods to protect the reader from
   * being tempered with by different threads and therefore make it safe to share.
   *
   * @author Christophe Lauret
   * @version 27 February 2013
   */
  private static final class SafeIndexReader extends FilterIndexReader {

    /**
     * Simply wraps the supplied index reader
     *
     * @param reader The reader to wrap.
     */
    public SafeIndexReader(IndexReader reader) {
      super(reader);
    }

    @Override
    protected void doClose() throws IOException {
      StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
      String caller = stacktrace.length > 0? stacktrace[0].toString() : null;
      LOGGER.warn("{} tried to close the IndexReader for", caller);
    }

    /**
     * @return the original Index reader used.
     */
    protected IndexReader original() {
      return this.in;
    }

  }

}

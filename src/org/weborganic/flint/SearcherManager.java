/*
 * This file is part of the Flint library.
 * 
 * For licensing information please see the file license.txt included in the release. A copy of this licence can also be
 * found at http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;

/**
 * Manager for searches
 * 
 * Usage:
 * 
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
  private static final Logger LOGGER = Logger.getLogger(SearcherManager.class);

  /**
   * The current searcher used to run searches on the index
   */
  private IndexSearcher currentSearcher;

  /**
   * Create a new SEarcherManager using the given writer
   * 
   * @param thewriter the IndexWriter used to load the real-time reader.
   * @throws IOException
   */
  public SearcherManager(IndexWriter thewriter) throws IOException {
    this.currentSearcher = new IndexSearcher(thewriter.getReader());
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
    while (this.reopening)
      wait();
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
        IndexReader newReader = this.currentSearcher.getIndexReader().reopen();
        if (newReader != this.currentSearcher.getIndexReader()) {
          IndexSearcher newSearcher = new IndexSearcher(newReader);
          swapSearcher(newSearcher);
        }
      } finally {
        release(searcher);
      }
    } finally {
      doneReopen();
    }
  }

  /**
   * Performs a swap between current searcher and given searcher.
   * 
   * @param newSearcher
   * @throws IOException
   */
  private synchronized void swapSearcher(IndexSearcher newSearcher) throws IOException {
    LOGGER.debug("Swapping searcher from " + this.currentSearcher.hashCode() + " to " + newSearcher.hashCode());
    release(this.currentSearcher);
    this.currentSearcher = newSearcher;
  }

  // ------------------ public methods -----------------------------

  /**
   * Return the current IndexSearcher. Important: call release() when finished with the searcher.
   * 
   * @return the current IndexSearcher
   * @throws InterruptedException 
   */
  public synchronized IndexSearcher get() {
    LOGGER.debug("Getting searcher " + this.currentSearcher.hashCode());
    this.currentSearcher.getIndexReader().incRef();
    return this.currentSearcher;
  }

  /**
   * Release the given searcher.
   * 
   * @param searcher
   * @throws IOException
   */
  public synchronized void release(IndexSearcher searcher) throws IOException {
    LOGGER.debug("Releasing searcher " + searcher.hashCode());
    searcher.getIndexReader().decRef();
    // check if we should close an old one
    if (this.currentSearcher != searcher && searcher.getIndexReader().getRefCount() == 0)
      searcher.close();
  }
}

/*
 * This file is part of the Flint library.
 * 
 * For licensing information please see the file license.txt included in the release. A copy of this licence can also be
 * found at http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.BalancedSegmentMergePolicy;
import org.apache.lucene.index.ConcurrentMergeScheduler;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.LockObtainFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.flint.content.DeleteRule;

/**
 * Provides a set of utility methods to deal with IO operations on an Index.
 * 
 * <p>This class is useful to centralise all operations on an index because it will create one
 * writer and share it with other classes if needed.
 * 
 * <p>This is a lower level API.
 * 
 * @author Jean-Baptiste Reure
 * @version 26 February 2010
 */
public final class IndexIOReadWrite extends IndexIO {

  /**
   * Logger.
   */
  static final Logger LOGGER = LoggerFactory.getLogger(IndexIOReadWrite.class);

  private final Index index;
  
  /**
   * The underlying index writer used by Flint for this index (there should only be one).
   */
  private IndexWriter writer;

  /**
   * A search manager using this writer.
   */
  private SearcherManager searcherManager;
  /**
   * The last time this reader was used
   */
  private long lastTimeUsed;

  /**
   * Sole constructor.
   * 
   * @param index The index on which IO operations will occur. 
   * 
   * @throws CorruptIndexException     If thrown by Lucene when creating the index writer.
   * @throws LockObtainFailedException If thrown by Lucene when creating the index writer.
   * @throws IOException               If thrown by Lucene when creating the index writer.
   */
  public IndexIOReadWrite(Index ind) throws IOException {
    super(ind);
    this.index = ind;
    start();
  }

  private void maybeReopen() {
    if (this.state != State.NEEDS_REOPEN) return;
    try {
      LOGGER.debug("Reopen searcher");
      this.searcherManager.maybeReopen();
      this.state = State.NEEDS_COMMIT;
      // FIXME exceptions are completely ignored here !!!
    } catch (InterruptedException ex) {
      LOGGER.error("Failed to reopen the Index Searcher because the thread has been interrupted", ex);
    } catch (IOException ex) {
      LOGGER.error("Failed to reopen Index Searcher because of an I/O error", ex);
    }
  }

  /**
   * 
   * @throws IndexException
   */
  public void maybeCommit() throws IndexException {
    if (this.state != State.NEEDS_COMMIT || this.writer == null) return;
    try {
      LOGGER.debug("Committing");
      this.writer.commit();
      this.searcherManager.maybeReopen();
      this.state = State.NEEDS_OPTIMISE;
    } catch (CorruptIndexException e) {
      throw new IndexException("Failed to commit Index because it is corrupted", e);
    } catch (IOException e) {
      throw new IndexException("Failed to commit Index because of an I/O error", e);
    } catch (InterruptedException e) {
      throw new IndexException("Failed to commit Index because of the thread has been interrupted", e);
    }
  }
  
  /**
   * {@inheritdoc}
   */
  public void maybeOptimise() throws IndexException {
    if (this.state != State.NEEDS_OPTIMISE || this.writer == null) return;
    try {
      LOGGER.debug("Optimising");
      this.writer.optimize();
      this.searcherManager.maybeReopen();
      this.state = State.CLEAN;
    } catch (CorruptIndexException e) {
      throw new IndexException("Failed to optimise Index because it is corrupted", e);
    } catch (IOException e) {
      throw new IndexException("Failed to optimise Index because of an I/O error", e);
    } catch (InterruptedException e) {
      throw new IndexException("Failed to optimise Index because of the thread has been interrupted", e);
    }
  }
  
  /**
   * {@inheritdoc}
   */
  public boolean clearIndex() throws IndexException {
    LOGGER.debug("Clearing Index");
    // add documents to index
    try {
      ensureOpen();
      writer.deleteAll();
      this.state = State.NEEDS_REOPEN;
    } catch (CorruptIndexException e) {
      throw new IndexException("Failed to clear Index because it is corrupted", e);
    } catch (IOException e) {
      throw new IndexException("Failed to clear Index because of an I/O error", e);
    }
    return true;
  }

  /**
   * Delete the documents defined in the detele rule.
   * 
   * @param rule
   * @return
   * @throws IndexException
   */
  public boolean deleteDocuments(DeleteRule rule) throws IndexException {
    LOGGER.debug("Deleting a document");
    // add documents to index
    try {
      ensureOpen();
      if (rule.useTerm()) writer.deleteDocuments(rule.toTerm());
      else writer.deleteDocuments(rule.toQuery());
      this.state = State.NEEDS_REOPEN;
    } catch (CorruptIndexException e) {
      throw new IndexException("Failed to delete document from Index because it is corrupted", e);
    } catch (IOException e) {
      throw new IndexException("Failed to delete document from Index because of an I/O error", e);
    }
    return true;
  }

  /**
   * 
   * @param rule
   * @param documents
   * @return
   * @throws IndexException
   */
  public boolean updateDocuments(DeleteRule rule, List<Document> documents) throws IndexException {
    LOGGER.debug("Updating {} documents", documents.size());
    try {
      ensureOpen();
      if (rule != null) {
        if (rule.useTerm()) writer.deleteDocuments(rule.toTerm());
        else writer.deleteDocuments(rule.toQuery());
      }
      for (Document doc : documents) {
        writer.addDocument(doc);
      }
      this.state = State.NEEDS_REOPEN;
    } catch (CorruptIndexException e) {
      throw new IndexException("Failed to update document in Index because it is corrupted", e);
    } catch (IOException e) {
      throw new IndexException("Failed to update document in Index because of an I/O error", e);
    }
    return true;
  }

  /**
   * Returns the index searcher for the index.
   * 
   * <p>Note: when a searcher is booked it must be released using {{@link #releaseSearcher(IndexSearcher)}.
   * 
   * @return The index searcher. 
   * 
   * @throws IOException
   */
  public IndexSearcher bookSearcher() throws IOException {
    ensureOpen();
    // check for reopening
    maybeReopen();
    return this.searcherManager.get();
  }

  /**
   * 
   * @param searcher
   * @throws IOException
   */
  public void releaseSearcher(IndexSearcher searcher) throws IOException {
    if (this.searcherManager != null)
      this.searcherManager.release(searcher);
    this.lastTimeUsed = System.currentTimeMillis();
  }

  protected IndexReader bookReader() throws IOException {
    ensureOpen();
    // check for reopening
    maybeReopen();
    return this.searcherManager.getReader();
  }

  protected void releaseReader(IndexReader reader) throws IOException {
    if (this.searcherManager != null)
      this.searcherManager.releaseReader(reader);
    this.lastTimeUsed = System.currentTimeMillis();
  }
  /**
   * @return the lastTimeUsed
   */
  public long getLastTimeUsed() {
    return this.lastTimeUsed;
  }
  
  private void ensureOpen() throws IOException {
    if (this.writer == null)
      start();
    this.lastTimeUsed = System.currentTimeMillis();
  }

  /**
   * Opens a new writer on the index.
   * 
   * @throws IndexException Wrapping an {@link CorruptIndexException} or an {@link IOException}.
   */
  public void start() throws IOException {
    this.writer = new IndexWriter(this.index.getIndexDirectory(), this.index.getAnalyzer(), IndexWriter.MaxFieldLength.LIMITED);
    this.writer.setMergeScheduler(new ConcurrentMergeScheduler());
    this.writer.setMergePolicy(new BalancedSegmentMergePolicy(this.writer));
    this.searcherManager = new SearcherManager(this.writer);
    this.lastTimeUsed = System.currentTimeMillis();
    OpenIndexManager.add(this);
  }

  /**
   * Closes the writer on this index.
   * 
   * @throws IndexException Wrapping an {@link CorruptIndexException} or an {@link IOException}.
   */
  public void stop() throws IndexException {
    try {
      this.searcherManager.close();
      this.searcherManager = null;
      this.writer.close();
      this.writer = null;
      OpenIndexManager.remove(this);
    } catch (CorruptIndexException e) {
      throw new IndexException("Failed to close Index because it is corrupted", e);
    } catch (IOException e) {
      throw new IndexException("Failed to close Index because of an I/O error", e);
    }
  }
}

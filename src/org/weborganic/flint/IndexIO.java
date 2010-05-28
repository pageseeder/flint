/*
 * This file is part of the Flint library.
 * 
 * For licensing information please see the file license.txt included in the release. A copy of this licence can also be
 * found at http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.BalancedSegmentMergePolicy;
import org.apache.lucene.index.ConcurrentMergeScheduler;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.LockObtainFailedException;
import org.weborganic.flint.content.DeleteRule;

/**
 * 
 * @author Jean-Baptiste Reure
 * @version 26 February 2010
 */
public class IndexIO {

  private static final Logger LOGGER = Logger.getLogger(IndexIO.class);

  private static enum STATE {
    CLEAN, NEEDS_REOPEN, NEEDS_COMMIT, NEEDS_OPTIMISE
  };

  private final IndexWriter writer;

  private final SearcherManager searcherManager;

  private IndexIO.STATE state = STATE.CLEAN;

  public IndexIO(Index index) throws CorruptIndexException, LockObtainFailedException, IOException {
    this.writer = new IndexWriter(index.getIndexDirectory(), index.getAnalyzer(), IndexWriter.MaxFieldLength.LIMITED);
    this.writer.setMergeScheduler(new ConcurrentMergeScheduler());
    this.writer.setMergePolicy(new BalancedSegmentMergePolicy(this.writer));
    this.searcherManager = new SearcherManager(this.writer);
  }

  private void maybeReopen() {
    if (this.state != STATE.NEEDS_REOPEN) return;
    try {
      LOGGER.debug("Reopen searcher");
      this.searcherManager.maybeReopen();
      this.state = STATE.NEEDS_COMMIT;
    } catch (InterruptedException e) {
      LOGGER.error("Failed to reopen the Index Searcher because the thread has been interrupted", e);
    } catch (IOException e) {
      LOGGER.error("Failed to reopen Index Searcher because of an I/O error", e);
    }
  }

  public void maybeCommit() throws IndexException {
    if (this.state != STATE.NEEDS_COMMIT) return;
    try {
      LOGGER.debug("Committing");
      this.writer.commit();
      this.searcherManager.maybeReopen();
      this.state = STATE.NEEDS_OPTIMISE;
    } catch (CorruptIndexException e) {
      throw new IndexException("Failed to commit Index because it is corrupted", e);
    } catch (IOException e) {
      throw new IndexException("Failed to commit Index because of an I/O error", e);
    } catch (InterruptedException e) {
      throw new IndexException("Failed to commit Index because of the thread has been interrupted", e);
    }
  }

  public void maybeOptimise() throws IndexException {
    if (this.state != STATE.NEEDS_OPTIMISE) return;
    try {
      LOGGER.debug("Optimising");
      this.writer.optimize();
      this.searcherManager.maybeReopen();
      this.state = STATE.CLEAN;
    } catch (CorruptIndexException e) {
      throw new IndexException("Failed to optimise Index because it is corrupted", e);
    } catch (IOException e) {
      throw new IndexException("Failed to optimise Index because of an I/O error", e);
    } catch (InterruptedException e) {
      throw new IndexException("Failed to optimise Index because of the thread has been interrupted", e);
    }
  }

  public boolean clearIndex() throws IndexException {
    LOGGER.debug("Clearing Index");
    // add documents to index
    try {
      writer.deleteAll();
      this.state = STATE.NEEDS_REOPEN;
    } catch (CorruptIndexException e) {
      throw new IndexException("Failed to clear Index because it is corrupted", e);
    } catch (IOException e) {
      throw new IndexException("Failed to clear Index because of an I/O error", e);
    }
    return true;
  }

  public boolean deleteDocuments(DeleteRule rule) throws IndexException {
    LOGGER.debug("Deleting a document");
    // add documents to index
    try {
      if (rule.useTerm()) writer.deleteDocuments(rule.toTerm());
      else writer.deleteDocuments(rule.toQuery());
      this.state = STATE.NEEDS_REOPEN;
    } catch (CorruptIndexException e) {
      throw new IndexException("Failed to delete document from Index because it is corrupted", e);
    } catch (IOException e) {
      throw new IndexException("Failed to delete document from Index because of an I/O error", e);
    }
    return true;
  }

  public boolean updateDocuments(DeleteRule rule, List<Document> documents) throws IndexException {
    LOGGER.debug("Updating " + documents.size() + " documents");
    try {
      if (rule != null) {
        if (rule.useTerm()) writer.deleteDocuments(rule.toTerm());
        else writer.deleteDocuments(rule.toQuery());
      }
      for (Document doc : documents)
        writer.addDocument(doc);
      this.state = STATE.NEEDS_REOPEN;
    } catch (CorruptIndexException e) {
      throw new IndexException("Failed to update document in Index because it is corrupted", e);
    } catch (IOException e) {
      throw new IndexException("Failed to update document in Index because of an I/O error", e);
    }
    return true;
  }

  public IndexSearcher bookSearcher() throws IOException {
    // check for reopening
    maybeReopen();
    return this.searcherManager.get();
  }

  public void releaseSearcher(IndexSearcher searcher) throws IOException {
    this.searcherManager.release(searcher);
  }

  /**
   * Closes the writer on this index.
   * 
   * @throws IndexException Wrapping an {@link CorruptIndexException} or an {@link IOException}.
   */
  public void stop() throws IndexException {
    try {
      this.writer.close();
    } catch (CorruptIndexException e) {
      throw new IndexException("Failed to close Index because it is corrupted", e);
    } catch (IOException e) {
      throw new IndexException("Failed to close Index because of an I/O error", e);
    }
  }

}

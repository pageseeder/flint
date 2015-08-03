/*
 * Copyright 2015 Allette Systems (Australia)
 * http://www.allette.com.au
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pageseeder.flint;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.BalancedSegmentMergePolicy;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.search.IndexSearcher;
import org.pageseeder.flint.api.Index;
import org.pageseeder.flint.content.DeleteRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
final class IndexIOReadWrite extends IndexIO {

  /**
   * Logger.
   */
  static final Logger LOGGER = LoggerFactory.getLogger(IndexIOReadWrite.class);

  /**
   * The index this class acts upon.
   */
  private final Index _index;

  /**
   * The last time this reader was used
   */
  private final AtomicLong lastTimeUsed = new AtomicLong(0);

  /**
   * The underlying index writer used by Flint for this index (there should only be one).
   */
  private IndexWriter writer;

  /**
   * A search manager using this writer.
   */
  private SearcherManager searcherManager;

  /**
   * Sole constructor.
   *
   * @param index The index on which IO operations will occur.
   *
   * @throws IOException               If thrown by Lucene when creating the index writer.
   */
  public IndexIOReadWrite(Index index) throws IOException {
    super(index);
    this._index = index;
    start();
  }

  /**
   * Attempts to reopen the readers.
   */
  @Override
  protected void maybeReopen() {
    if (this.state != State.NEEDS_REOPEN) return;
    try {
      LOGGER.debug("Reopen searcher");
      this.searcherManager.maybeReopen();
      this.state = State.NEEDS_COMMIT;
      // FIXME exceptions are completely ignored here !!!
    } catch (final InterruptedException ex) {
      LOGGER.error("Failed to reopen the Index Searcher because the thread has been interrupted", ex);
    } catch (final IOException ex) {
      LOGGER.error("Failed to reopen Index Searcher because of an I/O error", ex);
    }
  }

  /**
   * Attempts to commit all outstanding changes in the writer when the index is in 'NEEDS_COMMIT' state.
   *
   * <p>Does nothing if the writer is not in {@link State#NEEDS_COMMIT} state or if the writer is <code>null</code>.
   *
   * @throws IndexException Will wrap any exception thrown while trying to commit the index.
   */
  @Override
  public void maybeCommit() throws IndexException {
    if (this.state != State.NEEDS_COMMIT || this.writer == null) return;
    try {
      LOGGER.debug("Committing index changes");
      this.writer.commit();
      this.searcherManager.maybeReopen();
      this.state = State.NEEDS_OPTIMISE;
    } catch (final CorruptIndexException ex) {
      throw new IndexException("Failed to commit Index because it is corrupted", ex);
    } catch (final IOException ex) {
      throw new IndexException("Failed to commit Index because of an I/O error", ex);
    } catch (final InterruptedException ex) {
      throw new IndexException("Failed to commit Index because of the thread has been interrupted", ex);
    }
  }

  /**
   * Attempts to optimize the index is in 'NEEDS_OPTIMISE' state.
   *
   * <p>Does nothing if the writer is not in {@link State#NEEDS_OPTIMISE} state or if the writer is <code>null</code>.
   *
   * @throws IndexException Will wrap any exception thrown while trying to commit the index.
   */
  @Override
  public void maybeOptimise() throws IndexException {
    if (this.state != State.NEEDS_OPTIMISE || this.writer == null) return;
    try {
      LOGGER.debug("Optimising");
      this.writer.optimize();
      this.searcherManager.maybeReopen();
      this.state = State.CLEAN;
    } catch (final CorruptIndexException e) {
      throw new IndexException("Failed to optimise Index because it is corrupted", e);
    } catch (final IOException e) {
      throw new IndexException("Failed to optimise Index because of an I/O error", e);
    } catch (final InterruptedException e) {
      throw new IndexException("Failed to optimise Index because of the thread has been interrupted", e);
    }
  }

  @Override
  public boolean clearIndex() throws IndexException {
    LOGGER.debug("Clearing Index");
    // add documents to index
    try {
      ensureOpen();
      this.writer.deleteAll();
      this.state = State.NEEDS_REOPEN;
    } catch (final CorruptIndexException e) {
      throw new IndexException("Failed to clear Index because it is corrupted", e);
    } catch (final IOException e) {
      throw new IndexException("Failed to clear Index because of an I/O error", e);
    }
    return true;
  }

  @Override
  public boolean deleteDocuments(DeleteRule rule) throws IndexException {
    LOGGER.debug("Deleting a document");
    // add documents to index
    try {
      ensureOpen();
      if (rule.useTerm()) {
        this.writer.deleteDocuments(rule.toTerm());
      } else {
        this.writer.deleteDocuments(rule.toQuery());
      }
      this.state = State.NEEDS_REOPEN;
    } catch (final CorruptIndexException e) {
      throw new IndexException("Failed to delete document from Index because it is corrupted", e);
    } catch (final IOException e) {
      throw new IndexException("Failed to delete document from Index because of an I/O error", e);
    }
    return true;
  }

  @Override
  public boolean updateDocuments(DeleteRule rule, List<Document> documents) throws IndexException {
    LOGGER.debug("Updating {} documents", documents.size());
    try {
      ensureOpen();
      if (rule != null) {
        if (rule.useTerm()) {
          this.writer.deleteDocuments(rule.toTerm());
        } else {
          this.writer.deleteDocuments(rule.toQuery());
        }
      }
      for (final Document doc : documents) {
        this.writer.addDocument(doc);
      }
      this.state = State.NEEDS_REOPEN;
    } catch (final CorruptIndexException e) {
      throw new IndexException("Failed to update document in Index because it is corrupted", e);
    } catch (final IOException e) {
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
  @Override
  public IndexSearcher bookSearcher() throws IOException {
    ensureOpen();
    // check for reopening
    maybeReopen();
    return this.searcherManager.get();
  }

  @Override
  public void releaseSearcher(IndexSearcher searcher) throws IOException {
    if (this.searcherManager != null) {
      this.searcherManager.release(searcher);
    }
    this.lastTimeUsed.set(System.currentTimeMillis());
  }

  @Override
  protected IndexReader bookReader() throws IOException {
    ensureOpen();
    // check for reopening
    maybeReopen();
    return this.searcherManager.getReader();
  }

  @Override
  protected void releaseReader(IndexReader reader) throws IOException {
    if (this.searcherManager != null) {
      this.searcherManager.releaseReader(reader);
    }
    this.lastTimeUsed.set(System.currentTimeMillis());
  }

  @Override
  protected int countBookedReaders() {
    return this.searcherManager.getRefCount();
  }

  /**
   * @return the lastTimeUsed
   */
  public long getLastTimeUsed() {
    return this.lastTimeUsed.longValue();
  }

  /**
   * Ensures that it is open.
   *
   * @throws IOException
   */
  private void ensureOpen() throws IOException {
    if (this.writer == null) {
      start();
    }
    this.lastTimeUsed.set(System.currentTimeMillis());
  }

  /**
   * Opens a new writer on the index.
   *
   * @throws IndexException Wrapping an {@link CorruptIndexException} or an {@link IOException}.
   */
  public void start() throws IOException {
    // TODO: Handle Lucene 3.1+:
    // IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_30, this._index.getAnalyzer());
    // config.setMergeScheduler(new ConcurrentMergeScheduler());
    // config.setMergePolicy(new BalancedSegmentMergePolicy());
    this.writer = new IndexWriter(this._index.getIndexDirectory(), this._index.getAnalyzer(), IndexWriter.MaxFieldLength.LIMITED);
    SerialMergeScheduler sms = new SerialMergeScheduler();
    this.writer.setMergeScheduler(sms);
    this.writer.setMergePolicy(new BalancedSegmentMergePolicy(this.writer));
    this.searcherManager = new SearcherManager(this.writer);
    this.lastTimeUsed.set(System.currentTimeMillis());
    OpenIndexManager.add(this);
  }

  /**
   * Closes the writer on this index.
   *
   * @throws IndexException Wrapping an {@link CorruptIndexException} or an {@link IOException}.
   */
  @Override
  public void stop() throws IndexException {
    try {
      this.searcherManager.close();
      this.searcherManager = null;
      this.writer.close();
      this.writer = null;
      OpenIndexManager.remove(this);
    } catch (final CorruptIndexException ex) {
      throw new IndexException("Failed to close Index because it is corrupted", ex);
    } catch (final IOException ex) {
      throw new IndexException("Failed to close Index because of an I/O error", ex);
    }
  }
}

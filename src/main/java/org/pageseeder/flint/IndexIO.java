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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.pageseeder.flint.api.Index;
import org.pageseeder.flint.content.DeleteRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a set of utility methods to deal with IO operations on an Index.
 *
 * <p>This class is useful to centralise all operations on an index because it will
 * create one writer and share it with other classes if needed.
 *
 * <p>This is a lower level API.
 *
 * @author Jean-Baptiste Reure
 * @author Christophe Lauret
 *
 * @version 27 February 2013
 */
public final class IndexIO {

  /**
   * private logger
   */
  private final static Logger LOGGER = LoggerFactory.getLogger(IndexIO.class);

  public final static String LAST_COMMIT_DATE = "lastCommitDate";
  /**
   * Describes the state of an index.
   */
  private static enum State {

    /** The index is in a clean state, ready to use. */
    CLEAN,

    /** The index needs to be opened again. */
    NEEDS_REOPEN,

    /** The index has been modified but changes have not been committed. */
    NEEDS_COMMIT,

    /**
     * The index has been modified and changes have been committed, but it is
     * not in an optimal state.
     */
    NEEDS_OPTIMISE

  };

  /**
   * State of this index.
   */
  private volatile IndexIO.State state = State.CLEAN;

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
  private final IndexWriter _writer;

  /**
   * A search manager using this writer.
   */
  private final SearcherManager _searcher;

  /**
   * Sole constructor.
   *
   * @param idx The index on which IO operations will occur.
   * @throws IndexException if opening the index failed
   */
  public IndexIO(Index idx) throws IndexException {
    this._index = idx;
    try {
      // find directory
      if (isReadOnly(this._index)) {
        this._writer = null;
        this._searcher = new SearcherManager(DirectoryReader.open(idx.getIndexDirectory()));
      } else {
        Directory dir = idx.getIndexDirectory();
        IndexWriterConfig config = new IndexWriterConfig(this._index.getAnalyzer());
        config.setMergeScheduler(new SerialMergeScheduler());
        this._writer = new IndexWriter(dir, config);
        this._searcher = new SearcherManager(this._writer);
      }
    } catch (IOException ex) {
      throw new IndexException("Failed to create writer on index "+idx.getIndexID(), ex);
    }
    this.lastTimeUsed.set(System.currentTimeMillis());
  }

  /**
   * Returns the ID of the index used for this class.
   *
   * @return the ID of the index used for this class.
   */
  public final String indexID() {
    return this._index.getIndexID();
  }

  public long getLastTimeUsed() {
    return lastTimeUsed.get();
  }

  /**
   * Closes the writer on this index.
   *
   * @throws IndexException Wrapping an {@link CorruptIndexException} or an {@link IOException}.
   */
  public void stop() throws IndexException {
    if (this._writer == null) return;
    try {
      this._writer.close();
      this._searcher.close();
//      OpenIndexManager.remove(this);
    } catch (final CorruptIndexException ex) {
      throw new IndexException("Failed to close Index because it is corrupted", ex);
    } catch (final IOException ex) {
      throw new IndexException("Failed to close Index because of an I/O error", ex);
    }
  }

  /**
   * Commit any changes if the state of the index requires it.
   *
   * @throws IndexException should any error be thrown by Lucene while committing.
   */
  protected void maybeReopen() throws IndexException {
    if (this._writer == null || this.state != State.NEEDS_REOPEN) return;
    try {
      LOGGER.debug("Reopen searcher");
      this._searcher.maybeReopen();
      this.state = State.NEEDS_COMMIT;
      // FIXME exceptions are completely ignored here !!!
    } catch (final InterruptedException ex) {
      LOGGER.error("Failed to reopen the Index Searcher because the thread has been interrupted", ex);
    } catch (final IOException ex) {
      LOGGER.error("Failed to reopen Index Searcher because of an I/O error", ex);
    }
  }

  /**
   * Commit any changes if the state of the index requires it.
   *
   * @throws IndexException should any error be thrown by Lucene while committing.
   */
  public void maybeCommit() throws IndexException {
    if (this.state != State.NEEDS_COMMIT ||
        this._writer == null ||
        !this._writer.hasUncommittedChanges()) return;
    try {
      LOGGER.debug("Committing index changes");
      Map<String, String> commitUserData = new HashMap<String, String>();
      commitUserData.put(LAST_COMMIT_DATE, String.valueOf(System.currentTimeMillis()));
      this._writer.setCommitData(commitUserData);
      this._writer.commit();
      this.lastTimeUsed.set(System.currentTimeMillis());
      this.state = State.NEEDS_OPTIMISE;
    } catch (final CorruptIndexException ex) {
      throw new IndexException("Failed to commit Index because it is corrupted", ex);
    } catch (final IOException ex) {
      throw new IndexException("Failed to commit Index because of an I/O error", ex);
    }
  }
  /**
   * Clears the index as soon as possible (asynchronously).
   *
   * @return <code>true</code> if the indexed could be scheduled for clearing;
   *         <code>false</code> otherwise.
   * @throws IndexException should any error be thrown by Lucene.
   * @throws UnsupportedOperationException If this implementation is read only.
   */
  public boolean clearIndex() throws IndexException {
    if (this._writer == null) return true;
    // TODO
    try {
      this._writer.deleteAll();
      this.lastTimeUsed.set(System.currentTimeMillis());
      this.state = State.NEEDS_REOPEN;
    } catch (IOException ex) {
      throw new IndexException("Failed to clear Index", ex);
    }
    return true;
  }

  /**
   * Delete the documents defined in the delete rule as soon as possible
   * (asynchronously).
   *
   * @param rule the rule to identify the items to delete
   * @return <code>true</code> if the item could be be scheduled for deletion;
   *         <code>false</code>
   * @throws IndexException should any error be thrown by Lucene.
   * @throws UnsupportedOperationException If this implementation is read only.
   */
  public boolean deleteDocuments(DeleteRule rule) throws IndexException, UnsupportedOperationException {
    if (this._writer == null) return true;
    try {
      if (rule.useTerm()) {
        this._writer.deleteDocuments(rule.toTerm());
      } else {
        this._writer.deleteDocuments(rule.toQuery());
      }
      this.lastTimeUsed.set(System.currentTimeMillis());
      this.state = State.NEEDS_REOPEN;
    } catch (IOException ex) {
      throw new IndexException("Failed to clear Index", ex);
    }
    return true;
  }

  /**
   * Update the documents defined in the delete rule as soon as possible
   * (asynchronously).
   *
   * <p>
   * It is not possible to update an item in Lucene, instead it is first deleted
   * then inserted again.
   *
   * @param rule the rule to identify the items to delete before update.
   * @param documents the list of documents to replace with.
   * @return <code>true</code> if the item could be be scheduled for update;
   *         <code>false</code>
   * @throws IndexException should any error be thrown by Lucene
   * @throws UnsupportedOperationException If this implementation is read only.
   */
  public boolean updateDocuments(DeleteRule rule, List<Document> documents) throws IndexException, UnsupportedOperationException {
    if (this._writer == null) return true;
    try {
      if (rule != null) {
        if (rule.useTerm()) {
          this._writer.deleteDocuments(rule.toTerm());
        } else {
          this._writer.deleteDocuments(rule.toQuery());
        }
      }
      for (final Document doc : documents) {
        this._writer.addDocument(doc);
      }
      this.lastTimeUsed.set(System.currentTimeMillis());
      this.state = State.NEEDS_REOPEN;
    } catch (final IOException e) {
      throw new IndexException("Failed to update document in Index because of an I/O error", e);
    }
    return true;
  }

  public IndexSearcher bookSearcher() {
    return this._searcher.get();
  }

  public void releaseSearcher(IndexSearcher searcher) throws IOException {
    this._searcher.release(searcher);
  }

  public IndexReader bookReader() {
    return this._searcher.getReader();
  }

  public void releaseReader(IndexReader reader) throws IOException {
    this._searcher.releaseReader(reader);
  }

  // static helpers
  // ----------------------------------------------------------------------------------------------

  /**
   * Generate the appropriate IndexIO implementation to use based on the underlying {@link Directory}
   * used.
   *
   * <p>This method will try to return a read/write IndexIO instance if possible;
   * otherwise it will return a read only instance.
   *
   * @param index The index object.
   * @return The most appropriate IndexIO implementation to use.
   *
   * @throws IOException If thrown by the constructors
   */
  private static boolean isReadOnly(Index index) {
    Directory directory = index.getIndexDirectory();
    // not using file system? read only
    if (!(directory instanceof FSDirectory)) return true;
    // Detect if we can write on the files.
    try {
      File f = ((FSDirectory) directory).getDirectory().toFile();
      // ensure all files can write.
      if (!f.canWrite()) return true;
      for (File tf : f.listFiles()) {
        if (!tf.canWrite()) return true;
      }
    } catch (Exception ex) {
      // any error means readonly
      LOGGER.error("Index {} is readonly: {}", index.getIndexID(), ex);
      return true;
    }
    // not read only then
    return false;
  }
}

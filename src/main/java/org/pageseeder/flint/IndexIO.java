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
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.ConcurrentMergeScheduler;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.ReaderManager;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
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
    DIRTY,

    /** The index is closed. */
    CLOSED

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
   * The underlying index writer used by Flint for this index (there should only be one).
   */
  private ReaderManager _reader;

  /**
   * A search manager using this writer.
   */
  private final SearcherManager _searcher;

  // simple searcherfactory for now
  private final static SearcherFactory FACTORY = new SearcherFactory();
  /**
   * Sole constructor.
   *
   * @param idx The index on which IO operations will occur.
   * @throws IndexException if opening the index failed
   */
  public IndexIO(Index idx) throws IndexException {
    this._index = idx;
    try {
      // create it?
      boolean createIt = !DirectoryReader.indexExists(idx.getIndexDirectory());
      // read only?
      boolean readonly = isReadOnly(this._index);
      // find directory
      if (readonly && createIt)
        throw new IndexException("Cannot create index location for index "+idx.getIndexID(), new InvalidParameterException());
      if (readonly) {
        this._writer = null;
        this._reader = new ReaderManager(this._index.getIndexDirectory());
        this._searcher = new SearcherManager(this._index.getIndexDirectory(), FACTORY);
      } else {
        // create writer
        Directory dir = idx.getIndexDirectory();
        IndexWriterConfig config = new IndexWriterConfig(this._index.getAnalyzer());
        ConcurrentMergeScheduler merger = new ConcurrentMergeScheduler();
//        merger.setMaxMergesAndThreads(maxMergeCount, maxThreadCount);
        config.setMergeScheduler(merger);
        if (createIt) config.setOpenMode(OpenMode.CREATE);
        this._writer = new IndexWriter(dir, config);
        if (createIt) this._writer.commit();
        boolean applyAllDeletes = true;
        // create searcher
        this._searcher = new SearcherManager(this._writer, applyAllDeletes, FACTORY);
        // create reader
        this._reader = new ReaderManager(this._writer, applyAllDeletes);
      }
    } catch (IOException ex) {
      throw new IndexException("Failed to create writer on index "+idx.getIndexID(), ex);
    }
    this.lastTimeUsed.set(System.currentTimeMillis());
    // add it to list of opened indexes
    OpenIndexManager.remove(this);
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
  public synchronized void stop() throws IndexException {
    if (this._writer == null) return;
    LOGGER.debug("Stopping IO");
    // try to commit if needed
    maybeCommit();
    try {
      this._writer.close();
      this._searcher.close();
      this._reader.close();
      this._index.getIndexDirectory().close();
      this.state = State.CLOSED;
      OpenIndexManager.remove(this);
    } catch (final CorruptIndexException ex) {
      throw new IndexException("Failed to close Index because it is corrupted", ex);
    } catch (final IOException ex) {
      throw new IndexException("Failed to close Index because of an I/O error", ex);
    }
  }

  /**
   * @return <code>true</code> if closed.
   */
  public boolean isClosed() {
    return this.state == State.CLOSED;
  }

  /**
   * Commit any changes if the state of the index requires it.
   */
  protected synchronized void maybeReopen() {
    if (this._writer == null || this.state != State.DIRTY) return;
    try {
      LOGGER.debug("Reopen reader and searcher");
      this._reader.maybeRefresh();
      this._searcher.maybeRefresh();
      this.state = State.CLEAN;
    } catch (final IOException ex) {
      LOGGER.error("Failed to reopen Index Searcher because of an I/O error", ex);
    }
  }

  /**
   * Commit any changes if the state of the index requires it.
   *
   * @throws IndexException should any error be thrown by Lucene while committing.
   */
  public synchronized void maybeCommit() throws IndexException {
    if (this._writer == null || !this._writer.hasUncommittedChanges() || this.state != State.CLEAN)
      return;
    try {
      LOGGER.debug("Committing index changes");
      long now = System.currentTimeMillis();
      Map<String, String> commitUserData = new HashMap<String, String>();
      commitUserData.put(LAST_COMMIT_DATE, String.valueOf(now));
      this._writer.setCommitData(commitUserData);
      this._writer.commit();
      this.lastTimeUsed.set(now);
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
  public synchronized boolean clearIndex() throws IndexException {
    if (this._writer == null) return true;
    if (this.state == State.CLOSED) return false;
    try {
      this._writer.deleteAll();
      this.lastTimeUsed.set(System.currentTimeMillis());
      this.state = State.DIRTY;
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
  public synchronized boolean deleteDocuments(DeleteRule rule) throws IndexException, UnsupportedOperationException {
    if (this._writer == null) return true;
    if (this.state == State.CLOSED) return false;
    try {
      if (rule.useTerm()) {
        this._writer.deleteDocuments(rule.toTerm());
      } else {
        this._writer.deleteDocuments(rule.toQuery());
      }
      this.lastTimeUsed.set(System.currentTimeMillis());
      this.state = State.DIRTY;
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
  public synchronized boolean updateDocuments(DeleteRule rule, List<Document> documents) throws IndexException, UnsupportedOperationException {
    if (this._writer == null) return true;
    if (this.state == State.CLOSED) return false;
    try {
      // delete?
      if (rule != null) {
        if (rule.useTerm()) {
          // use update
          this._writer.updateDocuments(rule.toTerm(), documents);
        } else {
          // delete then add
          this._writer.deleteDocuments(rule.toQuery());
          this._writer.addDocuments(documents);
        }
      } else {
        // add
        this._writer.addDocuments(documents);
      }
      this.lastTimeUsed.set(System.currentTimeMillis());
      this.state = State.DIRTY;
    } catch (final IOException e) {
      throw new IndexException("Failed to update document in Index because of an I/O error", e);
    }
    return true;
  }

  public IndexSearcher bookSearcher() {
    if (this.state == State.CLOSED) return null;
    try {
      return this._searcher.acquire();
    } catch (IOException ex) {
      LOGGER.error("Failed to book searcher", ex);
      return null;
    }
  }

  public void releaseSearcher(IndexSearcher searcher) {
    if (this.state == State.CLOSED) return;
    try {
      this._searcher.release(searcher);
    } catch (IOException ex) {
      LOGGER.error("Failed to release searcher", ex);
    }
  }

  public IndexReader bookReader() {
    if (this.state == State.CLOSED) return null;
    try {
      return this._reader.acquire();
    } catch (IOException ex) {
      LOGGER.error("Failed to book reader", ex);
      return null;
    }
  }

  public void releaseReader(IndexReader reader) {
    if (this.state == State.CLOSED) return;
    if (!(reader instanceof DirectoryReader))
      throw new IllegalArgumentException("Reader must be a DirectoryReader");
    try {
      this._reader.release((DirectoryReader) reader);
    } catch (IOException ex) {
      LOGGER.error("Failed to release reader", ex);
    }
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

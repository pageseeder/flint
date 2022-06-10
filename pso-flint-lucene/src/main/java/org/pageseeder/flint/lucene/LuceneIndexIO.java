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
package org.pageseeder.flint.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexIO;
import org.pageseeder.flint.IndexOpenException;
import org.pageseeder.flint.OpenIndexManager;
import org.pageseeder.flint.content.DeleteRule;
import org.pageseeder.flint.indexing.FlintDocument;
import org.pageseeder.flint.indexing.IndexJob;
import org.pageseeder.flint.indexing.IndexListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

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
public final class LuceneIndexIO implements IndexIO {

  /**
   * private logger
   */
  private final static Logger LOGGER = LoggerFactory.getLogger(LuceneIndexIO.class);

  public final static String LAST_COMMIT_DATE = "lastCommitDate";

  /**
   * Describes the state of an index.
   */
  private enum State {

    /** The index is in a clean state, ready to use. */
    CLEAN,

    /** The index needs to be opened again. */
    DIRTY,

    /** The index is closing. */
    CLOSING,

    /** The index is closed. */
    CLOSED

  };

  /**
   * State of this index.
   */
  private volatile LuceneIndexIO.State state = State.CLEAN;

  /**
   * The last time this reader was used
   */
  private final AtomicLong lastTimeUsed = new AtomicLong(0);

  /**
   * The underlying index writer used by Flint for this index (there should only be one).
   */
  private IndexWriter _writer;

  /**
   * The underlying index writer used by Flint for this index (there should only be one).
   */
  private ReaderManager _reader;

  /**
   * The index directory
   */
  private final Directory _directory;

  /**
   * The analyzer used for the writer
   */
  private final Analyzer _analyzer;

  /**
   * A search manager using this writer.
   */
  private SearcherManager _searcher;

  private Integer writing = 0;
  private Integer committing = 0;

  private final Object lock = new Object();

  // simple searcherfactory for now
  private final static SearcherFactory FACTORY = new SearcherFactory();

  /**
   * Sole constructor.
   *
   * @param dir       The index's directory
   * @param analyzer  The analyzer
   *
   * @throws IndexException if opening the index failed
   */
  public LuceneIndexIO(Directory dir, Analyzer analyzer) throws IndexException {
    this._analyzer = analyzer;
    this._directory = dir;
    open();
    // get last commit data as last time used
    try {
      List<IndexCommit> commits = DirectoryReader.listCommits(dir);
      if (!commits.isEmpty()) {
        String lastCommitDate = commits.get(commits.size()-1).getUserData().get(LuceneIndexIO.LAST_COMMIT_DATE);
        if (lastCommitDate != null) {
          this.lastTimeUsed.set(Long.parseLong(lastCommitDate));
        }
      }
    } catch (IOException ex) {
      LOGGER.error("Failed to load last index commit date", ex);
    }
  }

  public long getLastTimeUsed() {
    return this.lastTimeUsed.get();
  }

  /**
   * @return <code>true</code> if closed.
   */
  public boolean isClosed() {
    return isState(State.CLOSED);
  }

  /**
   * Closes the writer on this index.
   *
   * @throws IndexException Wrapping an {@link CorruptIndexException} or an {@link IOException}.
   */
  public synchronized void stop() throws IndexException {
    if (this._writer == null || isClosed() || isState(State.CLOSING)) return;
    LOGGER.debug("Stopping IO");
    // try to commit if needed
    maybeCommit();
    if (this._writer == null || isClosed() || isState(State.CLOSING)) return;
    startClosing();
    try {
      this._writer.close();
      this._searcher.close();
      this._reader.close();
      state(State.CLOSED);
      OpenIndexManager.remove(this);
    } catch (final CorruptIndexException ex) {
      throw new IndexException("Failed to close Index because it is corrupted", ex);
    } catch (final IOException ex) {
      throw new IndexException("Failed to close Index because of an I/O error", ex);
    }
  }

  /**
   * Commit any changes if the state of the index requires it.
   */
  public synchronized void maybeRefresh() {
    if (this._writer == null || !this._writer.isOpen() || !isState(State.DIRTY)) return;
    try {
      LOGGER.debug("Reopen reader and searcher");
      this._reader.maybeRefresh();
      this._searcher.maybeRefresh();
      state(State.CLEAN);
    } catch (Throwable ex) {
      LOGGER.error("Failed to reopen Index Searcher because of an I/O error", ex);
    }
  }

  /**
   * Commit any changes if the state of the index requires it.
   */
  public synchronized void maybeCommit() {
    if (this._writer == null|| isState(State.CLOSING) || isClosed() ||
        this.committing > 0 || (!this._writer.hasDeletions() &&
        !this._writer.hasUncommittedChanges() &&
        !this._writer.hasPendingMerges()))
      return;
    // force refresh
    state(State.DIRTY);
    maybeRefresh();
    // closed?
    if (this._writer == null || !this._writer.isOpen()|| this.committing > 0 || isState(State.CLOSING) || isClosed()) return;
    startCommitting();
    try {
      LOGGER.debug("Committing index changes");
      long now = System.currentTimeMillis();
      Map<String, String> commitUserData = new HashMap<String, String>();
      commitUserData.put(LAST_COMMIT_DATE, String.valueOf(now));
      this._writer.setLiveCommitData(commitUserData.entrySet());
      this._writer.commit();
      this.lastTimeUsed.set(now);
    } catch (final CorruptIndexException ex) {
      LOGGER.error("Failed to commit Index because it is corrupted", ex);
    } catch (final IOException ex) {
      LOGGER.error("Failed to commit Index because of an I/O error", ex);
    } finally {
      endCommitting();
    }
  }

  /**
   * Clears the index as soon as possible (asynchronously).
   *
   * @return <code>true</code> if the indexed could be scheduled for clearing;
   *         <code>false</code> otherwise.
   * @throws IndexException should any error be thrown by Lucene.
   */
  public synchronized boolean clearIndex() throws IndexException {
    if (this._writer == null|| isState(State.CLOSING)) return false;
    try {
      if (isClosed()) open();
      startWriting();
      this._writer.deleteAll();
      this.lastTimeUsed.set(System.currentTimeMillis());
      state(State.DIRTY);
    } catch (Exception ex) {
      // try to delete all files then if possible
      if (this._directory != null) try {
        for (String n : this._directory.listAll()) {
          try {
            this._directory.deleteFile(n);
          } catch (AccessDeniedException ex2) {
            // file must be used by lucene, try other files
          }
        }
      } catch (IOException ex2) {
        throw new IndexException("Failed to clear Index", ex);
      }
    } finally {
      this.endWriting();
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
   */
  public synchronized boolean deleteDocuments(DeleteRule rule) throws IndexException {
    if (this._writer == null|| isState(State.CLOSING)) return false;
    if (!(rule instanceof LuceneDeleteRule)) return false;
    LuceneDeleteRule drule = (LuceneDeleteRule) rule;
    try {
      if (isClosed()) open();
      startWriting();
      if (drule.useTerm()) {
        this._writer.deleteDocuments(drule.toTerm());
      } else {
        this._writer.deleteDocuments(drule.toQuery());
      }
      this.lastTimeUsed.set(System.currentTimeMillis());
      state(State.DIRTY);
    } catch (IOException ex) {
      throw new IndexException("Failed to clear Index", ex);
    } finally {
      endWriting();
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
   */
  public synchronized boolean updateDocuments(DeleteRule rule, List<FlintDocument> documents,
                                              IndexListener listener, IndexJob job) throws IndexException {
    if (this._writer == null || isState(State.CLOSING)) return false;
    LuceneDeleteRule drule;
    if (rule == null) drule = null;
    else {
      if (!(rule instanceof LuceneDeleteRule)) return false;
      drule = (LuceneDeleteRule) rule;
    }
    try {
      if (isClosed()) open();
      startWriting();
      FlintDocumentConverter converter = new FlintDocumentConverter();
      List<Document> docs = converter.convert(documents);
      if (converter.hasWarnings()) {
        for (String fieldname : converter.fieldsWithWarnings()) {
          listener.warn(job, "Warning for field '"+fieldname+"': "+converter.getWarning(fieldname));
        }
      }
      // delete?
      if (rule != null) {
        if (drule.useTerm()) {
          // use update
          this._writer.updateDocuments(drule.toTerm(), docs);
        } else {
          // delete then add
          this._writer.deleteDocuments(drule.toQuery());
          this._writer.addDocuments(docs);
        }
      } else {
        // add
        this._writer.addDocuments(docs);
      }
      this.lastTimeUsed.set(System.currentTimeMillis());
      state(State.DIRTY);
    } catch (final IOException e) {
      throw new IndexException("Failed to update document in Index because of an I/O error", e);
    } finally {
      endWriting();
    }
    return true;
  }

  /**
   * Updates documents' DocValues fields to the given values.
   * Each field update is applied to the set of documents that are associated with the Term to the same value.
   * All updates are atomically applied and flushed together.
   *
   * @param term       the term defining the document(s) to update
   * @param newFields  the new fields
   *
   * @return <code>true</code> if the update was done successfully
   *
   * @throws IndexException if updating the doc values failed
   */
  public synchronized boolean updateDocValues(Term term, Field... newFields) throws IndexException {
    // check state
    if (this._writer == null || isState(State.CLOSING)) return false;
    try {
      if (isClosed()) open();
      startWriting();
      this._writer.updateDocValues(term, newFields);
      // set state
      this.lastTimeUsed.set(System.currentTimeMillis());
      state(State.DIRTY);
    } catch (IOException ex) {
      throw new IndexException("Failed to update docvalues in Index because of an I/O error", ex);
    } finally {
      endWriting();
    }
    return true;
  }

  public synchronized IndexSearcher bookSearcher() {
    while (this.isState(State.CLOSING)) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException ex) {
        LOGGER.error("Interrupted while waiting for closing to finish", ex);
      }
    }
    try {
      if (isClosed()) open();
      return this._searcher.acquire();
    } catch (IndexException | IOException ex) {
      LOGGER.error("Failed to book searcher", ex);
      return null;
    }
  }

  public void releaseSearcher(IndexSearcher searcher) {
    if (isClosed()|| isState(State.CLOSING)) return;
    try {
      this._searcher.release(searcher);
    } catch (IOException ex) {
      LOGGER.error("Failed to release searcher", ex);
    }
  }

  public synchronized IndexReader bookReader() {
    while (this.isState(State.CLOSING)) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException ex) {
        LOGGER.error("Interrupted while waiting for closing to finish", ex);
      }
    }
    try {
      if (isClosed()) open();
      return this._reader.acquire();
    } catch (IndexException | IOException ex) {
      LOGGER.error("Failed to book reader", ex);
      return null;
    }
  }

  public void releaseReader(IndexReader reader) {
    if (isClosed() || isState(State.CLOSING)) return;
    if (!(reader instanceof DirectoryReader))
      throw new IllegalArgumentException("Reader must be a DirectoryReader");
    try {
      this._reader.release((DirectoryReader) reader);
    } catch (IOException ex) {
      LOGGER.error("Failed to release reader", ex);
    }
  }

  // private helpers
  // ----------------------------------------------------------------------------------------------

  private void state(State s) {
    synchronized (this.lock) { this.state = s; }
  }
  private boolean isState(State s) {
    synchronized (this.lock) { return this.state == s; }
  }
  private void startClosing() {
    state(State.CLOSING);
    while (this.writing > 0 || this.committing > 0) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException ex) {
        LOGGER.error("Interrupted while waiting for writing to finish", ex);
      }
    }
  }

  private void startCommitting() {
    while (this.writing > 0) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException ex) {
        LOGGER.error("Interrupted while waiting for writing to finish", ex);
      }
    }
    synchronized(this.lock) { this.committing++; }
  }

  private void endCommitting() {
    synchronized(this.lock) { this.committing--; }
  }

  private void startWriting() {
    while (this.committing > 0) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException ex) {
        LOGGER.error("Interrupted while waiting for commit to finish", ex);
      }
    }
    synchronized(this.lock) { this.writing++; }
  }

  private void endWriting() {
    synchronized(this.lock) { this.writing--; }
  }

  private void open() throws IndexException {
    open(true);
  }
  private void open(boolean firsttime) throws IndexException {
    try {
    // create it?
    boolean createIt = !DirectoryReader.indexExists(this._directory);
    // read only?
    boolean readonly = isReadOnly(this._directory);
    if (readonly) {
      this._writer = null;
      this._reader = new ReaderManager(this._directory);
      this._searcher = new SearcherManager(this._directory, FACTORY);
    } else {
      // create writer
      IndexWriterConfig config = new IndexWriterConfig(this._analyzer);
      ConcurrentMergeScheduler merger = new ConcurrentMergeScheduler();
//      merger.setMaxMergesAndThreads(maxMergeCount, maxThreadCount);
      config.setMergeScheduler(merger);
      if (createIt) config.setOpenMode(OpenMode.CREATE);
      this._writer = new IndexWriter(this._directory, config);
      if (createIt) this._writer.commit();
      boolean applyAllDeletes = true;
      boolean writeAllDeletes = false;
      // create searcher
      this._searcher = new SearcherManager(this._writer, applyAllDeletes, writeAllDeletes, FACTORY);
      // create reader
      this._reader = new ReaderManager(this._writer, applyAllDeletes, writeAllDeletes);
    }
    // add it to list of opened indexes
    OpenIndexManager.add(this);
    // set state to clean
    state(State.CLEAN);

    } catch (IndexFormatTooOldException ex) {
      if (firsttime) {
        // try to delete all files and retry
        if (this._directory != null) try {
          for (String n : this._directory.listAll()) {
            this._directory.deleteFile(n);
          }
        } catch (IOException ex2) {
          throw new IndexException("Failed to delete index files from old index", ex);
        }
        // retry
        open(false);
      } else {
        throw new IndexOpenException("Failed to create index: format is too old!", ex);
      }
    } catch (LockObtainFailedException ex) {
      throw new IndexOpenException("Failed to create index: there's already a writer on this index", ex);
    } catch (IOException ex) {
      throw new IndexException("Failed to create writer on index", ex);
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
   * @param directory The index location.
   *
   * @return The most appropriate IndexIO implementation to use.
   */
  private static boolean isReadOnly(Directory directory) {
    // not using file system? not read only
    if (!(directory instanceof FSDirectory)) return false;
    // Detect if we can write on the files.
    try {
      File f = ((FSDirectory) directory).getDirectory().toFile();
      // ensure all files can write.
      if (!f.canWrite()) return true;
      File[] files = f.listFiles();
      if (files == null) return true;
      for (File tf : files) {
        if (!tf.canWrite()) return true;
      }
    } catch (Exception ex) {
      // any error means readonly
      LOGGER.error("Index is readonly", ex);
      return true;
    }
    // not read only then
    return false;
  }
}

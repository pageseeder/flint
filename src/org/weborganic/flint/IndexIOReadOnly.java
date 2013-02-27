/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.flint.api.Index;
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
final class IndexIOReadOnly extends IndexIO {

  /**
   * Logger.
   */
  static final Logger LOGGER = LoggerFactory.getLogger(IndexIOReadWrite.class);

  /**
   * Error to throw if an operation requiring write permissions is needed.
   */
  private static final UnsupportedOperationException UNSUPPORTED =
    new UnsupportedOperationException("Index I/O is ReadOnly");

  /**
   * The underlying index reader used by Flint for this index (there should only be one).
   */
  private final IndexReader _reader;

  /**
   * A search manager using this writer.
   */
  private final SearcherManager searcherManager;

  /**
   * Sole constructor.
   *
   * @param index The index on which IO operations will occur.
   *
   * @throws CorruptIndexException If thrown by Lucene when creating the index writer.
   * @throws IOException           If thrown by Lucene when creating the index writer.
   */
  public IndexIOReadOnly(Index index) throws CorruptIndexException, IOException {
    super(index);
    this._reader = IndexReader.open(index.getIndexDirectory(), true);
    this.searcherManager = new SearcherManager(this._reader);
  }

  /**
   * Does nothing.
   */
  @Override
  protected void maybeReopen() throws IndexException {
  }

  /**
   * Does nothing.
   */
  @Override
  public void maybeCommit() {
  }

  /**
   * Does nothing.
   */
  @Override
  public void maybeOptimise() {
  }

  /**
   * Does nothing.
   */
  @Override
  public void stop() {
  }

  /**
   * This operation is not supported in read only mode.
   *
   * @return nothing
   * @throws UnsupportedOperationException Always.
   */
  @Override
  public boolean clearIndex() throws UnsupportedOperationException {
    throw UNSUPPORTED;
  }

  /**
   * This operation is not supported in read only mode.
   *
   * @param rule Ignored
   * @return nothing
   * @throws UnsupportedOperationException Always.
   */
  @Override
  public boolean deleteDocuments(DeleteRule rule) throws UnsupportedOperationException {
    throw UNSUPPORTED;
  }

  /**
   * This operation is not supported in read only mode.
   *
   * @param rule      Ignored
   * @param documents Ignored
   * @return nothing
   *
   * @throws UnsupportedOperationException Always.
   */
  @Override
  public boolean updateDocuments(DeleteRule rule, List<Document> documents) throws UnsupportedOperationException {
    throw UNSUPPORTED;
  }

  /**
   * Returns the index searcher for the index.
   *
   * <p>Note: when a searcher is booked it must be released using {{@link #releaseSearcher(IndexSearcher)}.
   *
   * @return The index searcher.
   *
   * @throws IOException If reported by the {@link SearcherManager#get()}.
   */
  @Override
  public IndexSearcher bookSearcher() throws IOException {
    return this.searcherManager.get();
  }

  @Override
  public void releaseSearcher(IndexSearcher searcher) throws IOException {
    this.searcherManager.release(searcher);
  }

  @Override
  protected IndexReader bookReader() throws IOException {
    return this.searcherManager.getReader();
  }

  @Override
  protected void releaseReader(IndexReader reader) throws IOException {
    this.searcherManager.releaseReader(reader);
  }

  @Override
  protected int countBookedReaders() {
    return this.searcherManager.getRefCount();
  }

}

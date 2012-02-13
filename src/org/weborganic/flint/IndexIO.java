/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.weborganic.flint.content.DeleteRule;

/**
 * Provides a set of utility methods to deal with IO operations on an Index.
 *
 * <p>
 * This class is useful to centralise all operations on an index because it will
 * create one writer and share it with other classes if needed.
 *
 * <p>
 * This is a lower level API.
 *
 * @author Jean-Baptiste Reure
 * @version 26 February 2010
 */
public abstract class IndexIO {

  /**
   * Describes the state of an index.
   */
  static enum State {

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
  IndexIO.State state = State.CLEAN;

  /**
   * ID of the current index.
   */
  private final String indexID;

  /**
   * Sole constructor.
   *
   * @param index
   *          The index on which IO operations will occur.
   */
  IndexIO(Index index) {
    this.indexID = index.getIndexID();
  }

  /**
   * Returns the ID of the index used for this class.
   *
   * @return the ID of the index used for this class.
   */
  public final String indexID() {
    return this.indexID;
  }

  /**
   * Commit any changes if the state of the index requires it.
   *
   * @throws IndexException
   *           should any error be thrown by Lucene while committing.
   */
  public abstract void maybeCommit() throws IndexException;

  /**
   * Optimise the index if the state of the index requires it.
   *
   * @throws IndexException
   *           should any error be thrown by Lucene while optimising.
   */
  public abstract void maybeOptimise() throws IndexException;

  /**
   * Clears the index as soon as possible (asynchronously).
   *
   * @return <code>true</code> if the indexed could be scheduled for clearing;
   *         <code>false</code>
   * @throws IndexException
   *           should any error be thrown by Lucene.
   * @throws UnsupportedOperationException
   *           If this implementation is read only.
   */
  public abstract boolean clearIndex() throws IndexException, UnsupportedOperationException;

  /**
   * Delete the documents defined in the delete rule as soon as possible
   * (asynchronously).
   *
   * @param rule
   *          the rule to identify the items to delete
   * @return <code>true</code> if the item could be be scheduled for deletion;
   *         <code>false</code>
   * @throws IndexException
   *           should any error be thrown by Lucene.
   * @throws UnsupportedOperationException
   *           If this implementation is read only.
   */
  public abstract boolean deleteDocuments(DeleteRule rule) throws IndexException, UnsupportedOperationException;

  /**
   * Update the documents defined in the delete rule as soon as possible
   * (asynchronously).
   *
   * <p>
   * It is not possible to update an item in Lucene, instead it is first deleted
   * then inserted again.
   *
   * @param rule
   *          the rule to identify the items to delete before update.
   * @param documents
   *          the list of documents to replace with.
   * @return <code>true</code> if the item could be be scheduled for update;
   *         <code>false</code>
   * @throws IndexException
   *           should any error be thrown by Lucene
   * @throws UnsupportedOperationException
   *           If this implementation is read only.
   */
  public abstract boolean updateDocuments(DeleteRule rule, List<Document> documents)
      throws IndexException, UnsupportedOperationException;

  /**
   * Returns the index searcher for the index.
   *
   * <p>
   * Note: when a searcher is booked it must be released using
   * {@link #releaseSearcher(IndexSearcher)}.
   *
   * @return The index searcher.
   *
   * @throws IOException Should any error be thrown by lower level API.
   */
  public abstract IndexSearcher bookSearcher() throws IOException;

  /**
   * Releases the searcher.
   *
   * <p>This method should be called when the searcher is no longer needed by the calling object
   * and can be released for use by other objects.
   *
   * @param searcher The searcher to release.
   *
   * @throws IOException Should any error be thrown by lower level API.
   */
  public abstract void releaseSearcher(IndexSearcher searcher) throws IOException;

  /**
   * Requests a reader for exclusive use.
   *
   * <p>Note that when the reader is no longer needed, it should be released using
   * the {@link #releaseReader(IndexReader)} method.
   *
   * @return The reader.
   *
   * @throws IOException Should any error be thrown by lower level API.
   */
  protected abstract IndexReader bookReader() throws IOException;

  /**
   * Releases the reader.
   *
   * <p>This method should be called when the reader is no longer needed by the calling object
   * and can be released for use by other objects.
   *
   * @param reader the reader to release.
   *
   * @throws IOException Should any error be thrown by lower level API.
   */
  protected abstract void releaseReader(IndexReader reader) throws IOException;

  /**
   * Closes the writer on this index.
   *
   * @throws IndexException
   *           Wrapping an {@link CorruptIndexException} or an
   *           {@link IOException}.
   */
  public abstract void stop() throws IndexException;

  /**
   * Generate the appropriate IndexIO implementation to use.
   *
   * <p>
   * This method will try to return a read/write IndexIO instance if possible;
   * otherwise it will return a read only instance.
   *
   * @param index
   *          The index object.
   * @return The most appropriate IndexIO implementation to use.
   * @throws IOException
   *           If thrown by the constructors
   */
  public static IndexIO newInstance(Index index) throws IOException {
    Directory directory = index.getIndexDirectory();
    boolean readonly = false;
    // Detect if we can write on the files.
    try {
      if (directory instanceof FSDirectory) {
        File f = ((FSDirectory) directory).getDirectory();
        // ensure all files can write.
        if (!f.canWrite()) {
          readonly = true;
        }
        for (File tf : f.listFiles()) {
          if (!tf.canWrite()) {
            readonly = true;
          }
        }
      } else {
        readonly = true;
      }
    } catch (Exception ex) {
      readonly = true;
    }

    // Returns the correct Index IO implementation
    return readonly ? new IndexIOReadOnly(index) : new IndexIOReadWrite(index);
  }
}

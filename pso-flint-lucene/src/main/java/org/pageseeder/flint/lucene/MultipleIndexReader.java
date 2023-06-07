package org.pageseeder.flint.lucene;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.pageseeder.flint.Index;
import org.pageseeder.flint.IndexException;

/**
 * Handle a list of readers.
 *
 * @author Jean-Baptiste Reure
 * @version 20 September 2011
 *
 */
public final class MultipleIndexReader {

  /**
   * List of open readers.
   */
  private final Map<Index, IndexReader> _indexes = new HashMap<>();

  MultipleIndexReader(List<Index> indexes) {
    for (Index index: indexes)
      this._indexes.put(index, null);
  }
  /**
   * Grab a new index reader.
   *
   * @throws IndexException if getting the reader failed
   */
  public IndexReader grab() throws IndexException {
    IndexReader[] readers = new IndexReader[this._indexes.size()];
    // grab a reader for each indexes
    int i = 0;
    for (Index index : this._indexes.keySet()) {
      // grab what we need
      IndexReader reader = LuceneIndexQueries.grabReader(index);
      readers[i++] = reader;
      // store it so we can release it later on
      this._indexes.put(index, reader);
    }
    try {
      return new MultiReader(readers);
    } catch (IOException ex) {
      throw new IndexException("Failed to load multiple index reader", ex);
    }
  }

  /**
   * Release all the open readers we have listed.
   */
  public void releaseSilently() {
    // now release everything we used
    while (!this._indexes.isEmpty())  {
      Index index = this._indexes.keySet().iterator().next();
      IndexReader reader = this._indexes.remove(index);
      LuceneIndexQueries.releaseQuietly(index, reader);
    }
  }
}
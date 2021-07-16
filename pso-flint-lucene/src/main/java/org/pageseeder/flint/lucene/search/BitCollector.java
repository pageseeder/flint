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
package org.pageseeder.flint.lucene.search;

import java.io.IOException;
import java.util.BitSet;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;

/**
 * Collects Lucene search results into a bit set.
 *
 * <p>This class is useful when mixing and matching various criteria by using boolean operations
 * on the bit sets.
 *
 * @author Christophe Lauret
 * @version 2 August 2010
 */
public final class BitCollector extends SimpleCollector {

  /**
   * The document ID for the reader in use.
   */
  private int _docbase;

  /**
   * The bit set created by this collector.
   */
  private final BitSet _bits;

  /**
   * Creates a new collector by setting the size of the bit set.
   *
   * <p>The size of the bit should be the size of the index (number of documents).
   *
   * @param size the size of the {@link BitSet} to create.
   */
  public BitCollector(int size) {
   this._bits = new BitSet(size);
  }

  /**
   * Creates a new collector using the specified bit set.
   *
   * @param bits The bit set to use (must be the size of the index).
   */
  public BitCollector(BitSet bits) {
    this._bits = bits;
  }

  /**
   * Updates the {@link BitSet} to include the collected document.
   *
   * @param doc the position of the Lucene {@link Document} in the index
   */
  @Override
  public void collect(int doc) {
    this._bits.set(doc + this._docbase);
  }

  /**
   * Changes the document base to re-base the document position in the bit set.
   *
   * @param context the
   */
  @Override
  protected void doSetNextReader(LeafReaderContext context) throws IOException {
    // TODO Auto-generated method stub
    super.doSetNextReader(context);
    this._docbase = context.docBase;
  }

  /**
   * Returns the {@link BitSet} after a search.
   *
   * @return the {@link BitSet} after a search.
   */
  public BitSet getBits() {
    return this._bits;
  }

  /**
   * Returns the cardinality of the {@link BitSet} after a search.
   *
   * @return the cardinality of the {@link BitSet} after a search.
   */
  public int getCount() {
    return this._bits.cardinality();
  }

  @Override
  public ScoreMode scoreMode() {
    return ScoreMode.COMPLETE_NO_SCORES;
  }
}

package org.weborganic.flint.search;

import java.util.BitSet;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;

/**
 * Collects Lucene search results into a bit set. 
 * 
 * <p>This class is useful when mixing and matching various criteria by using boolean operations
 * on the bit sets. 
 * 
 * @author Christophe Lauret
 * @version 2 August 2010
 */
public final class BitCollector extends Collector {

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
   * Does nothing - the scorer is irrelevant when populating {@link BitSet}s.
   * @param scorer the scorer.
   */
  public void setScorer(Scorer scorer) {
    // ignore scorer
  }

  /**
   * Accept documents out of order - the order is irrelevant when populating {@link BitSet}s.
   * @return always <code>true</code>. 
   */
  public boolean acceptsDocsOutOfOrder() {
    return true;
  }

  /**
   * Updates the {@link BitSet} to include the collected document.
   * 
   * @param doc the position of the Lucene {@link Document} in the index
   */
  public void collect(int doc) {
    this._bits.set(doc + this._docbase);
  }

  /**
   * Changes the document base to re-base the document position in the bit set.
   * 
   * @param reader  the next index reader
   * @param docbase used to re-base document ids for the index.
   */
  public void setNextReader(IndexReader reader, int docbase) {
    this._docbase = docbase;
  }

  /**
   * Returns the {@link BitSet} generated after a search.
   * 
   * @return the {@link BitSet} generated after a search.
   */
  public BitSet getBits() {
    return this._bits;
  }

  /**
   * Returns the cardinality in the {@link BitSet} generated after a search.
   * 
   * @return the {@link BitSet} generated after a search.
   */
  public int getCount() {
    return this._bits.cardinality();
  }

}

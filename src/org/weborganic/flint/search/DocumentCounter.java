package org.weborganic.flint.search;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;

/**
 * Simply counts the number of documents in search results. 
 * 
 * @author Christophe Lauret
 * @version 2 August 2010
 */
public final class DocumentCounter extends Collector {

  /**
   * The number of documents collected (counted).
   */
  private int count = 0; 

  /**
   * Creates a new document counter.
   */
  public DocumentCounter() {
  }

  /**
   * Does nothing - the scorer is irrelevant when counting documents.
   * @param scorer the scorer. 
   */
  public void setScorer(Scorer scorer) {
    // ignore scorer
  }

  /**
   * Accept documents out of order - the order is irrelevant when counting.
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
    this.count++;
  }

  /**
   * Does nothing - the scorer is irrelevant when counting documents.
   * 
   * @param reader  the next index reader
   * @param docbase used to re-base document ids for the index.
   */
  public void setNextReader(IndexReader reader, int docbase) {
  }

  /**
   * Returns the {@link BitSet} generated after a search.
   * 
   * @return the {@link BitSet} generated after a search.
   */
  public int getCount() {
    return this.count;
  }

}

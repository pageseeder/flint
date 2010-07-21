package org.weborganic.flint.util;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.lucene.index.Term;

/**
 * A list of terms ordered by frequency.
 * 
 * @author Christophe Lauret
 * @version 21 July 2010
 */
public class TermFrequencyList {

  /**
   * The maximum size of the list of terms. 
   */
  private final int _maxSize; 

  /**
   * The list of terms sorted by frequency.
   */
  private final SortedSet<TermFrequency> _terms;

  /**
   * The minimum frequency to be considered in the list.
   */
  private transient int minFrequency = 0;

  /**
   * Creates a new term frequency list.
   */
  public TermFrequencyList(int max) {
    this._maxSize = max;
    this._terms = new TreeSet<TermFrequency>();
  }

  /**
   * 
   * @param term
   * @param frequency
   */
  public void add(Term term, int frequency) {
    if (frequency >= this.minFrequency) {
      this._terms.add(new TermFrequency(term, frequency));
      if (this._terms.size() > this._maxSize) {
        this._terms.remove(this._terms.first());
        this.minFrequency = this._terms.first().frequency();
      }
    }
  }

  /**
   * Returns
   *  
   * @return
   */
  public List<Term> asTermList() {
    return null; 
  }
}

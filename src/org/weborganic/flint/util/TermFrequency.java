package org.weborganic.flint.util;

import org.apache.lucene.index.Term;

/**
 * A basic class storing the frequency of each term.
 * 
 * <p>This class is only provided to allow list for terms and their frequency to be organised
 * easily.
 * 
 * @author Christophe Lauret
 * @version 16 July 2010
 */
public final class TermFrequency implements Comparable<TermFrequency> {

  /**
   * The term.
   */
  private final Term _term;

  /**
   * The term frequency.
   */
  private final int _frequency;

  /**
   * Creates a new term usage - the fields are final.
   * @param term      The term.
   * @param frequency Its frequency.
   */
  public TermFrequency(Term term, int frequency) {
    this._term = term;
    this._frequency = frequency;
  }

  /**
   * @return the wrapped term.
   */
  public Term term() {
    return this._term;
  }

  /**
   * @return the wrapped term frequency.
   */
  public int frequency() {
    return this._frequency;
  }

  /**
   * {@inheritDoc}
   */
  @Override public int hashCode() {
    return this._frequency * 19 + this._term.hashCode() + 7;
  }

  /**
   * {@inheritDoc}
   */
  @Override public boolean equals(Object o) {
    if (o instanceof TermFrequency) {
      return this.equals((TermFrequency)o);
    }
    return this._term.equals(o);
  }

  /**
   * Two {@link TermFrequency} terms are equals only if the term and the frequency are equal.
   * 
   * @param t the term usage to compare with.
   * @return <code>true</code> if equal; <code>false</code> otherwise.
   */
  public boolean equals(TermFrequency t) {
    return this._frequency == t._frequency && this._term.equals(t._term);
  }

  /**
   * Compare the using the frequency.
   * 
   * {@inheritDoc}
   */
  public int compareTo(TermFrequency t) {
    return this._frequency - t._frequency;
  }
}

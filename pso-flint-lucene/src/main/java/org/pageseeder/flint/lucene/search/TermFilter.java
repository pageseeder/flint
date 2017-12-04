package org.pageseeder.flint.lucene.search;

import org.apache.lucene.search.BooleanClause;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A base class for all filters based on terms.
 *
 * @param <T> The types of values stored for the term
 *
 * @author Christophe Lauret
 *
 * @version 5.1.3
 */
public class TermFilter<T> {

  /**
   * The name of the field
   */
  protected final String _name;

  /**
   * The list of terms to filter with
   */
  protected final Map<T, BooleanClause.Occur> _terms = new HashMap<>();

  TermFilter(String name,  Map<T, BooleanClause.Occur> terms) {
    this._name = name;
    this._terms.putAll(terms);
  }

  /**
   * @return The name of the term that the filter applies to.
   */
  public String name() {
    return this._name;
  }

  /**
   * @return The values mapped to the {@link org.apache.lucene.search.BooleanClause.Occur} instance.
   */
  public Map<T, BooleanClause.Occur> terms() {
    return Collections.unmodifiableMap(this._terms);
  }

  static String occurToString(BooleanClause.Occur occur) {
    if (occur == BooleanClause.Occur.MUST)     return "must";
    if (occur == BooleanClause.Occur.MUST_NOT) return "must_not";
    if (occur == BooleanClause.Occur.SHOULD)   return "should";
    return "unknown";
  }

}

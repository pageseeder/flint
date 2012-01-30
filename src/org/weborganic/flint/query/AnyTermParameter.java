/*
 * This file is part of the Flint library.
 * 
 * For licensing information please see the file license.txt included in the release. A copy of this licence can also be
 * found at http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.query;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.weborganic.flint.util.Terms;

import com.topologi.diffx.xml.XMLWriter;

/**
 * A basic search parameter wrapping a simple list of {@link Term}s.
 * 
 * <p>The final query must match at least one of the terms listed.
 * 
 * @author Christophe Lauret
 * @version 13 August 2010
 */
public final class AnyTermParameter implements SearchParameter {

  /**
   * An empty parameter.
   */
  private static final AnyTermParameter EMPTY = new AnyTermParameter();

  /**
   * The wrapped list of Lucene Terms.
   */
  private final List<Term> _terms;

  /**
   * The query (lazy initialised).
   */
  private volatile Query _query;

  /**
   * Creates an empty instance of this class.
   */
  private AnyTermParameter() {
    this._terms = Collections.emptyList();
  }

  /**
   * Creates a new term parameter from the specified field name and text.
   *
   * @param field The name of the field to search.
   * @param text  The text to match in the field value.
   * 
   * @throws NullPointerException If either parameter is <code>null</code>.
   */
  public AnyTermParameter(String field, String text) throws NullPointerException {
    if (field == null) throw new NullPointerException("field");
    if (text == null) throw new NullPointerException("text");
    this._terms = Collections.singletonList(new Term(field, text));
  }

  /**
   * Creates a new term parameter from the specified term.
   *
   * @param terms The term to match.
   * 
   * @throws NullPointerException If either is <code>null</code>.
   */
  public AnyTermParameter(List<Term> terms) throws NullPointerException {
    if (terms == null) throw new NullPointerException("terms");
    this._terms = terms;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isEmpty() {
    return this._terms.isEmpty();
  }

  /**
   * Returns the wrapped term.
   * @return the wrapped term.
   */
  public List<Term> term() {
    return this._terms;
  }

  /**
   * {@inheritDoc}
   */
  public Query toQuery() {
    if (this.isEmpty()) { return null; }
    if (this._query == null) {
      this._query = toQuery(this._terms);
    }
    return this._query;
  }

  /**
   * {@inheritDoc}
   */
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("any-term-parameter", true);
    // indicate whether this search term is empty
    xml.attribute("is-empty", Boolean.toString(this.isEmpty()));
    // not empty, give details
    Terms.toXML(xml, this._terms);
    xml.closeElement();
  }

  /**
   * {@inheritDoc}
   */
  public String toString() {
    return this._terms.toString();
  }

  // Private helpers
  // ----------------------------------------------------------------------------------------------

  /**
   * Returns an instance of this class which method {@link #isEmpty()} always return <code>true</code>.
   * @return an instance of this class that does not need to match any term.
   */
  public static AnyTermParameter empty() {
    return EMPTY;
  }

  // Private helpers
  // ----------------------------------------------------------------------------------------------

  /**
   * Returns the query that would match any one of the terms in the list. 
   * 
   * @param terms the list of terms
   * @return the corresponding query.
   */
  private Query toQuery(List<Term> terms) {
    if (this.isEmpty()) { return null; }
    if (this._terms.size() == 1) {
      return new TermQuery(this._terms.get(0));
    } else {
      BooleanQuery q = new BooleanQuery();
      for (Term t : this._terms) {
        q.add(new TermQuery(t), Occur.SHOULD);
      }
      return q; 
    }
  }

}

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
package org.pageseeder.flint.lucene.query;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.pageseeder.flint.lucene.search.Terms;
import org.pageseeder.xmlwriter.XMLWriter;

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
  @Override
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
  @Override
  public Query toQuery() {
    if (isEmpty()) return null;
    if (this._query == null) {
      this._query = toQuery(this._terms);
    }
    return this._query;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("any-term-parameter", true);
    // indicate whether this search term is empty
    xml.attribute("is-empty", Boolean.toString(isEmpty()));
    // not empty, give details
    Terms.toXML(xml, this._terms);
    xml.closeElement();
  }

  /**
   * {@inheritDoc}
   */
  @Override
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
    if (isEmpty()) return null;
    if (this._terms.size() == 1) return new TermQuery(this._terms.get(0));
    else {
      BooleanQuery q = new BooleanQuery();
      for (Term t : this._terms) {
        q.add(new TermQuery(t), Occur.SHOULD);
      }
      return q;
    }
  }

}

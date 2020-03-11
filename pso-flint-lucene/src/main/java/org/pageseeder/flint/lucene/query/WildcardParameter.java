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

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.WildcardQuery;
import org.pageseeder.xmlwriter.XMLWriter;

import java.io.IOException;

/**
 * A search parameter wrapping a simple {@link Term} wildcard and use to generate a Lucene
 * {@link WildcardQuery}.
 *
 * @author Christophe Lauret
 * @version 30 January 2012
 */
public final class WildcardParameter implements SearchParameter {

  /**
   * The wrapped Lucene Term.
   */
  private final Term _term;

  /**
   * Creates a new wildcard parameter from the specified field name and text.
   *
   * @param field  The name of the field to search.
   * @param value  The value text (with wildcards) to match in the field value.
   *
   * @throws NullPointerException If either parameter is <code>null</code>.
   * @throws IllegalArgumentException If value has no wildcard characters ('?' or '*')
   */
  public WildcardParameter(String field, String value) throws NullPointerException {
    if (field == null) throw new NullPointerException("field");
    if (value == null) throw new NullPointerException("value");
    if (value.indexOf('?') == -1 && value.indexOf('*') == -1)
      throw new IllegalArgumentException("value");
    this._term = new Term(field, value);
  }

  /**
   * Creates a new wildcard parameter from the specified term prefix.
   *
   * @param term The term wildcard to match.
   *
   * @throws NullPointerException If the specified term is <code>null</code>.
   * @throws IllegalArgumentException If term value has no wildcard characters ('?' or '*')
   */
  public WildcardParameter(Term term) throws NullPointerException {
    if (term == null) throw new NullPointerException("term");
    if (term.text().indexOf('?') == -1 && term.text().indexOf('*') == -1)
      throw new IllegalArgumentException("value");
    this._term = term;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEmpty() {
    return this._term.field().isEmpty() || this._term.text().isEmpty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Query toQuery() {
    if (isEmpty()) return null;
    return new WildcardQuery(this._term);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("wildcard-parameter");
    // indicate whether this search term is empty
    xml.attribute("is-empty", Boolean.toString(isEmpty()));
    // not empty, give details
    if (!isEmpty()) {
      xml.element("field", this._term.field());
      xml.element("prefix", this._term.text());
    }
    xml.closeElement();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return this._term.toString();
  }

}

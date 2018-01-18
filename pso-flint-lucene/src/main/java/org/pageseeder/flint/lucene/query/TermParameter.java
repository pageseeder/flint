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

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.pageseeder.xmlwriter.XMLWriter;

/**
 * A basic search parameter wrapping a simple {@link Term}.
 *
 * @author Christophe Lauret
 * @version 13 August 2010
 */
public final class TermParameter implements SearchParameter {

  /**
   * The wrapped Lucene Term.
   */
  private final Term _term;

  /**
   * Creates a new term parameter from the specified field name and text.
   *
   * @param field The name of the field to search.
   * @param text  The text to match in the field value.
   *
   * @throws NullPointerException If either parameter is <code>null</code>.
   */
  public TermParameter(String field, String text) throws NullPointerException {
    if (field == null) throw new NullPointerException("field");
    if (text == null) throw new NullPointerException("text");
    this._term = new Term(field, text);
  }

  /**
   * Creates a new term parameter from the specified term.
   *
   * @param term The term to match.
   *
   * @throws NullPointerException If either is <code>null</code>.
   */
  public TermParameter(Term term) throws NullPointerException {
    if (term == null) throw new NullPointerException("term");
    this._term = term;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEmpty() {
    return this._term.field().isEmpty();
  }

  /**
   * Returns the wrapped term.
   * @return the wrapped term.
   */
  public Term term() {
    return this._term;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Query toQuery() {
    if (isEmpty()) return null;
    return new TermQuery(this._term);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("term-parameter");
    // indicate whether this search term is empty
    xml.attribute("is-empty", Boolean.toString(isEmpty()));
    // not empty, give details
    if (!isEmpty()) {
      xml.element("field", this._term.field());
      xml.element("text", this._term.text());
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

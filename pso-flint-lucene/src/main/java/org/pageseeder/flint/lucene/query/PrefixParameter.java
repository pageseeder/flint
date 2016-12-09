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
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.pageseeder.xmlwriter.XMLWriter;

/**
 * A search parameter wrapping a simple {@link Term} prefix and use to generate a Lucene
 * {@link PrefixQuery}.
 *
 * @author Christophe Lauret
 * @version 30 January 2012
 */
public final class PrefixParameter implements SearchParameter {

  /**
   * The wrapped Lucene Term prefix.
   */
  private final Term _prefix;

  /**
   * Creates a new prefix parameter from the specified field name and text.
   *
   * @param field  The name of the field to search.
   * @param prefix The prefix text to match in the field value.
   *
   * @throws NullPointerException If either parameter is <code>null</code>.
   */
  public PrefixParameter(String field, String prefix) throws NullPointerException {
    if (field == null) throw new NullPointerException("field");
    if (prefix == null) throw new NullPointerException("prefix");
    this._prefix = new Term(field, prefix);
  }

  /**
   * Creates a new prefix parameter from the specified term prefix.
   *
   * @param prefix The term prefix to match.
   *
   * @throws NullPointerException If the specified prefix is <code>null</code>.
   */
  public PrefixParameter(Term prefix) throws NullPointerException {
    if (prefix == null) throw new NullPointerException("prefix");
    this._prefix = prefix;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEmpty() {
    return this._prefix.field().isEmpty() || this._prefix.text().isEmpty();
  }

  /**
   * Returns the wrapped term prefix.
   * @return the wrapped term prefix.
   */
  public Term prefix() {
    return this._prefix;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Query toQuery() {
    if (isEmpty()) return null;
    return new PrefixQuery(this._prefix);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("prefix-parameter");
    // indicate whether this search term is empty
    xml.attribute("is-empty", Boolean.toString(isEmpty()));
    // not empty, give details
    if (!isEmpty()) {
      xml.element("field", this._prefix.field());
      xml.element("prefix", this._prefix.text());
    }
    xml.closeElement();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return this._prefix.toString();
  }

}

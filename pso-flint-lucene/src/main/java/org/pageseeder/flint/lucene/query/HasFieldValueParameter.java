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

import org.apache.lucene.search.FieldValueQuery;
import org.apache.lucene.search.Query;
import org.pageseeder.xmlwriter.XMLWriter;

/**
 * A basic search parameter used to find documents that have a value for a specific field.
 *
 * @author Jean-Baptiste Reure
 * @version 21 June 2017
 */
public final class HasFieldValueParameter implements SearchParameter {

  /**
   * The field name.
   */
  private final String _field;

  /**
   * The query.
   */
  private volatile Query _query;

  /**
   * Creates an empty instance of this class.
   */
  public HasFieldValueParameter(String name) {
    this._field = name;
    this._query = isEmpty() ? null : new FieldValueQuery(this._field);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEmpty() {
    return this._field == null || this._field.isEmpty();
  }

  /**
   * Returns the wrapped term.
   * @return the wrapped term.
   */
  public String field() {
    return this._field;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Query toQuery() {
    return this._query;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("has-field-parameter", true);
    // indicate whether this search term is empty
    xml.attribute("is-empty", Boolean.toString(isEmpty()));
    xml.attribute("field", this._field);
    xml.closeElement();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return this._field;
  }

}

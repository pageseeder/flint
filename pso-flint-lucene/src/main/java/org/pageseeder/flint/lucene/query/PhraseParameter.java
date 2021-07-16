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
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.pageseeder.xmlwriter.XMLWriter;

/**
 * A basic search parameter wrapping a simple {@link PhraseQuery}.
 *
 * @author Jean-Baptiste Reure
 * @version 5.0.0
 */
public final class PhraseParameter implements SearchParameter {

  /**
   * The field.
   */
  private final String _field;

  /**
   * The text.
   */
  private final String _text;

  /**
   * The query.
   */
  private final PhraseQuery _query;

  /**
   * Creates a new phrase parameter from the specified field name and text.
   *
   * @param field The name of the field to search.
   * @param text  The text to match.
   *
   * @throws NullPointerException If either parameter is <code>null</code>.
   */
  public PhraseParameter(String field, String text) throws NullPointerException {
    if (field == null) throw new NullPointerException("field");
    if (text  == null) throw new NullPointerException("text");
    this._field = field;
    this._text  = text;
    if (isEmpty()) {
      this._query = null;
    } else {
      PhraseQuery.Builder pq = new PhraseQuery.Builder();
      for (String word : text.split("\\s")) {
        pq.add(new Term(field, word));
      }
      this._query = pq.build();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEmpty() {
    return this._text == null || this._text.isEmpty();
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
    xml.openElement("phrase-parameter");
    // indicate whether this search term is empty
    xml.attribute("is-empty", Boolean.toString(isEmpty()));
    // not empty, give details
    if (!isEmpty()) {
      xml.element("field", this._field);
      xml.element("text", this._text);
    }
    xml.closeElement();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return this._query.toString(this._field);
  }

}

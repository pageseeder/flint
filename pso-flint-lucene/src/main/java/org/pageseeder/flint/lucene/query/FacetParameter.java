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

import org.apache.lucene.search.Query;
import org.pageseeder.flint.lucene.search.Facet;
import org.pageseeder.xmlwriter.XMLWriter;

/**
 * The selected value for a facet.
 *
 * @author Christophe Lauret
 * @version 15 August 2010
 */
public final class FacetParameter implements SearchParameter {

  /**
   * The facet that is selected.
   */
  private final Facet _facet;

  /**
   * The value of this selected facet.
   */
  private final String _value;

  /**
   * The Query for this selected facet.
   */
  private final Query _query;

  /**
   * Creates a new facet value.
   *
   * @param facet the facet.
   * @param value the selected value.
   */
  public FacetParameter(Facet facet, String value) {
    this._facet = facet;
    this._value = value;
    this._query = this._facet.forValue(value);
  }

  /**
   * Returns the wrapped facet.
   *
   * @return the facet.
   */
  public Facet facet() {
    return this._facet;
  }

  /**
   * Returns the selected value.
   *
   * @return the selected value.
   */
  public String value() {
    return this._value;
  }

  /**
   * Indicates whether there is a query associated with the facet.
   *
   * @return <code>true</code> if empty; <code>false</code> otherwise.
   */
  @Override
  public boolean isEmpty() {
    return this._query == null;
  }

  /**
   * Returns the Query for this selected facet.
   * @return the Query for this selected facet.
   */
  @Override
  public Query toQuery() {
    return this._query;
  }

  /**
   * Returns the XML for this facet parameter.
   * <p>
   * {@inheritDoc}
   */
  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("selected-facet");
    xml.attribute("name", this._facet.name());
    xml.attribute("value", this._value);
    this._facet.toXML(xml);
    xml.closeElement();
  }

}

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
package org.pageseeder.flint.lucene.facet;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.pageseeder.flint.lucene.query.DateParameter;
import org.pageseeder.flint.lucene.util.Beta;
import org.pageseeder.flint.lucene.util.Dates;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A facet implementation using a simple index field.
 *
 * @author Christophe Lauret
 * @version 5.1.3
 */
@Beta
public class DateFieldFacet extends FlexibleFieldFacet {

  private final static Logger LOGGER = LoggerFactory.getLogger(DateFieldFacet.class);

  /**
   * If this facet is a date
   */
  private final Resolution _resolution;

  /**
   * Creates a new facet with the specified name;
   *
   * @param name       The name of the facet.
   * @param resolution If this facet is a date
   * @param maxterms   The maximum number of terms to return
   */
  protected DateFieldFacet(String name, Resolution resolution, int maxterms) {
    super(name, maxterms);
    this._resolution = resolution;
  }

  @Override
  public String getType() {
    return "date-field";
  }

  /**
   * @return the date resolution.
   */
  public Resolution getResolution() {
    return this._resolution;
  }

  /**
   * Create a query for the term given, using the numeric type if there is one.
   * 
   * @param t the term
   * 
   * @return the query
   */
  @Override
  protected Query termToQuery(Term t) {
    try {
      Date d = DateTools.stringToDate(t.text());
      return new DateParameter(this.name(), d, this._resolution, false).toQuery();
    } catch (ParseException ex) {
      LOGGER.warn("Ignoring invalid facet date {} for field {}", t.text(), this.name(), ex);
    }
    return new TermQuery(t);
  }

  @Override
  protected void termToXML(String term, int cardinality, XMLWriter xml) throws IOException {
    xml.openElement("term");
    xml.attribute("text", term);
    try {
      xml.attribute("date", Dates.format(DateTools.stringToDate(term), this._resolution));
    } catch (ParseException ex) {
      // should not happen as the string is coming from the date formatter in the first place
    }
    xml.attribute("cardinality", cardinality);
    xml.closeElement();
  }

  // Static helpers -------------------------------------------------------------------------------

  /**
   * Creates a new facet for the specified field.
   *
   * @param field     The name of the facet.
   * @param r         If this facet is a date.
   *
   * @return the corresponding Facet ready to use with a base query.
   */
  public static DateFieldFacet newFacet(String field, Resolution r) {
    return new DateFieldFacet(field, r, -1);
  }

  /**
   * Creates a new facet for the specified field.
   *
   * @param field     The name of the facet.
   * @param r         If this facet is a date.
   * @param maxValues The maximum number of terms to return
   *
   * @return the corresponding Facet ready to use with a base query.
   */
  public static DateFieldFacet newFacet(String field, Resolution r, int maxValues) {
    return new DateFieldFacet(field, r, maxValues);
  }

}

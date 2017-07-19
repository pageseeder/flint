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
package org.pageseeder.flint.lucene.search;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.pageseeder.flint.lucene.util.Beta;
import org.pageseeder.xmlwriter.XMLWriter;

/**
 * A facet implementation using a simple index field.
 *
 * @author Jean-Baptiste Reure
 * @version 14 July 2017
 */
@Beta
public class StringFieldFacet extends FlexibleFieldFacet {

  /**
   * Creates a new facet with the specified name;
   *
   * @param name     The name of the facet.
   * @param maxterms The maximum number of terms to return
   */
  private StringFieldFacet(String name, int maxterms) {
    super(name, maxterms);
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
    return new TermQuery(t);
  }

  @Override
  protected String getType() {
    return "string-field";
  }

  @Override
  protected void termToXML(String term, int cardinality, XMLWriter xml) throws IOException {
    xml.openElement("term");
    xml.attribute("text", term);
    xml.attribute("cardinality", cardinality);
    xml.closeElement();
  }

  // Static helpers -------------------------------------------------------------------------------

  /**
   * Creates a new facet for the specified field.
   *
   * @param field The name of the facet.
   *
   * @return the corresponding Facet ready to use with a base query.
   *
   * @throws IOException if thrown by the reader.
   */
  public static StringFieldFacet newFacet(String field) {
    return new StringFieldFacet(field, -1);
  }

  /**
   * Creates a new facet for the specified field.
   *
   * @param field     The name of the facet.
   * @param maxValues The maximum number of terms to return
   *
   * @return the corresponding Facet ready to use with a base query.
   *
   * @throws IOException if thrown by the reader.
   */
  public static StringFieldFacet newFacet(String field, int maxValues) {
    return new StringFieldFacet(field, maxValues);
  }

}

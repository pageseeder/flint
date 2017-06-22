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
import java.util.List;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.pageseeder.flint.lucene.util.Beta;
import org.pageseeder.xmlwriter.XMLWritable;


/**
 * Defines a facet for a search.
 *
 * @author Christophe Lauret
 * @version 2 August 2010
 */
@Beta
public interface Facet extends XMLWritable {

  /**
   * Returns the name of this facet.
   *
   * @return the name of this facet.
   */
  String name();

  /**
   * Returns the query that would correspond to this facet for the specified value.
   * 
   * @deprecated use {@link Filter} instead
   *
   * @param value the text of the term to match.
   * @return the requested query if it exists or <code>null</code>.
   */
  Query forValue(String value);

  /**
   * Compute the values for this facet.
   *
   * @param searcher The searcher to use to compute the facet values.
   * @param base     The base query to build the facet from.
   *
   * @throws IOException should it be reported by the searcher.
   */
  void compute(IndexSearcher searcher, Query base) throws IOException;

  /**
   * Compute the values for this flexible facet.
   * The filters provided are applied to the base query except for the ones with the same name as this facet.
   *
   * @param searcher The searcher to use to compute the facet values.
   * @param base     The base query to build the facet from.
   * @param filters  The filters to re-apply to the base query
   *
   * @throws IOException should it be reported by the searcher.
   */
  void compute(IndexSearcher searcher, Query base, List<Filter> filters) throws IOException;

}

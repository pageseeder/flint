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
package org.pageseeder.flint.query;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;

/**
 * An interface for defining search queries to the index search tool.
 *
 * <p>Implementations must translate the user parameters into a meaningful query.
 *
 * <p>The search query implementations must provide an XML representation.
 *
 * @author Christophe Lauret (Weborganic)
 *
 * @version 20 November 2006
 */
public interface SearchQuery extends SearchParameter {

  /**
   * Indicates whether the search query is empty.
   *
   * <p>A search query is considered empty when it does not contain any parameter that would
   * yield meaningful results with an index search.
   *
   * @return <code>true</code> if the search query is empty;
   *         <code>false</code> otherwise.
   */
  @Override
  boolean isEmpty();

  /**
   * Returns the Lucene query instance corresponding to this object.
   *
   * @return The query object.
   */
  @Override
  Query toQuery();

  /**
   * Returns the sorting key for the results.
   *
   * @return The sorting key for the results.
   */
  Sort getSort();

}

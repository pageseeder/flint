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

import org.apache.lucene.search.Query;
import org.pageseeder.xmlwriter.XMLWritable;

/**
 * An interface for defining search parameters to be aggregated into a query.
 *
 * <p>
 * Implementations must translate the user parameters into a meaningful query.
 *
 * <p>
 * The search query implementations must provide an XML representation.
 *
 * @author Christophe Lauret (Allette Systems)
 *
 * @version 19 November 2006
 */
public interface SearchParameter extends XMLWritable {

  /**
   * Indicates whether the search query is empty.
   *
   * <p>
   * A search query is considered empty when it does not contain any parameter that would yield
   * meaningful results with an index search.
   *
   * @return <code>true</code> if the search query is empty; <code>false</code> otherwise.
   */
  boolean isEmpty();

  /**
   * Returns the Lucene query instance corresponding to this object.
   *
   * @return The query object
   */
  Query toQuery();

}

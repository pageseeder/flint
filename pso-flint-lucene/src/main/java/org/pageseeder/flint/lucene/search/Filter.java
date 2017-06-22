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

import org.apache.lucene.search.Query;
import org.pageseeder.flint.lucene.util.Beta;
import org.pageseeder.xmlwriter.XMLWritable;

/**
 * A query filter.
 *
 * @author Jean-Baptiste Reure
 * @version 16 June 2016
 */
@Beta
public interface Filter extends XMLWritable {

  /**
   * The name of the filter which is the field the filter is applied to.
   * 
   * @return the name of the filter
   */
  public String name();

  /**
   * Returns the filtered query.
   * 
   * @param base the base query
   * 
   * @return the filtered query.
   */
  public Query filterQuery(Query base);

}

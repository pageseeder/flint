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

import java.util.Set;

import org.apache.lucene.index.Term;

/**
 * Indicates that terms can be extracted from the query.
 *
 * <p>Flint Query implementations that implement this interface have better control over the terms
 * which are relevant to generate extracts.
 *
 * <p>In particular for Lucene Range Queries, where the query have to be rewritten in order to get
 * the relevant parameters.
 *
 * @author Christophe Lauret
 * @version 10 September 2010
 */
@Deprecated
public interface TermExtractable {

  /**
   * Adds the terms occurring in this query to the terms set.
   *
   * <p>This method allows better control over the terms that needs to be extracted from a query.
   *
   * @param terms terms to collection
   */
  void extractTerms(Set<Term> terms) ;

}

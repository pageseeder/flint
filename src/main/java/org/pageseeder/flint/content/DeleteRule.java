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
package org.pageseeder.flint.content;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.pageseeder.flint.query.SearchQuery;

/**
 * Defines the rules used to delete Content from an Index.
 *
 * <p>A term can be used (defined by field name and field value) or a Lucene query
 * (defined by a <code>SearchQuery</code>).
 *
 * @author Jean-Baptiste Reure
 *
 * @version 10 March 2010
 */
public final class DeleteRule {

  /**
   * The Term used to delete
   */
  private final Term term;

  /**
   * A Lucene query used to delete
   */
  private final Query _query;

  /**
   * Build a rule based on a term.
   *
   * @param fieldname the name of the field
   * @param fieldvalue the value of the field
   */
  public DeleteRule(String fieldname, String fieldvalue) {
    this.term = new Term(fieldname, fieldvalue);
    this._query = null;
  }

  /**
   * Build a rule based on a Lucene query.
   *
   * @param query the search query to use to identify the file to delete
   */
  public DeleteRule(SearchQuery query) {
    this._query = query.toQuery();
    this.term = null;
  }

  /**
   * Return the Term used for deleting.
   *
   * @return the Term used for deleting
   */
  public Term toTerm() {
    return this.term;
  }

  /**
   * Return the Query used for deleting.
   *
   * @return the Query used for deleting
   */
  public Query toQuery() {
    return this._query;
  }

  /**
   * Indicates whether the delete rule is defined by an index Term.
   *
   * @return <code>true</code> if a Term defines the rule;
   *         <code>false</code> if a query does
   */
  public boolean useTerm() {
    return this._query == null;
  }

}

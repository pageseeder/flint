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

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.pageseeder.xmlwriter.XMLWriter;

/**
 * A search query to submit a predicate directly to the index.
 *
 * <p>This query uses the Lucene {@link QueryParser} to create a valid query instance from the
 * supplied predicate.
 *
 * @author Christophe Lauret (Weborganic)
 * @author Jean-Baptiste Reure (Weborganic)
 *
 * @version 2 August 2010
 */
public final class PredicateSearchQuery implements SearchQuery {

  /**
   * Bypasses all the other query parameters.
   */
  private final String _predicate;

  /**
   * The analyser used in this query.
   */
  private final Analyzer _analyser;

  /**
   * The sort order.
   */
  private final Sort _sort;

  /**
   * A flag to specify if wildcard ('*' or '?') is allowed as the first character of the predicate.
   */
  private boolean _allowLeadingWildcard = false;

  /**
   * Creates new predicate search query.
   *
   * @param predicate The predicate for this query.
   * @param sortField The field name to use to order the results.
   *
   * @throws IllegalArgumentException If the predicate is <code>null</code>.
   */
  public PredicateSearchQuery(String predicate, String sortField) throws IllegalArgumentException {
    this(predicate, sortField == null ? Sort.INDEXORDER : new Sort(new SortField(sortField, Type.STRING)));
  }

  /**
   * Creates new predicate search query.
   *
   * @param predicate The predicate for this query.
   * @param analyzer  The analyzer to use for the query, should be the same as the one used to write the Index.
   * @param sortField The field name to use to order the results.
   *
   * @throws IllegalArgumentException If the predicate is <code>null</code>.
   */
  public PredicateSearchQuery(String predicate, Analyzer analyzer, String sortField) throws IllegalArgumentException {
    this(predicate, analyzer, sortField == null ? Sort.INDEXORDER : new Sort(new SortField(sortField, Type.STRING)));
  }

  /**
   * Creates new predicate search query.
   *
   * @param predicate The predicate for this query.
   *
   * @throws IllegalArgumentException If the predicate is <code>null</code>.
   */
  public PredicateSearchQuery(String predicate) throws IllegalArgumentException {
    this(predicate, Sort.INDEXORDER);
  }

  /**
   * Creates new predicate search query.
   *
   * @param predicate The predicate for this query.
   * @param analyzer  The analyzer to use for the query, should be the same as the one used to write the Index.
   *
   * @throws IllegalArgumentException If the predicate is <code>null</code>.
   */
  public PredicateSearchQuery(String predicate, Analyzer analyzer) throws IllegalArgumentException {
    this(predicate, analyzer, Sort.INDEXORDER);
  }

  /**
   * Creates new predicate search query.
   *
   * @param predicate The predicate for this query.
   * @param sort      The sort order for the results.
   *
   * @throws IllegalArgumentException If the predicate is <code>null</code>.
   */
  public PredicateSearchQuery(String predicate, Sort sort) throws IllegalArgumentException {
    this(predicate, new StandardAnalyzer(), sort);
  }

  /**
   * Creates new predicate search query.
   *
   * @param predicate The predicate for this query.
   * @param analyzer  The analyzer to use for the query, should be the same as the one used to write the Index.
   * @param sort      The sort order for the results.
   *
   * @throws IllegalArgumentException If the predicate is <code>null</code>.
   */
  public PredicateSearchQuery(String predicate, Analyzer analyzer, Sort sort) throws IllegalArgumentException {
    if (predicate == null) throw new IllegalArgumentException("predicate is null");
    this._predicate = predicate;
    this._analyser = analyzer;
    this._sort = sort;
  }

  // getters and setters
  // --------------------------------------------------------------------------------------------

  /**
   * Whether or not a wildcard ('*' or '?') is allowed as the first character of the predicate.
   *
   * @param allowWildCardStart true if wildcard should be allowed, false otherwise
   */
  public void setAllowWildCardStart(boolean allowWildCardStart) {
    this._allowLeadingWildcard = allowWildCardStart;
  }

  /**
   * Returns the Lucene query instance corresponding to this object.
   *
   * <p>
   * This method uses a query parser to parse the predicate.
   *
   * @return The Lucene query instance or <code>null</code> if the predicate was <code>null</code>.
   */
  @Override
  public Query toQuery() {
    if (this._predicate == null)
      return null;
    try {
      QueryParser parser = new QueryParser(null, this._analyser);
      parser.setAllowLeadingWildcard(this._allowLeadingWildcard);
      return parser.parse(this._predicate);
    } catch (ParseException ex) {
      return null;
    }
  }

  /**
   * Returns <code>true</code> if the predicate has not been set.
   *
   * @return <code>true</code> if the predicate has not been set; <code>false</code> otherwise.
   */
  @Override
  public boolean isEmpty() {
    return this._predicate == null;
  }

  /**
   * Serialises the search query as XML.
   *
   * <pre>{@code
   *   <search-query>
   *     <predicate>[predicate]</predicate>
   *   <search-query>
   * }</pre>
   *
   * @param xml The XML writer.
   *
   * @throws IOException Should there be any I/O exception while writing the XML.
   */
  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("search-query", true);
    xml.element("predicate", this._predicate);
    xml.closeElement();
  }

  /**
   * Returns the sort order.
   *
   * @return the sorting rules for the search results
   */
  @Override
  public Sort getSort() {
    return this._sort;
  }

  @Override
  public String toString() {
    return this._predicate;
  }
}

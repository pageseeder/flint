/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.query;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.weborganic.flint.IndexManager;

import com.topologi.diffx.xml.XMLWriter;

/**
 * A search query to submit a predicate directly to the index.
 * 
 * @author Christophe Lauret (Weborganic)
 * @author Jean-Baptiste Reure (Weborganic)
 * 
 * @version 1 September 2006
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
   * A flag to specify if wildcard ('*' or '?') is allowed as the first character of the predicate
   */
  private boolean allowWildCardStart = false;
  /**
   * Creates new predicate search query.
   * 
   * @param predicate The predicate for this query.
   * @param sortField The field name to use to order the results.
   */
  public PredicateSearchQuery(String predicate, String sortField) {
    this(predicate, sortField == null ? Sort.INDEXORDER : new Sort(new SortField(sortField, SortField.STRING)));
  }
  /**
   * Creates new predicate search query.
   * 
   * @param predicate The predicate for this query.
   * @param analyzer  The analyzer to use for the query, should be the same as the one used to write the Index.
   * @param sortField The field name to use to order the results.
   */
  public PredicateSearchQuery(String predicate, Analyzer analyzer, String sortField) {
    this(predicate, analyzer, sortField == null ? Sort.INDEXORDER : new Sort(new SortField(sortField, SortField.STRING)));
  }

  /**
   * Creates new predicate search query.
   * 
   * @param predicate The predicate for this query.
   */
  public PredicateSearchQuery(String predicate) {
    this(predicate, Sort.INDEXORDER);
  }

  /**
   * Creates new predicate search query.
   * 
   * @param predicate The predicate for this query.
   * @param analyzer  The analyzer to use for the query, should be the same as the one used to write the Index.
   */
  public PredicateSearchQuery(String predicate, Analyzer analyzer) {
    this(predicate, analyzer, Sort.INDEXORDER);
  }

  /**
   * Creates new predicate search query.
   * 
   * @param predicate The predicate for this query.
   * @param sort      The sort order for the results.
   */
  public PredicateSearchQuery(String predicate, Sort sort) {
    this(predicate, new StandardAnalyzer(IndexManager.LUCENE_VERSION), sort);
  }

  /**
   * Creates new predicate search query.
   * 
   * @param predicate The predicate for this query.
   * @param analyzer  The analyzer to use for the query, should be the same as the one used to write the Index.
   * @param sort      The sort order for the results.
   */
  public PredicateSearchQuery(String predicate, Analyzer analyzer, Sort sort) {
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
    this.allowWildCardStart = allowWildCardStart;
  }
  
  /**
   * Returns null.
   * 
   * @return null.
   */
  public String getField() {
    return null;
  }

  /**
   * Returns the predicate to use for this search.
   * 
   * @return The predicate for this search.
   * 
   * @see org.weborganic.flint.query.SearchQuery#getPredicate()
   */
  public String getPredicate() {
    return this._predicate;
  }

  /**
   * Returns the Lucene query instance corresponding to this object.
   * 
   * <p>
   * This method uses a query parser to parse the predicate.
   * 
   * @return The Lucene query instance.
   */
  public Query toQuery() {
    if (this._predicate == null)
      return null;
    try {
      QueryParser parser = new QueryParser(IndexManager.LUCENE_VERSION, getField(), this._analyser);
      parser.setAllowLeadingWildcard(this.allowWildCardStart);
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
  public boolean isEmpty() {
    return this._predicate == null;
  }

  /**
   * Serialises the search query as XML.
   * 
   * <pre>
   *   <search-query>
   *     <predicate>[predicate]</predicate>
   *     <field>[field]</field>
   *   <search-query>
   * </pre>
   * 
   * @param xml The XML writer.
   * 
   * @throws IOException Should there be any I/O exception while writing the XML.
   */
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("search-query", true);
    // the predicate produced by the query
    xml.element("predicate", this._predicate);
    // close 'search-query'
    xml.closeElement();
  }

  /**
   * Returns the sort order.
   * 
   * @return the sorting rules for the search results
   */
  public Sort getSort() {
    return this._sort;
  }

}

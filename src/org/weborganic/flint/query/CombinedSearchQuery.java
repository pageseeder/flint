/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.query;

import java.io.IOException;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;


import com.topologi.diffx.xml.XMLWriter;

/**
 * Combines 2 search queries into 1 using an OR operator.
 *
 * @author Christophe Lauret (Weborganic)
 * 
 * @version 8 October 2009
 */
public final class CombinedSearchQuery implements SearchQuery {

  /**
   * 1st search query
   */
  private SearchQuery _query1;

  /**
   * 2nd search query
   */
  private SearchQuery _query2;

  /**
   * The sort order.
   */
  private Sort _sort;

  /**
   * Creates a new search query combining two queries.
   * 
   * @param query1 The first query to combine.
   * @param query2 The second query to combine.
   * @param sort   The sorting to apply to this combined query.
   */
  public CombinedSearchQuery(SearchQuery query1, SearchQuery query2, Sort sort) {
    this._query1 = query1;
    this._query2 = query2;
    this._sort = sort;
  }

  // methods
  // ----------------------------------------------------------------------------------------------

  /**
   * Indicates whether any of the search queries do not contain any parameter.
   * 
   * @return <code>true</code> if any of the search queries do not contain any parameter;
   *         <code>false</code> otherwise.
   */
  public boolean isEmpty() {
    return (this._query1.isEmpty() || this._query2.isEmpty());
  }

  /**
   * Returns the name of the field to search.
   * 
   * @return The name of the field to search.
   */
  public String getField() {
    return "";
  }

  /**
   * Generates the <code>Query</code> object corresponding to a combined search query.
   * 
   * @return The Lucene query instance.
   * 
   * @see org.weborganic.flint.query.SearchQuery#toQuery()
   */
  public Query toQuery() {
    BooleanQuery query = new BooleanQuery();
    query.add(this._query1.toQuery(), BooleanClause.Occur.SHOULD);
    query.add(this._query2.toQuery(), BooleanClause.Occur.SHOULD);
    return query;
  }

  /**
   * Serialises the search query as XML.
   * 
   * @param xml The XML writer.
   * 
   * @throws IOException Should there be any I/O exception while writing the XML.
   */
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("search-query", true);
    // serialise the first query
    xml.openElement("query1", true);
    this._query1.toXML(xml);
    xml.closeElement(); 
    // serialise the second query
    xml.openElement("query2", true);
    this._query2.toXML(xml);
    xml.closeElement();
    // close 'search-query'
    xml.closeElement();
  }

  /**
   * {@inheritDoc}
   */
  public String toString() {
    return ('(' + this._query1.toString() + " and " + this._query2.toString() + ')');
  }

  /**
   * {@inheritDoc}
   */
  public String getPredicate() {
    return '(' + this._query1.getPredicate() + " or " +this._query2.getPredicate() + ')';
  }

  /**
   * {@inheritDoc}
   */
  public Sort getSort() {
    return this._sort;
  }

}

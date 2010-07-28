/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;

import com.topologi.diffx.xml.XMLWriter;

/**
 * A base class for all the Flint search queries.
 *
 * @author  Christophe Lauret (Weborganic)
 * @author  Tu Tak Tran (Allette Systems)
 * @author  William Liem (Allette Systems)
 * 
 * @version 26 Feb 2009
 */
public final class GenericSearchQuery implements SearchQuery {

  /**
   * The list of search parameters.
   */
  private List<SearchParameter> _parameters = new ArrayList<SearchParameter>();

  /**
   * The sort order.
   */
  private Sort _sort = Sort.INDEXORDER;

// setters for the class attributes ---------------------------------------------------------------

  /**
   * Adds a parameter to this search query.
   * 
   * @param parameter A search parameter.
   */
  public void add(SearchParameter parameter) {
    this._parameters.add(parameter);
  }

  /**
   * Indicates whether this query is without parameters.
   * 
   * @return <code>true</code> if this search query does not contain any parameter;
   *         <code>false</code> otherwise.
   */
  public boolean isEmpty() {
    for (int i = 0; i < this._parameters.size(); i++) {
      SearchParameter parameter = this._parameters.get(i);
      if (!(parameter.isEmpty()))
        return false;
    }
    return true;
  }

  /**
   * Defines the sort order.
   * 
   * @param sort The sort order. 
   */
  public void setSort(Sort sort) {
    this._sort = sort; 
  }

  /**
   * Defines the sort order.
   * 
   * @return the sorting rules for this 
   */
  public Sort getSort() {
    return this._sort;
  }

  /**
   * Generates the <code>Query</code> object corresponding to a search query.
   * 
   * @return The Lucene query instance.
   * 
   * @see org.weborganic.flint.query.SearchQuery#toQuery()
   */
  public Query toQuery() {
    BooleanQuery query = new BooleanQuery();

    // iterate over the possible parameters
    for (int i = 0; i < this._parameters.size(); i++) {
      SearchParameter parameter = (SearchParameter)this._parameters.get(i);
      Query q = parameter.toQuery();
      if (q != null && 
          !(q instanceof BooleanQuery && ((BooleanQuery)q).clauses().isEmpty())) {
        query.add(q, BooleanClause.Occur.MUST);
      } else {
//        LOGGER.debug(parameter.getClass().toString()+" was an empty query.");
      }
    }
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

    // serialise each parameter individually
    for (int i = 0; i < this._parameters.size(); i++) {
      SearchParameter parameter = (SearchParameter)this._parameters.get(i);
      parameter.toXML(xml);
    }

    // the predicate produced by the query
    xml.element("predicate", this.getPredicate());

    // close 'search-query'
    xml.closeElement();
  }

  /**
   * Returns the predicate corresponding to the Lucene query.
   *
   * @see Query#toString()
   *
   * @return The predicate corresponding to the Lucene query.
   */
  public String getPredicate() {
    return toQuery().toString();
  }

  /**
   * Unused as this query does not use a single field but may use multiple.
   */
  public String getField() {
    return "";
  }

  /**
   * {@inheritDoc}
   */
  public String toString() {
    StringBuffer s = new StringBuffer();
    s.append('(');
    for (Iterator<SearchParameter> i = this._parameters.iterator(); i.hasNext();) {
      s.append(i.next().toString());
      if (i.hasNext()) s.append(" and ");
    }
    s.append(')');
    return s.toString();
  }

}

/*
 * This file is part of the Flint library.
 * 
 * For licensing information please see the file license.txt included in the release. A copy of this licence can also be
 * found at http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.query;

import java.io.IOException;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.weborganic.flint.FieldUtils;

import com.topologi.diffx.xml.XMLWriter;

/**
 * A class for representing a search term parameter
 * 
 * <p>It has two values:
 *   1) search field
 *   2) search value
 * 
 * @author Jean-Baptiste Reure
 *
 * @version 08 May 2010
 */

public final class SearchPhraseParameter implements SearchParameter {

  /**
   * The search field
   */
  private final String _searchfield;

  /**
   * The search value
   */
  private final String _searchvalue;

  /**
   * Creates a new query.
   *
   * @param searchterm The searchterm string given by the user.
   * 
   * @throws IllegalArgumentException If the searchterm is <code>null</code>.
   */
  public SearchPhraseParameter(String searchfield, String searchvalue) throws IllegalArgumentException {
    if (searchfield == null)
      throw new IllegalArgumentException("search field cannot be null!");
    if (searchvalue == null)
      throw new IllegalArgumentException("search value cannot be null!");
    this._searchvalue = searchvalue.trim();
    this._searchfield = searchfield;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isEmpty() {
    return this._searchvalue.isEmpty();
  }

  /**
   * {@inheritDoc}
   */
  public String getField() {
    return this._searchfield;
  }

  /**
   * {@inheritDoc}
   */
  public Query toQuery() {
    if (this.isEmpty()) return null;
    BooleanQuery boolquery = new BooleanQuery();
    Query query = FieldUtils.toTermOrPhrase(this._searchfield, this._searchvalue);
    boolquery.add(query, BooleanClause.Occur.SHOULD);
    return boolquery;
  }

  /**
   * {@inheritDoc}
   */
  public String toPredicate() {
    Query query = toQuery();
    if (query == null) return "";
    return query.toString();
  }

  /**
   * {@inheritDoc}
   */
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("searchterm");
    // indicate whether this search term is empty
    xml.attribute("is-empty", Boolean.toString(this.isEmpty()));
    // not empty, give details
    if (!this.isEmpty()) {
      xml.openElement("searchfield");
      xml.writeText(this._searchfield);
      xml.closeElement();
      xml.openElement("searchvalue");
      xml.writeText(this._searchvalue);
      xml.closeElement();
      xml.openElement("predicate");
      xml.writeText(toPredicate());
      xml.closeElement();
    }
    xml.closeElement();
  }

  /**
   * {@inheritDoc}
   */
  public String toString() {
    return toPredicate();
  }

}

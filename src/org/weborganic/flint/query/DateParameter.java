/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.query;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermRangeQuery;

import com.topologi.diffx.xml.XMLWriter;

/**
 * 
 *
 * @author  Christophe Lauret (Weborganic)
 * @author  Jean-Baptiste Reure (Weborganic)
 *
 * @version 21 November 2006
 */
public final class DateParameter implements SearchParameter {

  /**
   * ISO date formatter.
   */
  private static final SimpleDateFormat ISO_DATE = new SimpleDateFormat("yyyyMMdd");

  /**
   * The date field.
   */
  private static final String DATE_FIELD = "date";

  /**
   * The FROM date, if null, then no limit
   */
  private String _from;

  /**
   * The TO date, if null, then today
   */
  private String _to;

//Implementation methods ----------------------------------------------------------------------

  /**
   * Set the value of the lower limit for the date.
   * 
   * @param from The date or <code>null</code>.
   */
  public void setFrom(Date from) {
    this._from = (from != null)? ISO_DATE.format(from) : null;
  }

  /**
   * Set the value of the upper limit for the date.
   * 
   * @param to The date or <code>null</code>.
   */
  public void setTo(Date to) {
    this._to = (to != null)? ISO_DATE.format(to) : null;
  }

  /**
   * Returns the name of the field to search.
   * 
   * @return The name of the field to search.
   */
  public String getField() {
    return DATE_FIELD;
  }

  /**
   * Generates the <code>Query</code> object corresponding to a date range search query.
   * 
   * @return The Lucene query instance.
   * 
   * @see org.weborganic.flint.query.SearchQuery#toQuery()
   */
  public Query toQuery() {
    if (this._from == null && this._to == null) return null;
    // an including range query on the date
    return new TermRangeQuery(DATE_FIELD, this._from, this._to, true, true);
  }

  /**
   * Returns <code>true</code> if this search query does not contain any parameter.
   * 
   * @return <code>true</code> if this search query does not contain any parameter;
   *         <code>false</code> otherwise.
   */
  public boolean isEmpty() {
    return this._from == null && this._to == null;
  }

  /**
   * Serialises the search query as XML.
   * 
   * @param xml The XML writer.
   * 
   * @throws IOException Should there be any I/O exception while writing the XML.
   */
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("date-range", true);
    if (this._from != null)
      xml.element("after", this._from);
    if (this._to != null)
      xml.element("before", this._to);
    xml.closeElement();
  }

}

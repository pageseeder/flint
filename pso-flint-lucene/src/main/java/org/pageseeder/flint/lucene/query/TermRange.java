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

import java.io.IOException;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermRangeQuery;
import org.pageseeder.flint.lucene.util.Beta;
import org.pageseeder.xmlwriter.XMLWriter;

/**
 * Create a range parameter using numeric values.
 * <p>
 * This is API is still experimental and subject to change in Lucene, any change in Lucene may also
 * be reflected in this API.
 *
 * @author Christophe Lauret (Weborganic)
 * @author Jean-Baptiste Reure (Weborganic)
 *
 * @version 18 July 2017
 */
@Beta
public final class TermRange implements SearchParameter {

  /**
   * The numeric field.
   */
  private final String _field;

  /**
   * The minimum value, if <code>null</code>, then no lower limit.
   */
  private final String _min;

  /**
   * The maximum value, if <code>null</code>, then no upper limit.
   */
  private final String _max;

  /**
   * Whether the minimum value should be included; ignored if the minimum value is <code>null</code>
   */
  private final boolean _minInclusive;

  /**
   * Whether the maximum value should be included; ignored if the maximum value is <code>null</code>
   */
  private final boolean _maxInclusive;

  /**
   * The actual Lucene query (lazy initialised)
   */
  private volatile Query _query;

// Implementation methods ----------------------------------------------------------------------

  /**
   * Creates a new numeric range parameter.
   *
   * @param field        the numeric field to search
   * @param min          the minimum value in the range (can be <code>null</code>)
   * @param max          the maximum value in the range (can be <code>null</code>)
   * @param minInclusive <code>true</code> to include values matching the lower limit in the range;
   *                     <code>false</code> to exclude it.
   * @param maxInclusive <code>true</code> to include values matching the upper limit in the range;
   *                     <code>false</code> to exclude it.
   */
  private TermRange(String field, String min, String max, boolean minInclusive, boolean maxInclusive) {
    if (field == null) throw new NullPointerException("field");
    this._field = field;
    this._min = min;
    this._max = max;
    this._maxInclusive = maxInclusive;
    this._minInclusive = minInclusive;
  }

  /**
   * Returns the value of the lower limit of the range.
   *
   * @return A minimum value or <code>null</code>.
   */
  public String min() {
    return this._min;
  }

  /**
   * Returns the value of the upper limit for the range.
   *
   * @return A maximum value or <code>null</code>.
   */
  public String max() {
    return this._max;
  }

  /**
   * Returns the name of the numeric field to search.
   *
   * @return The name of the numeric field to search.
   */
  public String field() {
    return this._field;
  }

  /**
   * Indicates whether the lower limit is inclusive.
   *
   * @return <code>true</code> to include the lower limit in the range; <code>false</code> to included it.
   */
  public boolean includesMin() {
    return this._minInclusive;
  }

  /**
   * Indicates whether the upper limit is inclusive.
   *
   * @return <code>true</code> to include the upper limit in the range; <code>false</code> to included it.
   */
  public boolean includesMax() {
    return this._maxInclusive;
  }

  /**
   * Returns <code>true</code> if this search query does not contain any parameter.
   *
   * @return <code>true</code> if this search query does not contain any parameter;
   *         <code>false</code> otherwise.
   */
  @Override
  public boolean isEmpty() {
    return this._min == null && this._max == null;
  }

  /**
   * Generates the <code>Query</code> object corresponding to a numeric range search query.
   * <p>
   * Returns a <code>NumericRangeQuery</code> based on the values in this object.
   *
   * @return a <code>NumericRangeQuery</code> or <code>null</code> if empty.
   */
  @Override
  public Query toQuery() {
    if (this._min == null && this._max == null) return null;
    if (this._query == null)  {
      this._query = TermRangeQuery.newStringRange(this._field, this._min, this._max, this._minInclusive, this._maxInclusive);
    }
    return this._query;
  }

  /**
   * Serialises the search query as XML.
   *
   * @param xml The XML writer.
   *
   * @throws IOException Should there be any I/O exception while writing the XML.
   */
  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("term-range", false);
    xml.attribute("type", "string");
    xml.attribute("field", this._field);
    if (this._min != null) {
      xml.attribute("min", this._min);
      xml.attribute("min-included", Boolean.toString(this._minInclusive));
    }
    if (this._max != null) {
      xml.attribute("max", this._max);
      xml.attribute("max-included", Boolean.toString(this._maxInclusive));
    }
    xml.closeElement();
  }


  /**
   * Factory that creates a <code>TermRange</code>, that queries a string field.
   *
   * @param field        the numeric field to search
   * @param min          the minimum value in the range (can be <code>null</code>)
   * @param max          the maximum value in the range (can be <code>null</code>)
   * @param minInclusive <code>true</code> to include values matching the lower limit in the range;
   *                     <code>false</code> to exclude it.
   * @param maxInclusive <code>true</code> to include values matching the upper limit in the range;
   *                     <code>false</code> to exclude it.
   *
   * @return a new range.
   */
  public static TermRange newRange(String field, String min, String max,
      boolean minInclusive, boolean maxInclusive) {
    return new TermRange(field, min, max, minInclusive, maxInclusive);
  }
}

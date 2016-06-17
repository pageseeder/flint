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

import org.apache.lucene.document.FieldType.NumericType;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.pageseeder.flint.catalog.Catalog;
import org.pageseeder.flint.catalog.Catalogs;
import org.pageseeder.flint.util.Beta;
import org.pageseeder.xmlwriter.XMLWriter;

/**
 * Create a range parameter using numeric values.
 *
 * <p>This class simply wraps a {@link NumericRangeQuery} instance and is therefore closely related to it.
 * This is API is still experimental and subject to change in Lucene, any change in Lucene may also
 * be reflected in this API.
 *
 * @param <T> The number type for this numeric range
 *
 * @author Christophe Lauret (Weborganic)
 * @author Jean-Baptiste Reure (Weborganic)
 *
 * @version 22 September 2010
 */
@Beta
public final class NumericRange<T extends Number> implements SearchParameter {

  /**
   * The numeric field.
   */
  private final String _field;

  /**
   * The minimum value, if <code>null</code>, then no lower limit.
   */
  private T _min;

  /**
   * The maximum value, if <code>null</code>, then no upper limit.
   */
  private T _max;

  /**
   * Whether the minimum value should be included; ignored if the minimum value is <code>null</code>
   */
  private boolean _minInclusive;

  /**
   * Whether the maximum value should be included; ignored if the maximum value is <code>null</code>
   */
  private boolean _maxInclusive;

  /**
   * The actual Lucene query (lazy initialised)
   */
  private volatile Query _query;

// Implementation methods ----------------------------------------------------------------------

  /**
   * Creates a new numeric range parameter.
   *
   * @param field        the numeric field to search
   * @param min          the minimum value in the range (may be <code>null</code>)
   * @param max          the maximum value in the range (may be <code>null</code>)
   * @param minInclusive <code>true</code> to include values matching the lower limit in the range;
   *                     <code>false</code> to exclude it.
   * @param maxInclusive <code>true</code> to include values matching the upper limit in the range;
   *                     <code>false</code> to exclude it.
   */
  private NumericRange(String field, T min, T max, boolean minInclusive, boolean maxInclusive) {
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
  public T min() {
    return this._min;
  }

  /**
   * Returns the value of the upper limit for the range.
   *
   * @return A maximum value or <code>null</code>.
   */
  public T max() {
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
   *
   * Returns a <code>NumericRangeQuery</code> based on the values in this object.
   *
   * @return a <code>NumericRangeQuery</code> or <code>null</code> if empty.
   */
  @Override
  public Query toQuery() {
    if (this._min == null && this._max == null) return null;
    if (this._query == null)  {
      this._query = toNumericRangeQuery(this._field, this._min, this._max, this._minInclusive, this._maxInclusive);
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
    xml.openElement("numeric-range", false);
    xml.attribute("field", this._field);
    if (this._min != null) {
      xml.attribute("min", this._min.toString());
      xml.attribute("min-included", Boolean.toString(this._minInclusive));
    }
    if (this._max != null) {
      xml.attribute("max", this._max.toString());
      xml.attribute("max-included", Boolean.toString(this._maxInclusive));
    }
    xml.closeElement();
  }

  // Private helpers ------------------------------------------------------------------------------

  /**
   * Returns the numeric range query that corresponds to the specified parameters.
   *
   * @param field        the numeric field
   * @param min          the lower limit (may be null)
   * @param max          the upper limit (may be null)
   * @param minInclusive <code>true</code> to include the minimum value in the range; <code>false</code> to excluded it.
   * @param maxInclusive <code>true</code> to include the maximum value in the range; <code>false</code> to excluded it.
   *
   * @return the corresponding <code>NumericRangeQuery</code>
   */
  private static NumericRangeQuery<? extends Number>
      toNumericRangeQuery(String field, Number min, Number max, boolean minInclusive, boolean maxInclusive) {
    // Long
    if (min instanceof Long || (min == null && max instanceof Long)) return NumericRangeQuery.newLongRange(field, (Long)min, (Long)max, minInclusive, maxInclusive);
    // Integer
    if (min instanceof Integer || (min == null && max instanceof Integer)) return NumericRangeQuery.newIntRange(field, (Integer)min, (Integer)max, true, true);
    // Double
    if (min instanceof Double || (min == null && max instanceof Double)) return NumericRangeQuery.newDoubleRange(field, (Double)min, (Double)max, true, true);
    // Float
    if (min instanceof Float || (min == null && max instanceof Float)) return NumericRangeQuery.newFloatRange(field, (Float)min, (Float)max, true, true);
    // Should never happen
    return null;
  }

  // factory methods ------------------------------------------------------------------------------

  /**
   * Factory that creates a <code>NumericRangeParameter</code>, that queries a double range using
   * the default precisionStep.
   *
   * @param field        the numeric field to search
   * @param min          the minimum value in the range (may be <code>null</code>)
   * @param max          the maximum value in the range (may be <code>null</code>)
   * @param minInclusive <code>true</code> to include values matching the lower limit in the range;
   *                     <code>false</code> to exclude it.
   * @param maxInclusive <code>true</code> to include values matching the upper limit in the range;
   *                     <code>false</code> to exclude it.
   *
   * @return a new range.
   */
  public static NumericRange<Double> newDoubleRange(String field, Double min, Double max,
      boolean minInclusive, boolean maxInclusive) {
    return new NumericRange<Double>(field, min, max, minInclusive, maxInclusive);
  }

  /**
   * Factory that creates a <code>NumericRangeParameter</code>, that queries a float range using the
   * default precisionStep.
   *
   * @param field        the numeric field to search
   * @param min          the minimum value in the range (may be <code>null</code>)
   * @param max          the maximum value in the range (may be <code>null</code>)
   * @param minInclusive <code>true</code> to include values matching the lower limit in the range;
   *                     <code>false</code> to exclude it.
   * @param maxInclusive <code>true</code> to include values matching the upper limit in the range;
   *                     <code>false</code> to exclude it.
   *
   * @return a new range.
   */
  public static NumericRange<Float> newFloatRange(String field, Float min, Float max,
      boolean minInclusive, boolean maxInclusive) {
    return new NumericRange<Float>(field, min, max, minInclusive, maxInclusive);
  }

  /**
   * Factory that creates a <code>NumericRangeParameter</code>, that queries a int range using the
   * default precisionStep.
   *
   * @param field        the numeric field to search
   * @param min          the minimum value in the range (may be <code>null</code>)
   * @param max          the maximum value in the range (may be <code>null</code>)
   * @param minInclusive <code>true</code> to include values matching the lower limit in the range;
   *                     <code>false</code> to exclude it.
   * @param maxInclusive <code>true</code> to include values matching the upper limit in the range;
   *                     <code>false</code> to exclude it.
   *
   * @return a new range.
   */
  public static NumericRange<Integer> newIntRange(String field, Integer min, Integer max,
      boolean minInclusive, boolean maxInclusive) {
    return new NumericRange<Integer>(field, min, max, minInclusive, maxInclusive);
  }

  /**
   * Factory that creates a <code>NumericRangeParameter</code>, that queries a long range using the
   * default precisionStep.
   *
   * @param field        the numeric field to search
   * @param min          the minimum value in the range (may be <code>null</code>)
   * @param max          the maximum value in the range (may be <code>null</code>)
   * @param minInclusive <code>true</code> to include values matching the lower limit in the range;
   *                     <code>false</code> to exclude it.
   * @param maxInclusive <code>true</code> to include values matching the upper limit in the range;
   *                     <code>false</code> to exclude it.
   *
   * @return a new range.
   */
  public static NumericRange<Long> newLongRange(String field, Long min, Long max,
      boolean minInclusive, boolean maxInclusive) {
    return new NumericRange<Long>(field, min, max, minInclusive, maxInclusive);
  }

  /**
   * Factory that creates a <code>NumericRangeParameter</code>, that queries a long range using the
   * default precisionStep.
   *
   * @param field        the numeric field to search
   * @param min          the minimum value in the range (may be <code>null</code>)
   * @param max          the maximum value in the range (may be <code>null</code>)
   * @param minInclusive <code>true</code> to include values matching the lower limit in the range;
   *                     <code>false</code> to exclude it.
   * @param maxInclusive <code>true</code> to include values matching the upper limit in the range;
   *                     <code>false</code> to exclude it.
   *
   * @return a new range.
   */
  public static NumericRange<?> newNumberRange(String field, String catalog, Number min, Number max,
      boolean minInclusive, boolean maxInclusive) {
    Catalog thecatalog = Catalogs.getCatalog(catalog);
    if (thecatalog == null) return null;
    NumericType nt = thecatalog.getSearchNumericType(field);
    if (nt == null) return null;
    switch (nt) {
      case DOUBLE: return newDoubleRange(field, (Double)  min, (Double)  max, minInclusive, maxInclusive);
      case FLOAT : return newFloatRange(field,  (Float)   min, (Float)   max, minInclusive, maxInclusive);
      case INT   : return newIntRange(field,    (Integer) min, (Integer) max, minInclusive, maxInclusive);
      case LONG  : return newLongRange(field,   (Long)    min, (Long)    max, minInclusive, maxInclusive);
    }
    return null;
  }

}

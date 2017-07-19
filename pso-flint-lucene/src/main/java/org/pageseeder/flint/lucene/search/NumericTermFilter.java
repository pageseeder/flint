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
package org.pageseeder.flint.lucene.search;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.NumericUtils;
import org.pageseeder.flint.indexing.FlintField.NumericType;
import org.pageseeder.flint.lucene.query.NumberParameter;
import org.pageseeder.flint.lucene.query.Queries;
import org.pageseeder.flint.lucene.util.Beta;
import org.pageseeder.xmlwriter.XMLWriter;

/**
 * A facet implementation using a simple index field.
 *
 * @author Christophe Lauret
 * @version 16 February 2012
 */
@Beta
public class NumericTermFilter implements Filter {

  /**
   * The name of the field
   */
  private final String _name;

  /**
   * A numeric type
   */
  private NumericType _numeric = null;

  /**
   * The list of terms to filter with
   */
  private final Map<Number, Occur> _values = new HashMap<>();

  /**
   * Creates a new filter with the specified name;
   *
   * @param name    The name of the filter.
   */
  private NumericTermFilter(Builder builder) {
    this._name = builder._name;
    this._numeric = builder._numeric;
    this._values.putAll(builder._values);
  }

  @Override
  public Query filterQuery(Query base) {
    BooleanQuery filterQuery = new BooleanQuery();
    for (Number value : this._values.keySet()) {
      Occur clause = this._values.get(value);
      filterQuery.add(numberToQuery(value), clause);
    }
    return base == null ? filterQuery : Queries.and(base, filterQuery);
  }

  public String name() {
    return this._name;
  }

  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("filter");
    xml.attribute("field", this._name);
    xml.attribute("type", "numeric");
    for (Number value : this._values.keySet()) {
      xml.openElement("term");
      xml.attribute("text", String.valueOf(value));
      xml.attribute("occur", occurToString(this._values.get(value)));
      xml.closeElement();
    }
    xml.closeElement();
  }

  public static NumericTermFilter newFilter(String name, Number value) {
    return newFilter(name, value, Occur.MUST);
  }

  public static NumericTermFilter newFilter(String name, Number value, Occur occur) {
    NumericType numeric;
    if (value instanceof Integer)     numeric = NumericType.INT;
    else if (value instanceof Float)  numeric = NumericType.FLOAT;
    else if (value instanceof Double) numeric = NumericType.DOUBLE;
    else if (value instanceof Long)   numeric = NumericType.LONG;
    else return null;
    return new Builder().name(name).numeric(numeric).addNumber(value, occur).build();
  }

  /**
   * Create a query for the term given, using the numeric type if there is one.
   * 
   * @param t the term
   * 
   * @return the query
   */
  private Query numberToQuery(Number value) {
    switch (this._numeric) {
      case INT:
        return NumberParameter.newIntParameter(this._name, (Integer) value).toQuery();
      case LONG:
        return NumberParameter.newLongParameter(this._name, (Long) value).toQuery();
      case DOUBLE:
        return NumberParameter.newDoubleParameter(this._name, NumericUtils.sortableLongToDouble((Long) value)).toQuery();
      case FLOAT:
        return NumberParameter.newFloatParameter(this._name, NumericUtils.sortableIntToFloat((Integer) value)).toQuery();
    }
    return null;
  }

  private static String occurToString(Occur occur) {
    if (occur == Occur.MUST)     return "must";
    if (occur == Occur.MUST_NOT) return "must_not";
    if (occur == Occur.SHOULD)   return "should";
    return "unknown";
  }

  public static class Builder {

    /**
     * The name of the field
     */
    private String _name = null;

    /**
     * A numeric type
     */
    private NumericType _numeric = null;

    /**
     * The list of terms to filter with
     */
    private final Map<Number, Occur> _values = new HashMap<>();

    public Builder name(String name) {
      this._name = name;
      return this;
    }

    public Builder numeric(NumericType numeric) {
      this._numeric = numeric;
      return this;
    }

    public Builder addNumber(Number value, Occur when) {
      this._values.put(value, when == null ? Occur.MUST : when);
      return this;
    }

    public NumericTermFilter build() {
      if (this._name == null) throw new NullPointerException("name");
      if (this._numeric == null) throw new NullPointerException("numeric");
      if (this._values.isEmpty()) throw new IllegalStateException("no values to filter with!");
      return new NumericTermFilter(this);
    }
  }
}

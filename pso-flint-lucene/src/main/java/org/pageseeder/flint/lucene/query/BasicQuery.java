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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.pageseeder.flint.lucene.util.Beta;
import org.pageseeder.xmlwriter.XMLWriter;

/**
 * An unmodifiable query based on a base query and a set of additional search parameters.
 *
 * @param <T> the type of the base query.
 *
 * @author Christophe Lauret
 * @version 16 August 2010
 */
@Beta
public class BasicQuery<T extends SearchParameter> implements SearchQuery {

  /**
   * The query to use as a base.
   */
  private final T _base;

  /**
   * A list of query parameters which can be used on top of the base query.
   */
  private final Map<SearchParameter, Occur> _parameters;

  /**
   * The Lucene query corresponding to this object.
   */
  private volatile Query _query = null;

  /**
   * How the results should be sorted.
   */
  private Sort _sort = Sort.RELEVANCE;

  /**
   * Constructs a new query.
   *
   * <p>For safety and to ensure that the parameters remain unmodifiable, the specified list should
   * be unmodifiable. Use the factory method to ensure an unmodifiable list.
   *
   * @param base       The query to use as a base.
   * @param parameters A list of query parameters which can be used on top of the base query.
   *
   * @throws NullPointerException if either argument is <code>null</code>.
   */
  BasicQuery(T base, List<SearchParameter> parameters) throws NullPointerException {
    if (base == null) throw new NullPointerException("base");
    if (parameters == null) throw new NullPointerException("parameters");
    this._base = base;
    this._parameters = new HashMap<>();
    for (SearchParameter param : parameters)
      this._parameters.put(param, Occur.MUST);
  }

  /**
   * Constructs a new query.
   *
   * <p>For safety and to ensure that the parameters remain unmodifiable, the specified list should
   * be unmodifiable. Use the factory method to ensure an unmodifiable list.
   *
   * @param base       The query to use as a base.
   * @param parameters A list of query parameters which can be used on top of the base query.
   *
   * @throws NullPointerException if either argument is <code>null</code>.
   */
  BasicQuery(T base, Map<SearchParameter, Occur> parameters) throws NullPointerException {
    if (base == null) throw new NullPointerException("base");
    if (parameters == null) throw new NullPointerException("parameters");
    this._base = base;
    this._parameters = parameters;
  }

  /**
   * Returns the query used as base.
   *
   * @return the query used as base.
   */
  public final T base() {
    return this._base;
  }

  /**
   * Returns the list of additional search parameters associated with this query.
   *
   * @return the list of additional search parameters associated with this query.
   */
  public final List<SearchParameter> parameters() {
    return new ArrayList<>(this._parameters.keySet());
  }

  /**
   * Returns the list of additional search parameters associated with this query.
   *
   * @return the map of additional search parameters associated with this query.
   */
  public final Map<SearchParameter, Occur> parametersMap() {
    return this._parameters;
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
   * Returns the sort order for the results.
   *
   * @return the sort order for the results (defaults to relevance).
   */
  @Override
  public final Sort getSort() {
    return this._sort != null? this._sort : Sort.RELEVANCE;
  }

  /**
   * This query is empty if the base query is empty.
   *
   * @return <code>true</code> if the base query is empty;
   *         <code>false</code> otherwise.
   */
  @Override
  public final boolean isEmpty() {
    return this._base == null || this._base.isEmpty();
  }

  /**
   * Returns the Lucene query corresponding to this class.
   *
   * @return the Lucene query corresponding to this class.
   */
  @Override
  public Query toQuery() {
    if (this._query == null) {
      this._query = toQuery(this._base, this._parameters);
    }
    return this._query;
  }

  /**
   * Generates the XML for this query.
   *
   * <p>As:
   * <pre>{@code
   * <basic-query empty="[true|false]" query="[lucene query]">
   *   <base>
   *     <!-- The base query using the toXML() method -->
   *   </base>
   *   <parameters>
   *     <!-- Each parameters in order using the toXML() method -->
   *   </parameters>
   * </basic-query>
   * }</pre>
   *
   * {@inheritDoc}
   */
  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("basic-query", true);
    xml.attribute("empty", Boolean.toString(isEmpty()));
    if (!isEmpty()) {
      xml.attribute("query", this._query.toString());
      // Base query
      xml.openElement("base", true);
      this._base.toXML(xml);
      xml.closeElement();
      // Parameters
      xml.openElement("parameters", !this.parameters().isEmpty());
      for (SearchParameter p : parameters()) {
        p.toXML(xml);
      }
      xml.closeElement();
      xml.openElement("sort", this._sort != Sort.RELEVANCE);
      if (this._sort == Sort.RELEVANCE) {
        xml.attribute("by", "relevance");
      } else {
        xml.attribute("by", "fields");
        for (SortField sf : this._sort.getSort()) {
          xml.openElement("sortfield");
          xml.attribute("field", sf.getField());
          xml.attribute("type", sf.getType().toString().toLowerCase());
          xml.attribute("reverse", Boolean.toString(sf.getReverse()));
          xml.closeElement();
        }
      }
      xml.closeElement();
    }
    xml.closeElement();
  }

  /**
   * Returns a string representation of this query for human consumption.
   *
   * @return a string representation of this query.
   */
  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    s.append(this._base.toString());
    if (this.parameters().size() > 0) {
      s.append(" with (");
      boolean first = true;
      for (Map.Entry<SearchParameter, Occur> p : this._parameters.entrySet()) {
        if (!first) {
          s.append(p.getValue() == Occur.MUST ? " and " : p.getValue() == Occur.MUST_NOT ? " and not " : " or ");
        }
        s.append(p.getKey().toString());
        first = false;
      }
      s.append(')');
    }
    return s.toString();
  }

  // private helpers
  // ----------------------------------------------------------------------------------------------

  /**
   * Builds the query for the specified arguments using the base query and
   *
   * @param base       the base query
   * @param parameters the parameters
   *
   * @return the corresponding Lucene Query
   */
  private static Query toQuery(SearchParameter base, Map<SearchParameter, Occur> parameters) {
    // No parameters, just use the base.
    if (parameters.isEmpty()) return base.toQuery();
    // Make an AND of all parameters
    BooleanQuery.Builder query = new BooleanQuery.Builder();
    query.add(base.toQuery(), Occur.MUST);
    for (Map.Entry<SearchParameter, Occur> parameter : parameters.entrySet()) {
      query.add(parameter.getKey().toQuery(), parameter.getValue());
    }
    return query.build();
  }

  // factory methods
  // ----------------------------------------------------------------------------------------------

  /**
   * Builds a basic query using only the specified base query.
   *
   * @param <X> The type of base query
   *
   * @param base  the base query
   *
   * @return the corresponding Lucene Query
   */
  public static <X extends SearchParameter> BasicQuery<X> newBasicQuery(X base) {
    List<SearchParameter> none = Collections.emptyList();
    return new BasicQuery<>(base, none);
  }

  /**
   * Builds a basic query using only the specified base query.
   *
   * @param <X> The type of base query
   *
   * @param base       the base query
   * @param parameters the parameters
   *
   * @return the corresponding Lucene Query
   */
  public static <X extends SearchParameter> BasicQuery<X> newBasicQuery(X base, List<SearchParameter> parameters) {
    return new BasicQuery<>(base, List.copyOf(parameters));
  }

}

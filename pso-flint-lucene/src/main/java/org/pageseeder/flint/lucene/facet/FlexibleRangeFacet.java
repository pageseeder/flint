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
package org.pageseeder.flint.lucene.facet;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.pageseeder.flint.lucene.search.DocumentCounter;
import org.pageseeder.flint.lucene.search.Filter;
import org.pageseeder.flint.lucene.search.Terms;
import org.pageseeder.flint.lucene.util.Beta;
import org.pageseeder.flint.lucene.util.Bucket;
import org.pageseeder.flint.lucene.util.Bucket.Entry;
import org.pageseeder.flint.lucene.util.Dates;
import org.pageseeder.xmlwriter.XMLWriter;

/**
 * A facet implementation using a simple index field.
 *
 * @author Jean-Baptiste Reure
 *
 * @version 5.1.3
 */
@Beta
public abstract class FlexibleRangeFacet extends FlexibleFacet<FlexibleRangeFacet.Range> {

  /**
   * The default number of facet values if not specified.
   */
  public static final int DEFAULT_MAX_NUMBER_OF_VALUES = 10;

  /**
   * The queries used to calculate each facet.
   */
  protected transient Bucket<Range> bucket;

  /**
   * If the facet was computed in a "flexible" way
   */
  protected transient boolean flexible = false;

  /**
   * The total number of ranges containing the field used in this facet
   */
  protected transient int totalRanges = 0;

  /**
   * Creates a new facet with the specified name;
   *
   * @param name     The name of the facet.
   */
  protected FlexibleRangeFacet(String name) {
    super(name);
  }

  /**
   * Computes each facet option as a flexible facet.
   * All filters but the ones using the same field as this facet are applied to the base query before computing the numbers.
   *
   * @param searcher the index search to use.
   * @param base     the base query.
   * @param filters  the filters applied to the base query
   * @param size     the maximum number of field values to compute.
   *
   * @throws IOException if thrown by the searcher.
   */
  public void compute(IndexSearcher searcher, Query base, List<Filter> filters, int size) throws IOException {
    // If the base is null, simply calculate for each query
    if (base == null) {
      compute(searcher, size);
    } else {
      if (size < 0) throw new IllegalArgumentException("size < 0");
      this.totalRanges = 0;
      // find all terms
      List<Term> terms = Terms.terms(searcher.getIndexReader(), this._name);
      // Otherwise, re-compute the query without the corresponding filter
      Query filtered = base;
      if (filters != null) {
        this.flexible = true;
        for (Filter filter : filters) {
          if (!this._name.equals(filter.name()))
            filtered = filter.filterQuery(filtered);
        }
      }
      DocumentCounter counter = new DocumentCounter();
      Map<Range, Integer> ranges = new HashMap<>();
      for (Term t : terms) {
        // find range
        Range r = findRange(t);
        if (r == null) r = OTHER;
        // find count
        BooleanQuery.Builder query = new BooleanQuery.Builder();
        query.add(filtered, Occur.MUST);
        query.add(termToQuery(t), Occur.MUST);
        searcher.search(query.build(), counter);
        int count = counter.getCount();
        if (count > 0) {
          // add to map
          Integer ec = ranges.get(r);
          ranges.put(r, count + (ec == null ? 0 : ec));
        }
        counter.reset();
      }
      this.totalRanges = ranges.size();
      // add to bucket
      Bucket<Range> b = new Bucket<>(size);
      for (Map.Entry<Range, Integer> range : ranges.entrySet()) {
        b.add(range.getKey(), range.getValue());
      }
      this.bucket = b;
    }
  }

  /**
   * Computes each facet option.
   *
   * <p>Same as <code>compute(searcher, base, 10);</code>.
   *
   * <p>Defaults to 10.
   *
   * @see #compute(IndexSearcher, Query, int)
   *
   * @param searcher the index search to use.
   * @param base     the base query.
   *
   * @throws IOException if thrown by the searcher.
   */
  public void compute(IndexSearcher searcher, Query base, int size) throws IOException {
    compute(searcher, base, null, size);
  }

  /**
   * Computes each facet option.
   *
   * <p>Same as <code>compute(searcher, base, 10);</code>.
   *
   * <p>Defaults to 10.
   *
   * @see #compute(IndexSearcher, Query, int)
   *
   * @param searcher the index search to use.
   *
   * @throws IOException if thrown by the searcher.
   */
  public void compute(IndexSearcher searcher) throws IOException {
    compute(searcher, null, null, DEFAULT_MAX_NUMBER_OF_VALUES);
  }

  /**
   * Computes each facet option.
   *
   * <p>Same as <code>compute(searcher, base, 10);</code>.
   *
   * <p>Defaults to 10.
   *
   * @see #compute(IndexSearcher, Query, int)
   *
   * @param searcher the index search to use.
   * @param base     the base query.
   *
   * @throws IOException if thrown by the searcher.
   */
  public void compute(IndexSearcher searcher, Query base) throws IOException {
    compute(searcher, base, null, DEFAULT_MAX_NUMBER_OF_VALUES);
  }

  /**
   * Computes each facet option as a flexible facet.
   *
   * <p>Same as <code>computeFlexible(searcher, base, filters, 10);</code>.
   *
   * <p>Defaults to 10.
   *
   * @see #compute(IndexSearcher, Query, List, int)
   *
   * @param searcher the index search to use.
   * @param base     the base query.
   * @param filters  the filters applied to the base query
   *
   * @throws IOException if thrown by the searcher.
   */
  public void compute(IndexSearcher searcher, Query base, List<Filter> filters) throws IOException {
    compute(searcher, base, filters, DEFAULT_MAX_NUMBER_OF_VALUES);
  }

  /**
   * Computes each facet option without a base query.
   *
   * @param searcher the index search to use.
   * @param size     the number of facet values to calculate.
   *
   * @throws IOException if thrown by the searcher.
   */
  protected void compute(IndexSearcher searcher, int size) throws IOException {
    // find all terms
    List<Term> terms = Terms.terms(searcher.getIndexReader(), this._name);
    DocumentCounter counter = new DocumentCounter();
    Map<Range, Integer> ranges = new HashMap<>();
    for (Term t : terms) {
      // find the range
      Range r = findRange(t);
      if (r == null) r = OTHER;
      // find number
      searcher.search(termToQuery(t), counter);
      int count = counter.getCount();
      if (count > 0) {
        // add to map
        Integer ec = ranges.get(r);
        ranges.put(r, count + (ec == null ? 0 : ec));
      }
      counter.reset();
    }
    // set totals
    this.totalRanges = ranges.size();
    // add to bucket
    Bucket<Range> b = new Bucket<>(size);
    for (Map.Entry<Range, Integer> range : ranges.entrySet()) {
      b.add(range.getKey(), range.getValue());
    }
    this.bucket = b;
  }

  /**
   * Create a query for the term given.
   *
   * @param t the term
   *
   * @return the query
   */
  protected Query termToQuery(Term t) {
    return new TermQuery(t);
  }

  public abstract String getType();

  protected abstract void rangeToXML(Range range, int cardinality, XMLWriter xml) throws IOException;

  protected abstract Range findRange(Term t);

  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("facet", true);
    xml.attribute("name", this._name);
    xml.attribute("type", getType());
    xml.attribute("flexible", String.valueOf(this.flexible));
    if (!this.flexible) {
      xml.attribute("total-ranges", this.totalRanges);
    }
    if (this.bucket != null) {
      for (Entry<Range> e : this.bucket.entrySet()) {
        if (e.item() == OTHER) {
          xml.openElement("remaining-range");
          xml.attribute("cardinality", e.count());
          xml.closeElement();
        } else {
          rangeToXML(e.item(), e.count(), xml);
        }
      }
    }
    xml.closeElement();
  }

  public Bucket<Range> getValues() {
    return this.bucket;
  }

  public int getTotalRanges() {
    return this.totalRanges;
  }

  public static class Range implements Comparable<Range> {
    private final String _min;
    private final String _max;
    private final boolean _includeMin;
    private final boolean _includeMax;
    private final Resolution _resolution;
    private Range(String min, boolean withMin, String max, boolean withMax) {
      this(min, withMin, max, withMax, null);
    }
    private Range(String min, boolean withMin, String max, boolean withMax, Resolution resolution) {
      this._max = max;
      this._min = min;
      this._includeMin = withMin;
      this._includeMax = withMax;
      this._resolution = resolution;
    }
    public String getMin() {
      return this._min;
    }
    public String getMax() {
      return this._max;
    }
    public String getFormattedMin() {
      return this._resolution != null? toDateString(this._min, this._resolution) : this._min;
    }
    public String getFormattedMax() {
      return this._resolution != null? toDateString(this._max, this._resolution) : this._max;
    }
    public boolean includeMax() {
      return this._includeMax;
    }
    public boolean includeMin() {
      return this._includeMin;
    }

    @Override
    public String toString() {
      return (this._includeMin?'[':'{')+this._min+'-'+this._max+(this._includeMax?']':'}');
    }
    @Override
    public boolean equals(Object obj) {
      if (obj instanceof Range) {
        Range r = (Range) obj;
        return ((r._min == null && this._min == null) || (r._min != null && r._min.equals(this._min))) &&
               ((r._max == null && this._max == null) || (r._max != null && r._max.equals(this._max))) &&
               this._includeMin == r._includeMin && this._includeMax == r._includeMax;
      }
      return false;
    }

    @Override
    public int hashCode() {
      return (this._min != null ? this._min.hashCode() * 13 : 13) +
             (this._max != null ? this._max.hashCode() * 11 : 11) +
             (this._includeMin ? 17 : 7) +
             (this._includeMax ? 5  : 3);
    }
    @Override
    public int compareTo(Range o) {
      if (this._min == null) {
        if (o._min != null) return -1;
        if (this._max == null) return -1;
        if (o._max == null) return 1;
        return this._max.compareTo(o._max);
      } else {
        if (o._min == null) return 1;
        return this._min.compareTo(o._min);
      }
    }
    public static Range stringRange(String min, String max) {
      return stringRange(min, true, max, true);
    }
    public static Range stringRange(String min, boolean withMin, String max, boolean withMax) {
      return new Range(min, withMin, max, withMax);
    }
    public static Range numericRange(Number min, Number ma) {
      return numericRange(min, true, ma, true);
    }
    public static Range numericRange(Number min, boolean withMin, Number ma, boolean withMax) {
      return new Range(min == null ? null : min.toString(), withMin, ma == null ? null : ma.toString(), withMax);
    }
    public static Range dateRange(Date min, Date max, Resolution res) {
      return dateRange(min, true, max, true, res);
    }
    public static Range dateRange(Date min, boolean withMin, Date max, boolean withMax, Resolution res) {
      return new Range(Dates.toString(min, res), withMin, Dates.toString(max, res), withMax, res);
    }
  }

  public static boolean isOther(Range range) {
    return range == OTHER;
  }

  public static final Range OTHER = new Range(null, false, null, false);

}

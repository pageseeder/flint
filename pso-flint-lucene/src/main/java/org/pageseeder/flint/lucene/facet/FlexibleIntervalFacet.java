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

import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.pageseeder.flint.lucene.search.DocumentCounter;
import org.pageseeder.flint.lucene.search.Filter;
import org.pageseeder.flint.lucene.search.Terms;
import org.pageseeder.flint.lucene.util.Beta;
import org.pageseeder.flint.lucene.util.Bucket;
import org.pageseeder.flint.lucene.util.Bucket.Entry;
import org.pageseeder.flint.lucene.util.Dates;
import org.pageseeder.xmlwriter.XMLWriter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A facet implementation using a simple index field.
 *
 * @author Jean-Baptiste Reure
 *
 * @version 5.1.3
 */
@Beta
public abstract class FlexibleIntervalFacet extends FlexibleFacet<FlexibleIntervalFacet.Interval> {

  /**
   * The default number of facet values if not specified.
   */
  public static final int DEFAULT_MAX_NUMBER_OF_VALUES = 10;

  /**
   * The point of reference for intervals
   */
  private final String _start;

  /**
   * A max value not to search after
   */
  private final String _end;

  /**
   * If the lower limit of each interval is included
   */
  private final boolean _includeLower;

  /**
   * If the upper limit of the last interval is included
   */
  private final boolean _includeLastUpper;

  /**
   * The max nb of intervals
   */
  protected final int _maxIntervals;

  /**
   * The queries used to calculate each facet.
   */
  protected transient Bucket<Interval> bucket;

  /**
   * The total number of intervals with results
   */
  protected transient int totalIntervals = 0;

  /**
   * Creates a new facet with the specified name;
   *
   * @param name         The name of the facet.
   * @param start        The starting point when computing intervals
   * @param end          An end value not to search past
   * @param maxIntervals The maximum number of intervals to load
   */
  protected FlexibleIntervalFacet(String name, String start, String end,
      boolean includeLower, boolean includeLastUpper, int maxIntervals) {
    super(name);
    this._start = start;
    this._end = end;
    this._includeLower = includeLower;
    this._includeLastUpper = includeLastUpper;
    this._maxIntervals = maxIntervals;
  }

  /**
   * Returns the point of reference for intervals.
   * @return the point of reference for intervals.
   */
  public String start() {
    return this._start;
  }

  /**
   * Returns the point of reference for intervals.
   * @return the point of reference for intervals.
   */
  public String formattedStart() {
    return this._start;
  }

  /**
   * Returns the end value not to search past.
   * @return the end value not to search past.
   */
  public String end() {
    return this._end;
  }

  /**
   * Returns the end value not to search past.
   * @return the end value not to search past.
   */
  public String formattedEnd() {
    return this._end;
  }

  /**
   * Returns <code>true</code> if the lower limit of each interval is included
   * @return <code>true</code> if the lower limit of each interval is included
   */
  public boolean includeLower() {
    return this._includeLower;
  }

  /**
   * Returns <code>true</code> if the upper limit of the last interval is included
   * @return <code>true</code> if the upper limit of the last interval is included
   */
  public boolean includeLastUpper() {
    return this._includeLastUpper;
  }

  /**
   * Computes each facet option as a flexible facet.
   * All filters but the ones using the same field as this facet are applied to the base query before computing the numbers.
   *
   * @param searcher the index search to use.
   * @param base     the base query.
   * @param filters  the filters applied to the base query (ignored if the base query is null)
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
      // reset total terms
      this.totalIntervals = 0;
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
      Map<Interval, Integer> intervals = new HashMap<>();
      DocumentCounter counter = new DocumentCounter();
      for (Term t : terms) {
        // find range
        Interval r = findInterval(t);
        if (r == null) continue;
        // find count
        BooleanQuery.Builder query = new BooleanQuery.Builder();
        query.add(filtered, Occur.MUST);
        query.add(termToQuery(t), Occur.MUST);
        searcher.search(query.build(), counter);
        int count = counter.getCount();
        if (count > 0) {
          // add to map
          Integer ec = intervals.get(r);
          intervals.put(r, count + (ec == null ? 0 : ec));
          // check size to stop computing if too big
          if (this._maxIntervals > 0 && intervals.size() > this._maxIntervals)
            return;
        }
        counter.reset();
      }
      this.totalIntervals = intervals.size();
      // add to bucket
      Bucket<Interval> b = new Bucket<>(size);
      for (Map.Entry<Interval, Integer> interval : intervals.entrySet()) {
        b.add(interval.getKey(), interval.getValue());
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
  private void compute(IndexSearcher searcher, int size) throws IOException {
    // find all terms
    List<Term> terms = Terms.terms(searcher.getIndexReader(), this._name);
    DocumentCounter counter = new DocumentCounter();
    Map<Interval, Integer> intervals = new HashMap<>();
    for (Term t : terms) {
      // find the range
      Interval interval = findInterval(t);
      if (interval == null) continue;
      // find number
      searcher.search(termToQuery(t), counter);
      int count = counter.getCount();
      if (count > 0) {
        // add to map
        Integer ec = intervals.get(interval);
        intervals.put(interval, count + (ec == null ? 0 : ec));
        // check size to stop computing if too big
        if (this._maxIntervals > 0 && intervals.size() > this._maxIntervals)
          return;
      }
      counter.reset();
    }
    // set totals
    this.totalIntervals = intervals.size();
    // add to bucket
    Bucket<Interval> b = new Bucket<>(size);
    for (Map.Entry<Interval, Integer> interval : intervals.entrySet()) {
      b.add(interval.getKey(), interval.getValue());
    }
    this.bucket = b;
  }

  /**
   * Create a query for the term given, using the numeric type if there is one.
   *
   * @param t the term
   *
   * @return the query
   */
  protected abstract Query termToQuery(Term t);

  public abstract String getType();

  protected abstract void intervalToXML(Interval interval, int cardinality, XMLWriter xml) throws IOException;

  protected abstract Interval findInterval(Term t);

  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("facet", true);
    xml.attribute("name", this._name);
    xml.attribute("start", this._start);
    if (this._end != null) xml.attribute("end", this._end);
    xml.attribute("type", getType());
    xml.attribute("flexible", String.valueOf(this.flexible));
    if (!this.flexible) {
      xml.attribute("total-intervals", this.totalIntervals);
    }
    if (this.bucket != null) {
      for (Entry<Interval> e : this.bucket.entrySet()) {
        intervalToXML(e.item(), e.count(), xml);
      }
    }
    xml.closeElement();
  }

  public Bucket<Interval> getValues() {
    return this.bucket;
  }

  public int getTotalIntervals() {
    return this.totalIntervals;
  }

  public static class Interval implements Comparable<Interval> {
    private final String _min;
    private final String max;
    private final boolean _includeMin;
    private final boolean _includeMax;
    private final Resolution _resolution;
    private Interval(String min, boolean withMin, String max, boolean withMax) {
      this(min, withMin, max, withMax, null);
    }
    private Interval(String min, boolean withMin, String max, boolean withMax, Resolution resolution) {
      this.max = max;
      this._min = min;
      this._includeMin = withMin;
      this._includeMax = withMax;
      this._resolution = resolution;
    }
    public String getMin() {
      return this._min;
    }
    public String getMax() {
      return this.max;
    }

    public String getFormattedMin() {
      return this._resolution != null? toDateString(this._min, this._resolution) : this._min;
    }
    public String getFormattedMax() {
      return this._resolution != null? toDateString(this.max, this._resolution) : this.max;
    }

    public boolean includeMax() {
      return this._includeMax;
    }
    public boolean includeMin() {
      return this._includeMin;
    }

    @Override
    public String toString() {
      return (this._includeMin ?'[':'{')+this._min +'-'+this.max+(this._includeMax ?']':'}');
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof Interval) {
        Interval r = (Interval) obj;
        return ((r._min == null && this._min == null) || (r._min != null && r._min.equals(this._min))) &&
               ((r.max == null && this.max == null) || (r.max != null && r.max.equals(this.max))) &&
               this._includeMin == r._includeMin && this._includeMax == r._includeMax;
      }
      return false;
    }
    @Override
    public int hashCode() {
      return (this._min != null ? this._min.hashCode() * 13 : 13) +
             (this.max != null ? this.max.hashCode() * 11 : 11) +
             (this._includeMin ? 17 : 7) +
             (this._includeMax ? 5  : 3);
    }
    @Override
    public int compareTo(Interval o) {
      if (this._min == null) {
        if (o._min != null) return -1;
        if (this.max == null) return -1;
        if (o.max == null) return 1;
        return this.max.compareTo(o.max);
      } else {
        if (o._min == null) return 1;
        return this._min.compareTo(o._min);
      }
    }
    public static Interval stringInterval(String mi, String ma) {
      return stringInterval(mi, true, ma, false);
    }
    public static Interval stringInterval(String mi, boolean withMin, String ma, boolean withMax) {
      return new Interval(mi, withMin, ma, withMax);
    }
    public static Interval numericInterval(Number mi, Number ma) {
      return numericInterval(mi, true, ma, false);
    }
    public static Interval numericInterval(Number mi, boolean withMin, Number ma, boolean withMax) {
      return new Interval(mi == null ? null : mi.toString(), withMin, ma == null ? null : ma.toString(), withMax);
    }
    public static Interval dateInterval(Date mi, Date ma, Resolution res) {
      return dateInterval(mi, true, ma, false, res);
    }
    public static Interval dateInterval(Date mi, boolean withMin, Date ma, boolean withMax, Resolution res) {
      return new Interval(Dates.toString(mi, res), withMin, Dates.toString(ma, res), withMax, res);
    }
    public static Interval dateInterval(OffsetDateTime mi, OffsetDateTime ma, Resolution res) {
      return dateInterval(mi, true, ma, false, res);
    }
    public static Interval dateInterval(OffsetDateTime mi, boolean withMin, OffsetDateTime ma, boolean withMax, Resolution res) {
      return new Interval(Dates.toString(mi, res), withMin, Dates.toString(ma, res), withMax, res);
    }
  }

}

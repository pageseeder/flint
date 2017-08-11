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
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.pageseeder.flint.indexing.FlintField.NumericType;
import org.pageseeder.flint.lucene.query.NumericRange;
import org.pageseeder.flint.lucene.search.DocumentCounter;
import org.pageseeder.flint.lucene.search.Filter;
import org.pageseeder.flint.lucene.util.Beta;
import org.pageseeder.flint.lucene.util.Bucket;
import org.pageseeder.xmlwriter.XMLWriter;

/**
 * A facet implementation using a simple index field.
 *
 * @author Jean-Baptiste Reure
 * @version 14 July 2017
 */
@Beta
public abstract class NumericIntervalFacet extends FlexibleIntervalFacet {

  /**
   * The length of an interval
   */
  protected final Number _interval;

  /**
   * The end date
   */
  protected final List<Interval> _intervals = new ArrayList<>();

  /**
   * Creates a new facet with the specified name;
   *
   * @param name     The name of the facet.
   * @param maxterms The maximum number of terms to return
   */
  private NumericIntervalFacet(String name, Number start, Number end, Number interval, boolean includeMin, boolean includeLastMax, int maxIntervals) {
    super(name, String.valueOf(start), String.valueOf(end), includeMin, includeLastMax, maxIntervals);
    this._interval = interval;
    if (start.equals(end) || end.equals(min(start, end)))
      throw new IllegalArgumentException("start must be less than end");
    // compute intervals
    for (Number i = start; i.equals(min(i, end)); i = increaseByInterval(i)) {
      boolean withMax = increaseByInterval(i).equals(end) ? includeLastUpper() : !includeLower();
      Number max = increaseByInterval(i);
      this._intervals.add(Interval.numericInterval(i, includeLower(), min(max, end), withMax));
    }
  }

  protected abstract Number increaseByInterval(Number toIncrease);

  protected abstract Number min(Number first, Number second);

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
      // Otherwise, re-compute the query without the corresponding filter 
      Query filtered = base;
      if (filters != null) {
        this.flexible = true;
        for (Filter filter : filters) {
          if (!name().equals(filter.name()))
            filtered = filter.filterQuery(filtered);
        }
      }
      this.totalIntervals = 0;
      Bucket<Interval> bucket = new Bucket<Interval>(size);
      DocumentCounter counter = new DocumentCounter();
      for (Interval i : this._intervals) {
        // build query
        BooleanQuery query = new BooleanQuery();
        query.add(filtered, Occur.MUST);
        query.add(intervalToQuery(i), Occur.MUST);
        searcher.search(query, counter);
        int count = counter.getCount();
        // add to bucket
        bucket.add(i, count);
        counter.reset();
        if (count > 0) this.totalIntervals++;
      }
      this._bucket = bucket;
    }
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
    this.totalIntervals = 0;
    Bucket<Interval> bucket = new Bucket<Interval>(size);
    DocumentCounter counter = new DocumentCounter();
    for (Interval i : this._intervals) {
      // build query
      searcher.search(intervalToQuery(i), counter);
      int count = counter.getCount();
      // add to bucket
      bucket.add(i, count);
      counter.reset();
      if (count > 0) this.totalIntervals++;
    }
    this._bucket = bucket;
  }

  protected abstract Query intervalToQuery(Interval i);

  @Override
  protected Interval findInterval(Term t) { return null; }

  @Override
  protected Query termToQuery(Term t) { return null; }

  @Override
  protected String getType() {
    return "numeric-interval";
  }

  @Override
  protected void intervalToXML(Interval interval, int cardinality, XMLWriter xml) throws IOException {
    xml.openElement("interval");
    if (interval.getMin() != null) xml.attribute("min", interval.getMin());
    if (interval.getMax() != null) xml.attribute("max", interval.getMax());
    if (interval.getMin() != null) xml.attribute("include-min", interval.includeMin() ? "true" : "false");
    if (interval.getMax() != null) xml.attribute("include-max", interval.includeMax() ? "true" : "false");
    xml.attribute("cardinality", cardinality);
    xml.closeElement();
  }

  private static class IntIntervalFacet extends NumericIntervalFacet {
    public IntIntervalFacet(String name, int start, int end, int interval, boolean includeMin, boolean includeLastMax, int maxIntervals) {
      super(name, start, end, interval, includeMin, includeLastMax, maxIntervals);
    }
    @Override
    protected Query intervalToQuery(Interval i) {
      return NumericRange.newIntRange(name(),
          i.getMin() == null ? null : Integer.parseInt(i.getMin()),
          i.getMax() == null ? null : Integer.parseInt(i.getMax()),
          i.includeMin(), i.includeMax()).toQuery();
    }
    @Override
    protected Number increaseByInterval(Number toIncrease) {
      return toIncrease.intValue() + this._interval.intValue();
    }
    @Override
    protected Number min(Number first, Number second) {
      return Math.min(first.intValue(), second.intValue());
    }
  }
  private static class FloatIntervalFacet extends NumericIntervalFacet {
    public FloatIntervalFacet(String name, float start, float end, float interval, boolean includeMin, boolean includeLastMax, int maxIntervals) {
      super(name, start, end, interval, includeMin, includeLastMax, maxIntervals);
    }
    @Override
    protected Query intervalToQuery(Interval i) {
      return NumericRange.newFloatRange(name(),
          i.getMin() == null ? null : Float.parseFloat(i.getMin()),
          i.getMax() == null ? null : Float.parseFloat(i.getMax()),
          i.includeMin(), i.includeMax()).toQuery();
    }
    @Override
    protected Number increaseByInterval(Number toIncrease) {
      return toIncrease.floatValue() + this._interval.floatValue();
    }
    @Override
    protected Number min(Number first, Number second) {
      return Math.min(first.floatValue(), second.floatValue());
    }
  }
  private static class DoubleIntervalFacet extends NumericIntervalFacet {
    public DoubleIntervalFacet(String name, double start, double end, double interval, boolean includeMin, boolean includeLastMax, int maxIntervals) {
      super(name, start, end, interval, includeMin, includeLastMax, maxIntervals);
    }
    @Override
    protected Query intervalToQuery(Interval i) {
      return NumericRange.newDoubleRange(name(),
          i.getMin() == null ? null : Double.parseDouble(i.getMin()),
          i.getMax() == null ? null : Double.parseDouble(i.getMax()),
          i.includeMin(), i.includeMax()).toQuery();
    }
    @Override
    protected Number increaseByInterval(Number toIncrease) {
      return toIncrease.doubleValue() + this._interval.doubleValue();
    }
    @Override
    protected Number min(Number first, Number second) {
      return Math.min(first.doubleValue(), second.doubleValue());
    }
  }
  private static class LongIntervalFacet extends NumericIntervalFacet {
    public LongIntervalFacet(String name, long start, long end, long interval, boolean includeMin, boolean includeLastMax, int maxIntervals) {
      super(name, start, end, interval, includeMin, includeLastMax, maxIntervals);
    }
    @Override
    protected Query intervalToQuery(Interval i) {
      return NumericRange.newLongRange(name(),
          i.getMin() == null ? null : Long.parseLong(i.getMin()),
          i.getMax() == null ? null : Long.parseLong(i.getMax()),
          i.includeMin(), i.includeMax()).toQuery();
    }
    @Override
    protected Number increaseByInterval(Number toIncrease) {
      return toIncrease.longValue() + this._interval.longValue();
    }
    @Override
    protected Number min(Number first, Number second) {
      return Math.min(first.longValue(), second.longValue());
    }
  }
  // Builder ------------------------------------------------------------------------------------------
  public static class Builder {

    private NumericType numeric = null;

    private boolean includeMin = true;

    private boolean includeLastMax = true;

    private Number interval = null;

    private Number start = null;

    private Number end = null;

    private String name = null;

    private int maxIntervals = -1;

    public Builder numeric(NumericType num) {
      this.numeric = num;
      return this;
    }

    public Builder name(String n) {
      this.name = n;
      return this;
    }

    public Builder includeLastMax(boolean include) {
      this.includeLastMax = include;
      return this;
    }

    public Builder includeMin(boolean include) {
      this.includeMin = include;
      return this;
    }

    public Builder maxIntervals(int max) {
      this.maxIntervals = max;
      return this;
    }

    public Builder start(Number s) {
      this.start = s;
      return this;
    }

    public Builder end(Number e) {
      this.end = e;
      return this;
    }

    public Builder intervalLength(Number i) {
      this.interval = i;
      return this;
    }

    public NumericIntervalFacet build() {
      if (this.name == null) throw new NullPointerException("Must have a field name");
      if (this.start == null) throw new NullPointerException("Must have a start");
      if (this.end == null) throw new NullPointerException("Must have an end");
      if (this.interval == null || this.interval.intValue() <= 0) throw new NullPointerException("Must have an interval");
      if (this.numeric == null) throw new NullPointerException("Must have a numeric type");
      switch (this.numeric) {
        case INT:    return new IntIntervalFacet(name, start.intValue(), end.intValue(), interval.intValue(), includeMin, includeLastMax, maxIntervals);
        case DOUBLE: return new DoubleIntervalFacet(name, start.doubleValue(), end.doubleValue(), interval.doubleValue(), includeMin, includeLastMax, maxIntervals);
        case FLOAT:  return new FloatIntervalFacet(name, start.floatValue(), end.floatValue(), interval.floatValue(), includeMin, includeLastMax, maxIntervals);
        case LONG:   return new LongIntervalFacet(name, start.longValue(), end.longValue(), interval.longValue(), includeMin, includeLastMax, maxIntervals);
      }
      return null;
    }
  }
}

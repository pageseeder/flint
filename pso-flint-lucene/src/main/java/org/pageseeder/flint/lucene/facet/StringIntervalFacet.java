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

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.pageseeder.flint.lucene.util.Beta;
import org.pageseeder.xmlwriter.XMLWriter;

/**
 * A facet implementation using a simple index field.
 *
 * @author Jean-Baptiste Reure
 *
 * @version 5.1.3
 */
@Beta
public class StringIntervalFacet extends FlexibleIntervalFacet {

  /**
   * The length of an interval.
   */
  private final int _intervalLength;

  /**
   * If the string comparison is case sensitive.
   */
  private final boolean _caseSensitive;

  /**
   * Creates a new facet with the specified name;
   *
   * @param name  The name of the facet.
   * @param start The start of the interval
   * @param end   The end of the interval
   * @param maxIntervals The maximum number of terms to return
   */
  private StringIntervalFacet(String name, String start, String end, int maxIntervals,
      boolean caseSensitive, int length, boolean includeMin, boolean includeLastMax) {
    super(name, start, end, includeMin, includeLastMax, maxIntervals);
    this._intervalLength = length;
    this._caseSensitive = caseSensitive;
  }

  /**
   * Create a query for the term given, using the numeric type if there is one.
   * 
   * @param t the term
   * 
   * @return the query
   */
  @Override
  protected Query termToQuery(Term t) {
    return new TermQuery(t);
  }

  @Override
  public String getType() {
    return "string-interval";
  }

  @Override
  protected Interval findInterval(Term t) {
    if (t == null) return null;
    String maxTerm = end();
    String minTerm = end() == null ? null : start();
    String termValue = t.text();
    if (termValue.isEmpty()) return null;
    // go forwards or backwards?
    boolean forward = isAfter(termValue, start(), includeLower());
    String from = start();
    while (true) {
      String to = next(from, this._intervalLength, minTerm, maxTerm, forward);
      String lower = forward ? from : to;
      String upper = forward ? to   : from;
      boolean includeMax = maxTerm != null && to.equals(next(to, this._intervalLength, minTerm, maxTerm, forward)) ? includeLastUpper() : !includeLower();
      // make sure we're still within limits
      if (maxTerm != null) {
        if (isAfter(lower, maxTerm, true) || isBefore(upper, minTerm, true))
          return null;
      }
      boolean lowerLimit = isAfter(termValue, lower, includeLower());
      boolean upperLimit = isBefore(termValue, upper, includeMax);
      if (lowerLimit && upperLimit) {
        return Interval.stringInterval(lower, includeLower(), upper, includeMax);
      }
      // safety check
      if (forward  && upperLimit) return null;
      if (!forward && lowerLimit) return null;
      // not in any interval that can be computed with ascii characters
      if (from.equals(to)) return null;
      from = to;
    }
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

  private boolean isAfter(String s1, String s2, boolean includeEquals) {
    int comparison = (this._caseSensitive ? s1.compareTo(s2) : s1.compareToIgnoreCase(s2));
    return comparison > 0 || (includeEquals && comparison == 0);
  }

  private boolean isBefore(String s1, String s2, boolean includeEquals) {
    int comparison = (this._caseSensitive ? s1.compareTo(s2) : s1.compareToIgnoreCase(s2));
    return comparison < 0 || (includeEquals && comparison == 0);
  }

  private static String next(String s, int interval, String min, String max, boolean increase) {
    if (s.isEmpty()) return s;
    char nextFirstChar = (char) (s.charAt(0) + ((increase ? 1 : -1) * interval));
    // deal with edges: .. - 0 - 9 - ... - A - Z - ... - a - z - ...
    if (nextFirstChar < '0' && min != null) return min;
    else if (nextFirstChar < '0') nextFirstChar = '0' - 1;
    else if (nextFirstChar > '9' && nextFirstChar < 'A') nextFirstChar = increase ? 'A' : '9';
    else if (nextFirstChar > 'Z' && nextFirstChar < 'a') nextFirstChar = increase ? 'a' : 'Z';
    else if (nextFirstChar > 'z' && max != null) return max;
    else if (nextFirstChar > 'z') nextFirstChar = 'z' + 1;
    else if (min != null && String.valueOf(nextFirstChar).compareTo(min) < 0) return min;
    else if (max != null && String.valueOf(nextFirstChar).compareTo(max) > 0) return max;
    return nextFirstChar + s.substring(1);
  }

  // Builder
  // ------------------------------------------------------------------------------------------

  public static class Builder {

    private int intervalLength = -1;

    private String start = null;

    private String end = null;

    private boolean includeMin = true;

    private boolean includeLastMax = true;

    private boolean caseSensitive = true;

    private String name = null;

    private int maxIntervals = -1;

    public Builder caseSensitive(boolean cs) {
      this.caseSensitive = cs;
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

    public Builder intervalLength(int length) {
      this.intervalLength = length;
      return this;
    }

    public Builder start(String thestart) {
      this.start = thestart;
      return this;
    }

    public Builder end(String theend) {
      this.end = theend;
      return this;
    }

    public Builder maxIntervals(int max) {
      this.maxIntervals = max;
      return this;
    }

    public StringIntervalFacet build() {
      if (this.name == null) throw new IllegalStateException("Must have a field name");
      if (this.start == null) throw new IllegalStateException("Must have a start");
      if (this.intervalLength <= 0) throw new IllegalStateException("Must have a valid interval length");
      return new StringIntervalFacet(this.name, this.start, this.end,
          this.maxIntervals, this.caseSensitive, this.intervalLength, this.includeMin, this.includeLastMax);
    }
  }

}

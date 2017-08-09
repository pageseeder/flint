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
 * @version 14 July 2017
 */
@Beta
public class StringIntervalFacet extends FlexibleIntervalFacet {

  /**
   * The length of an interval
   */
  private final String _intervalLength;

  /**
   * If the lower limit of the interval is included
   */
  private final boolean _includeMin;

  /**
   * If the upper limit of the interval is included
   */
  private final boolean _includeMax;

  /**
   * If the string comparison is case sensitive
   */
  private final boolean _caseSensitive;

  /**
   * Creates a new facet with the specified name;
   *
   * @param name     The name of the facet.
   * @param maxterms The maximum number of terms to return
   */
  private StringIntervalFacet(String name, String start, int maxIntervals, boolean caseSensitive, String length, boolean includeMin, boolean includeMax) {
    super(name, start, maxIntervals);
    this._intervalLength = length;
    this._includeMin = includeMin;
    this._includeMax = includeMax;
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
  protected String getType() {
    return "string-interval";
  }

  @Override
  protected Interval findInterval(Term t) {
    if (t == null) return null;
    String termValue = t.text();
    if (termValue.isEmpty()) return null;
    // go forwards or backwards?
    boolean forward = isAfter(termValue, start());
    String from = start();
    while (true) {
      String to = next(from, this._intervalLength, forward);
      String lower = forward ? from : to;
      String upper = forward ? to   : from;
      boolean lowerLimit = isAfter(termValue, lower);
      boolean upperLimit = isBefore(termValue, upper);
      if (lowerLimit && upperLimit) {
        return Interval.stringInterval(lower, this._includeMin, upper, this._includeMax);
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

  private boolean isAfter(String s1, String s2) {
    int comparison = (this._caseSensitive ? s1.compareTo(s2) : s1.compareToIgnoreCase(s2));
    return comparison > 0 || (this._includeMin && comparison == 0); 
  }

  private boolean isBefore(String s1, String s2) {
    int comparison = (this._caseSensitive ? s1.compareTo(s2) : s1.compareToIgnoreCase(s2));
    return comparison < 0 || (this._includeMax && comparison == 0); 
  }

  private static String next(String s, String interval, boolean increase) {
    StringBuilder increased = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (i < interval.length()) {
        char ic = interval.toLowerCase().charAt(i);
        c += (ic - 'a' + 1) * (increase ? 1 : -1);
        // deal with edges: .. - 0 - 9 - ... - A - Z - ... - a - z - ...
        if (c < '0') c = ' ';
        if (c > '9' && c < 'A') c = increase ? 'A' : '9';
        if (c > 'Z' && c < 'a') c = increase ? 'a' : 'Z';
        if (c > 'z') c = '~';
      }
      increased.append(c);
    }
    return increased.toString();
  }

  // Builder ------------------------------------------------------------------------------------------
  public static class Builder {

    private String intervalLength = null;

    private String start = null;

    private boolean includeMin = true;

    private boolean includeMax = false;

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

    public Builder includeMax(boolean include) {
      this.includeMax = include;
      return this;
    }

    public Builder includeMin(boolean include) {
      this.includeMin = include;
      return this;
    }

    public Builder intervalLength(String length) {
      this.intervalLength = length;
      return this;
    }

    public Builder start(String thestart) {
      this.start = thestart;
      return this;
    }

    public Builder maxIntervals(int max) {
      this.maxIntervals = max;
      return this;
    }

    public StringIntervalFacet build() {
      if (this.name == null) throw new NullPointerException("Must have a field name");
      if (this.start == null) throw new NullPointerException("Must have a start date");
      if (this.intervalLength == null) throw new NullPointerException("Must have an interval length");
      return new StringIntervalFacet(this.name, this.start, this.maxIntervals, this.caseSensitive, this.intervalLength, this.includeMin, this.includeMax);
    }
  }

}

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
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.pageseeder.flint.lucene.query.DateParameter;
import org.pageseeder.flint.lucene.util.Beta;
import org.pageseeder.flint.lucene.util.Dates;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A facet implementation using a simple index field.
 *
 * @author Jean-Baptiste Reure
 * @version 5.1.3
 */
@Beta
public class DateIntervalFacet extends FlexibleIntervalFacet {

  private final static Logger LOGGER = LoggerFactory.getLogger(DateIntervalFacet.class);

  /**
   * If this facet is a date
   */
  private final Resolution _resolution;

  /**
   * The length of an interval
   */
  private final Period _intervalDate;

  /**
   * The length of an interval
   */
  private final Duration _intervalTime;

  /**
   * The start date
   */
  private final OffsetDateTime _start;

  /**
   * The end date
   */
  private final OffsetDateTime _end;

  /**
   * Creates a new facet with the specified name;
   *
   * @param name     The name of the facet.
   */
  private DateIntervalFacet(String name, OffsetDateTime start, OffsetDateTime end, int maxIntervals, Resolution resolution, Period p, Duration d, boolean includeMin, boolean includeLastMax) {
    super(name, Dates.toString(start, resolution), Dates.toString(end, resolution), includeMin, includeLastMax, maxIntervals);
    this._start = start;
    this._end = end;
    this._resolution = resolution;
    this._intervalDate = p;
    this._intervalTime = d;
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
    try {
      // handle potential empty string
      if (t.text().isEmpty()) return new TermQuery(t);
      Date d = DateTools.stringToDate(t.text());
      return new DateParameter(this.name(), d, this._resolution, false).toQuery();
    } catch (ParseException ex) {
      LOGGER.warn("Ignoring invalid facet date {} for field {}", t.text(), this.name(), ex);
    }
    return new TermQuery(t);
  }

  @Override
  public String getType() {
    return "date-interval";
  }

  @Override
  protected Interval findInterval(Term t) {
    if (t == null) return null;
    Date d;
    try {
      d = DateTools.stringToDate(t.text());
    } catch (ParseException ex) {
      LOGGER.warn("Ignoring invalid facet date {} for field {}", t.text(), this.name(), ex);
      return null;
    }
    return findInterval(OffsetDateTime.ofInstant(Instant.ofEpochMilli(d.getTime()), ZoneId.of("GMT")));
  }

  private Interval findInterval(OffsetDateTime date) {
    // go forwards or backwards?
    boolean forward = date.isAfter(this._start);
    OffsetDateTime from = this._start;
    while (true) {
      OffsetDateTime to = next(from, forward);
      OffsetDateTime lower = forward ? from : to;
      OffsetDateTime upper = forward ? to : from;
      // make sure we're still within limits
      if (this._end != null) {
        if (lower.isAfter(this._end)    || lower.equals(this._end) ||
            upper.isBefore(this._start) || upper.equals(this._start))
          return null;
      }
      boolean includeMax = this._end != null && to.equals(next(to, forward)) ? includeLastUpper() : !includeLower();
      boolean lowerLimit = date.isAfter(lower)  || (includeLower() && date.equals(lower));
      boolean upperLimit = date.isBefore(upper) || (includeMax     && date.equals(upper));
      if (lowerLimit && upperLimit) {
        return Interval.dateInterval(new Date(lower.toEpochSecond() * 1000), includeLower(), new Date(upper.toEpochSecond() * 1000), includeMax, this._resolution);
      }
      // safety checks
      if (forward  && upperLimit) return null;
      if (!forward && lowerLimit) return null;
      if (from.equals(to)) return null;
      from = to;
    }
  }

  private OffsetDateTime next(OffsetDateTime from, boolean forward) {
    OffsetDateTime to;
    if (forward) {
      to = this._intervalDate == null ? from : from.plus(this._intervalDate);
      to = this._intervalTime == null ? to : to.plus(this._intervalTime);
      if (this._end != null && to.isAfter(this._end)) to = this._end;
    } else {
      to = this._intervalDate == null ? from : from.minus(this._intervalDate);
      to = this._intervalTime == null ? to : to.minus(this._intervalTime);
      if (this._end != null && to.isBefore(this._start)) to = this._start;
    }
    return to;
  }

  @Override
  protected void intervalToXML(Interval interval, int cardinality, XMLWriter xml) throws IOException {
    xml.openElement("interval");
    if (interval.getMin() != null) try {
      xml.attribute("min", Dates.format(DateTools.stringToDate(interval.getMin()), this._resolution));
    } catch (ParseException ex) {
      // should not happen as the string is coming from the date formatter in the first place
    }
    if (interval.getMax() != null) try {
      xml.attribute("max", Dates.format(DateTools.stringToDate(interval.getMax()), this._resolution));
    } catch (ParseException ex) {
      // should not happen as the string is coming from the date formatter in the first place
    }
    if (interval.getMin() != null) xml.attribute("include-min", interval.includeMin() ? "true" : "false");
    if (interval.getMax() != null) xml.attribute("include-max", interval.includeMax() ? "true" : "false");
    xml.attribute("cardinality", cardinality);
    xml.closeElement();
  }

  // Builder ------------------------------------------------------------------------------------------
  public static class Builder {

    private OffsetDateTime start = null;

    private OffsetDateTime end = null;

    private boolean includeMin = true;

    private boolean includeLastMax = true;

    private Period intervalDate = null;

    private Duration intervalTime = null;

    private Resolution resolution = null;

    private String name = null;

    private int maxIntervals = -1;

    public Builder resolution(Resolution res) {
      this.resolution = res;
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

    public Builder intervalDate(Period p) {
      this.intervalDate = p;
      return this;
    }

    public Builder intervalTime(Duration d) {
      this.intervalTime = d;
      return this;
    }

    public Builder maxIntervals(int max) {
      this.maxIntervals = max;
      return this;
    }

    public Builder start(Date startDate) {
      this.start = OffsetDateTime.ofInstant(Instant.ofEpochMilli(startDate.getTime()), ZoneId.of("GMT"));
      return this;
    }

    public Builder end(OffsetDateTime endDate) {
      this.end = endDate;
      return this;
    }

    public Builder end(Date endDate) {
      this.end = OffsetDateTime.ofInstant(Instant.ofEpochMilli(endDate.getTime()), ZoneId.of("GMT"));
      return this;
    }

    public Builder start(OffsetDateTime startDate) {
      this.start = startDate;
      return this;
    }

    public DateIntervalFacet build() {
      if (this.name == null) throw new IllegalStateException("Must have a field name");
      if (this.resolution == null) throw new IllegalStateException("Must have a resolution");
      if (this.start == null) throw new IllegalStateException("Must have a start date");
      if (this.intervalTime == null && this.intervalDate == null) throw new IllegalStateException("Must have a valid interval (period or duration)");
      return new DateIntervalFacet(this.name, this.start, this.end, this.maxIntervals, this.resolution, this.intervalDate, this.intervalTime, this.includeMin, this.includeLastMax);
    }
  }

}

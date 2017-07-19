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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.pageseeder.flint.lucene.query.DateParameter;
import org.pageseeder.flint.lucene.util.Beta;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A facet implementation using a simple index field.
 *
 * @author Jean-Baptiste Reure
 * @version 14 July 2017
 */
@Beta
public class DateRangeFacet extends FlexibleRangeFacet {

  private final static Logger LOGGER = LoggerFactory.getLogger(DateRangeFacet.class);

  /**
   * If this facet is a date
   */
  private final Resolution _resolution;

  private final List<Range> _ranges = new ArrayList<>();

  /**
   * Creates a new facet with the specified name;
   *
   * @param name     The name of the facet.
   * @param maxterms The maximum number of terms to return
   */
  private DateRangeFacet(String name, Resolution resolution, List<Range> ranges) {
    super(name);
    this._resolution = resolution;
    this._ranges.addAll(ranges);
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
      Date d = DateTools.stringToDate(t.text());
      return new DateParameter(this.name(), d, this._resolution, false).toQuery();
    } catch (ParseException ex) {
      LOGGER.warn("Ignoring invalid facet date {} for field {}", t.text(), this.name(), ex);
    }
    return new TermQuery(t);
  }

  /**
   * Create a query for the term given, using the numeric type if there is one.
   * 
   * @param t the term
   * 
   * @return the query
   */
  @Override
  protected String termToText(Term t) {
    return t.text();
  }

  @Override
  protected String getType() {
    return "date-range";
  }

  @Override
  protected Range findRange(Term t) {
    if (t == null) return null;
    Date d;
    try {
      d = DateTools.stringToDate(t.text());
    } catch (ParseException ex) {
      LOGGER.warn("Ignoring invalid facet date {} for field {}", t.text(), this.name(), ex);
      return null;
    }
    for (Range r : this._ranges) {
      boolean passMin = r.getMin() == null;
      if (!passMin) {
        Date m;
        try {
          m = DateTools.stringToDate(r.getMin());
          passMin = m.before(d) || (r.includeMin() && m.equals(d));
        } catch (ParseException ex) {
          LOGGER.warn("Ignoring invalid facet range date {} for field {}", r.getMin(), this.name(), ex);
        }
      }
      boolean passMax = r.getMax() == null;
      if (!passMax) {
        Date m;
        try {
          m = DateTools.stringToDate(r.getMax());
          passMax = d.before(m) || (r.includeMax() && m.equals(d));
        } catch (ParseException ex) {
          LOGGER.warn("Ignoring invalid facet range date {} for field {}", r.getMax(), this.name(), ex);
        }
      }
      if (passMin && passMax) return r;
    }
    return null;
  }

  @Override
  protected void rangeToXML(Range range, int cardinality, XMLWriter xml) throws IOException {
    xml.openElement("range");
    if (range.getMin() != null) try {
      Calendar cal = java.util.Calendar.getInstance();
      cal.setTime(DateTools.stringToDate(range.getMin()));
      xml.attribute("min", DatatypeConverter.printDateTime(cal));
    } catch (ParseException ex) {
      // should not happen as the string is coming from the date formatter in the first place
    }
    if (range.getMax() != null) try {
      Calendar cal = java.util.Calendar.getInstance();
      cal.setTime(DateTools.stringToDate(range.getMax()));
      xml.attribute("max", DatatypeConverter.printDateTime(cal));
    } catch (ParseException ex) {
      // should not happen as the string is coming from the date formatter in the first place
    }
    xml.attribute("cardinality", cardinality);
    xml.closeElement();
  }

  // Builder ------------------------------------------------------------------------------------------
  public static class Builder {

    private final List<DateRange> dateranges = new ArrayList<>();

    private final List<Range> ranges = new ArrayList<>();

    private Resolution resolution = null;

    private String name = null;

    public Builder resolution(Resolution res) {
      this.resolution = res;
      return this;
    }

    public Builder name(String n) {
      this.name = n;
      return this;
    }

    public Builder addRange(Date min, boolean withMin, Date max, boolean withMax) {
      DateRange dr = new DateRange();
      dr.min = min;
      dr.max = max;
      dr.withMax = withMax;
      dr.withMin = withMin;
      this.dateranges.add(dr);
      return this;
    }

    /**
     * Will include min and max.
     * @param min
     * @param max
     * @return
     */
    public Builder addRange(Date min, Date max) {
      return addRange(min, true, max, true);
    }

    public Builder addRange(Range range) {
      this.ranges.add(range);
      return this;
    }

    public DateRangeFacet build() {
      if (this.name == null) throw new NullPointerException("Must have a field name");
      if (this.resolution == null) throw new NullPointerException("Must have a resolution");
      for (DateRange dr : this.dateranges) {
        this.ranges.add(Range.dateRange(dr.min, dr.withMin, dr.max, dr.withMax, this.resolution));
      }
      return new DateRangeFacet(this.name, this.resolution, ranges);
    }
  }
  private static class DateRange {
    private Date min;
    private Date max;
    private boolean withMin;
    private boolean withMax;
  }
}

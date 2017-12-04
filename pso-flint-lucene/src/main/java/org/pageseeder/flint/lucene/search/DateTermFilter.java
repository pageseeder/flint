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
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.pageseeder.flint.lucene.query.DateParameter;
import org.pageseeder.flint.lucene.query.Queries;
import org.pageseeder.flint.lucene.util.Beta;
import org.pageseeder.flint.lucene.util.Dates;
import org.pageseeder.xmlwriter.XMLWriter;

/**
 * A facet implementation using a simple index field.
 *
 * @author Christophe Lauret
 *
 * @version 5.1.3
 */
@Beta
public class DateTermFilter extends TermFilter<OffsetDateTime> implements Filter {

  /**
   * If this facet is a date
   */
  private final Resolution _resolution;

  /**
   * Creates a new filter from the builder.
   */
  private DateTermFilter(Builder builder) {
    super(builder._name, builder._dates);
    this._resolution = builder._resolution;
  }

  public Resolution getResolution() {
    return this._resolution;
  }

  @Override
  public Query filterQuery(Query base) {
    BooleanQuery filterQuery = new BooleanQuery();
    for (OffsetDateTime date : this._terms.keySet()) {
      Occur clause = this._terms.get(date);
      filterQuery.add(new DateParameter(this._name, date, this._resolution, false).toQuery(), clause);
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
    xml.attribute("type", "date");
    for (OffsetDateTime date : this._terms.keySet()) {
      xml.openElement("term");
      xml.attribute("text", Dates.toString(date, this._resolution));
      xml.attribute("date", Dates.format(date, this._resolution));
      xml.attribute("occur", occurToString(this._terms.get(date)));
      xml.closeElement();
    }
    xml.closeElement();
  }

  public static DateTermFilter newFilter(String name, Date date, Resolution res) {
    return newFilter(name, date, res, Occur.MUST);
  }

  public static DateTermFilter newFilter(String name, Date date, Resolution res, Occur occur) {
    return new Builder().name(name).resolution(res).addDate(date, occur).build();
  }

  public static DateTermFilter newFilter(String name, OffsetDateTime date, Resolution res) {
    return newFilter(name, date, res, Occur.MUST);
  }

  public static DateTermFilter newFilter(String name, OffsetDateTime date, Resolution res, Occur occur) {
    return new Builder().name(name).resolution(res).addDate(date, occur).build();
  }

  public static class Builder {

    /**
     * The name of the field
     */
    private String _name = null;

    /**
     * A numeric type
     */
    private Resolution _resolution = null;

    /**
     * The list of terms to filter with
     */
    private final Map<OffsetDateTime, Occur> _dates = new HashMap<>();

    public Builder name(String name) {
      this._name = name;
      return this;
    }

    public Builder resolution(Resolution resolution) {
      this._resolution = resolution;
      return this;
    }

    public Builder addDate(Date date, Occur when) {
      if (date != null)
        this._dates.put(OffsetDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZoneOffset.UTC), when == null ? Occur.MUST : when);
      return this;
    }

    public Builder addDate(OffsetDateTime date, Occur when) {
      this._dates.put(date, when == null ? Occur.MUST : when);
      return this;
    }

    public DateTermFilter build() {
      if (this._name == null) throw new IllegalStateException("name");
      if (this._resolution == null) throw new IllegalStateException("resolution");
      if (this._dates.isEmpty()) throw new IllegalStateException("no dates to filter with!");
      return new DateTermFilter(this);
    }
  }
}

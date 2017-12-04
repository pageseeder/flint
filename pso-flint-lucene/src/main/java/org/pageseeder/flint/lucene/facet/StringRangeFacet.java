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
public class StringRangeFacet extends FlexibleRangeFacet {

  private final List<Range> _ranges = new ArrayList<>();

  private final boolean _caseSensitive;

  /**
   * Creates a new facet with the specified name;
   *
   * @param name     The name of the facet.
   */
  private StringRangeFacet(String name, boolean caseSensitive, List<Range> ranges) {
    super(name);
    this._ranges.addAll(ranges);
    this._caseSensitive = caseSensitive;
  }

  @Override
  public String getType() {
    return "string-range";
  }

  @Override
  protected Range findRange(Term t) {
    if (t == null) return null;
    String word = t.text();
    for (Range r : this._ranges) {
      int compareMin = r.getMin() == null ?  1 : (this._caseSensitive ? word.compareToIgnoreCase(r.getMin()) : word.compareTo(r.getMin()));
      int compareMax = r.getMax() == null ? -1 : (this._caseSensitive ? word.compareToIgnoreCase(r.getMax()) : word.compareTo(r.getMax()));
      boolean passMin = r.includeMin() ? compareMin >= 0 : compareMin > 0;
      boolean passMax = r.includeMax() ? compareMax <= 0 : compareMax < 0;
      if (passMin && passMax) return r;
    }
    return null;
  }

  @Override
  protected void rangeToXML(Range range, int cardinality, XMLWriter xml) throws IOException {
    xml.openElement("range");
    if (range.getMin() != null) xml.attribute("min", range.getMin());
    if (range.getMax() != null) xml.attribute("max", range.getMax());
    if (range.getMin() != null) xml.attribute("include-min", range.includeMin() ? "true" : "false");
    if (range.getMax() != null) xml.attribute("include-max", range.includeMax() ? "true" : "false");
    xml.attribute("cardinality", cardinality);
    xml.closeElement();
  }

  // Builder ------------------------------------------------------------------------------------------
  public static class Builder {

    private final List<Range> ranges = new ArrayList<>();

    private boolean caseSensitive = false;

    private String name = null;

    public Builder caseSensitive(boolean cs) {
      this.caseSensitive = cs;
      return this;
    }

    public Builder name(String n) {
      this.name = n;
      return this;
    }

    public Builder addRange(String min, boolean withMin, String max, boolean withMax) {
      this.ranges.add(Range.stringRange(min, withMin, max, withMax));
      return this;
    }

    public Builder addRange(Range range) {
      this.ranges.add(range);
      return this;
    }

    /**
     * Will include min and max.
     * @param min
     * @param max
     * @return
     */
    public Builder addRange(String min, String max) {
      return addRange(min, true, max, true);
    }

    public StringRangeFacet build() {
      if (this.name == null) throw new NullPointerException("Must have a field name");
      StringRangeFacet fr = new StringRangeFacet(this.name, this.caseSensitive, this.ranges);
      return fr;
    }
  }
}

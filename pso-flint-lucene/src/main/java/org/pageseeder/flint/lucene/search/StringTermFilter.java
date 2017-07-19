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
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.pageseeder.flint.lucene.query.Queries;
import org.pageseeder.flint.lucene.util.Beta;
import org.pageseeder.xmlwriter.XMLWriter;

/**
 * A facet implementation using a simple index field.
 *
 * @author Christophe Lauret
 * @version 16 February 2012
 */
@Beta
public class StringTermFilter implements Filter {

  /**
   * The name of the field
   */
  private final String _name;

  /**
   * The list of terms to filter with
   */
  private final Map<String, Occur> _terms = new HashMap<>();

  /**
   * Creates a new filter with the specified name;
   *
   * @param name    The name of the filter.
   */
  private StringTermFilter(Builder builder) {
    this._name = builder._name;
    this._terms.putAll(builder._terms);
  }

  @Override
  public Query filterQuery(Query base) {
    BooleanQuery filterQuery = new BooleanQuery();
    for (String word : this._terms.keySet()) {
      Occur clause = this._terms.get(word);
      filterQuery.add(new TermQuery(new Term(this._name, word)), clause);
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
    xml.attribute("type", "string");
    for (String word : this._terms.keySet()) {
      xml.openElement("term");
      xml.attribute("text", word);
      xml.attribute("occur", occurToString(this._terms.get(word)));
      xml.closeElement();
    }
    xml.closeElement();
  }

  public static StringTermFilter newFilter(String name, String word) {
    return newFilter(name, word, Occur.MUST);
  }

  public static StringTermFilter newFilter(String name, String word, Occur occur) {
    return new Builder().name(name).addTerm(word, occur).build();
  }

  private static String occurToString(Occur occur) {
    if (occur == Occur.MUST)     return "must";
    if (occur == Occur.MUST_NOT) return "must_not";
    if (occur == Occur.SHOULD)   return "should";
    return "unknown";
  }

  public static class Builder {

    /**
     * The name of the field
     */
    private String _name = null;

    /**
     * The list of terms to filter with
     */
    private final Map<String, Occur> _terms = new HashMap<>();

    public Builder name(String name) {
      this._name = name;
      return this;
    }

    public Builder addTerm(String term, Occur when) {
      this._terms.put(term, when == null ? Occur.MUST : when);
      return this;
    }

    public StringTermFilter build() {
      if (this._name == null) throw new NullPointerException("name");
      if (this._terms.isEmpty()) throw new IllegalStateException("no terms to filter with!");
      return new StringTermFilter(this);
    }
  }
}

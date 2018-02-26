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
 *
 * @version 5.1.3
 */
@Beta
public class StringTermFilter extends TermFilter<String> implements Filter {

  /**
   * Creates a new filter from the builder.
   */
  private StringTermFilter(Builder builder) {
    super(builder._name, builder._terms);
  }

  @Override
  public Query filterQuery(Query base) {
    // if should, create filter query
    if (this._terms.values().contains(Occur.SHOULD)) {
      BooleanQuery filterQuery = new BooleanQuery();
      for (String word : this._terms.keySet()) {
        filterQuery.add(new TermQuery(new Term(this._name, word)), this._terms.get(word));
      }
      // and join to base if there
      return base == null ? filterQuery : Queries.and(base, filterQuery);
    }
    // create filter query then
    BooleanQuery filterQuery = new BooleanQuery();
    if (base != null) filterQuery.add(base, Occur.MUST);
    for (String word : this._terms.keySet()) {
      filterQuery.add(new TermQuery(new Term(this._name, word)), this._terms.get(word));
    }
    return filterQuery;
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

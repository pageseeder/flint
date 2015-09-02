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
package org.pageseeder.flint.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.pageseeder.flint.util.Beta;
import org.pageseeder.flint.util.Bucket;
import org.pageseeder.flint.util.Bucket.Entry;
import org.pageseeder.flint.util.Terms;
import org.pageseeder.xmlwriter.XMLWritable;
import org.pageseeder.xmlwriter.XMLWriter;

/**
 * A facet implementation using a simple index field.
 *
 * @author Christophe Lauret
 * @version 16 February 2012
 */
@Beta
public final class FieldFacet implements XMLWritable, Facet {

  /**
   * The default number of facet values if not specified.
   */
  public static final int DEFAULT_MAX_NUMBER_OF_VALUES = 10;

  /**
   * The name of this facet
   */
  private final String _name;

  /**
   * The queries used to calculate each facet.
   */
  private final List<TermQuery> _queries;

  /**
   * The queries used to calculate each facet.
   */
  private transient Bucket<Term> _bucket;

  /**
   * Creates a new facet with the specified name;
   *
   * @param name    The name of the facet.
   * @param queries The subqueries to use on top of the base query to calculate the facet values.
   */
  private FieldFacet(String name, List<TermQuery> queries) {
    this._name = name;
    this._queries = queries;
  }

  /**
   * Returns the name of the field.
   * @return the name of the field.
   */
  @Override
  public String name() {
    return this._name;
  }

  /**
   * Returns the query for given value if it the specified value matches the text for the term.
   *
   * @param value the text of the term to match.
   * @return the requested query if it exists or <code>null</code>.
   */
  @Override
  public Query forValue(String value) {
    if (value == null) return null;
    for (TermQuery t : this._queries) {
      if (value.equals(t.getTerm().text())) return t;
    }
    // Why null?
    return new TermQuery(new Term(this._name, value));
  }

  /**
   * Computes each facet option.
   *
   * @param searcher the index search to use.
   * @param base     the base query.
   * @param size     the maximum number of field values to compute.
   *
   * @throws IOException if thrown by the searcher.
   */
  public void compute(IndexSearcher searcher, Query base, int size) throws IOException {
    // If the base is null, simply calculate for each query
    if (base == null) { compute(searcher, size); }
    if (size < 0) throw new IllegalArgumentException("size < 0");
    // Otherwise, make a boolean query of the base AND each facet query
    Bucket<Term> bucket = new Bucket<Term>(size);
    DocumentCounter counter = new DocumentCounter();
    for (TermQuery q : this._queries) {
      BooleanQuery query = new BooleanQuery();
      query.add(base, Occur.MUST);
      query.add(q, Occur.MUST);
      searcher.search(query, counter);
      bucket.add(q.getTerm(), counter.getCount());
      counter.reset();
    }
    this._bucket = bucket;
  }

  /**
   * Computes each facet option.
   *
   * <p>Same as <code>compute(searcher, base, 10);</code>.
   *
   * <p>Defaults to 10.
   *
   * @see #compute(Searcher, Query, int)
   *
   * @param searcher the index search to use.
   * @param base     the base query.
   *
   * @throws IOException if thrown by the searcher.
   */
  @Override
  public void compute(IndexSearcher searcher, Query base) throws IOException {
    compute(searcher, base, DEFAULT_MAX_NUMBER_OF_VALUES);
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
    Bucket<Term> bucket = new Bucket<Term>(size);
    DocumentCounter counter = new DocumentCounter();
    for (TermQuery q : this._queries) {
      searcher.search(q, counter);
      bucket.add(q.getTerm(), counter.getCount());
      counter.reset();
    }
    this._bucket = bucket;
  }

  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("facet", true);
    xml.attribute("name", this._name);
    xml.attribute("type", "field");
    xml.attribute("computed", Boolean.toString(this._bucket != null));
    if (this._bucket != null) {
      xml.attribute("total", this._bucket.getConsidered());
      for (Entry<Term> e : this._bucket.entrySet()) {
        xml.openElement("term");
        xml.attribute("field", e.item().field());
        xml.attribute("text", e.item().text());
        xml.attribute("cardinality", e.count());
        xml.closeElement();
      }
    }
    xml.closeElement();
  }

  // Static helpers -------------------------------------------------------------------------------

  /**
   * Creates a new facet for the specified field.
   *
   * @param field  the field for this facet.
   * @param reader the reader to use.
   *
   * @return the corresponding Facet ready to use with a base query.
   *
   * @throws IOException if thrown by the reader.
   */
  public static FieldFacet newFacet(String field, IndexReader reader) throws IOException {
    List<Term> terms = Terms.terms(reader, field);
    List<TermQuery> subs = new ArrayList<TermQuery>(terms.size());
    for (Term t : terms) {
      subs.add(new TermQuery(t));
    }
    return new FieldFacet(field, subs);
  }

}

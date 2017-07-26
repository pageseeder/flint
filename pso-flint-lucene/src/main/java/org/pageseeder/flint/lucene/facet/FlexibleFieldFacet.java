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
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.pageseeder.flint.lucene.search.DocumentCounter;
import org.pageseeder.flint.lucene.search.FieldDocumentCounter;
import org.pageseeder.flint.lucene.search.Filter;
import org.pageseeder.flint.lucene.search.Terms;
import org.pageseeder.flint.lucene.util.Beta;
import org.pageseeder.flint.lucene.util.Bucket;
import org.pageseeder.flint.lucene.util.Bucket.Entry;
import org.pageseeder.xmlwriter.XMLWritable;
import org.pageseeder.xmlwriter.XMLWriter;

/**
 * A facet implementation using a simple index field.
 *
 * @author Jean-Baptiste Reure
 * @version 14 July 2017
 */
@Beta
public abstract class FlexibleFieldFacet implements XMLWritable {

  /**
   * The default number of facet values if not specified.
   */
  public static final int DEFAULT_MAX_NUMBER_OF_VALUES = 10;

  /**
   * The name of this facet
   */
  private final String _name;

  /**
   * The max nb of terms
   */
  private final int _maxTerms;

  /**
   * The queries used to calculate each facet.
   */
  private transient Bucket<String> _bucket;

  /**
   * If the facet was computed in a "flexible" way
   */
  private transient boolean flexible = false;

  /**
   * The total number of results containing the field used in this facet
   */
  private transient int totalResults = 0;

  /**
   * The total number of terms found in the search results
   */
  private transient int totalTerms = 0;

  /**
   * Creates a new facet with the specified name;
   *
   * @param name     The name of the facet.
   * @param numeric  If this facet is numeric
   * @param r        If this facet is a date
   * @param maxterms The maximum number of terms to return
   */
  protected FlexibleFieldFacet(String name, int maxterms) {
    this._name = name;
    this._maxTerms = maxterms;
  }

  /**
   * Returns the name of the field.
   * @return the name of the field.
   */
  public String name() {
    return this._name;
  }

  /**
   * Computes each facet option as a flexible facet.
   * All filters but the ones using the same field as this facet are applied to the base query before computing the numbers.
   *
   * @param searcher the index search to use.
   * @param base     the base query.
   * @param filters  the filters applied to the base query (ignored if the base query is null)
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
      // reset total terms
      this.totalTerms = 0;
      // find all terms
      List<Term> terms = Terms.terms(searcher.getIndexReader(), name());
      if (this._maxTerms > 0 && terms.size() > this._maxTerms) return;
      // Otherwise, re-compute the query without the corresponding filter 
      Query filtered = base;
      if (filters != null) {
        this.flexible = true;
        for (Filter filter : filters) {
          if (!this._name.equals(filter.name()))
            filtered = filter.filterQuery(filtered);
        }
      }
      Bucket<String> bucket = new Bucket<String>(size);
      DocumentCounter counter = new DocumentCounter();
      for (Term t : terms) {
        BooleanQuery query = new BooleanQuery();
        query.add(filtered, Occur.MUST);
        query.add(termToQuery(t), Occur.MUST);
        searcher.search(query, counter);
        int count = counter.getCount();
        bucket.add(t.text(), count);
        counter.reset();
        if (count > 0) this.totalTerms++;
      }
      this._bucket = bucket;
      // compute total results
      FieldDocumentCounter totalCounter = new FieldDocumentCounter(this._name);
      searcher.search(filtered, totalCounter);
      this.totalResults = totalCounter.getCount();
    }
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
  public void compute(IndexSearcher searcher, Query base, int size) throws IOException {
    compute(searcher, base, null, size);
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
  public void compute(IndexSearcher searcher, Query base) throws IOException {
    compute(searcher, base, null, DEFAULT_MAX_NUMBER_OF_VALUES);
  }

  /**
   * Computes each facet option as a flexible facet.
   *
   * <p>Same as <code>computeFlexible(searcher, base, filters, 10);</code>.
   *
   * <p>Defaults to 10.
   *
   * @see #computeFlexible(IndexSearcher, Query, List, int)
   *
   * @param searcher the index search to use.
   * @param base     the base query.
   * @param filters  the filters applied to the base query
   *
   * @throws IOException if thrown by the searcher.
   */
  public void compute(IndexSearcher searcher, Query base, List<Filter> filters) throws IOException {
    compute(searcher, base, filters, DEFAULT_MAX_NUMBER_OF_VALUES);
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
    // find all terms
    List<Term> terms = Terms.terms(searcher.getIndexReader(), this._name);
    if (this._maxTerms > 0 && terms.size() > this._maxTerms) return;
    this.totalTerms = 0;
    Bucket<String> bucket = new Bucket<String>(size);
    DocumentCounter counter = new DocumentCounter();
    for (Term t : terms) {
      searcher.search(termToQuery(t), counter);
      bucket.add(t.text(), counter.getCount());
      counter.reset();
      this.totalTerms++;
    }
    // set totals
    this.totalResults = 0;
    this._bucket = bucket;
  }

  /**
   * Create a query for the term given, using the numeric type if there is one.
   * 
   * @param t the term
   * 
   * @return the query
   */
  protected abstract Query termToQuery(Term t);

  protected abstract String getType();

  protected abstract void termToXML(String term, int cardinality, XMLWriter xml) throws IOException;

  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("facet", true);
    xml.attribute("name", this._name);
    xml.attribute("type", getType());
    xml.attribute("flexible", String.valueOf(this.flexible));
    if (!this.flexible) {
      xml.attribute("total-terms", this.totalTerms);
      xml.attribute("total-results", this.totalResults);
    }
    if (this._bucket != null) {
      for (Entry<String> e : this._bucket.entrySet()) {
        termToXML(e.item(), e.count(), xml);
      }
    }
    xml.closeElement();
  }

  public Bucket<String> getValues() {
    return this._bucket;
  }

  public int getTotalTerms() {
    return this.totalTerms;
  }

  public int getTotalResults() {
    return this.totalResults;
  }

}

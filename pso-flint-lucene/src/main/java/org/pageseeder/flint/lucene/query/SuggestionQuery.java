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
package org.pageseeder.flint.lucene.query;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.*;
import org.pageseeder.flint.lucene.search.Terms;
import org.pageseeder.flint.lucene.util.Beta;
import org.pageseeder.xmlwriter.XMLWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A Search query for use as auto suggest.
 *
 * @author Christophe Lauret
 * @version 21 July 2010
 */
@Beta
public final class SuggestionQuery implements SearchQuery {

  /**
   * The list of terms.
   */
  private final List<Term> _terms;

  /**
   * The condition that all the suggested results must meet.
   */
  private final Query _condition;

  /**
   * The underlying boolean query.
   */
  private BooleanQuery query = null;

  /**
   * Create a new auto-suggest query for the specified list of terms with no condition.
   *
   * @param terms     The list of terms that should be matched.
   */
  public SuggestionQuery(List<Term> terms) {
    this(terms, null);
  }

  /**
   * Create a new auto-suggest query for the specified list of terms.
   *
   * @param terms     The list of terms that should be matched.
   * @param condition The condition that must be met by all suggested results (may be <code>null</code>).
   */
  public SuggestionQuery(List<Term> terms, Query condition) {
    this._terms = terms;
    this._condition = condition;
  }

  /**
   * Computes the list of terms to generate the actual suggestion query.
   *
   * @param reader Computes the list.
   * @throws IOException should an error occurs while reading the index.
   */
  public void compute(IndexReader reader) throws IOException {
    // Compute the list of terms
    HashMap<Term, Boolean> terms = new HashMap<>();
    for (Term term : this._terms) {
      // try exact match
      terms.put(term, true);
      // try prefixed terms
      List<String> values = Terms.prefix(reader, term);
      for (String v : values) {
        Term t = new Term(term.field(), v);
        if (!terms.containsKey(t)) terms.put(t, false);
      }
    }
    // When the number of term exceeds Max Clause Count
    if (terms.size() >= BooleanQuery.getMaxClauseCount()) {
      List<Term> tokeep = new ArrayList<>(terms.keySet()).subList(0, BooleanQuery.getMaxClauseCount()-1);
      terms.keySet().retainAll(tokeep);
    }
    // Generate the query
    BooleanQuery.Builder bq = new BooleanQuery.Builder();
    for (Term t : terms.keySet()) {
      if (terms.get(t)) {
        bq.add(new BoostQuery(new TermQuery(t), 2.0f), Occur.SHOULD);
      } else {
        bq.add(new TermQuery(t), Occur.SHOULD);
      }
    }
    // Any condition ?
    if (this._condition != null) {
      BooleanQuery.Builder dad = new BooleanQuery.Builder();
      dad.add(this._condition, Occur.MUST);
      dad.add(bq.build(), Occur.MUST);
      this.query = dad.build();
    } else {
      this.query = bq.build();
    }
  }

  /**
   * Prints this query as XML on the specified XML writer.
   *
   * <p>XML:
   * <pre>{@code
   *  <suggestion-query>
   *    <terms>
   *      <term field="[field]" text="[text]"/>
   *      <term field="[field]" text="[text]"/>
   *      <term field="[field]" text="[text]"/>
   *      ...
   *    </terms>
   *    <condition>
   *     <!-- Condition as a Lucene Query -->
   *     ...
   *    </condition>
   *  </suggestion-query>
   * }</pre>
   *
   * @see Query#toString()
   *
   * @param xml The XML writer to use.
   *
   * @throws IOException If thrown by
   */
  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("suggestion-query");
    xml.openElement("terms");
    for (Term t : this._terms) {
      xml.openElement("term");
      xml.attribute("field", t.field());
      xml.attribute("text", t.text());
      xml.closeElement();
    }
    xml.closeElement();
    if (this._condition != null) {
      xml.openElement("condition");
      xml.writeText(this._condition.toString());
      xml.closeElement();
    }
    xml.closeElement();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Query toQuery() {
    return this.query;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEmpty() {
    return this._terms.isEmpty();
  }

  /**
   * Always sort by relevance.
   *
   * {@inheritDoc}
   */
  @Override
  public Sort getSort() {
    return Sort.RELEVANCE;
  }

};


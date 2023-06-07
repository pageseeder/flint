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
   * If the main query uses 'OR' or 'AND' between term results.
   */
  private final boolean _unionTermResults;

  /**
   * The underlying boolean query.
   */
  private BooleanQuery query = null;

  /**
   * If there are too many clauses in the query.
   */
  private boolean tooManyPrefixes = false;

  /**
   * Create a new auto-suggest query for the specified list of terms with no condition.
   *
   * @param terms     The list of terms that should be matched.
   */
  public SuggestionQuery(List<Term> terms) {
    this(terms, null);
  }

  /**
   * Create a new auto-suggest query for the specified list of terms with no condition.
   *
   * @param terms             The list of terms that should be matched.
   * @param unionTermResults  If the suggest query uses 'OR' or 'AND' between term results.
   */
  public SuggestionQuery(List<Term> terms, boolean unionTermResults) {
    this(terms, null, unionTermResults);
  }

  /**
   * Create a new auto-suggest query for the specified list of terms.
   *
   * @param terms     The list of terms that should be matched.
   * @param condition The condition that must be met by all suggested results (may be <code>null</code>).
   */
  public SuggestionQuery(List<Term> terms, Query condition) {
    this(terms, condition, true);
  }

  /**
   * Create a new auto-suggest query for the specified list of terms.
   *
   * @param terms             The list of terms that should be matched.
   * @param condition         The condition that must be met by all suggested results (may be <code>null</code>).
   * @param unionTermResults  If the suggest query uses 'OR' or 'AND' between term results.
   */
  public SuggestionQuery(List<Term> terms, Query condition, boolean unionTermResults) {
    this._terms = terms;
    this._condition = condition;
    this._unionTermResults = unionTermResults;
  }

  /**
   * Computes the list of terms to generate the actual suggestion query.
   *
   * @param reader Computes the list.
   * @throws IOException should an error occurs while reading the index.
   */
  public void compute(IndexReader reader) throws IOException {
    BooleanQuery mainQuery = this._unionTermResults ? computeORQuery(reader) : computeANDQuery(reader);
    // Any condition ?
    if (this._condition == null) {
      this.query = mainQuery;
    } else {
      // combine with condition then
      BooleanQuery.Builder dad = new BooleanQuery.Builder();
      dad.add(this._condition, Occur.MUST);
      dad.add(mainQuery, Occur.MUST);
      this.query = dad.build();
    }
  }

  public boolean isIncomplete() {
    return this.tooManyPrefixes;
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
    if (this.tooManyPrefixes) xml.attribute("incomplete", "true");
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
   * <p> {@inheritDoc}
   */
  @Override
  public Sort getSort() {
    return Sort.RELEVANCE;
  }

  // -------------------------------------------------------
  // private helpers
  // -------------------------------------------------------

  private BooleanQuery computeORQuery(IndexReader reader) throws IOException {
    // Generate the query
    BooleanQuery.Builder builder = new BooleanQuery.Builder();
    try {
      for (Term term : this._terms) {
        // find prefixed terms
        List<String> values = Terms.prefix(reader, term);
        for (String v : values) {
          addTermQuery(term, v, builder);
        }
      }
    } catch (IndexSearcher.TooManyClauses ex) {
      this.tooManyPrefixes = true;
    }
    return builder.build();
  }

  private BooleanQuery computeANDQuery(IndexReader reader) throws IOException {
    // one term, simpler query
    if (this._terms.size() == 1) {
      BooleanQuery.Builder builder = new BooleanQuery.Builder();
      Term only = this._terms.get(0);
      // find prefixed terms
      List<String> values = Terms.prefix(reader, only);
      try {
        for (String v : values) {
          addTermQuery(only, v, builder);
        }
      } catch (IndexSearcher.TooManyClauses ex) {
        this.tooManyPrefixes = true;
      }
      // if empty, will match nothing, that's ok
      return builder.build();
    }
    // group queries by word
    HashMap<String, BooleanQuery.Builder> wordQueries = new HashMap<>();
    // Compute the list of terms
    for (Term term : this._terms) {
      try {
        BooleanQuery.Builder q = wordQueries.computeIfAbsent(term.text(), s -> new BooleanQuery.Builder());
        // find prefixed terms
        List<String> values = Terms.prefix(reader, term);
        for (String v : values) {
          addTermQuery(term, v, q);
        }
      } catch (IndexSearcher.TooManyClauses ex) {
        this.tooManyPrefixes = true;
      }
    }
    BooleanQuery.Builder bq = new BooleanQuery.Builder();
    for (BooleanQuery.Builder subq : wordQueries.values()) {
      BooleanQuery q = subq.build();
      if (!q.clauses().isEmpty())
        bq.add(subq.build(), Occur.MUST);
    }
    return bq.build();
  }

  private void addTermQuery(Term original, String value, BooleanQuery.Builder query) throws IndexSearcher.TooManyClauses {
    if (value.equals(original.text())) {
      query.add(new BoostQuery(new TermQuery(original), 2.0f), Occur.SHOULD);
    } else {
      query.add(new TermQuery(new Term(original.field(), value)), Occur.SHOULD);
    }
  }
}


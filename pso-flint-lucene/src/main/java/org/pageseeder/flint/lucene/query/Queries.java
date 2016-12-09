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

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.pageseeder.flint.lucene.search.Fields;
import org.pageseeder.flint.lucene.search.Terms;
import org.pageseeder.flint.lucene.util.Beta;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 * A set of utility methods related to query objects in Lucene.
 *
 * @author  Christophe Lauret (Weborganic)
 * @version 13 August 2010
 */
public final class Queries {

  /**
   * Text that matches this pattern is considered a phrase.
   */
  private static final Pattern IS_A_PHRASE = Pattern.compile("\\\"[^\\\"]+\\\"");

  /**
   * Prevents creation of instances.
   */
  private Queries() {
  }

  /**
   * Returns the term or phrase query corresponding to the specified text.
   *
   * <p>If the text is surrounded by double quotes, this method will
   * return a {@link PhraseQuery} otherwise, it will return a simple {@link TermQuery}.
   *
   * <p>Note: Quotation marks are thrown away.
   *
   * @param field the field to construct the terms.
   * @param text  the text to construct the query from.
   * @return the corresponding query.
   */
  @Beta
  public static Query toTermOrPhraseQuery(String field, String text) {
    if (field == null) throw new NullPointerException("field");
    if (text == null) throw new NullPointerException("text");
    boolean isPhrase = IS_A_PHRASE.matcher(text).matches();
    if (isPhrase) {
      PhraseQuery phrase = new PhraseQuery();
      String[] terms = text.substring(1, text.length()-1).split("\\s+");
      for (String t : terms) {
        phrase.add(new Term(field, t));
      }
      return phrase;
    } else return new TermQuery(new Term(field, text));
  }

  /**
   * Returns a boolean query combining all the specified queries in {@link Occur#MUST} clauses
   * as it is were an AND operator.
   *
   * @param queries the queries to combine with an AND.
   * @return The combined queries.
   */
  public static Query and(Query... queries) {
    BooleanQuery query = new BooleanQuery();
    for (Query q : queries) {
      query.add(q, Occur.MUST);
    }
    return query;
  }

  /**
   * Returns a boolean query combining all the specified queries in {@link Occur#MUST} clauses
   * as it is were an OR operator.
   *
   * @param queries the queries to combine with an OR.
   * @return The combined queries.
   */
  public static Query or(Query... queries) {
    BooleanQuery query = new BooleanQuery();
    for (Query q : queries) {
      query.add(q, Occur.SHOULD);
    }
    return query;
  }

  /**
   * Returns the list of similar queries by substituting one term only in the query.
   *
   * @param query  The original query
   * @param reader A reader to extract the similar terms.
   *
   * @return A list of similar queries to the specified one.
   *
   * @throws IOException If thrown by the reader while extracting fuzzy terms.
   */
  @Beta
  public static List<Query> similar(Query query, Collection<Term> terms, IndexReader reader) throws IOException {
    List<Query> similar = new ArrayList<Query>();
    // Extract the list of similar terms
    for (Term t : terms) {
      List<String> fuzzy = Terms.fuzzy(reader, t);
      for (String f : fuzzy) {
        Query sq = substitute(query, t, new Term(t.field(), f));
        similar.add(sq);
      }
    }
    return similar;
  }


  /**
   * Returns the term or phrase query corresponding to the specified text.
   *
   * <p>If the text is surrounded by double quotes, this method will
   * return a {@link PhraseQuery} otherwise, it will return a simple {@link TermQuery}.
   *
   * <p>Note: Quotation marks are thrown away.
   *
   * @param field the field to construct the terms.
   * @param text  the text to construct the query from.
   *
   * @return the corresponding query.
   */
  @Beta
  public static List<Query> toTermOrPhraseQueries(String field, String text, Analyzer analyzer) {
    if (field == null) throw new NullPointerException("field");
    if (text == null) throw new NullPointerException("text");
    boolean isPhrase = IS_A_PHRASE.matcher(text).matches();
    if (isPhrase) {
      PhraseQuery phrase = new PhraseQuery();
      addTermsToPhrase(field, text.substring(1, text.length()-1), analyzer, phrase);
      return Collections.singletonList((Query)phrase);
    } else {
      List<Query> q = new ArrayList<Query>();
      for (String t : Fields.toTerms(field, text, analyzer)) {
        q.add(new TermQuery(new Term(field, t)));
      }
      return q;
    }
  }

  /**
   * Returns the terms for a field
   *
   * @param field    The field
   * @param text     The text to analyze
   * @param analyzer The analyzer
   *
   * @return the corresponding list of terms produced by the analyzer.
   *
   * @throws IOException
   */
  private static void addTermsToPhrase(String field, String text, Analyzer analyzer, PhraseQuery phrase) {
    try {
      TokenStream stream = analyzer.tokenStream(field, new StringReader(text));
      PositionIncrementAttribute increment = stream.addAttribute(PositionIncrementAttribute.class);
      CharTermAttribute attribute = stream.addAttribute(CharTermAttribute.class);
      int position = -1;
      stream.reset();
      while (stream.incrementToken()) {
        position += increment.getPositionIncrement();
        Term term = new Term(field, attribute.toString());
        phrase.add(term, position);
      }
      stream.end();
      stream.close();
    } catch (IOException ex) {
      // Should not occur since we use a StringReader
      ex.printStackTrace();
    }
  }

  // Substitutions
  // ==============================================================================================

  /**
   * Substitutes one term in the query for another.
   *
   * <p>This method only creates new query object if required; it does not modify the given query.
   *
   * <p>This method simply delegates to the appropriate <code>substitute</code> method based
   * on the query class. Only query types for which there is an applicable <code>substitute</code>
   * method can be substituted.
   *
   * @param query       the query where the substitution should occur.
   * @param original    the original term to replace.
   * @param replacement the term it should be replaced with.
   *
   * @return A new query where the term has been substituted;
   *         or the same query if no substitution was required or possible.
   */
  @Beta
  public static Query substitute(Query query, Term original, Term replacement) {
    if (query instanceof TermQuery) return substitute((TermQuery)query, original, replacement);
    else if (query instanceof PhraseQuery) return substitute((PhraseQuery)query, original, replacement);
    else if (query instanceof BooleanQuery) return substitute((BooleanQuery)query, original, replacement);
    else return query;
  }

  /**
   * Substitutes one term in the term query for another.
   *
   * <p>This method only creates new query object if required; it does not modify the given query.
   *
   * @param query       the query where the substitution should occur.
   * @param original    the original term to replace.
   * @param replacement the term it should be replaced with.
   *
   * @return A new term query where the term has been substituted;
   *         or the same query if no substitution was needed.
   */
  @Beta
  public static Query substitute(BooleanQuery query, Term original, Term replacement) {
    BooleanQuery q = new BooleanQuery();
    for (BooleanClause clause : query.getClauses()) {
      Query qx = substitute(clause.getQuery(), original, replacement);
      q.add(qx, clause.getOccur());
    }
    q.setBoost(query.getBoost());
    return q;
  }

  /**
   * Substitutes one term in the term query for another.
   *
   * <p>This method only creates new query object if required; it does not modify the given query.
   *
   * @param query       the query where the substitution should occur.
   * @param original    the original term to replace.
   * @param replacement the term it should be replaced with.
   *
   * @return A new term query where the term has been substituted;
   *         or the same query if no substitution was needed.
   */
  @Beta
  public static TermQuery substitute(TermQuery query, Term original, Term replacement) {
    Term t = query.getTerm();
    if (t.equals(original)) return new TermQuery(replacement);
    else return query;
  }

  /**
   * Substitutes one term in the phrase query for another.
   *
   * <p>In a phrase query the replacement term must be on the same field as the original term.
   *
   * <p>This method only creates new query object if required; it does not modify the given query.
   *
   * @param query       the query where the substitution should occur.
   * @param original    the original term to replace.
   * @param replacement the term it should be replaced with.
   *
   * @return A new term query where the term has been substituted;
   *         or the same query if no substitution was needed.
   *
   * @throws IllegalArgumentException if the replacement term is not on the same field as the original term.
   */
  @Beta
  public static PhraseQuery substitute(PhraseQuery query, Term original, Term replacement)
      throws IllegalArgumentException {
    boolean doSubstitute = false;
    // Check if we need to substitute
    for (Term t : query.getTerms()) {
      if (t.equals(original)) {
        doSubstitute = true;
      }
    }
    // Substitute if required
    if (doSubstitute) {
      PhraseQuery q = new PhraseQuery();
      for (Term t : query.getTerms()) {
        q.add(t.equals(original)? replacement : t);
      }
      q.setSlop(query.getSlop());
      q.setBoost(query.getBoost());
      return q;
    // No substitution return the query
    } else return query;
  }

}

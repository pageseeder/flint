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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.search.BooleanClause.Occur;
import org.pageseeder.flint.lucene.search.Fields;
import org.pageseeder.flint.lucene.search.Terms;
import org.pageseeder.flint.lucene.util.Beta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
   * Returns a boolean query combining all the specified queries in {@link Occur#MUST} clauses
   * as it is were an AND operator.
   *
   * @param queries the queries to combine with an AND.
   * @return The combined queries, may be empty if no arguments/empty argument provided.
   */
  public static Query and(Query... queries) {
    if (queries.length == 1) return queries[0];
    BooleanQuery.Builder query = new BooleanQuery.Builder();
    for (Query q : queries) {
      query.add(q, Occur.MUST);
    }
    return query.build();
  }

  /**
   * Returns a boolean query combining all the specified queries in {@link Occur#MUST} clauses
   * as it is were an OR operator.
   *
   * @param queries the queries to combine with an OR.
   * @return The combined queries, may be empty if no arguments/empty argument provided.
   */
  public static Query or(Query... queries) {
    if (queries.length == 1) return queries[0];
    BooleanQuery.Builder query = new BooleanQuery.Builder();
    for (Query q : queries) {
      query.add(q, Occur.SHOULD);
    }
    return query.build();
  }

  /**
   * Returns a boolean query combining all the specified queries using OR or AND operator.
   *
   * @param withOR  whether to use OR or AND between queries
   * @param queries the queries to combine with an OR.
   * @return The combined queries, may be empty if no arguments/empty argument provided.
   */
  public static Query combine(boolean withOR, List<Query> queries) {
    return withOR ? or(queries.toArray(new Query[]{})) : and(queries.toArray(new Query[]{}));
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

  public static boolean isAPhrase(String text) {
    return IS_A_PHRASE.matcher(text).matches();
  }

  /**
   * Build a query from the term provided, could be wildcard query or term query.
   * @param field             the term field name
   * @param text              the term value
   * @param supportWildcards  if wildcards are supported
   * @return the query
   */
  public static Query termQuery(String field, String text, boolean supportWildcards) {
    Term t = new Term(field, text);
    return supportWildcards && hasWildcards(text) ? new WildcardQuery(t) : new TermQuery(t);
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
    return toTermOrPhraseQuery(field, text, false);
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
   * @param supportWildcards  if wildcards are supported.
   * @return the corresponding query.
   */
  @Beta
  public static Query toTermOrPhraseQuery(String field, String text, boolean supportWildcards) {
    if (field == null) throw new NullPointerException("field");
    if (text == null) throw new NullPointerException("text");
    boolean isPhrase = isAPhrase(text);
    if (isPhrase) {
      PhraseQuery.Builder phrase = new PhraseQuery.Builder();
      String[] terms = text.substring(1, text.length()-1).split("\\s+");
      for (String t : terms) {
        phrase.add(new Term(field, t));
      }
      return phrase.build();
    } else {
      return termQuery(field, text, supportWildcards);
    }
  }

  /**
   * Returns the term or phrase query corresponding to the specified text.
   *
   * <p>If the text is surrounded by double quotes, this method will
   * return a {@link PhraseQuery} otherwise, it will return a simple {@link TermQuery}.
   *
   * <p>Note: Quotation marks are thrown away.
   *
   * @param field     the field to construct the terms.
   * @param text      the text to construct the query from.
   * @param analyzer  used to analyze the text
   *
   * @return the corresponding query.
   */
  @Beta
  public static List<Query> toTermOrPhraseQueries(String field, String text, Analyzer analyzer) {
    return toTermOrPhraseQueries(field, text, false, analyzer);
  }

  /**
   * Returns the term or phrase query corresponding to the specified text.
   *
   * <p>If the text is surrounded by double quotes, this method will
   * return a {@link PhraseQuery} otherwise, it will return a simple {@link TermQuery}.
   *
   * <p>Note: Quotation marks are thrown away.
   *
   * @param field     the field to construct the terms.
   * @param text      the text to construct the query from.
   * @param supportWildcards  if wildcards are supported.
   * @param analyzer  used to analyze the text
   *
   * @return the corresponding query.
   */
  @Beta
  public static List<Query> toTermOrPhraseQueries(String field, String text, boolean supportWildcards, Analyzer analyzer) {
    if (field == null) throw new NullPointerException("field");
    if (text == null) throw new NullPointerException("text");
    if (analyzer == null) return Collections.singletonList(toTermOrPhraseQuery(field, text, supportWildcards));
    boolean isPhrase = isAPhrase(text);
    if (isPhrase && isTokenized(field, analyzer)) {
      PhraseQuery.Builder phrase = new PhraseQuery.Builder();
      addTermsToPhrase(field, text.substring(1, text.length() - 1), analyzer, phrase);
      return Collections.singletonList(phrase.build());
    } else if (supportWildcards && hasWildcards(text)) {
      if (isTokenized(field, analyzer)) {
        List<Query> q = new ArrayList<>();
        for (String t : text.split("\\s+")) {
          q.add(termQuery(field, t, true));
        }
        return q;
      } else {
        return Collections.singletonList(termQuery(field, text, true));
      }
    } else {
      List<Query> q = new ArrayList<>();
      for (String t : Fields.toTerms(field, text, analyzer)) {
        q.add(termQuery(field, t, supportWildcards));
      }
      return q;
    }
  }

  /**
   * Returns the query corresponding to the specified text after parsing it.
   * <p>Supported operators are <code>AND</code> and <code>OR</code>, parentheses are also handled.
   *
   * <p>The examples below show the resulting query as a Lucene predicate from the text specified using "field" as the field name:
   * <pre>
   * |Big|             => field:Big
   * |Big Bang|        => field:Big field:Bang
   * |   Big   bang |  => field:Big field:Bang
   * |"Big Bang"|      => field:"Big Bang"
   * |Big AND Bang|    => +field:Big +field:Bang
   * |Big OR Bang|     => field:Big field:Bang
   * |"Big AND Bang"|  => field:"Big AND Bang"
   * |First "Big Bang"|  => field:First field:"Big bang"
   * |First "Big Bang|   => field:First field:"Big field:Bang
   * |First AND (Big Bang)|  => +field:First +(field:Big field:Bang)
   * </pre>
   *
   * @param field     the field to construct the terms.
   * @param text      the text to construct the query from.
   * @param analyzer  used to analyze the text
   *
   * @return the corresponding query.
   */
  @Beta
  public static Query parseToQuery(String field, String text, Analyzer analyzer) {
    return parseToQuery(field, text, analyzer, true);
  }

  /**
   * Returns the query corresponding to the specified text after parsing it.
   * <p>Supported operators are <code>AND</code> and <code>OR</code>, parentheses are also handled.
   *
   * <p>The examples below show the resulting query as a Lucene predicate from the text specified using "field" as the field name:
   * <pre>
   * |Big|             => field:Big
   * |Big Bang|        => field:Big field:Bang
   * |   Big   bang |  => field:Big field:Bang
   * |"Big Bang"|      => field:"Big Bang"
   * |Big AND Bang|    => +field:Big +field:Bang
   * |Big OR Bang|     => field:Big field:Bang
   * |"Big AND Bang"|  => field:"Big AND Bang"
   * |First "Big Bang"|  => field:First field:"Big bang"
   * |First "Big Bang|   => field:First field:"Big field:Bang
   * |First AND (Big Bang)|  => +field:First +(field:Big field:Bang)
   * </pre>
   *
   * @param field     the field to construct the terms.
   * @param text      the text to construct the query from.
   * @param analyzer  used to analyze the text
   * @param defaultOperatorOR if the operator between terms is OR or AND
   *
   * @return the corresponding query.
   */
  @Beta
  public static Query parseToQuery(String field, String text, Analyzer analyzer, boolean defaultOperatorOR) {
    return parseToQuery(field, text, analyzer, defaultOperatorOR, false);
  }

  /**
   * Returns the query corresponding to the specified text after parsing it.
   * <p>Supported operators are <code>AND</code> and <code>OR</code>, parentheses are also handled.
   *
   * <p>The examples below show the resulting query as a Lucene predicate from the text specified using "field" as the field name:
   * <pre>
   * |Big|             => field:Big
   * |Big Bang|        => field:Big field:Bang
   * |   Big   bang |  => field:Big field:Bang
   * |"Big Bang"|      => field:"Big Bang"
   * |Big AND Bang|    => +field:Big +field:Bang
   * |Big OR Bang|     => field:Big field:Bang
   * |"Big AND Bang"|  => field:"Big AND Bang"
   * |First "Big Bang"|  => field:First field:"Big bang"
   * |First "Big Bang|   => field:First field:"Big field:Bang
   * |First AND (Big Bang)|  => +field:First +(field:Big field:Bang)
   * </pre>
   *
   * @param field     the field to construct the terms.
   * @param text      the text to construct the query from.
   * @param analyzer  used to analyze the text
   * @param defaultOperatorOR if the operator between terms is OR or AND
   * 
   * @return the corresponding query.
   */
  @Beta
  public static Query parseToQuery(String field, String text, Analyzer analyzer, boolean defaultOperatorOR, boolean supportWildcards) {
    if (field == null) throw new NullPointerException("field");
    if (text == null) throw new NullPointerException("text");
    // shortcut for single word or single sentence
    if (!text.trim().matches(".*?\\s.*?") || isAPhrase(text) || (analyzer != null && !isTokenized(field, analyzer))) {
      if (analyzer == null) return toTermOrPhraseQuery(field, text, supportWildcards);
      return combine(defaultOperatorOR, toTermOrPhraseQueries(field, text, supportWildcards, analyzer));
    }
    // get last query
    Query query = null;
    boolean lastIsAND = !defaultOperatorOR;
    // parse text
    Pattern p = Pattern.compile("(\\([^\\(]+\\))|(\\S+)");
    Matcher m = p.matcher(text);
    while (m.find()) {
      // compute query for this item
      Query thisQuery = null;
      String g = m.group().trim();
      if (g.charAt(0) == '(' && g.charAt(g.length()-1) == ')') {                // parentheses?
        thisQuery = parseToQuery(field, g.substring(1, g.length()-1), analyzer, true, supportWildcards);
      } else if ("AND".equals(g)) {                                             // AND?
        lastIsAND = true;
      } else if ("OR".equals(g)) {                                              // OR?
        lastIsAND = false;
      } else if (analyzer == null) {                                            // phrase or normal word then
        thisQuery = toTermOrPhraseQuery(field, g, supportWildcards);
      } else {                                                                  // phrase or normal word then
        List<Query> combined = toTermOrPhraseQueries(field, g, supportWildcards, analyzer);
        // check if no resulting queries (word is a stop word for example)
        if (!combined.isEmpty()) thisQuery = combine(defaultOperatorOR, combined);
      }
      if (thisQuery != null) {
        if (query == null) {
          query = thisQuery;
        } else if (lastIsAND) {
          query = and(query, thisQuery);
        } else {
          query = or(query, thisQuery);
        }
        lastIsAND = !defaultOperatorOR;
      }
    }
    return query;
  }

  /**
   * Returns the terms for a field
   *
   * @param field    The field
   * @param text     The text to analyze
   * @param analyzer The analyzer
   *
   * @return the corresponding list of terms produced by the analyzer.
   */
  private static void addTermsToPhrase(String field, String text, Analyzer analyzer, PhraseQuery.Builder phrase) {
    try {
      TokenStream stream = analyzer.tokenStream(field, text);
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

  private static boolean isTokenized(String field, Analyzer analyzer) {
    // try to load terms for a phrase and return true if more than one term
    TokenStream stream = null;
    try {
      stream = analyzer.tokenStream(field, "word1 word2");
      stream.reset();
      if (stream.incrementToken()) {
        return stream.incrementToken();
      }
    } catch (IOException ex) {
      // Should not occur since we use a StringReader
      ex.printStackTrace();
    } finally {
      if (stream != null) try {
        stream.end();
        stream.close();
      } catch (IOException ex) {
        // Should not occur since we use a StringReader
        ex.printStackTrace();
      }
    }
    return false;
  }

  private static boolean hasWildcards(String text) {
    return text != null && (text.indexOf('?') != -1 || text.indexOf('*') != -1);
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
    else if (query instanceof BoostQuery) return substitute((BoostQuery)query, original, replacement);
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
    BooleanQuery.Builder q = new BooleanQuery.Builder();
    for (BooleanClause clause : query.clauses()) {
      Query qx = substitute(clause.getQuery(), original, replacement);
      q.add(qx, clause.getOccur());
    }
    return q.build();
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
  public static Query substitute(BoostQuery query, Term original, Term replacement) {
    return new BoostQuery(substitute(query.getQuery(), original, replacement), query.getBoost());
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
      PhraseQuery.Builder q = new PhraseQuery.Builder();
      for (Term t : query.getTerms()) {
        q.add(t.equals(original)? replacement : t);
      }
      q.setSlop(query.getSlop());
      return q.build();
    // No substitution return the query
    } else return query;
  }

}

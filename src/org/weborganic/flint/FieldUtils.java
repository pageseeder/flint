/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.weborganic.flint.legacy.LegacyLuceneVersion;

/**
 * A set of utility methods for dealing with search fields.
 *
 * @author Tu Tak Tran (Allette Systems)
 * @author Christophe Lauret (Weborganic)
 * 
 * @version 8 October 2009
 */
public final class FieldUtils {

  /**
   * The pattern used for removing unwanted characters from user queries.
   */
  private static final Pattern TOKENIZED_CLEANER = Pattern.compile("[^a-zA-Z0-9]+");

  /**
   * The pattern used for removing unwanted characters from user queries.
   */
  private static final Pattern UNTOKENIZED_CLEANER = Pattern.compile("[^a-zA-Z0-9\\-]+");

  /**
   * The list of stop words in used. 
   */
  private static final Set<?> STOP_WORDS = StandardAnalyzer.STOP_WORDS_SET;

  /**
   * Prevents creation of instances.
   */
  private FieldUtils() {
  }

//static helpers ---------------------------------------------------------------------------------

  /**
   * Returns a clean version of the specified parameter.
   * 
   * <p>This method removes any character that is not:
   * <ul>
   *   <li>A letter [a-z]</li>
   *   <li>A digit [0-9]</li>
   *   <li>A dash \- </li>
   *   <li>Double quotes " </li>
   * </ul>
   * 
   * <p>Spaces are normalised (only one between terms) and the string is trimmed.
   * 
   * <p>This method returns <code>null</code> if the specified parameter is <code>null</code>
   * or if the normalised and trimmed string is equal to an empty string "".
   * 
   * @param parameter The parameter to clean up.
   * 
   * @return A clean parameter. 
   */
  public static String cleanTokenized(String parameter) {
    if (parameter == null) return null;
    String clean = TOKENIZED_CLEANER.matcher(parameter).replaceAll(" ").toLowerCase().trim();
    return (clean.length() > 0)? clean : null;
  }

  /**
   * Returns a clean version of the specified parameter.
   * 
   * <p>This method removes any character that is not:
   * <ul>
   *   <li>A letter [a-z]</li>
   *   <li>A digit [0-9]</li>
   *   <li>A dash \- </li>
   *   <li>Double quotes " </li>
   * </ul>
   * 
   * <p>Spaces are normalised (only one between terms) and the string is trimmed.
   * 
   * <p>This method returns <code>null</code> if the specified parameter is <code>null</code>
   * or if the normalised and trimmed string is equal to an empty string "".
   * 
   * @param parameter The parameter to clean up.
   * 
   * @return A clean parameter. 
   */
  public static String cleanUntokenized(String parameter) {
    if (parameter == null) return null;
    String clean = UNTOKENIZED_CLEANER.matcher(parameter).replaceAll(" ").trim();
    return (clean.length() > 0)? clean : null;
  }

  /**
   * Returns an list of indexable terms from the specified value.
   * 
   * @param value       The value to parse.
   * @param isTokenized <code>true</code> if the word is tokenised;
   *                    <code>false</code> otherwise.
   * 
   * @return The list of indexable words (Strings).
   */
  public static List<String> toIndexWords(String value, boolean isTokenized) {
    if (value == null || "".equals(value)) return Collections.emptyList();
    String[] candidates = isTokenized? TOKENIZED_CLEANER.split(value.toLowerCase()) : UNTOKENIZED_CLEANER.split(value);
    List<String> words = new ArrayList<String>();
    for (String candidate : candidates) {
      if (!isStopWord(candidate) && !"".equals(candidate)) {
        words.add(candidate);
      }
    }
    return words;
  }

  /**
   * Parses the given value for the specified field using the query parser and the 
   * <code>StandardAnalyzer</code>. 
   * 
   * @param field The field for this query.
   * @param value The value to parse.
   * 
   * @return The corresponding query or <code>null</code>.
   * @deprecated 
   */
  public static Query parse(String field, String value) {
    try {
      QueryParser parser = new QueryParser(LegacyLuceneVersion.VERSION, field, new StandardAnalyzer(LegacyLuceneVersion.VERSION));
      parser.setDefaultOperator(QueryParser.AND_OPERATOR);

      // Escape before parsing
      String escapedValue = QueryParser.escape(value);
      Query query = parser.parse(escapedValue);
      // check whether the query is empty
      if ("".equals(query.toString())) return null;
      return query;
    } catch (Exception ex) {
// TODO      LOGGER.info("The query '"+value+"'+ for field '"+field+"' could not be parsed.", ex);
      return null;
    }
  }

  /**
   * Generates a query instances for the given field and for the specified value.
   * 
   * @param field The field for this query.
   * @param words The list of words.
   * 
   * @return The corresponding query or <code>null</code>.
   */
  public static Query toTermOrPhrase(String field, List<String> words) {
    if (words == null || words.size() == 0) return null;
    // a phrase
    if (words.size() > 1) {
      PhraseQuery query = new PhraseQuery();
      for (String word : words) {
        query.add(new Term(field, word));
      }
      return query;
    // a term
    } else {
      return new TermQuery(new Term(field, words.get(0).toString()));
    }
  }

  /**
   * Generates a query instances for the given field and for the specified value.
   * 
   * @param field The field for this query.
   * @param value The value to parse.
   * 
   * @return The corresponding query or <code>null</code>.
   */
  public static Query toTermOrPhrase(String field, String value) {
    if (value == null) return null;
    // a phrase
    if (value.indexOf(' ') != -1) {
      PhraseQuery query = new PhraseQuery();
      StringTokenizer tokenizer = new StringTokenizer(value, " \"");
      boolean isEmpty = true;
      while (tokenizer.hasMoreTokens()) {
        String token = tokenizer.nextToken();
        if (!isStopWord(token)) {
          query.add(new Term(field, token.toLowerCase()));
          isEmpty = false;
        }
      }
      return (isEmpty)? null : query;
    // a term
    } else {
      if (isStopWord(value)) return null;
      return new TermQuery(new Term(field, value.toLowerCase()));
    }
  }

  /**
   * Indicates whether the given query is empty.
   * 
   * @param q The query to test
   * 
   * @return <code>true</code> is the query does not contain any clause or subquery
   *         <code>false</code> otherwise.
   */
  public static boolean isEmpty(Query q) {
    if (q == null) return true;
    if ("".equals(q.toString())) return true;
    Set<Term> terms = new HashSet<Term>();
    q.extractTerms(terms);
    return terms.isEmpty();
  }

  /**
   * Indicates whether the specified word is a stop word for the <code>StandardAnalyzer</code>.
   * 
   * @param word The word to check.
   * 
   * @return <code>true</code> If the word is a stop word;
   *         <code>false</code> otherwise.
   */
  public static boolean isStopWord(String word) {
    for (Object stop : STOP_WORDS) {
      if (!(stop instanceof String))
        if (((String)stop).equalsIgnoreCase(word)) return true;
    }
    return false;
  }

  /**
   * Returns the list of stop words.
   * 
   * @return The list of stop words.
   */
  public static Set<?> listStopWords() {
    return Collections.unmodifiableSet(STOP_WORDS);
  }

  /**
   * Add to a <code>BooleanQuery</code> for <code>fieldName</code>, which 
   * searches for the terms (original and cleaned) given.
   * Adds <code>term</code> as a query, if original and uncleaned versions are 
   * the same or <code>cleanedTerm</code> is null, otherwise creates a boolean 
   * query of the original and cleaned versions of the term. 
   * 
   * @param fieldName The field in the index to query
   * @param boolQuery The boolean query to be added to
   * @param term The original term to search for
   * @param cleanedTerm The cleaned up term to search for. <code>null</code> if
   *        term is not cleaned
   */
  public static void addTermToQuery(String fieldName, BooleanQuery boolQuery, 
      String term, String cleanedTerm) {

    if ((term).equals(cleanedTerm) || (cleanedTerm == null)) { // term same as cleaned term
      Query termQ = FieldUtils.toTermOrPhrase(fieldName, term);
      if (termQ != null && !"".equals(termQ.toString())) {
        boolQuery.add(termQ , BooleanClause.Occur.SHOULD);
      }
    } else { // term is different to cleaned term - search for both

      // Boolean query - check cleaned and uncleaned term of user query
      BooleanQuery termBoolQ = new BooleanQuery();
      termBoolQ.add(FieldUtils.parse(fieldName, cleanedTerm), BooleanClause.Occur.SHOULD);
      termBoolQ.add(FieldUtils.parse(fieldName, term), BooleanClause.Occur.SHOULD);
      if (termBoolQ != null && !"".equals(termBoolQ.toString())) {
        boolQuery.add(termBoolQ , BooleanClause.Occur.SHOULD);
      }
    }
  }

}

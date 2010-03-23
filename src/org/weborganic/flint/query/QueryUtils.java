/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.FuzzyTermEnum;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.WildcardTermEnum;

/**
 * A set of utility methods related to predicates for index functions using Lucene.
 *
 * @author  Christophe Lauret (Weborganic)
 * 
 * @version 28 October 2006
 */
public final class QueryUtils {

  /**
   * The logger for this class.
   */
  private static final Logger LOGGER = Logger.getLogger(QueryUtils.class);

  /**
   * Prevents creation of instances. 
   */
  private QueryUtils() {
  }

  /**
   * Returns a collection of required terms from the specified query.
   *
   * <p>This method will only work for well formed Lucene queries.
   *
   * <p>NOTE: This method does not expand wildcard queries or fuzzy terms.
   *
   * @since Lucene 1.3
   * @since Java 5
   * 
   * @see org.apache.lucene.search.Query
   * @see org.apache.lucene.index.Term
   *
   * @param query The Lucene query
   *
   * @return a Collection of Terms
   */
  public static Collection<Term> getTerms(Query query) {
    ArrayList<Term> terms = new ArrayList<Term>();
    addTerms(terms, query);
    return terms;
  }

  /**
   * Add terms by going through the query in depth and recursively.
   * 
   * @param terms The list of terms being built.
   * @param query The query to analyse.
   * 
   * @throws NullPointerException If the list of terms is <code>null</code>.
   */
  private static void addTerms(Collection<Term> terms, Query query) throws NullPointerException {
    // do nothing if the query is null
    if (query != null) {

      // a term query, get term
      if (query instanceof TermQuery) {
        terms.add(((TermQuery)query).getTerm());

      // boolean query, evaluate each non prohibited clause
      } else if (query instanceof BooleanQuery) {
        BooleanClause[] clauses = ((BooleanQuery)query).getClauses();
        for (int i = 0; i < clauses.length; i++) {
          if (!clauses[i].isProhibited()) addTerms(terms, clauses[i].getQuery());
        }

      // a phrase query, get all terms
      } else if (query instanceof PhraseQuery) {
        Term[] ts = ((PhraseQuery)query).getTerms();
        for (int i = 0; i < ts.length; i++) {
          terms.add(ts[i]);
        }

      // a multi-term query, get the pattern term
      } else if (query instanceof MultiTermQuery) {
        Set<Term> tms = new HashSet<Term>();
        ((MultiTermQuery)query).extractTerms(tms);
        terms.addAll(tms);
      // ignore all other cases (inc. PrefixQuery & RangeQuery)
      }
    }
  }

  /**
   * Returns a collection of required phrases.
   *
   * <p>This method will only work for well formed Lucene queries.
   *
   * <p>NOTE: This method does not expand wildcard queries or fuzzy terms.
   *
   * @since Lucene 1.3
   *
   * @param query The Lucene query object.
   * 
   * @return A <code>Collection</code> of phrases.
   * 
   */
  public static Collection<PhraseQuery> getPhrases(Query query) {
    ArrayList<PhraseQuery> terms = new ArrayList<PhraseQuery>();
    addPhrases(terms, query);
    return terms;
  }

  /**
   * Adds phrases by going through the query in depth and recursively.
   *
   * @param phrases The list of phrases being built.
   * @param query   The query to analyse.
   *
   * @throws NullPointerException If the list of terms is <code>null</code>.
   */
  private static void addPhrases(Collection<PhraseQuery> phrases, Query query) throws NullPointerException {
    // do nothing if the query is null
    if (query != null) {

      // boolean query, evaluate each non prohibited clause
      if (query instanceof BooleanQuery) {
        BooleanClause[] clauses = ((BooleanQuery)query).getClauses();
        for (int i = 0; i < clauses.length; i++) {
          if (!clauses[i].isProhibited()) addPhrases(phrases, clauses[i].getQuery());
        }
      // a phrase query, get all terms
      } else if (query instanceof PhraseQuery) {
        phrases.add((PhraseQuery)query);
      // ignore all other cases (inc. PrefixQuery & RangeQuery)
      }
    }
  }

  /**
   * Return a collection of required top level terms or phrases.
   *
   * <p>This method will go through the Query instance and retrieve the phrases
   * and terms which are at the top level, so that only the terms exactly searched
   * will be returned in order, it will not decompose phrases into terms.
   *
   * <p>This method will also expand fuzzy / wildcard terms into a possible list
   * using the reader provided.
   *
   * <p>Note: This method will only work for well formed Lucene queries.
   *
   * @since Lucene 1.3
   *
   * @param ireader The index reader to use.
   * @param query   The Lucene query.
   *
   * @return the list of exact terms used.
   */
  public static List getExactTerms(IndexReader ireader, Query query) {
    ArrayList list = new ArrayList();
    try {
      addExactTerms(ireader, list, query);
    } catch (Exception ex) {
      LOGGER.error("Error whilst extracting exact terms", ex);
    }
    return list;
  }

  /**
   * Add the top level terms or phrases to the query.
   *
   * <p>This method does expand wildcard queries and fuzzy terms.
   *
   * <p>NOTE: recursive method.
   * 
   * @param ireader The index reader to use.
   * @param tops    The list of top level terms.
   * @param query   The Lucene query.
   * 
   * @throws IOException Should an I/O exception be thrown by the reader.
   */
  private static void addExactTerms(IndexReader ireader, List tops, Query query) throws IOException {
    // do nothing if the query is null
    if (query != null) {
      // boolean query, evaluate each non prohibited clause
      if (query instanceof BooleanQuery) {
        BooleanClause[] clauses = ((BooleanQuery)query).getClauses();
        for (int i = 0; i < clauses.length; i++) {
          if (!clauses[i].isProhibited()) addExactTerms(ireader, tops, clauses[i].getQuery());
        }
      // a phrase query, get all terms
      } else if (query instanceof PhraseQuery) {
        tops.add(0, query);
      // add the term of a term query
      } else if (query instanceof TermQuery) {
        tops.add(((TermQuery)query).getTerm());
      // expand the wildcard queries
      } else if (query instanceof WildcardQuery) {
        Term t = ((WildcardQuery)query).getTerm();
        WildcardTermEnum wildcards = new WildcardTermEnum(ireader, t);
        if (wildcards.term() != null)
          do {
            tops.add(wildcards.term().text());
          } while (wildcards.next());
      // expand the fuzzy queries
      } else if (query instanceof FuzzyQuery) {
        Term t = ((FuzzyQuery)query).getTerm();
        FuzzyTermEnum fuzzy = new FuzzyTermEnum(ireader, t);
        if (fuzzy.term() != null)
          do {
            tops.add(fuzzy.term().text());
          } while (fuzzy.next());
      }
    }
  }
}

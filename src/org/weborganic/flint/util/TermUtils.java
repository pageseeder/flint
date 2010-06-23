package org.weborganic.flint.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.FuzzyTermEnum;
import org.apache.lucene.search.PrefixTermEnum;

/**
 * A collection of utility methods to manipulate and extract terms.
 * 
 * @author Christophe Lauret
 * @version 23 June 2010
 */
public final class TermUtils {

  /** Utility class. */
  private TermUtils() {}

  /**
   * Returns the list of fuzzy terms given a term and using the specified index reader.
   * 
   * @see #loadPrefixTerms(IndexReader, List, Term)
   * 
   * @param reader Index reader to use.  
   * @param term   The term to use.
   * 
   * @return The corresponding list of fuzzy terms.
   */
  public static List<Term> getFuzzyTerms(IndexReader reader, Term term) throws IOException {
    List<Term> terms = new ArrayList<Term>();
    loadFuzzyTerms(reader, terms, term);
    return terms;
  }

  /**
   * Returns the list of prefix terms given a term and using the specified index reader.
   * 
   * @see #loadPrefixTerms(IndexReader, List, Term)
   * 
   * @param reader Index reader to use.  
   * @param term   The term to use.
   * 
   * @return The corresponding list of prefix terms.
   */
  public static List<Term> getPrefixTerms(IndexReader reader, Term term) throws IOException {
    List<Term> terms = new ArrayList<Term>();
    loadPrefixTerms(reader, terms, term);
    return terms;
  }

  /**
   * Loads all the fuzzy terms in the list of terms given the reader.
   * 
   * @param reader Index reader to use.  
   * @param terms  The list of terms to load.
   * @param term   The term to use.
   */
  public static void loadFuzzyTerms(IndexReader reader, List<Term> terms, Term term) throws IOException {
    FuzzyTermEnum e = new FuzzyTermEnum(reader, term);
    do {
      Term t = e.term();
      if (t != null) terms.add(t);
    } while (e.next());
    e.close();
  }

  /**
   * Loads all the prefix terms in the list of terms given the reader.
   * 
   * @param reader Index reader to use.  
   * @param terms  The list of terms to load.
   * @param term   The term to use.
   */
  public static void loadPrefixTerms(IndexReader reader, List<Term> terms, Term term) throws IOException {
    PrefixTermEnum e = new PrefixTermEnum(reader, term);
    do {
      Term t = e.term();
      if (t != null && !terms.contains(t)) terms.add(t);
    } while (e.next());
    e.close();
  }

}

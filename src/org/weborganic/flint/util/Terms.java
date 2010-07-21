package org.weborganic.flint.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
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
public final class Terms {

  /**
   * Compares terms using their text value instead of their field value.
   */
  private final static Comparator<Term> TEXT_COMPARATOR = new Comparator<Term>()  {
    /** {@inheritDoc} */
    public int compare(Term t1, Term t2) {
      return t1.text().compareTo(t2.text());
    }
  };

  /** Utility class. */
  private Terms() {}

  /**
   * Returns a comparator to order terms using their text value.
   * 
   * @return a comparator to order terms using their text value.
   */
  public static Comparator<Term> textComparator() {
    return TEXT_COMPARATOR;
  }

  /**
   * Returns the list of terms based on the given list of fields and texts.
   * 
   * <p>The number of the terms returns is (number of fields) x (number of texts).
   * 
   * @param fields The list of fields.  
   * @param texts  The list of texts. 
   * 
   * @return The corresponding list of terms.
   */
  public static List<Term> terms(List<String> fields, List<String> texts) throws IOException {
    List<Term> terms = new ArrayList<Term>();
    for (String field : fields) {
      for (String text : texts) {
        terms.add(new Term(field, text));
      }
    }
    return terms;
  }

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
  public static List<Term> fuzzy(IndexReader reader, Term term) throws IOException {
    List<Term> terms = new ArrayList<Term>();
    fuzzy(reader, terms, term);
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
  public static List<Term> prefix(IndexReader reader, Term term) throws IOException {
    List<Term> terms = new ArrayList<Term>();
    prefix(reader, terms, term);
    return terms;
  }

  /**
   * Loads all the fuzzy terms in the list of terms given the reader.
   * 
   * @param reader Index reader to use.  
   * @param terms  The list of terms to load.
   * @param term   The term to use.
   */
  public static void fuzzy(IndexReader reader, List<Term> terms, Term term) throws IOException {
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
  public static void prefix(IndexReader reader, List<Term> terms, Term term) throws IOException {
    PrefixTermEnum e = new PrefixTermEnum(reader, term);
    do {
      Term t = e.term();
      if (t != null && !terms.contains(t)) terms.add(t);
    } while (e.next());
    e.close();
  }

}

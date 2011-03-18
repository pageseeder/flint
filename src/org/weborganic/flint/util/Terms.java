package org.weborganic.flint.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.FuzzyTermEnum;
import org.apache.lucene.search.PrefixTermEnum;
import org.weborganic.flint.util.Bucket.Entry;

import com.topologi.diffx.xml.XMLWriter;

/**
 * A collection of utility methods to manipulate and extract terms.
 * 
 * @author Christophe Lauret
 * @version 18 March 2011
 */
public final class Terms {

  /**
   * Compares terms using their text value instead of their field value.
   */
  private static final Comparator<Term> TEXT_COMPARATOR = new Comparator<Term>()  {
    /** {@inheritDoc} */
    public int compare(Term t1, Term t2) {
      return t1.text().compareTo(t2.text());
    }
  };

  /** Utility class. */
  private Terms() {
  }

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
  public static List<Term> terms(List<String> fields, List<String> texts) {
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
   * 
   * @throws IOException If an error is thrown by the fuzzy term enumeration.
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
   * 
   * @throws IOException If an error is thrown by the prefix term enumeration.
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
   * 
   * @throws IOException If an error is thrown by the fuzzy term enumeration.
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
   * Loads all the fuzzy terms in the list of terms given the reader.
   * 
   * @param reader Index reader to use.
   * @param terms  The bucket of terms to load.
   * @param term   The term to use.
   * 
   * @throws IOException If an error is thrown by the fuzzy term enumeration.
   */
  @Beta public static void fuzzy(IndexReader reader, Bucket<Term> terms, Term term) throws IOException {
    FuzzyTermEnum e = new FuzzyTermEnum(reader, term);
    do {
      Term t = e.term();
      if (t != null) terms.add(t, e.docFreq());
    } while (e.next());
    e.close();
  }

  /**
   * Loads all the prefix terms in the list of terms given the reader.
   * 
   * @param reader Index reader to use.
   * @param terms  The list of terms to load.
   * @param term   The term to use.
   * 
   * @throws IOException If an error is thrown by the prefix term enumeration. 
   */
  public static void prefix(IndexReader reader, List<Term> terms, Term term) throws IOException {
    PrefixTermEnum e = new PrefixTermEnum(reader, term);
    do {
      Term t = e.term();
      if (t != null && !terms.contains(t)) terms.add(t);
    } while (e.next());
    e.close();
  }

  /**
   * Loads all the prefix terms in the list of terms given the reader.
   * 
   * @param reader Index reader to use.
   * @param terms  The list of terms to load.
   * @param term   The term to use.
   * 
   * @throws IOException If an error is thrown by the prefix term enumeration. 
   */
  @Beta public static void prefix(IndexReader reader, Bucket<Term> terms, Term term) throws IOException {
    PrefixTermEnum e = new PrefixTermEnum(reader, term);
    do {
      Term t = e.term();
      if (t != null) terms.add(t, e.docFreq());
    } while (e.next());
    e.close();
  }

  /**
   * Returns the list of terms for the specified field.
   * 
   * @param reader The index reader
   * @param field  The field
   * 
   * @return the list of terms for this field
   * 
   * @throws IOException should any IO error be reported by the {@link IndexReader#terms(Term)} method.
   */
  @Beta public static List<Term> terms(IndexReader reader, String field) throws IOException {
    TermEnum e = null;
    List<Term> terms = new ArrayList<Term>();
    try {
      e = reader.terms(new Term(field, ""));
      if (e.term() != null) {
        while (field.equals(e.term().field())) {
          terms.add(e.term());
          if (!e.next())
            break;
        }
      }
    } finally {
      e.close();
    }
    return terms;
  }

  /**
   * Returns the list of term values for the specified field.
   * 
   * @param reader The index reader to use
   * @param field  The field
   * 
   * @return the list of terms for this field
   * 
   * @throws IOException should any IO error be reported by the {@link IndexReader#terms(Term)} method.
   */
  @Beta public List<String> values(IndexReader reader, String field) throws IOException {
    List<String> values = new ArrayList<String>();
    TermEnum e = null;
    try {
      e = reader.terms(new Term(field, ""));
      if (e.term() != null) {
        while (field.equals(e.term().field())) {
          values.add(e.term().text());
          if (!e.next())
            break;
        }
      }
    } finally {
      e.close();
    }
    return values;
  }

  // XML Serialisers ==============================================================================

  /**
   * Returns the XML for a list of terms.
   * 
   * @param xml   The XML writer.
   * @param terms The list of terms to serialise as XML.
   * 
   * @throws IOException Any I/O error thrown by the XML writer.
   */
  public static void toXML(XMLWriter xml, List<Term> terms) throws IOException {
    for (Term t : terms) {
      toXML(xml, t);
    }
  }

  /**
   * Returns the XML for a list of terms.
   * 
   * @param xml   The XML writer.
   * @param terms The list of terms to serialise as XML.
   * 
   * @throws IOException Any I/O error thrown by the XML writer.
   */
  public static void toXML(XMLWriter xml, Bucket<Term> terms) throws IOException {
    for (Entry<Term> t : terms.entrySet()) {
      toXML(xml, t.item(), t.count());
    }
  }

  /**
   * Returns the XML for a term.
   * 
   * @param xml The XML writer.
   * @param t   Term to serialise as XML.
   * 
   * @throws IOException Any I/O error thrown by the XML writer.
   */
  public static void toXML(XMLWriter xml, Term t) throws IOException {
    xml.openElement("term");
    xml.attribute("field", t.field());
    xml.attribute("text", t.text());
    xml.closeElement();
  }

  /**
   * Returns the XML for a term.
   * 
   * @param xml       The XML writer.
   * @param t         Term to serialise as XML.
   * @param frequency The term document frequency.
   * 
   * @throws IOException Any I/O error thrown by the XML writer.
   */
  public static void toXML(XMLWriter xml, Term t, int frequency) throws IOException {
    xml.openElement("term");
    xml.attribute("field", t.field());
    xml.attribute("text", t.text());
    xml.attribute("frequency", frequency);
    xml.closeElement();
  }

}

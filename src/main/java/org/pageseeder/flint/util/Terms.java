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
package org.pageseeder.flint.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.search.suggest.Lookup.LookupResult;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.pageseeder.flint.api.Index;
import org.pageseeder.flint.util.Bucket.Entry;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A collection of utility methods to manipulate and extract terms.
 *
 * @author Christophe Lauret
 * @version 18 March 2011
 */
public final class Terms {

  /**
   * private logger
   */
  private final static Logger LOGGER = LoggerFactory.getLogger(Terms.class);

  /**
   * Compares terms using their text value instead of their field value.
   */
  private static final Comparator<Term> TEXT_COMPARATOR = new Comparator<Term>()  {
    /** {@inheritDoc} */
    @Override
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
  public static List<String> fuzzy(Index index, IndexReader reader, Term term) throws IOException {
    List<String> values = new ArrayList<String>();
    fuzzy(index, reader, values, term);
    return values;
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
  public static List<String> prefix(Index index, IndexReader reader, Term term) throws IOException {
    List<String> terms = new ArrayList<String>();
    prefix(index, reader, terms, term);
    return terms;
  }

  /**
   * Loads all the fuzzy terms in the list of terms given the reader.
   *
   * @param reader Index reader to use.
   * @param values The list of terms to load.
   * @param term   The term to use.
   *
   * @throws IOException If an error is thrown by the fuzzy term enumeration.
   */
  public static void fuzzy(Index index, IndexReader reader, List<String> values, Term term) throws IOException {
    SpellChecker spellchecker = new SpellChecker(new RAMDirectory());
    spellchecker.indexDictionary(new LuceneDictionary(reader, term.field()), new IndexWriterConfig(index.getAnalyzer()), false);
    String[] suggestions = spellchecker.suggestSimilar(term.text(), 10);
    spellchecker.close();
    values.addAll(Arrays.asList(suggestions));
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
  @Beta public static void fuzzy(Index index, IndexReader reader, Bucket<Term> terms, Term term) throws IOException {
    SpellChecker spellchecker = new SpellChecker(new RAMDirectory());
    spellchecker.indexDictionary(new LuceneDictionary(reader, term.field()), new IndexWriterConfig(index.getAnalyzer()), false);
    String[] suggestions = spellchecker.suggestSimilar(term.text(), 10);
    spellchecker.close();
    for (String suggestion : suggestions) {
      Term t = new Term(term.field(), suggestion);
      terms.add(t, reader.docFreq(t));
    }
  }

  /**
   * Loads all the prefix terms in the list of terms given the reader.
   *
   * @param reader  Index reader to use.
   * @param values  The list of values to load.
   * @param term    The term to use.
   *
   * @throws IOException If an error is thrown by the prefix term enumeration.
   */
  public static void prefix(Index index, IndexReader reader, List<String> values, Term term) throws IOException {
    AnalyzingInfixSuggester suggester = new AnalyzingInfixSuggester(new RAMDirectory(), index.getAnalyzer());
    try {
      suggester.build(new LuceneDictionary(reader, term.field()));
      List<LookupResult> results = suggester.lookup(term.text(), false, 10);
      if (results == null) return;
      for (LookupResult result : results) {
        String key = result.key.toString();
        if (key == null) break;
        values.add(key);
      }
    } finally {
      suggester.close();
    }
  }

  /**
   * Loads all the prefix terms in the list of terms given the reader.
   *
   * @param reader  Index reader to use.
   * @param values  The list of values to load.
   * @param term    The term to use.
   *
   * @throws IOException If an error is thrown by the prefix term enumeration.
   */
  public static void prefix(Index index, IndexReader reader, List<String> values, List<String> fields, String text) throws IOException {
    AnalyzingInfixSuggester suggester = new AnalyzingInfixSuggester(new RAMDirectory(), index.getAnalyzer());
    try {
      for (String field : fields)
        suggester.build(new LuceneDictionary(reader, field));
      List<LookupResult> results = suggester.lookup(text, 10, true, false);
      if (results == null) return;
      for (LookupResult result : results) {
        String key = result.key.toString();
        if (key == null) break;
        values.add(key);
      }
    } finally {
      suggester.close();
    }
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
    LOGGER.debug("Loading terms for field {}", field);
    List<Term> termsList = new ArrayList<Term>();
    Fields fields = MultiFields.getFields(reader);
    org.apache.lucene.index.Terms terms = fields.terms(field);
    if (terms == null) return termsList;
    TermsEnum termsEnum = terms.iterator();
    if (termsEnum == TermsEnum.EMPTY) return termsList;
    while (termsEnum.next() != null) {
      BytesRef t = termsEnum.term();
      if (t == null) break;
      termsList.add(new Term(field, BytesRef.deepCopyOf(t)));
    }
    return termsList;
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
  @Beta public static List<String> values(IndexReader reader, String field) throws IOException {
    LOGGER.debug("Loading term values for field {}", field);
    List<String> values = new ArrayList<String>();
    org.apache.lucene.index.Terms terms = MultiFields.getTerms(reader, field);
    if (terms == null) return values;
    TermsEnum termsEnum = terms.iterator();
    if (termsEnum == TermsEnum.EMPTY) return values;
    while (termsEnum.next() != null) {
      BytesRef t = termsEnum.term();
      if (t == null) break;
      values.add(t.utf8ToString());
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

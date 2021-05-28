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
package org.pageseeder.flint.lucene.search;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.automaton.CompiledAutomaton;
import org.pageseeder.flint.Index;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexIO;
import org.pageseeder.flint.lucene.LuceneIndexIO;
import org.pageseeder.flint.lucene.LuceneIndexQueries;
import org.pageseeder.flint.lucene.query.SearchResults;
import org.pageseeder.flint.lucene.util.Beta;
import org.pageseeder.flint.lucene.util.Bucket;
import org.pageseeder.flint.lucene.util.Bucket.Entry;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

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
   * @param reader Index reader to use.
   * @param term   The term to use.
   *
   * @return The corresponding list of fuzzy terms.
   *
   * @throws IOException If an error is thrown by the fuzzy term enumeration.
   */
  public static List<String> fuzzy(IndexReader reader, Term term) throws IOException {
    List<String> values = new ArrayList<String>();
    fuzzy(reader, values, term);
    return values;
  }

  /**
   * Returns the list of prefix terms given a term and using the specified index reader.
   *
   * @param reader Index reader to use.
   * @param term   The term to use.
   *
   * @return The corresponding list of prefix terms.
   *
   * @throws IOException If an error is thrown by the prefix term enumeration.
   */
  public static List<String> prefix(IndexReader reader, Term term) throws IOException {
    List<String> terms = new ArrayList<String>();
    prefix(reader, terms, term);
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
  public static void fuzzy(IndexReader reader, List<String> values, Term term) throws IOException {
    fuzzy(reader, values, term, 2);
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
  public static void fuzzy(IndexReader reader, List<String> values, Term term, int minSimilarity) throws IOException {
    AttributeSource atts = new AttributeSource();
    Fields fields = MultiFields.getFields(reader);
    org.apache.lucene.index.Terms terms = fields == null ? null : fields.terms(term.field());
    if (terms == null) return;
    FuzzyTermsEnum fuzzy = new FuzzyTermsEnum(terms, atts, term, minSimilarity, 0, false);
    BytesRef val;
    BytesRef searched = term.bytes();
    while ((val = fuzzy.next()) != null) {
      if (!searched.bytesEquals(val))
        values.add(val.utf8ToString());
    }
  }

  /**
   * Loads all the fuzzy terms in the list of terms given the reader.
   *
   * @param reader  Index reader to use.
   * @param bucket  Where to store the terms.
   * @param term    The term to use.
   *
   * @throws IOException If an error is thrown by the fuzzy term enumeration.
   */
  @Beta
  public static void fuzzy(IndexReader reader, Bucket<Term> bucket, Term term) throws IOException {
    fuzzy(reader, bucket, term, 2);
  }

  /**
   * Loads all the fuzzy terms in the list of terms given the reader.
   *
   * @param reader  Index reader to use.
   * @param bucket  Where to store the terms.
   * @param term    The term to use.
   *
   * @throws IOException If an error is thrown by the fuzzy term enumeration.
   */
  @Beta
  public static void fuzzy(IndexReader reader, Bucket<Term> bucket, Term term, int minSimilarity) throws IOException {
    AttributeSource atts = new AttributeSource();
    Fields fields = MultiFields.getFields(reader);
    org.apache.lucene.index.Terms terms = fields == null ? null : fields.terms(term.field());
    if (terms == null) return;
    FuzzyTermsEnum fuzzy = new FuzzyTermsEnum(terms, atts, term, minSimilarity, 0, true);
    BytesRef val;
    BytesRef searched = term.bytes();
    while ((val = fuzzy.next()) != null) {
      if (!searched.bytesEquals(val)) {
        Term t = new Term(term.field(), BytesRef.deepCopyOf(val));
        bucket.add(t, reader.docFreq(t));
      }
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
  public static void prefix(IndexReader reader, List<String> values, Term term) throws IOException {
    Fields fields = MultiFields.getFields(reader);
    org.apache.lucene.index.Terms terms = fields == null ? null : fields.terms(term.field());
    if (terms == null) return;
    TermsEnum prefixes = terms.intersect(new CompiledAutomaton(PrefixQuery.toAutomaton(term.bytes())), null);
    BytesRef val;
    while ((val = prefixes.next()) != null) {
      values.add(val.utf8ToString());
    }
  }

  /**
   * Loads all the prefix terms in the list of terms given the reader.
   *
   * @param reader  Index reader to use.
   * @param bucket  Where to store the terms.
   * @param term    The term to use.
   *
   * @throws IOException If an error is thrown by the prefix term enumeration.
   */
  public static void prefix(IndexReader reader, Bucket<Term> bucket, Term term) throws IOException {
    Fields fields = MultiFields.getFields(reader);
    org.apache.lucene.index.Terms terms = fields == null ? null : fields.terms(term.field());
    if (terms == null) return;
    TermsEnum prefixes = terms.intersect(new CompiledAutomaton(PrefixQuery.toAutomaton(term.bytes())), term.bytes());
    BytesRef val;
    while ((val = prefixes.next()) != null) {
      Term t = new Term(term.field(), BytesRef.deepCopyOf(val));
      bucket.add(t, reader.docFreq(t));
    }
  }

  /**
   * Returns the list of field names for the specified reader.
   *
   * @param reader The index reader
   *
   * @return the list of field names
   *
   * @throws IOException should any IO error be reported by the {@link MultiFields#getFields(IndexReader)} method.
   */
  @Beta public static List<String> fields(IndexReader reader) throws IOException {
    LOGGER.debug("Loading fields");
    List<String> fieldnames = new ArrayList<>();
    Fields fields = MultiFields.getFields(reader);
    if (fields == null) return fieldnames;
    for (String field : fields) {
      fieldnames.add(field);
    }
    return fieldnames;
  }


  /**
   * Returns the list of terms for the specified field.
   *
   * @param reader The index reader
   * @param field  The field
   *
   * @return the list of terms for this field
   *
   * @throws IOException should any IO error be reported.
   */
  @Beta public static List<Term> terms(IndexReader reader, String field) throws IOException {
    LOGGER.debug("Loading terms for field {}", field);
    org.apache.lucene.index.Terms terms = MultiFields.getTerms(reader, field);
    if (terms == null) return Collections.emptyList();
    TermsEnum termsEnum = terms.iterator();
    if (termsEnum == TermsEnum.EMPTY) return Collections.emptyList();
    Map<BytesRef, Term> termsList = new HashMap<>();
    while (termsEnum.next() != null) {
      BytesRef t = termsEnum.term();
      if (t == null) break;
      termsList.put(t, new Term(field, BytesRef.deepCopyOf(t)));
    }
    return new ArrayList<>(termsList.values());
  }

  /**
   * Returns the list of term fields from the list of the fields provided which are in the search results of the query provided.
   *
   * @param searcher   a searcher on the index desired
   * @param query      the base query
   * @param candidates the list of candidate fields
   *
   * @return the list of fields with search results
   *
   * @throws IOException should any IO error be reported when querying the index.
   */
  @Beta public static List<String> fields(IndexSearcher searcher, Query query, List<String> candidates) throws IOException {
    LOGGER.debug("Loading fields for query {}", query);
    List<String> fields = new ArrayList<>();
    for (String field : candidates) {
      FieldDocumentChecker checker = new FieldDocumentChecker(field);
      searcher.search(query, checker);
      if (checker.fieldFound()) fields.add(field);
    }
    return fields;
  }

  /**
   * Returns the list of term values for the specified field.
   *
   * @param reader The index reader to use
   * @param field  The field
   *
   * @return the list of terms for this field
   *
   * @throws IOException should any IO error be reported.
   */
  @Beta public static List<String> values(IndexReader reader, String field) throws IOException {
    LOGGER.debug("Loading term values for field {}", field);
    List<String> values = new ArrayList<>();
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

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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;
import org.weborganic.flint.IndexManager;
import org.weborganic.flint.util.Beta;
import org.weborganic.flint.util.Fields;
import org.weborganic.flint.util.Queries;
import org.weborganic.flint.util.Terms;

import com.topologi.diffx.xml.XMLWritable;
import com.topologi.diffx.xml.XMLWriter;

/**
 * A simple parameter to represent a Lucene predicate produced by a query parser.
 *
 * <p>The <i>predicate</i> is similar to the {@link Question} except that it can be used for
 * more powerful search; it is however more difficult to produce similar predicates.
 *
 * <p>It acts on one or multiple fields, each field can have a different boost level.
 *
 * <p>Use the factory methods to create new predicate.
 *
 * @author Christophe Lauret (Weborganic)
 * @version 16 August 2010
 */
@Beta
public final class Predicate implements SearchParameter, XMLWritable {

  /**
   * The lucene predicate entered by the user.
   */
  private final String _predicate;

  /**
   * The field names mapped to their boost value.
   */
  private final Map<String, Float> _fields;

  /**
   * The computed query.
   */
  private Query _query = null;

  // Constructors
  // ==============================================================================================

  /**
   * Creates a new question.
   *
   * <p>This is a low level API constructor; to ensure that this class works well, ensure that the
   * fields cannot be modified externally and that field names do not include empty strings.
   *
   * @param fields    The fields to search mapped to their respective boost.
   * @param predicate The text before parsing.
   *
   * @throws NullPointerException If either argument is <code>null</code>.
   */
  protected Predicate(Map<String, Float> fields, String predicate) throws NullPointerException {
    if (fields == null) throw new NullPointerException("fields");
    if (predicate == null) throw new NullPointerException("predicate");
    this._fields = fields;
    this._predicate = predicate;
  }

  // Methods
  // ==============================================================================================

  /**
   * Returns the list of fields this question applies to.
   *
   * @return the list of fields this question applies to.
   */
  public Collection<String> fields() {
    return this._fields.keySet();
  }

  /**
   * Returns the underlying predicate string.
   *
   * @return the underlying predicate string.
   */
  public String predicate() {
    return this._predicate;
  }

  /**
   * Returns the boost value for the specified field.
   *
   * @param field the name of the field.
   *
   * @return the corresponding boost value.
   */
  public float getBoost(String field) {
    Float boost = this._fields.get(field);
    return boost != null? boost.floatValue() : 1.0f;
  }

  /**
   * A question is empty if either the predicate or the fields are empty.
   *
   * @return <code>true</code> if either the predicate or the fields are empty;
   *         <code>false</code> if the predicate has a value and there is at least one field.
   */
  @Override
  public boolean isEmpty() {
    return this._predicate.isEmpty() || this._fields.isEmpty();
  }

  /**
   * Computes the query for this question.
   *
   * <p>This only needs to be done once.
   *
   * @param analyzer The analyser used by the underlying index.
   *
   * @throws ParseException If the question could not be parsed properly.
   */
  private void compute(Analyzer analyzer) throws ParseException {
    String[] fields = this._fields.keySet().toArray(new String[]{});
    MultiFieldQueryParser parser = new MultiFieldQueryParser(IndexManager.LUCENE_VERSION, fields, analyzer);
    this._query = parser.parse(this._predicate);
  }

  /**
   * Computes the query for this question using the {@link StandardAnalyzer}.
   *
   * <p>This method ignores any Lucene specific syntax by removing it from the input string.
   *
   * @throws ParseException If the question could not be parsed properly.
   */
  private void compute() throws ParseException {
    compute(new StandardAnalyzer(IndexManager.LUCENE_VERSION));
  }

  /**
   * Returns a list of predicates which are considered similar, that where one term was substituted
   * for a similar term.
   *
   * @param reader the reader to use to extract the similar (fuzzy) terms.
   *
   * @return a list of similar predicates.
   *
   * @throws IOException If thrown by the reader while getting the fuzzy terms.
   */
  public List<Predicate> similar(IndexReader reader) throws IOException {
    List<Predicate> similar = new ArrayList<Predicate>();
    // Extract the list of similar terms
    Set<Term> terms = new HashSet<Term>();
    this._query.extractTerms(terms);
    for (Term t : terms) {
      List<Term> fuzzy = Terms.fuzzy(reader, t);
      for (Term f : fuzzy) {
        Query sq = Queries.substitute(this._query, t, f);
        Predicate sqn = new Predicate(this._fields, sq.toString());
        sqn._query = sq;
        similar.add(sqn);
      }
    }
    return similar;
  }


  /**
   * Returns this object as Lucene query instance.
   *
   * @see #isEmpty()
   *
   * @return this object as a Lucene query instance, or <code>null</code> if this query is empty.
   * @throws IllegalStateException if the query has not been computed before - should not happen if using factory
   *           methods.
   */
  @Override
  public Query toQuery() throws IllegalStateException {
    // Return null if empty
    if (this.isEmpty()) { return null; }
    // Query was not computed
    if (this._query == null) throw new IllegalStateException("Query has not been computed - call compute(Analyzer)");
    return this._query;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("predicate", true);
    // indicate whether this search term is empty
    xml.attribute("is-empty", Boolean.toString(this.isEmpty()));
    if (this._query != null) {
      xml.attribute("query", this._query.toString());
    }
    // details of the question
    for (Entry<String, Float> field : this._fields.entrySet()) {
      xml.openElement("field");
      Float boost = field.getValue();
      xml.attribute("boost", boost != null? boost.toString() : "1.0");
      xml.writeText(field.getKey());
      xml.closeElement();
    }
    xml.element("text", this._predicate);
    xml.closeElement();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return this._predicate + " in " + this._fields.entrySet();
  }

  // Factory methods
  // ==============================================================================================

  /**
   * A factory method to create a new predicate and compute it using the Lucene {@link MultiFieldQueryParser}.
   *
   * @param field     The default field for the predicate.
   * @param predicate The predicate to parse
   * @param analyzer  The analyser to use when parsing the predicate.
   *
   * @return a new predicate.
   *
   * @throws ParseException if the predicate could not be parsed.
   */
  public static Predicate newPredicate(String field, String predicate, Analyzer analyzer) throws ParseException {
    Map<String, Float> fields = Collections.singletonMap(field, 1.0f);
    Predicate q = new Predicate(fields, predicate);
    q.compute(analyzer);
    return q;
  }

  /**
   * A factory method to create a new predicate and compute it using the Lucene {@link MultiFieldQueryParser}.
   *
   * @param fields    The list of default fields for the predicate.
   * @param predicate The predicate to parse
   * @param analyzer  The analyser to use when parsing the predicate.
   *
   * @return a new predicate.
   *
   * @throws ParseException if the predicate could not be parsed.
   */
  public static Predicate newPredicate(List<String> fields, String predicate, Analyzer analyzer) throws ParseException {
    List<String> names = Fields.filterNames(fields);
    Map<String, Float> map = Fields.asBoostMap(names);
    Predicate q = new Predicate(map, predicate);
    q.compute(analyzer);
    return q;
  }

  /**
   * A factory method to create a new question and compute it using the Lucene {@link MultiFieldQueryParser}.
   *
   * @param fields    The list of fields for the question.
   * @param predicate The predicate to parse
   * @param analyzer  The analyser to use when parsing the predicate.
   *
   * @return a new predicate.
   *
   * @throws ParseException if the predicate could not be parsed.
   */
  public static Predicate newPredicate(Map<String, Float> fields, String predicate, Analyzer analyzer)
      throws ParseException {
    Predicate q = new Predicate(fields, predicate);
    q.compute(analyzer);
    return q;
  }

  /**
   * A factory method to create a new question and compute it using the Lucene {@link MultiFieldQueryParser}
   * and the {@link StandardAnalyzer}.
   *
   * @param fields    The list of fields for the question.
   * @param predicate The predicate to parse
   *
   * @return a new predicate.
   *
   * @throws ParseException if the predicate could not be parsed.
   */
  public static Predicate newPredicate(List<String> fields, String predicate) throws ParseException {
    List<String> names = Fields.filterNames(fields);
    Map<String, Float> map = Fields.asBoostMap(names);
    Predicate q = new Predicate(map, predicate);
    q.compute();
    return q;
  }

  /**
   * A factory method to create a new question and compute it using the Lucene {@link MultiFieldQueryParser}
   * and the {@link StandardAnalyzer}.
   *
   * @param fields    The list of fields for the question.
   * @param predicate The predicate to parse
   *
   * @return a new predicate.
   *
   * @throws ParseException if the predicate could not be parsed.
   */
  public static Predicate newPredicate(Map<String, Float> fields, String predicate) throws ParseException {
    Predicate q = new Predicate(fields, predicate);
    q.compute();
    return q;
  }

}

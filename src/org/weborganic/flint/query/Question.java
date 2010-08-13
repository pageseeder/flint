/*
 * This file is part of the Flint library.
 * 
 * For licensing information please see the file license.txt included in the release. A copy of this licence can also be
 * found at http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.query;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanClause.Occur;
import org.weborganic.flint.IndexManager;
import org.weborganic.flint.util.Beta;
import org.weborganic.flint.util.Fields;
import org.weborganic.flint.util.Queries;

import com.topologi.diffx.xml.XMLWritable;
import com.topologi.diffx.xml.XMLWriter;

/**
 * A simple parameter to capture a search entered by a user.
 * 
 * <p>The <i>question</i> is what the user enters in the input box and its scope can be a number of fields.
 * 
 * <p>It acts on one or multiple fields, each field can have a different boost level.
 * 
 * <p>Use the factory methods to create new question.
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 13 August 2010
 */
@Beta
public final class Question implements SearchParameter, XMLWritable {

  /**
   * The search value
   */
  private final String _question;

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
   * @param fields   The fields to search mapped to their respective boost.
   * @param question The text entered by the user.
   * 
   * @throws NullPointerException If either argument is <code>null</code>.
   */
  protected Question(Map<String, Float> fields, String question) throws NullPointerException {
    if (fields == null) throw new NullPointerException("fields");
    if (question == null) throw new NullPointerException("question");
    this._fields = fields;
    this._question = question;
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
   * Returns the underlying question string.
   * 
   * @return the underlying questions string.
   */
  public String question() {
    return this._question;
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
   * A question is empty if either the question or the fields are empty.
   * 
   * @return <code>true</code> if either the question or the fields are empty;
   *         <code>false</code> if the question has a value and there is at least one field.
   */
  public boolean isEmpty() {
    return this._question.isEmpty() || this._fields.isEmpty();
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
  protected void compute(Analyzer analyzer) throws ParseException {
    String[] fields = this._fields.keySet().toArray(new String[]{});
    MultiFieldQueryParser parser = new MultiFieldQueryParser(IndexManager.LUCENE_VERSION, fields, analyzer);
    this._query = parser.parse(this._question);
  }

  /**
   * Computes the query for this question by removing any special character from the question and
   * closing quotation marks.
   * 
   * <p>This method ignores any Lucene specific syntax by removing it from the input string.
   */
  protected void compute() {
    List<String> values = Fields.toValues(this._question);
    BooleanQuery query = new BooleanQuery();
    for (String value : values) {
      for (Entry<String, Float> e : this._fields.entrySet()) {
        Query q = Queries.toTermOrPhraseQuery(e.getKey(), value);
        q.setBoost(e.getValue());
        query.add(q, Occur.SHOULD);
      }
    }
    this._query = query;
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
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("question");
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
    xml.element("text", this._question);
    xml.closeElement();
  }

  /**
   * {@inheritDoc}
   */
  public String toString() {
    return this._question + " in " + this._fields.entrySet();
  }

  // Factory methods
  // ==============================================================================================

  /**
   * A factory method to create a new question and compute it using the Lucene {@link MultiFieldQueryParser}.
   * 
   * @param field    The field for the question.
   * @param question The question itself.
   * @param analyzer The analyser to use when parsing the question.
   * 
   * @return a new question.
   * 
   * @throws ParseException if the question could not be parsed.
   */
  public static Question newQuestion(String field, String question, Analyzer analyzer) throws ParseException {
    Map<String, Float> fields = Collections.singletonMap(field, 1.0f);
    Question q = new Question(fields, question);
    q.compute(analyzer);
    return q;
  }

  /**
   * A factory method to create a new question and compute it using the Lucene {@link MultiFieldQueryParser}.
   * 
   * @param fields   The list of fields for the question.
   * @param question The question itself.
   * @param analyzer The analyser to use when parsing the question.
   * 
   * @return a new question.
   * 
   * @throws ParseException if the question could not be parsed.
   */
  public static Question newQuestion(List<String> fields, String question, Analyzer analyzer) throws ParseException {
    List<String> names = Fields.filterNames(fields);
    Map<String, Float> map = Fields.asBoostMap(names);
    Question q = new Question(map, question);
    q.compute(analyzer);
    return q;
  }

  /**
   * A factory method to create a new question and compute it using the Lucene {@link MultiFieldQueryParser}.
   * 
   * @param fields The list of fields for the question.
   * @param question The question itself.
   * @param analyzer The analyser to use when parsing the question.
   *
   * @return a new question.
   * 
   * @throws ParseException if the question could not be parsed.
   */
  public static Question newQuestion(Map<String, Float> fields, String question, Analyzer analyzer) 
      throws ParseException {
    Question q = new Question(fields, question);
    q.compute(analyzer);
    return q;
  }

  /**
   * A factory method to create a new question and compute it using the basic syntax.
   * 
   * @param fields The list of fields for the question.
   * @param question The question itself.
   *
   * @return a new question.
   */
  public static Question newQuestion(List<String> fields, String question) {
    List<String> names = Fields.filterNames(fields);
    Map<String, Float> map = Fields.asBoostMap(names);
    Question q = new Question(map, question);
    q.compute();
    return q;
  }

  /**
   * A factory method to create a new question and compute it using the basic syntax.
   * 
   * @param fields The list of fields for the question.
   * @param question The question itself.
   *
   * @return a new question.
   */
  public static Question newQuestion(Map<String, Float> fields, String question) {
    Question q = new Question(fields, question);
    q.compute();
    return q;
  }

}

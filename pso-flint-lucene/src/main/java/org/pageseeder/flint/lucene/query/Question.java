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
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Query;
import org.pageseeder.flint.catalog.Catalog;
import org.pageseeder.flint.lucene.search.Fields;
import org.pageseeder.flint.lucene.search.Terms;
import org.pageseeder.flint.lucene.util.Beta;
import org.pageseeder.xmlwriter.XMLWritable;
import org.pageseeder.xmlwriter.XMLWriter;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

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
   * If operators are supported in the question
   */
  private final boolean _supportOperators;

  /**
   * If the default operator between terms is OR or AND
   */
  private final boolean _defaultOperatorOR;

  /**
   * If wildcards '?' and '*' are supported
   */
  private final boolean _supportWildcards;

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
   * @param fields            The fields to search mapped to their respective boost.
   * @param question          The text entered by the user.
   * @param supportWildcards  If wildcards ('?', '*') are supported in the question
   * @param supportOperators  If operators (AND, OR) are supported in the question
   * @param defaultOperatorOR If the default operator between terms is OR or AND
   *
   * @throws NullPointerException If either argument is <code>null</code>.
   */
  protected Question(Map<String, Float> fields, String question, boolean supportWildcards,
                     boolean supportOperators, boolean defaultOperatorOR) throws NullPointerException {
    if (fields == null) throw new NullPointerException("fields");
    if (question == null) throw new NullPointerException("question");
    this._fields = fields;
    this._question = question;
    this._supportWildcards = supportWildcards;
    this._supportOperators = supportOperators;
    this._defaultOperatorOR = defaultOperatorOR;
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
  @Override
  public boolean isEmpty() {
    return this._question.isEmpty() || this._fields.isEmpty();
  }

  /**
   * Computes the query for this question.
   *
   * <p>This only needs to be done once.
   *
   * @param analyzer The analyser used by the underlying index.
   * @param catalog  The catalog to know if a field is analyzed or not
   */
  private void compute(Analyzer analyzer, Catalog catalog) {
    BooleanQuery.Builder query = new BooleanQuery.Builder();
    if (this._supportOperators) {
      for (Entry<String, Float> e : this._fields.entrySet()) {
        Query q = catalog != null && !catalog.isTokenized(e.getKey()) && Queries.hasNoOperators(this._question) ?
            Queries.termQuery(e.getKey(), this._question, this._supportWildcards) :
            Queries.parseToQuery(e.getKey(), this._question, analyzer, this._defaultOperatorOR, this._supportWildcards);
        if (e.getValue() != 1f) q = new BoostQuery(q, e.getValue());
        query.add(q, Occur.SHOULD);
      }
    } else {
      Occur with = this._defaultOperatorOR ? Occur.SHOULD : Occur.MUST;
      List<String> values = Fields.toValues(this._question);
      for (Entry<String, Float> e : this._fields.entrySet()) {
        boolean analyzed = catalog == null || catalog.isTokenized(e.getKey());
        BooleanQuery.Builder fieldQuery = new BooleanQuery.Builder();
        for (String value : values) {
          if (!analyzed) {
            Query q = Queries.termQuery(e.getKey(), value, this._supportWildcards);
            if (e.getValue() != 1f) q = new BoostQuery(q, e.getValue());
            fieldQuery.add(q, with);
          } else {
            for(Query q : Queries.toTermOrPhraseQueries(e.getKey(), value, this._supportWildcards, analyzer)) {
              if (e.getValue() != 1f) q = new BoostQuery(q, e.getValue());
              fieldQuery.add(q, with);
            }
          }
        }
        query.add(fieldQuery.build(), Occur.SHOULD);
      }
    }
    this._query = query.build();
  }

  /**
   * Computes the query for this question by removing any special character from the question and
   * closing quotation marks.
   *
   * <p>This method ignores any Lucene specific syntax by removing it from the input string.
   */
  private void compute() {
    BooleanQuery.Builder query = new BooleanQuery.Builder();
    if (this._supportOperators) {
      for (Entry<String, Float> e : this._fields.entrySet()) {
        Query q = Queries.parseToQuery(e.getKey(), this._question, null, false, this._supportWildcards);
        query.add(new BoostQuery(q, e.getValue()), Occur.SHOULD);
      }
    } else {
      List<String> values = Fields.toValues(this._question);
      for (Entry<String, Float> e : this._fields.entrySet()) {
        BooleanQuery.Builder sub = new BooleanQuery.Builder();
        for (String value : values) {
          Query q = Queries.toTermOrPhraseQuery(e.getKey(), value, this._supportWildcards);
          sub.add(new BoostQuery(q, e.getValue()), Occur.SHOULD);
        }
        query.add(sub.build(), Occur.SHOULD);
      }
    }
    this._query = query.build();
  }

  /**
   * Returns a list of questions which are considered similar, that where one term was substituted
   * for a similar term.
   * Max size is set to 50.
   *
   * @param reader the reader to use to extract the similar (fuzzy) terms.
   *
   * @return a list of similar questions.
   *
   * @throws IOException If thrown by the reader while getting the fuzzy terms.
   */
  public List<Question> similar(IndexReader reader) throws IOException {
    return similar(reader, 50);
  }

  /**
   * Returns a list of questions which are considered similar, that where one term was substituted
   * for a similar term.
   *
   * @param reader  the reader to use to extract the similar (fuzzy) terms.
   * @param maxSize the maximum number of suggestions to return
   *
   * @return a list of similar questions.
   *
   * @throws IOException If thrown by the reader while getting the fuzzy terms.
   */
  public List<Question> similar(IndexReader reader, int maxSize) throws IOException {
    List<Question> similar = new ArrayList<Question>();
    // If the question contains a phrase, try removing the phrase
    if (this._question.indexOf('"') >= 0) {
      Question nophrase = newQuestion(this._fields, this._question.replace('"', ' '));
      nophrase.compute();
      similar.add(nophrase);

    // No phrase, try substitution for each term
    } else {
      List<String> values = Fields.toValues(this._question);
      big: for (String value : values) {
        Set<String> fuzzy = new HashSet<String>();
        // collect fuzzy terms based on index
        for (String field : this._fields.keySet()) {
          List<String> fuzzies = Terms.fuzzy(reader, new Term(field, value));
          fuzzy.addAll(fuzzies);
        }
        // rewrite question
        for (String x : fuzzy) {
          Question q = newQuestion(this._fields, this._question.replaceAll("\\Q"+value+"\\E", x));
          q.compute();
          similar.add(q);
          if (similar.size() >= maxSize) break big;
        }
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
    if (isEmpty()) return null;
    // Query was not computed
    if (this._query == null) throw new IllegalStateException("Query has not been computed - call compute(Analyzer)");
    return this._query;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("question", true);
    // indicate whether this search term is empty
    xml.attribute("is-empty", Boolean.toString(isEmpty()));
    xml.attribute("operators", this._supportOperators ? "true" : "false");
    xml.attribute("wildcards", this._supportWildcards ? "true" : "false");
    xml.attribute("default-operator", this._defaultOperatorOR ? "or" : "and");
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
  @Override
  public String toString() {
    return this._question + " in " + this._fields.entrySet();
  }

  // Factory methods
  // ==============================================================================================

  /**
   * A factory method to create a new question and compute it using the basic syntax.
   *
   * @param field The field for the question.
   * @param question The question itself.
   *
   * @deprecated use {@link Builder} class
   *
   * @return a new question.
   */
  public static Question newQuestion(String field, String question) {
    return newQuestion(Collections.singletonMap(field, 1.0f), question);
  }

  /**
   * A factory method to create a new question and compute it using a simple tokenizer.
   *
   * @param field    The field for the question.
   * @param question The question itself.
   * @param analyzer The analyser to use when parsing the question.
   *
   * @deprecated use {@link Builder} class
   *
   * @return a new question.
   */
  public static Question newQuestion(String field, String question, Analyzer analyzer) {
    return newQuestion(field, question, analyzer, null);
  }

  /**
   * A factory method to create a new question and compute it using a simple tokenizer.
   *
   * @param field    The field for the question.
   * @param question The question itself.
   * @param analyzer The analyser to use when parsing the question.
   *
   * @deprecated use {@link Builder} class
   *
   * @return a new question.
   */
  public static Question newQuestion(String field, String question, Analyzer analyzer, Catalog catalog) {
    return newQuestion(Collections.singletonMap(field, 1.0f), question, analyzer, catalog);
  }

  /**
   * A factory method to create a new question and compute it using the basic syntax.
   *
   * @param fields The list of fields for the question.
   * @param question The question itself.
   *
   * @deprecated use {@link Builder} class
   *
   * @return a new question.
   */
  public static Question newQuestion(List<String> fields, String question) {
    return newQuestion(Fields.asBoostMap(Fields.filterNames(fields)), question);
  }

  /**
   * A factory method to create a new question and compute it using a simple tokenizer.
   *
   * @param fields   The list of fields for the question.
   * @param question The question itself.
   * @param analyzer The analyser to use when parsing the question.
   *
   * @deprecated use {@link Builder} class
   *
   * @return a new question.
   */
  public static Question newQuestion(List<String> fields, String question, Analyzer analyzer) {
    return newQuestion(fields, question, analyzer, null);
  }

  /**
   * A factory method to create a new question and compute it using a simple tokenizer.
   *
   * @param fields   The list of fields for the question.
   * @param question The question itself.
   * @param analyzer The analyser to use when parsing the question.
   *
   * @deprecated use {@link Builder} class
   *
   * @return a new question.
   */
  public static Question newQuestion(List<String> fields, String question, Analyzer analyzer, Catalog catalog) {
    return newQuestion(Fields.asBoostMap(Fields.filterNames(fields)), question, analyzer, catalog);
  }

  /**
   * A factory method to create a new question and compute it using the basic syntax.
   *
   * @param fields The list of fields for the question.
   * @param question The question itself.
   *
   * @deprecated use {@link Builder} class
   *
   * @return a new question.
   */
  public static Question newQuestion(Map<String, Float> fields, String question) {
    return newQuestion(fields, question, true);
  }

  /**
   * A factory method to create a new question and compute it using the basic syntax.
   *
   * @param fields The list of fields for the question.
   * @param question The question itself.
   * @param defaultOperatorOR If the default operator between terms is OR
   *
   * @deprecated use {@link Builder} class
   *
   * @return a new question.
   */
  public static Question newQuestion(Map<String, Float> fields, String question, boolean defaultOperatorOR) {
    return new Builder()
        .fields(fields)
        .question(question)
        .defaultOperatorOR(defaultOperatorOR)
        .supportOperators(false).build();
  }

  /**
   * A factory method to create a new question and compute it using a simple tokenizer.
   *
   * @param fields The list of fields for the question.
   * @param question The question itself.
   * @param analyzer The analyser to use when parsing the question.
   *
   * @deprecated use {@link Builder} class
   *
   * @return a new question.
   */
  public static Question newQuestion(Map<String, Float> fields, String question, Analyzer analyzer) {
    return newQuestion(fields, question, analyzer, null);
  }

  /**
   * A factory method to create a new question and compute it using a simple tokenizer.
   *
   * @param fields The list of fields for the question.
   * @param question The question itself.
   * @param analyzer The analyser to use when parsing the question.
   *
   * @deprecated use {@link Builder} class
   *
   * @return a new question.
   */
  public static Question newQuestion(Map<String, Float> fields, String question, Analyzer analyzer, Catalog catalog) {
    return newQuestion(fields, question, analyzer, catalog, true);
  }

  /**
   * A factory method to create a new question and compute it using a simple tokenizer.
   *
   * @param fields The list of fields for the question.
   * @param question The question itself.
   * @param analyzer The analyser to use when parsing the question.
   * @param defaultOperatorOR If the default operator between terms is OR
   *
   * @deprecated use {@link Builder} class
   *
   * @return a new question.
   */
  public static Question newQuestion(Map<String, Float> fields, String question, Analyzer analyzer,
                                     Catalog catalog, boolean defaultOperatorOR) {
    return new Builder()
        .fields(fields)
        .question(question)
        .analyzerAndCatalog(analyzer, catalog)
        .defaultOperatorOR(defaultOperatorOR)
        .supportOperators(false).build();
  }

  /**
   * A factory method to create a new question and compute it using the basic syntax.
   *
   * @param field The field for the question.
   * @param question The question itself.
   *
   * @deprecated use {@link Builder} class
   *
   * @return a new question.
   */
  public static Question newQuestionWithOperators(String field, String question) {
    return newQuestionWithOperators(Collections.singletonMap(field, 1.0f), question);
  }

  /**
   * A factory method to create a new question and compute it using a simple tokenizer.
   *
   * @param field    The field for the question.
   * @param question The question itself.
   * @param analyzer The analyser to use when parsing the question.
   *
   * @deprecated use {@link Builder} class
   *
   * @return a new question.
   */
  public static Question newQuestionWithOperators(String field, String question, Analyzer analyzer) {
    return newQuestionWithOperators(field, question, analyzer, null);
  }

  /**
   * A factory method to create a new question and compute it using a simple tokenizer.
   *
   * @param field    The field for the question.
   * @param question The question itself.
   * @param analyzer The analyser to use when parsing the question.
   *
   * @deprecated use {@link Builder} class
   *
   * @return a new question.
   */
  public static Question newQuestionWithOperators(String field, String question, Analyzer analyzer, Catalog catalog) {
    return newQuestion(Collections.singletonMap(field, 1.0f), question, analyzer, catalog);
  }

  /**
   * A factory method to create a new question and compute it using the basic syntax.
   *
   * @param fields The list of fields for the question.
   * @param question The question itself.
   *
   * @deprecated use {@link Builder} class
   *
   * @return a new question.
   */
  public static Question newQuestionWithOperators(List<String> fields, String question) {
    return newQuestionWithOperators(Fields.asBoostMap(Fields.filterNames(fields)), question);
  }

  /**
   * A factory method to create a new question and compute it using a simple tokenizer.
   *
   * @param fields   The list of fields for the question.
   * @param question The question itself.
   * @param analyzer The analyser to use when parsing the question.
   *
   * @deprecated use {@link Builder} class
   *
   * @return a new question.
   */
  public static Question newQuestionWithOperators(List<String> fields, String question, Analyzer analyzer) {
    return newQuestionWithOperators(fields, question, analyzer, null);
  }

  /**
   * A factory method to create a new question and compute it using a simple tokenizer.
   *
   * @param fields   The list of fields for the question.
   * @param question The question itself.
   * @param analyzer The analyser to use when parsing the question.
   *
   * @deprecated use {@link Builder} class
   *
   * @return a new question.
   */
  public static Question newQuestionWithOperators(List<String> fields, String question,
                                                  Analyzer analyzer, Catalog catalog) {
    return newQuestionWithOperators(Fields.asBoostMap(Fields.filterNames(fields)), question, analyzer, catalog);
  }

  /**
   * A factory method to create a new question and compute it using the basic syntax.
   *
   * @param fields The list of fields for the question.
   * @param question The question itself.
   *
   * @deprecated use {@link Builder} class
   *
   * @return a new question.
   */
  public static Question newQuestionWithOperators(Map<String, Float> fields, String question) {
    return new Question.Builder().fields(fields).question(question).build();
  }

  /**
   * A factory method to create a new question and compute it using a simple tokenizer.
   *
   * @param fields The list of fields for the question.
   * @param question The question itself.
   * @param analyzer The analyser to use when parsing the question.
   *
   * @deprecated use {@link Builder} class
   *
   * @return a new question.
   */
  public static Question newQuestionWithOperators(Map<String, Float> fields, String question, Analyzer analyzer) {
    return newQuestionWithOperators(fields, question, analyzer, null);
  }

  /**
   * A factory method to create a new question and compute it using a simple tokenizer.
   *
   * @param fields The list of fields for the question.
   * @param question The question itself.
   * @param analyzer The analyser to use when parsing the question.
   *
   * @deprecated use {@link Builder} class
   *
   * @return a new question.
   */
  public static Question newQuestionWithOperators(Map<String, Float> fields, String question,
                                                  Analyzer analyzer, Catalog catalog) {
    return newQuestionWithOperators(fields, question, analyzer, catalog, true);
  }

  /**
   * A factory method to create a new question and compute it using a simple tokenizer.
   *
   * @param fields The list of fields for the question.
   * @param question The question itself.
   * @param analyzer The analyser to use when parsing the question.
   * @param defaultOperatorOR If the default operator between terms is OR
   *
   * @deprecated use {@link Builder} class
   *
   * @return a new question.
   */
  public static Question newQuestionWithOperators(Map<String, Float> fields, String question,
                                                  Analyzer analyzer, Catalog catalog, boolean defaultOperatorOR) {
    return new Question.Builder()
        .fields(fields)
        .question(question)
        .analyzerAndCatalog(analyzer, catalog)
        .defaultOperatorOR(defaultOperatorOR)
        .build();
  }

  public static class Builder {
    private boolean supportOperators = true;
    private boolean defaultOperatorOR = true;
    private boolean supportWildcards = false;
    private String question = null;
    private Map<String, Float> fields = null;
    private Analyzer analyzer = null;
    private Catalog catalog = null;
    public Builder field(String fieldname) {
      this.fields = Fields.asBoostMap(Fields.filterNames(Collections.singletonList(fieldname)));
      return this;
    }
    public Builder fields(List<String> fs) {
      this.fields = Fields.asBoostMap(Fields.filterNames(fs));
      return this;
    }
    public Builder fields(Map<String, Float> fs) {
      this.fields = fs;
      return this;
    }
    public Builder question(String q) {
      this.question = q;
      return this;
    }
    public Builder supportWildcards(boolean support) {
      this.supportWildcards = support;
      return this;
    }
    public Builder supportOperators(boolean support) {
      this.supportOperators = support;
      return this;
    }
    public Builder defaultOperatorOR(boolean or) {
      this.defaultOperatorOR = or;
      return this;
    }
    public Builder analyzerAndCatalog(Analyzer a, Catalog c) {
      this.analyzer = a;
      this.catalog = c;
      return this;
    }
    public Question build() {
      if (this.fields == null) throw new IllegalStateException("Missing fields");
      if (this.question == null) throw new IllegalStateException("Missing question");
      Question q = new Question(this.fields, this.question, this.supportWildcards, this.supportOperators, this.defaultOperatorOR);
      if (this.analyzer == null && this.catalog == null)
        q.compute();
      else
        q.compute(this.analyzer, this.catalog);
      return q;
    }
  }
}

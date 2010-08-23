package org.weborganic.flint.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.weborganic.flint.util.Beta;
import org.weborganic.flint.util.Bucket;
import org.weborganic.flint.util.Terms;
import org.weborganic.flint.util.Bucket.Entry;

import com.topologi.diffx.xml.XMLWritable;
import com.topologi.diffx.xml.XMLWriter;

/**
 * A facet implementation using a simple index field.
 * 
 * @author Christophe Lauret
 * @version 2 August 2010
 */
@Beta
public final class FieldFacet implements XMLWritable, Facet {

  /**
   * The name of this facet
   */
  private final String _name;

  /**
   * The queries used to calculate each facet.
   */
  private final List<TermQuery> _queries;

  /**
   * The queries used to calculate each facet.
   */
  private transient Bucket<Term> _bucket;

  /**
   * Creates a new facet with the specified name;
   * 
   * @param name    The name of the facet.
   * @param queries The subqueries to use on top of the base query to calculate the facet values.
   */
  private FieldFacet(String name, List<TermQuery> queries) {
    this._name = name;
    this._queries = queries;
  }

  /**
   * Returns the name of the field.
   * @return the name of the field.
   */
  public String name() {
    return this._name;
  }

  /**
   * Returns the query for given value if it the specified value matches the text for the term.
   * 
   * @param value the text of the term to match.
   * @return the requested query if it exists or <code>null</code>.
   */
  public Query forValue(String value) {
    if (value == null) return null;
    for (TermQuery t : this._queries) {
      if (value.equals(t.getTerm().text())) return t;
    }
    return null;
  }

  /**
   * Computes each facet option.
   * 
   * @param searcher the index search to use.
   * @param base     the base query.
   * 
   * @throws IOException if thrown by the searcher.
   */
  public void compute(IndexSearcher searcher, Query base) throws IOException {
    // If the base is null, simply calculate for each query
    if (base == null) { compute(searcher); }
    // Otherwise, make a boolean query of the base AND each facet query
    Bucket<Term> bucket = new Bucket<Term>(10);
    DocumentCounter counter = new DocumentCounter();
    for (TermQuery q : this._queries) {
      BooleanQuery query = new BooleanQuery();
      query.add(base, Occur.MUST);
      query.add(q, Occur.MUST);
      searcher.search(query, counter);
      bucket.add(q.getTerm(), counter.getCount());
      counter.reset();
    }
    this._bucket = bucket;
  }

  /**
   * Computes each facet option without a base query.
   * 
   * @param searcher the index search to use.
   * 
   * @throws IOException if thrown by the searcher.
   */
  private void compute(IndexSearcher searcher) throws IOException {
    Bucket<Term> bucket = new Bucket<Term>(10);
    DocumentCounter counter = new DocumentCounter();
    for (TermQuery q : this._queries) {
      searcher.search(q, counter);
      bucket.add(q.getTerm(), counter.getCount());
      counter.reset();
    }
    this._bucket = bucket;
  }

  /**
   * {@inheritDoc}
   */
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("facet", true);
    xml.attribute("name", this._name);
    xml.attribute("type", "field");
    xml.attribute("computed", Boolean.toString(this._bucket != null));
    if (this._bucket != null) {
      for (Entry<Term> e : this._bucket.entrySet()) {
        xml.openElement("term");
        xml.attribute("field", e.item().field());
        xml.attribute("text", e.item().text());
        xml.attribute("cardinality", e.count());
        xml.closeElement();
      }
    }
    xml.closeElement();
  }

  // Static helpers -------------------------------------------------------------------------------

  /**
   * Creates a new facet for the specified field.
   * 
   * @param field  the field for this facet.
   * @param reader the reader to use.
   * 
   * @return the corresponding Facet ready to use with a base query.
   * 
   * @throws IOException if thrown by the reader.
   */
  public static FieldFacet newFacet(String field, IndexReader reader) throws IOException {
    List<Term> terms = Terms.terms(reader, field);
    List<TermQuery> subs = new ArrayList<TermQuery>(terms.size());
    for (Term t : terms) {
      subs.add(new TermQuery(t));
    }
    return new FieldFacet(field, subs);
  }

}

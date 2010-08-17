package org.weborganic.flint.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.BooleanClause.Occur;
import org.weborganic.flint.util.Beta;

import com.topologi.diffx.xml.XMLWriter;

/**
 * An unmodifiable query based on a base query and a set of additional search parameters.
 * 
 * @param <T> the type of the base query.
 * 
 * @author Christophe Lauret
 * @version 16 August 2010
 */
@Beta
public class BasicQuery<T extends SearchParameter> implements FlintQuery {

  /**
   * The query to use as a base.
   */
  private final T _base;

  /**
   * A list of query parameters which can be used on top of the base query. 
   */
  private final List<SearchParameter> _parameters;

  /**
   * The Lucene query corresponding to this object. 
   */
  private volatile Query _query = null;

  /**
   * Constructs a new query.
   * 
   * <p>For safety and to ensure that the parameters remain unmodifiable, the specified list should
   * be unmodifiable. Use the factory method to ensure create create an unmodifiable list.
   * 
   * @param base       The query to use as a base.
   * @param parameters A list of query parameters which can be used on top of the base query.
   * 
   * @throws NullPointerException if either argument is <code>null</code>.
   */
  BasicQuery(T base, List<SearchParameter> parameters) throws NullPointerException {
    if (base == null) throw new NullPointerException("base");
    if (parameters == null) throw new NullPointerException("parameters");
    this._base = base;
    this._parameters = parameters;
  }

  /**
   * Returns the query used as as base.
   * 
   * @return the query used as as base.
   */
  public final T base() {
    return this._base;
  }

  /**
   * Returns the list of additional search parameters associated with this query.
   * 
   * @return the list of additional search parameters associated with this query.
   */
  public final List<SearchParameter> parameters() {
    return this._parameters;
  }

  /**
   * Returns the sort order for the results.
   * 
   * @return the sort order for the results (defaults to relevance).
   */
  public final Sort getSort() {
    return Sort.RELEVANCE;
  }

  /**
   * This query is empty if the base query is empty.
   * 
   * @return <code>true</code> if the base query is empty;
   *         <code>false</code> otherwise.
   */
  public final boolean isEmpty() {
    return this._base == null || this._base.isEmpty();
  }

  /**
   * Returns the Lucene query corresponding to this class.
   * 
   * @return the Lucene query corresponding to this class.
   */
  public Query toQuery() {
    if (this._query == null) {
      this._query = toQuery(this._base, this._parameters);
    }
    return this._query;
  }

  /**
   * Generates the XML for this query.
   * 
   * <p>As:
   * <pre>{@code
   * <basic-query empty="[true|false]" query="[lucene query]">
   *   <base>
   *     <!-- The base query using the toXML() method -->
   *   </base>
   *   <parameters>
   *     <!-- Each parameters in order using the toXML() method -->
   *   </parameters>
   * </basic-query>
   * }</pre>
   * 
   * {@inheritDoc}
   */
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("generic-query", true);
    xml.attribute("empty", Boolean.toString(isEmpty()));
    if (!isEmpty()) {
      xml.attribute("query", this._query.toString());
      // Base query
      xml.openElement("base", true);
      this._base.toXML(xml);
      xml.closeElement();
      // Parameters
      xml.openElement("parameters", !this.parameters().isEmpty());
      for (SearchParameter p : this._parameters) {
        p.toXML(xml);
      }
      xml.closeElement();
    }
    xml.closeElement();
  }

  // private helpers 
  // ----------------------------------------------------------------------------------------------

  /**
   * Builds the query for the specified arguments using the base query and 
   * 
   * @param base       the base query
   * @param parameters the parameters
   * 
   * @return the corresponding Lucene Query
   */
  private static Query toQuery(SearchParameter base, List<SearchParameter> parameters) {
    // No parameters, just use the base.
    if (parameters.isEmpty()) return base.toQuery();
    // Make an AND of all parameters
    BooleanQuery query = new BooleanQuery();
    query.add(base.toQuery(), Occur.MUST);
    for (SearchParameter p : parameters) {
      query.add(p.toQuery(), Occur.MUST);
    }
    return query;
  }

  // factory methods 
  // ----------------------------------------------------------------------------------------------

  /**
   * Builds a basic query using only the specified base query. 
   * 
   * @param <X> The type of base query
   * 
   * @param base  the base query
   * 
   * @return the corresponding Lucene Query
   */
  public static <X extends SearchParameter> BasicQuery<X> newBasicQuery(X base) {
    List<SearchParameter> none = Collections.emptyList();
    return new BasicQuery<X>(base, none);
  }

  /**
   * Builds a basic query using only the specified base query. 
   * 
   * @param <X> The type of base query
   * 
   * @param base       the base query
   * @param parameters the parameters
   * 
   * @return the corresponding Lucene Query
   */
  public static <X extends SearchParameter> BasicQuery<X> newBasicQuery(X base, List<SearchParameter> parameters) {
    List<SearchParameter> unmodifiable = Collections.unmodifiableList(new ArrayList<SearchParameter>(parameters));
    return new BasicQuery<X>(base, unmodifiable);
  }
}

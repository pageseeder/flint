package org.pageseeder.flint.solr.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.response.FacetField;

public class Facets {
  protected final List<String> _facets = new ArrayList<>();
  protected int limit = 100;
  protected int mincount = 0;
  protected boolean missing = false;
  protected String prefix = null;
  protected Map<String, String> prefixes = null;
  protected String sort = null;
  protected List<FacetField> facetFields;
  protected List<FacetField> facetDates;

  public Facets(List<String> facets) {
    this._facets.addAll(facets);
  }

  /**
   * @param limit the limit to set
   */
  public Facets limit(int limit) {
    this.limit = limit;
    return this;
  }

  /**
   * @param mincount the mincount to set
   */
  public Facets mincount(int mincount) {
    this.mincount = mincount;
    return this;
  }

  /**
   * @param missing the missing to set
   */
  public Facets missing(boolean missing) {
    this.missing = missing;
    return this;
  }

  /**
   * @param prefix the prefix to set
   */
  public Facets prefix(String prefix) {
    this.prefix = prefix;
    return this;
  }

  /**
   * @param prefixes the prefixes to set
   */
  public void setPrefix(String field, String prefix) {
    if (this.prefixes == null)
      this.prefixes = new HashMap<>();
    this.prefixes.put(field, prefix);
  }

  /**
   * @param prefixes the prefixes to set
   */
  public Facets prefixes(Map<String, String> prefixes) {
    this.prefixes = prefixes;
    return this;
  }

  /**
   * @param sort the sort to set
   */
  public Facets sort(String sort) {
    this.sort = sort;
    return this;
  }

  public List<FacetField> getFacetDates() {
    return this.facetDates;
  }

  public List<FacetField> getFacetFields() {
    return this.facetFields;
  }
  
}
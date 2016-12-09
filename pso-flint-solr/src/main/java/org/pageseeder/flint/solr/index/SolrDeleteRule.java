package org.pageseeder.flint.solr.index;

import org.pageseeder.flint.content.DeleteRule;

public class SolrDeleteRule implements DeleteRule {

  private final String _deleteID;

  private final String _deleteQuery;

  private SolrDeleteRule(String id, String query) {
    this._deleteID = id;
    this._deleteQuery = query;
  }

  public boolean deleteByID() {
    return this._deleteID != null;
  }

  public String getDeleteID() {
    return this._deleteID;
  }

  public String getDeleteQuery() {
    return this._deleteQuery;
  }

  public static SolrDeleteRule deleteByID(String id) {
    return new SolrDeleteRule(id, null);
  }

  public static SolrDeleteRule deleteByQuery(String query) {
    return new SolrDeleteRule(null, query);
  }

}

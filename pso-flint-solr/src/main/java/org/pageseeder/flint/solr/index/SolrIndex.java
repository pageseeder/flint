package org.pageseeder.flint.solr.index;

import org.pageseeder.flint.Index;

public class SolrIndex extends Index {

  private final SolrIndexIO _io;

  public SolrIndex(String name) {
    super(name);
    this._io = new SolrIndexIO(this);
  }

  public SolrIndex(String name, String catalog) {
    super(name, catalog);
    this._io = new SolrIndexIO(this);
  }

  @Override
  public SolrIndexIO getIndexIO() {
    return this._io;
  }

}

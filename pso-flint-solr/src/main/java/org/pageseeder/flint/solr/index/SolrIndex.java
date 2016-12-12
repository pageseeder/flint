package org.pageseeder.flint.solr.index;

import org.pageseeder.flint.Index;
import org.pageseeder.flint.IndexException;

public class SolrIndex extends Index {

  private final SolrIndexIO _io;

  public SolrIndex(String name, String catalog) throws IndexException {
    super(name, catalog);
    this._io = new SolrIndexIO(this);
  }

  @Override
  public SolrIndexIO getIndexIO() {
    return this._io;
  }

}

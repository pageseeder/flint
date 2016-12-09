package org.pageseeder.flint.berlioz.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.pageseeder.flint.Index;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.lucene.LuceneIndexQueries;
import org.pageseeder.flint.lucene.MultipleIndexReader;
import org.pageseeder.flint.lucene.query.SearchPaging;
import org.pageseeder.flint.lucene.query.SearchQuery;
import org.pageseeder.flint.lucene.query.SearchResults;
import org.pageseeder.flint.lucene.search.Facets;
import org.pageseeder.flint.lucene.search.FieldFacet;

public class MultipleIndexesMaster {

  private final List<Index> _indexes;

  private MultipleIndexReader currentReader;

  @Deprecated
  public MultipleIndexesMaster(List<IndexMaster> indexes, IndexManager manager) {
    this._indexes = buildIndexes(indexes);
  }

  public MultipleIndexesMaster(List<IndexMaster> indexes) {
    this._indexes = buildIndexes(indexes);
  }

  public IndexReader grabReader() throws IndexException {
    if (this.currentReader == null)
      this.currentReader = LuceneIndexQueries.getMultipleIndexReader(this._indexes);
    return this.currentReader.grab();
  }

  public void releaseReader() {
    if (this.currentReader != null)
      this.currentReader.releaseSilently();
  }

  public SearchResults query(SearchQuery query, SearchPaging paging) throws IndexException {
    return LuceneIndexQueries.query(this._indexes, query, paging);
  }

  public List<FieldFacet> getFacets(List<String> fields, int max, SearchQuery query)
      throws IOException, IndexException {
    return Facets.getFacets(fields, max, query.toQuery(), this._indexes);
  }

  // private helpers

  private List<Index> buildIndexes(List<IndexMaster> indexes) {
    List<Index> idxes = new ArrayList<>();
    for (IndexMaster master : indexes) {
      idxes.add(master.getIndex());
    }
    return idxes;
  }
}

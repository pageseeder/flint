package org.pageseeder.flint.berlioz.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.flint.Index;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.berlioz.model.IndexMaster;
import org.pageseeder.flint.berlioz.util.GeneratorErrors;
import org.pageseeder.flint.lucene.LuceneIndexQueries;
import org.pageseeder.flint.lucene.query.AnyTermParameter;
import org.pageseeder.flint.lucene.query.BasicQuery;
import org.pageseeder.flint.lucene.query.SearchPaging;
import org.pageseeder.flint.lucene.query.SearchQuery;
import org.pageseeder.flint.lucene.query.SearchResults;
import org.pageseeder.flint.lucene.query.TermParameter;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Beta
public final class TermSearch extends LuceneIndexGenerator {
  private static final Logger LOGGER = LoggerFactory.getLogger(TermSearch.class);

  @Override
  public void processMultiple(Collection<IndexMaster> indexes, ContentRequest req, XMLWriter xml) throws IOException {
    SearchPaging paging = buildPaging(req);
    SearchQuery query   = buildQuery(req, xml);
    if (query == null) return;
    ArrayList<Index> theIndexes = new ArrayList<>();
    for (IndexMaster index : indexes) {
      theIndexes.add(index.getIndex());
    }
    try {
      outputResults(query, LuceneIndexQueries.query(theIndexes, query, paging), xml);
    } catch (IndexException ex) {
      LOGGER.warn("Fail to retrieve search result using query: {}", query, ex);
    }
  }

  @Override
  public void processSingle(IndexMaster index, ContentRequest req, XMLWriter xml) throws IOException {
    SearchPaging paging = buildPaging(req);
    SearchQuery query   = buildQuery(req, xml);
    if (query == null) return;
    try {
      outputResults(query, index.query(query, paging), xml);
    } catch (IndexException ex) {
      LOGGER.warn("Fail to retrieve search result using query: {}", query, ex);
      GeneratorErrors.error(req, xml, "server", "Failed to perform search: "+ex.getMessage(), ContentStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private SearchQuery buildQuery(ContentRequest req, XMLWriter xml) throws IOException {
    String field = req.getParameter("field", "");
    String term  = req.getParameter("term", "");
    // try empty query
    if (field.isEmpty() && term.isEmpty())
      return BasicQuery.newBasicQuery(AnyTermParameter.empty());
    // field and term must both be there
    if (field.isEmpty()) {
      GeneratorErrors.noParameter(req, xml, "field");
      return null;
    }
    if (term.isEmpty()) {
      GeneratorErrors.noParameter(req, xml, "term");
      return null;
    }
    // build query
    return BasicQuery.newBasicQuery(new TermParameter(field, term));
  }

  private SearchPaging buildPaging(ContentRequest req) {
    SearchPaging paging = new SearchPaging();
    paging.setPage(req.getIntParameter("page", 1));
    paging.setHitsPerPage(req.getIntParameter("results", 100));
    return paging;
  }

  private void outputResults(SearchQuery query, SearchResults results, XMLWriter xml) throws IOException {
    xml.openElement("index-search", true);
    query.toXML(xml);
    results.toXML(xml);
    xml.closeElement();
  }

}

package org.pageseeder.flint.berlioz.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.search.Query;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.flint.Index;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.berlioz.model.FlintConfig;
import org.pageseeder.flint.berlioz.model.IndexDefinition;
import org.pageseeder.flint.berlioz.model.IndexMaster;
import org.pageseeder.flint.berlioz.util.GeneratorErrors;
import org.pageseeder.flint.lucene.LuceneIndexQueries;
import org.pageseeder.flint.lucene.query.PredicateSearchQuery;
import org.pageseeder.flint.lucene.query.SearchPaging;
import org.pageseeder.flint.lucene.query.SearchQuery;
import org.pageseeder.flint.lucene.query.SearchResults;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Beta
public final class PredicateSearch extends LuceneIndexGenerator {
  private static final Logger LOGGER = LoggerFactory.getLogger(PredicateSearch.class);

  @Override
  public void processMultiple(Collection<IndexMaster> indexes, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    SearchPaging paging = buildPaging(req);
    if (indexes.isEmpty()) return;
    SearchQuery query   = buildQuery(req, indexes.iterator().next().getIndexDefinition(), xml);
    if (query == null) return;
    ArrayList<Index> theIndexes = new ArrayList<Index>();
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
  public void processSingle(IndexMaster index, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    SearchPaging paging = buildPaging(req);
    SearchQuery query   = buildQuery(req, index.getIndexDefinition(), xml);
    if (query == null) return;
    try {
      outputResults(query, index.query(query, paging), xml);
    } catch (IndexException ex) {
      LOGGER.warn("Fail to retrieve search result using query: {}", query, ex);
    }
  }

  private SearchQuery buildQuery(ContentRequest req, IndexDefinition def, XMLWriter xml) throws IOException {
    String predicate = req.getParameter("predicate", "");
    if (predicate.isEmpty()) {
      GeneratorErrors.noParameter(req, xml, "predicate");
      return null;
    }
    boolean tokenize = "true".equals(req.getParameter("tokenize", "true"));
    boolean valid = predicate.indexOf(58) != -1;
    if (tokenize) {
      for (String p : predicate.split("\\s+")) {
        if (p.indexOf(58) != -1) continue;
        valid = false;
      }
    }
    if (!valid) {
      GeneratorErrors.invalidParameter(req, xml, "predicate");
      return null;
    }
    Analyzer analyzer = tokenize ? FlintConfig.newAnalyzer(def) : new KeywordAnalyzer();
    PredicateSearchQuery query = new PredicateSearchQuery(predicate, analyzer);
    Query q = query.toQuery();
    if (q == null) {
      GeneratorErrors.invalidParameter(req, xml, "predicate");
      return null;
    }
    return query;
  }

  private SearchPaging buildPaging(ContentRequest req) {
    SearchPaging paging = new SearchPaging();
    int page = req.getIntParameter("page", 1);
    paging.setPage(page);
    paging.setHitsPerPage(100);
    return paging;
  }

  private void outputResults(SearchQuery query, SearchResults results, XMLWriter xml) throws IOException {
    xml.openElement("index-search", true);
    query.toXML(xml);
    results.toXML(xml);
    xml.closeElement();
  }
}

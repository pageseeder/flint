package org.pageseeder.flint.berlioz.solr;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.common.SolrDocument;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.flint.berlioz.model.SolrIndexMaster;
import org.pageseeder.flint.berlioz.util.SolrXMLUtils;
import org.pageseeder.flint.catalog.Catalog;
import org.pageseeder.flint.catalog.Catalogs;
import org.pageseeder.flint.solr.query.Facets;
import org.pageseeder.flint.solr.query.SearchResultsMetadata;
import org.pageseeder.flint.solr.query.SolrQueryManager;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Supported parameters are:
 * 
 *   index       name of the index
 *   facets      comma-separated list of fields to use as facets
 *   max-facets  the max number of values for a facet (default is 20)
 *   field       the field being searched (default fulltext)
 *   term        the text that is searched
 *   with        comma-separated list of facets values with the format [field]:[value]
 *   sort        field name, if starting with "-", the order is reversed
 *   sort-type   [int|double|float|long|document|string|score,set], default is score
 *   page        the page number
 *   results     the nb of results per page
 *   
 */
public class BasicSearch extends SolrIndexGenerator {
  private static final Logger LOGGER = LoggerFactory.getLogger(BasicSearch.class);

  @Override
  public void process(SolrIndexMaster master, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    // parameters
    String field  = req.getParameter("field");
    String term   = req.getParameter("term", "").trim();
    String facets = req.getParameter("facets");
    String sort   = req.getParameter("sort");
    String with   = req.getParameter("with");
    int maxFacets = req.getIntParameter("max-facets", 20);
    int page      = req.getIntParameter("page", 1);
    int results   = req.getIntParameter("results", 100);
    
    // build facets
    Facets thefacets = null;
    if (facets != null) {
      thefacets = new Facets(Arrays.asList(facets.replaceAll("(^,)|(,$)", "").split(",")));
      thefacets.limit(maxFacets);
    }


    // check if we should tokenize
    Catalog theCatalog = Catalogs.getCatalog(master.getCatalog());
    boolean tokenize = theCatalog != null && theCatalog.isTokenized(field);
    String queryAsString;
    if (tokenize && term.indexOf(' ') > 0) {
      queryAsString = "";
      for (String t : term.split(" ")) queryAsString += " " + field + ':' + t;
    } else {
      queryAsString = field + ':' + term;
    }
    queryAsString = queryAsString.trim();
    LOGGER.debug("SolrQuery is {}", field, queryAsString);
    // build query
    SolrQuery query = new SolrQuery(queryAsString);
    if (with != null) {
      for (String fq : with.split(","))
        query.addFilterQuery(fq.trim());
    }
    if (sort != null)
      query.setSort(sort.replaceFirst("^-", ""), sort.charAt(0) == '-' ? ORDER.desc : ORDER.asc);

    // output
    Consumer<SolrDocument> output = new Consumer<SolrDocument>() {
      @Override
      public void accept(SolrDocument doc) {
        try {
          xml.openElement("document");
          for (String field : doc.getFieldNames()) {
            for (Object value : doc.getFieldValues(field)) {
              xml.openElement("field");
              xml.attribute("name", field);
              if (value instanceof Number) {
                xml.attribute("numeric-type", value.getClass().getName().toLowerCase());
              }
              xml.writeText(value.toString());
              xml.closeElement();
            }
          }
          xml.closeElement();
        } catch (IOException ex) {
          LOGGER.error("Failed to output document {}", doc, ex);
        }
      }
    };

    // start output
    xml.openElement("index-search");
    xml.attribute("field", field);
    xml.attribute("term", term);

    // run query
    xml.openElement("documents");
    SolrQueryManager queries = new SolrQueryManager(master.getIndex(), (page - 1) * results, results);
    SearchResultsMetadata meta = queries.select(query, output, thefacets, null);
    xml.closeElement();

    // facets
    if (thefacets != null) {
      SolrXMLUtils.facetsToXML(thefacets, xml);
    }

    // meta
    meta.toXML(xml);

    // end of root element
    xml.closeElement();
  }

}

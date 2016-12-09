/*
 * Decompiled with CFR 0_110.
 * 
 * Could not load the following classes:
 *  org.apache.lucene.search.Query
 *  org.pageseeder.berlioz.BerliozException
 *  org.pageseeder.berlioz.content.Cacheable
 *  org.pageseeder.berlioz.content.ContentRequest
 *  org.pageseeder.berlioz.content.ContentStatus
 *  org.pageseeder.berlioz.util.MD5
 *  org.pageseeder.flint.IndexException
 *  org.pageseeder.flint.IndexManager
 *  org.pageseeder.flint.api.Index
 *  org.pageseeder.flint.search.Facet
 *  org.pageseeder.xmlwriter.XMLWriter
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package org.pageseeder.flint.berlioz.solr;

import java.io.IOException;
import java.util.Arrays;

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.util.MD5;
import org.pageseeder.flint.berlioz.model.SolrIndexMaster;
import org.pageseeder.flint.berlioz.util.SolrXMLUtils;
import org.pageseeder.flint.solr.query.Facets;
import org.pageseeder.flint.solr.query.SolrQueryManager;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public final class GetFacets extends SolrIndexGenerator implements Cacheable {

  private static final Logger LOGGER = LoggerFactory .getLogger(GetFacets.class);

  @Override
  public String getETag(ContentRequest req) {
    StringBuilder etag = new StringBuilder();
    etag.append(req.getParameter("facets", "")).append('%');
    etag.append(req.getParameter("max-number", "20")).append('%');
    etag.append(buildIndexEtag(req));
    return MD5.hash(etag.toString());
  }

  @Override
  public void process(SolrIndexMaster master, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {

    // parameters
    String facets = req.getParameter("facets", "");
    int maxNumber = req.getIntParameter("max-number", 20);

    // quick check
    if (facets.isEmpty()) {
      xml.emptyElement("facets");
      return;
    }
    LOGGER.debug("Loading facets {} from {}", facets, master.getIndex().getIndexID());

    // build facets
    Facets thefacets = new Facets(Arrays.asList(facets.split(",")));
    thefacets.limit(maxNumber);

    // find index
    SolrQueryManager queries = new SolrQueryManager(master.getIndex());
    queries.facets(thefacets);

    // output
    SolrXMLUtils.facetsToXML(thefacets, xml);
  }
}

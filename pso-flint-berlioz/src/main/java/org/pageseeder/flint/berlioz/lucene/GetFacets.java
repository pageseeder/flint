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
package org.pageseeder.flint.berlioz.lucene;

import org.apache.lucene.search.Query;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.util.MD5;
import org.pageseeder.flint.Index;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.berlioz.model.IndexDefinition;
import org.pageseeder.flint.berlioz.model.IndexMaster;
import org.pageseeder.flint.lucene.facet.FlexibleFieldFacet;
import org.pageseeder.flint.lucene.search.Facets;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public final class GetFacets extends LuceneIndexGenerator implements Cacheable {

  private static final Logger LOGGER = LoggerFactory .getLogger(GetFacets.class);

  @Override
  public String getETag(ContentRequest req) {
    String etag = req.getParameter("base", "") + '%' +
        req.getParameter("facets", "") + '%' +
        req.getParameter("max-number", "20") + '%' +
        buildIndexEtag(req);
    return MD5.hash(etag);
  }

  @Override
  public void processMultiple(Collection<IndexMaster> indexes, ContentRequest req, XMLWriter xml) throws IOException {
    String base = req.getParameter("base", "");
    String facets = req.getParameter("facets", "");
    if (facets.isEmpty() && base.isEmpty()) {
      xml.emptyElement("facets");
      return;
    }
    int maxNumber = req.getIntParameter("max-number", 20);
    Query query = null;
    if (!base.isEmpty() && !indexes.isEmpty()) {
      query = buildQuery(base, indexes.iterator().next().getIndexDefinition(), req, xml);
      if (query == null) return;
    }
    ArrayList<Index> theIndexes = new ArrayList<>();
    for (IndexMaster index : indexes) {
      theIndexes.add(index.getIndex());
    }
    List<FlexibleFieldFacet> facetsList = query == null ?
          Facets.getFlexibleFacets(facets.isEmpty() ? null : Arrays.asList(facets.split(",")), maxNumber, theIndexes) :
          Facets.getFlexibleFacets(Arrays.asList(facets.split(",")), maxNumber, query, theIndexes);
    this.outputResults(base, facetsList, xml);
  }

  @Override
  public void processSingle(IndexMaster index, ContentRequest req, XMLWriter xml) throws IOException {
    String base = req.getParameter("base", "");
    String facets = req.getParameter("facets", "");
    int maxNumber = req.getIntParameter("max-number", 20);
    if (facets.isEmpty() && base.isEmpty()) {
      xml.emptyElement("facets");
      return;
    }
    Query query = null;
    if (!base.isEmpty()) {
      query = buildQuery(base, index.getIndexDefinition(), req, xml);
      if (query == null) return;
    }
    List<FlexibleFieldFacet> facetsList = Facets.getFlexibleFacets(facets.isEmpty() ? null : Arrays.asList(facets.split(",")), maxNumber, query, index.getIndex());
    this.outputResults(base, facetsList, xml);
  }

  public Query buildQuery(String base, IndexDefinition def, ContentRequest req, XMLWriter xml) throws IOException {
    Query query;
    try {
      query = IndexMaster.toQuery(base, def);
    } catch (IndexException ex) {
      LOGGER.error("Unable to parse query", ex);
      xml.openElement("error");
      xml.attribute("type", "invalid-parameter");
      xml.attribute("message", "Unable to create query from condition " + base);
      xml.closeElement();
      req.setStatus(ContentStatus.BAD_REQUEST);
      return null;
    }
    LOGGER.debug("Computing facets for {}", base);
    return query;
  }

  public void outputResults(String base, Collection<FlexibleFieldFacet> facets, XMLWriter xml) throws IOException {
    xml.openElement("facets");
    xml.attribute("for", base);
    for (FlexibleFieldFacet facet : facets) {
      facet.toXML(xml);
    }
    xml.closeElement();
  }
}

package org.pageseeder.flint.berlioz.util;

import java.io.IOException;
import java.util.Collection;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.pageseeder.flint.solr.query.Facets;
import org.pageseeder.xmlwriter.XMLWriter;

public class SolrXMLUtils {

  public static void facetsToXML(Facets facets, XMLWriter xml) throws IOException {
    xml.openElement("facets");
    facetFieldsToXML(facets.getFacetFields(), xml);
    facetFieldsToXML(facets.getFacetDates(), xml);
    xml.closeElement();
  }

  private static void facetFieldsToXML(Collection<FacetField> facets, XMLWriter xml) throws IOException {
    if (facets == null) return;
    for (FacetField facet : facets) {
      facetFieldToXML(facet, xml);
    }
  }

  private static void facetFieldToXML(FacetField facet, XMLWriter xml) throws IOException {
    xml.openElement("facet");
    xml.attribute("name", facet.getName());
    xml.attribute("count", facet.getValueCount());
    for (Count count : facet.getValues()) {
      xml.openElement("term");
      xml.attribute("field", facet.getName());
      xml.attribute("text",  count.getName());
      xml.attribute("cardinality", String.valueOf(count.getCount()));
      xml.closeElement();
    }
    xml.closeElement();
  }
}

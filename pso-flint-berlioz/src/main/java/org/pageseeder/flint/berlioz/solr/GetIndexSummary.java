/*
 * Decompiled with CFR 0_110.
 * 
 * Could not load the following classes:
 *  org.apache.lucene.index.DirectoryReader
 *  org.apache.lucene.index.IndexReader
 *  org.apache.lucene.store.Directory
 *  org.pageseeder.berlioz.BerliozException
 *  org.pageseeder.berlioz.content.Cacheable
 *  org.pageseeder.berlioz.content.ContentRequest
 *  org.pageseeder.berlioz.util.ISO8601
 *  org.pageseeder.flint.IndexException
 *  org.pageseeder.flint.api.Index
 *  org.pageseeder.flint.util.Terms
 *  org.pageseeder.xmlwriter.XMLWriter
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package org.pageseeder.flint.berlioz.solr;

import java.io.IOException;

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.flint.berlioz.model.FlintConfig;
import org.pageseeder.flint.berlioz.model.IndexDefinition;
import org.pageseeder.flint.berlioz.model.SolrIndexMaster;
import org.pageseeder.flint.berlioz.util.Files;
import org.pageseeder.flint.solr.SolrFlintConfig;
import org.pageseeder.flint.solr.index.SolrIndexStatus;
import org.pageseeder.xmlwriter.XMLWriter;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public final class GetIndexSummary extends SolrIndexGenerator implements Cacheable {

  public String getETag(ContentRequest req) {
    return buildIndexEtag(req);
  }

  @Override
  public void process(SolrIndexMaster master, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    // find index
    xml.openElement("index-summary");
    xml.attribute("solr", SolrFlintConfig.getInstance().getSolrServerURL());
    xml.openElement("index");
    xml.attribute("name", master.getIndex().getIndexID());
    xml.attribute("content", '/' + Files.path(GlobalSettings.getAppData(), master.getIndex().getContentLocation()));
    // definition
    IndexDefinition def = FlintConfig.get().getIndexDefinitionFromIndexName(master.getIndex().getIndexID());
    if (def != null) {
      xml.attribute("definition", def.getName());
      xml.attribute("template", def.getTemplate().getName());
    }
    // status
    SolrIndexStatus status = master.getIndex().getIndexStatus();
    if (status != null) status.toXML(xml);
    xml.closeElement();
    xml.closeElement();
  }

}

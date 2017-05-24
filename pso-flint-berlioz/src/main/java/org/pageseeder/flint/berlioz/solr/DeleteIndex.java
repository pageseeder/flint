/*
 * Decompiled with CFR 0_110.
 * 
 * Could not load the following classes:
 *  org.pageseeder.berlioz.BerliozException
 *  org.pageseeder.berlioz.Beta
 *  org.pageseeder.berlioz.content.ContentRequest
 *  org.pageseeder.xmlwriter.XMLWriter
 */
package org.pageseeder.flint.berlioz.solr;

import java.io.IOException;

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.flint.berlioz.model.FlintConfig;
import org.pageseeder.flint.berlioz.model.SolrIndexMaster;
import org.pageseeder.xmlwriter.XMLWriter;

/*
 * Used to clear an index.
 */
@Beta
public final class DeleteIndex extends SolrIndexGenerator {

  @Override
  public void process(SolrIndexMaster master, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    xml.openElement("index");
    xml.attribute("name", master.getName());
    xml.attribute("status", FlintConfig.get().deleteMaster(master.getName()) ? "deleted" : "delete-failed");
    xml.closeElement();
  }

}

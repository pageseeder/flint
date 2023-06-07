/*
 * Decompiled with CFR 0_110.
 *
 * Could not load the following classes:
 *  org.pageseeder.berlioz.BerliozException
 *  org.pageseeder.berlioz.content.ContentGenerator
 *  org.pageseeder.berlioz.content.ContentRequest
 *  org.pageseeder.flint.IndexJob
 *  org.pageseeder.flint.IndexManager
 *  org.pageseeder.flint.api.Index
 *  org.pageseeder.xmlwriter.XMLWriter
 */
package org.pageseeder.flint.berlioz;

import java.io.IOException;
import java.util.Collection;

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.flint.berlioz.helper.BatchXMLWriter;
import org.pageseeder.flint.berlioz.model.FlintConfig;
import org.pageseeder.flint.indexing.IndexBatch;
import org.pageseeder.xmlwriter.XMLWriter;

public class GetBatches implements ContentGenerator {

  public void process(ContentRequest req, XMLWriter xml) throws IOException {
    Collection<IndexBatch> batches = FlintConfig.get().getPastBatches();
    xml.openElement("batches");
    for (IndexBatch batch : batches) {
      BatchXMLWriter.batchToXML(batch, xml);
    }
    xml.closeElement();
  }
}

/*
 * Decompiled with CFR 0_110.
 * 
 * Could not load the following classes:
 *  org.pageseeder.berlioz.BerliozException
 *  org.pageseeder.berlioz.content.ContentGenerator
 *  org.pageseeder.berlioz.content.ContentRequest
 *  org.pageseeder.flint.IndexManager
 *  org.pageseeder.flint.local.LocalIndex
 *  org.pageseeder.flint.local.LocalIndexer
 *  org.pageseeder.xmlwriter.XMLWriter
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package org.pageseeder.flint.berlioz.solr;

import java.io.IOException;

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.flint.berlioz.helper.AsynchronousIndexer;
import org.pageseeder.flint.berlioz.model.SolrIndexMaster;
import org.pageseeder.flint.berlioz.util.GeneratorErrors;
import org.pageseeder.xmlwriter.XMLWriter;

public final class CheckIndexProgress extends SolrIndexGenerator {

  @Override
  public void process(SolrIndexMaster index, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {

    // use asynchronous indexer
    AsynchronousIndexer indexer = AsynchronousIndexer.getIndexer(index.getName());
    if (indexer == null) {
      GeneratorErrors.error(req, xml, "not-found", "No indexing found on that index", ContentStatus.OK);
    } else {
      indexer.toXML(xml);      
    }

  }

}

/*
 * Decompiled with CFR 0_110.
 *
 * Could not load the following classes:
 *  org.pageseeder.berlioz.BerliozException
 *  org.pageseeder.berlioz.content.ContentGenerator
 *  org.pageseeder.berlioz.content.ContentRequest
 *  org.pageseeder.xmlwriter.XMLWriter
 */
package org.pageseeder.flint.berlioz.solr;

import java.io.IOException;

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.flint.berlioz.model.FlintConfig;
import org.pageseeder.flint.berlioz.model.SolrIndexMaster;
import org.pageseeder.flint.berlioz.util.GeneratorErrors;
import org.pageseeder.xmlwriter.XMLWriter;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public abstract class SolrIndexGenerator implements ContentGenerator {
  public static final String INDEX_PARAMETER = "index";

  @Override
  public void process(ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    String name = req.getParameter(INDEX_PARAMETER);
    FlintConfig config = FlintConfig.get();
    SolrIndexMaster master = name == null ? config.getSolrMaster() : config.getSolrMaster(name);
    if (master == null)
      GeneratorErrors.error(req, xml, "configuration", "No default index found!", ContentStatus.INTERNAL_SERVER_ERROR);
    else
      process(master, req, xml);
  }

  public String buildIndexEtag(ContentRequest req) {
    String name = req.getParameter(INDEX_PARAMETER);
    FlintConfig config = FlintConfig.get();
    SolrIndexMaster master = name == null ? config.getSolrMaster() : config.getSolrMaster(name);
    return master == null ? null : String.valueOf(master.lastModified());
  }

  public abstract void process(SolrIndexMaster master, ContentRequest req, XMLWriter xml) throws BerliozException, IOException;
}

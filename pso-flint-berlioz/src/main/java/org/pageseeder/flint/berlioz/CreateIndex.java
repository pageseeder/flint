package org.pageseeder.flint.berlioz;

import java.io.IOException;

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.flint.berlioz.model.FlintConfig;
import org.pageseeder.flint.berlioz.model.IndexDefinition;
import org.pageseeder.flint.berlioz.util.GeneratorErrors;
import org.pageseeder.xmlwriter.XMLWriter;

public final class CreateIndex implements ContentGenerator {

  @Override
  public void process(ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    String indexes = req.getParameter("index");
    if (indexes == null) {
      GeneratorErrors.noParameter(req, xml, "index");
    }
    xml.openElement("indexes");
    for (String index : indexes.split(",")) {
      createIndex(index, xml);
    }
    xml.closeElement();
  }

  private void createIndex(String index, XMLWriter xml) throws IOException {
    // find def and create master
    IndexDefinition def = FlintConfig.get().getIndexDefinitionFromIndexName(index);
    Object master;
    FlintConfig cfg = FlintConfig.get();
    if (cfg.useSolr()) {
      master = def == null ? null : cfg.getSolrMaster(index, true);
    } else {
      master = def == null ? null : cfg.getMaster(index, true);
    }
    // output
    xml.openElement("index");
    xml.attribute("name", index);
    xml.attribute("status", master != null ? "created" : "create-failed");
    xml.closeElement();
  }
}

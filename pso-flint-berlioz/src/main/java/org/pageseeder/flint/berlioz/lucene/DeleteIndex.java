/*
 * Decompiled with CFR 0_110.
 * 
 * Could not load the following classes:
 *  org.pageseeder.berlioz.BerliozException
 *  org.pageseeder.berlioz.Beta
 *  org.pageseeder.berlioz.content.ContentRequest
 *  org.pageseeder.xmlwriter.XMLWriter
 */
package org.pageseeder.flint.berlioz.lucene;

import java.io.IOException;
import java.util.Collection;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.flint.berlioz.model.FlintConfig;
import org.pageseeder.flint.berlioz.model.IndexMaster;
import org.pageseeder.flint.berlioz.util.GeneratorErrors;
import org.pageseeder.xmlwriter.XMLWriter;

/*
 * Used to clear an index.
 */
@Beta
public final class DeleteIndex extends LuceneIndexGenerator {

  @Override
  public void processSingle(IndexMaster index, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    xml.openElement("index");
    xml.attribute("name", index.getName());
    xml.attribute("status", FlintConfig.get().deleteMaster(index.getName()) ? "deleted" : "delete-failed");
    xml.closeElement();
  }

  @Override
  public void processMultiple(Collection<IndexMaster> indexes, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    GeneratorErrors.error(req, xml, "forbidden", "Cannnot delete multiple indexes", ContentStatus.BAD_REQUEST);
  }
}

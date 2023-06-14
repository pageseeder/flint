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

import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.flint.berlioz.model.IndexMaster;
import org.pageseeder.xmlwriter.XMLWriter;

/*
 * Used to clear an index.
 */
@Beta
public final class ClearIndex extends LuceneIndexGenerator {

  @Override
  public void processSingle(IndexMaster index, ContentRequest req, XMLWriter xml) throws IOException {
    index.clear();
    xml.openElement("index");
    xml.attribute("status", "clear");
    xml.closeElement();
  }

  @Override
  public void processMultiple(Collection<IndexMaster> indexes, ContentRequest req, XMLWriter xml) throws IOException {
    xml.openElement("indexes");
    for (IndexMaster index : indexes) {
      index.clear();
      xml.openElement("index");
      xml.attribute("status", "clear");
      xml.closeElement();
    }
    xml.closeElement();
  }
}

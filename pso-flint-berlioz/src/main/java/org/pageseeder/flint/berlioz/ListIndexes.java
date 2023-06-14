/*
 * Decompiled with CFR 0_110.
 *
 * Could not load the following classes:
 *  org.apache.lucene.index.DirectoryReader
 *  org.apache.lucene.index.IndexReader
 *  org.apache.lucene.store.Directory
 *  org.pageseeder.berlioz.BerliozException
 *  org.pageseeder.berlioz.content.Cacheable
 *  org.pageseeder.berlioz.content.ContentGenerator
 *  org.pageseeder.berlioz.content.ContentRequest
 *  org.pageseeder.berlioz.util.ISO8601
 *  org.pageseeder.berlioz.util.MD5
 *  org.pageseeder.flint.IndexException
 *  org.pageseeder.flint.api.Index
 *  org.pageseeder.xmlwriter.XMLWriter
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package org.pageseeder.flint.berlioz;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.util.ISO8601;
import org.pageseeder.berlioz.util.MD5;
import org.pageseeder.flint.berlioz.model.FlintConfig;
import org.pageseeder.flint.berlioz.model.IndexMaster;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public final class ListIndexes implements ContentGenerator, Cacheable {
  private static final Logger LOGGER = LoggerFactory.getLogger(ListIndexes.class);

  public String getETag(ContentRequest req) {
    if ("true".equals(req.getParameter("refresh", "false"))) return null;
    StringBuilder etag = new StringBuilder();
    FlintConfig config = FlintConfig.get();
    for (IndexMaster master : config.listIndexes()) {
      etag.append(master.lastModified()).append('%');
    }
    return MD5.hash(etag.toString());
  }

  public void process(ContentRequest req, XMLWriter xml) throws IOException {
    FlintConfig config = FlintConfig.get();
    xml.openElement("indexes");
    try {
      // loop through index folders
      for (IndexMaster index : config.listIndexes()) {
        indexToXML(index, xml);
      }
    } finally {
      xml.closeElement();
    }
  }

  /**
   * Output index.
   *
   * @param index the index
   * @param xml   the output destination
   *
   * @throws IOException If writing the output failed
   */
  private void indexToXML(IndexMaster index, XMLWriter xml) throws IOException {
    xml.openElement("index");
    xml.attribute("name", index.getName());
    IndexReader reader = index.grabReader();
    if (reader != null) {
      try (DirectoryReader dreader = DirectoryReader.open(index.getIndex().getIndexDirectory())) {
        // index details
        long lm = index.getIndex().getIndexIO().getLastTimeUsed();
        if (lm > 0) xml.attribute("last-modified", ISO8601.DATETIME.format(lm));
        xml.attribute("current", Boolean.toString(dreader.isCurrent()));
        xml.attribute("version", Long.toString(dreader.getVersion()));
        // document counts
        xml.openElement("documents");
        xml.attribute("count", reader.numDocs());
        xml.attribute("max", reader.maxDoc());
        xml.closeElement();
      } catch (IOException ex) {
        LOGGER.error("Error while extracting index statistics", ex);
      } finally {
        // release objects
        index.releaseSilently(reader);
      }
    }
    xml.closeElement();
  }
}

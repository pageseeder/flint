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

import java.io.IOException;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.util.ISO8601;
import org.pageseeder.berlioz.util.MD5;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.berlioz.model.FlintConfig;
import org.pageseeder.flint.berlioz.model.IndexMaster;
import org.pageseeder.flint.berlioz.model.SolrIndexMaster;
import org.pageseeder.flint.berlioz.util.Files;
import org.pageseeder.flint.solr.ClusterStatus;
import org.pageseeder.flint.solr.SolrCollectionManager;
import org.pageseeder.flint.solr.SolrFlintException;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ListIndexes implements ContentGenerator, Cacheable {
  private static final Logger LOGGER = LoggerFactory.getLogger(ListIndexes.class);

  public String getETag(ContentRequest req) {
    if ("true".equals(req.getParameter("refresh", "false"))) return null;
    StringBuilder etag = new StringBuilder();
    FlintConfig config = FlintConfig.get();
    for (IndexMaster master : config.listLuceneIndexes()) {
      etag.append(master.lastModified()).append('%');
    }
    try {
      for (SolrIndexMaster master : config.listSolrIndexes()) {
        etag.append(master.lastModified()).append('%');
      }
    } catch (SolrFlintException ex) {
      return null;
    }
    return MD5.hash((String) etag.toString());
  }

  public void process(ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    FlintConfig config = FlintConfig.get();
    xml.openElement("indexes");
    try {
      if (config.useSolr()) {
        xml.attribute("solr", "true");
        try {
          for (SolrIndexMaster index : config.listSolrIndexes("true".equals(req.getParameter("refresh", "false")))) {
            xml.openElement("index");
            xml.attribute("solr", "true");
            xml.attribute("name", index.getIndex().getIndexID());
            xml.attribute("content", '/' + Files.path(GlobalSettings.getAppData(), index.getIndex().getContentLocation()));
            xml.closeElement();
          }
        } catch (SolrFlintException ex) {
          if (ex.cannotConnect()) {
            xml.attribute("error", "Cannot connect to Solr server, please check the configuration.");
          } else {
            xml.attribute("error", "Failed to list Solr indexes: "+ex.getMessage()+".");
            LOGGER.error("Failed to list indexes", ex);
          }
        }
        // load details from solr directly
        try {
          ClusterStatus status = new SolrCollectionManager().getClusterStatus();
          if (status != null) status.toXML(xml);
        } catch (SolrFlintException ex) {
          if (ex.cannotConnect()) {
            xml.attribute("error", "Cannot connect to Solr server, please check the configuration.");
          } else {
            xml.attribute("error", "Failed to get cluster status: "+ex.getMessage()+".");
            LOGGER.error("Failed to get cluster status", ex);
          }
        }
      } else {
        // loop through index folders
        for (IndexMaster index : config.listLuceneIndexes()) {
          indexToXML(index, xml);
        }
      }
    } finally {
      xml.closeElement();
    }
  }

  /**
   * Output index.
   * 
   * @param index the index
   * @param xml
   * @throws IOException
   */
  private void indexToXML(IndexMaster index, XMLWriter xml) throws IOException {
    xml.openElement("index");
    xml.attribute("name", index.getName());
    IndexReader reader = null;
    try {
      reader = index.grabReader();
    } catch (IndexException ex) {
      xml.attribute("error", "Failed to load reader: " + ex.getMessage());
    }
    if (reader != null) {
      DirectoryReader dreader = null;
      try {
        dreader = DirectoryReader.open((Directory) index.getIndex().getIndexDirectory());
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
        LOGGER.error("Error while extracting index statistics", (Throwable) ex);
      } finally {
        // release objects
        index.releaseSilently(reader);
        if (dreader != null) dreader.close();
      }
    }
    xml.closeElement();
  }
}

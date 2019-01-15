/*
 * Decompiled with CFR 0_110.
 * 
 * Could not load the following classes:
 *  org.apache.lucene.index.Fields
 *  org.apache.lucene.index.IndexReader
 *  org.apache.lucene.index.MultiFields
 *  org.apache.lucene.index.Terms
 *  org.apache.lucene.index.TermsEnum
 *  org.apache.lucene.util.BytesRef
 *  org.pageseeder.berlioz.BerliozException
 *  org.pageseeder.berlioz.content.Cacheable
 *  org.pageseeder.berlioz.content.ContentGenerator
 *  org.pageseeder.berlioz.content.ContentRequest
 *  org.pageseeder.flint.IndexException
 *  org.pageseeder.xmlwriter.XMLWriter
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package org.pageseeder.flint.berlioz.lucene;

import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.util.MD5;
import org.pageseeder.flint.berlioz.model.IndexMaster;
import org.pageseeder.flint.berlioz.util.GeneratorErrors;
import org.pageseeder.flint.catalog.Catalog;
import org.pageseeder.flint.catalog.Catalogs;
import org.pageseeder.xmlwriter.XMLWriter;

import java.io.IOException;
import java.util.Collection;

public final class GetCatalog extends LuceneIndexGenerator implements Cacheable {

  public String getETag(ContentRequest req) {
    String etag = buildIndexEtag(req);
    return etag == null ? null : MD5.hash(etag);
  }

  @Override
  public void processSingle(IndexMaster master, ContentRequest req, XMLWriter xml) throws IOException {
    Catalog cat = Catalogs.getCatalog(master.getCatalog());
    if (cat == null) {
      xml.emptyElement("catalog");
    } else {
      cat.toXML(xml);
    }
  }

  @Override
  public void processMultiple(Collection<IndexMaster> masters, ContentRequest req, XMLWriter xml) throws IOException {
    GeneratorErrors.error(req, xml, "forbidden", "Cannnot view catalog for multiple indexes", ContentStatus.BAD_REQUEST);
  }

}

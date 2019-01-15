/*
 * Decompiled with CFR 0_110.
 * 
 * Could not load the following classes:
 *  org.apache.lucene.index.DirectoryReader
 *  org.apache.lucene.index.IndexReader
 *  org.apache.lucene.store.Directory
 *  org.pageseeder.berlioz.BerliozException
 *  org.pageseeder.berlioz.content.Cacheable
 *  org.pageseeder.berlioz.content.ContentRequest
 *  org.pageseeder.berlioz.util.ISO8601
 *  org.pageseeder.flint.IndexException
 *  org.pageseeder.flint.api.Index
 *  org.pageseeder.flint.util.Terms
 *  org.pageseeder.xmlwriter.XMLWriter
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package org.pageseeder.flint.berlioz.lucene;

import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.berlioz.model.FlintConfig;
import org.pageseeder.flint.berlioz.model.IndexMaster;
import org.pageseeder.flint.berlioz.util.GeneratorErrors;
import org.pageseeder.flint.indexing.FlintDocument;
import org.pageseeder.flint.indexing.FlintField;
import org.pageseeder.flint.ixml.IndexParserFactory;
import org.pageseeder.flint.lucene.util.Dates;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.TimeZone;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public final class GenerateIXML extends LuceneIndexGenerator {
  private static final Logger LOGGER = LoggerFactory.getLogger(GenerateIXML.class);

  @Override
  public void processSingle(IndexMaster index, ContentRequest req, XMLWriter xml) throws IOException {

    // get file
    String path = req.getParameter("path");
    if (path == null) {
      GeneratorErrors.noParameter(req, xml, "path");
      return;
    }
    File f = new File(index.getIndex().getContentLocation(), path);
    if (!f.exists() || !f.isFile()) {
      GeneratorErrors.invalidParameter(req, xml, "path");
      return;
    }
    TimeZone tz = TimeZone.getDefault();
    int timezoneOffset = tz.getRawOffset();

    // output
    xml.openElement("generated");
    xml.attribute("document-path", path);
    xml.attribute("index", index.getName());
    xml.attribute("template", FlintConfig.get().getIndexDefinitionFromIndexName(index.getName()).getTemplate().getName());

    // content
    if ("true".equals(req.getParameter("source", "false"))) {
      xml.openElement("source", true);
      try {
        StringWriter out = new StringWriter();
        index.generateContent(f, out);
        xml.writeXML(out.toString());
      } catch (IndexException ex) {
        xml.element("error", "Failed to generate source: "+ex.getMessage());
        LOGGER.error("Failed to generate source for {}", f, ex);
      } finally {
        xml.closeElement();
      }
    }
    // iXML
    xml.openElement("ixml");
    String ixml = null;
    try {
      // generate ixml
      StringWriter out = new StringWriter();
      index.generateIXML(f, out);
      ixml = out.toString();
      xml.writeXML(ixml.replaceAll("<(!DOCTYPE|\\?xml)([^>]+)>", "")); // remove xml and doctype declarations
      
    } catch (IndexException ex) {
      xml.element("error", "Failed to generate iXML: "+ex.getMessage());
      LOGGER.error("Failed to generate iXML for {}", f, ex);
    } finally {
      xml.closeElement();
    }

    if (ixml != null) {
      // documents
      xml.openElement("documents");
      try {
        // load documents
        List<FlintDocument> docs = IndexParserFactory.getInstance().process(new InputSource(new StringReader(ixml)), null);
        for (FlintDocument doc : docs) {
          xml.openElement("document", true);

          // display the value of each field
          for (FlintField field : doc.fields()) {
            // Retrieve the value
            String value = field.value().toString();
            boolean date = false, datetime = false;
            // format dates using ISO 8601 when possible
            if (value.length() > 0 && field.name().contains("date") && Dates.isLuceneDate(value)) {
              try {
                if (value.length() > 8) {
                  value = Dates.toISODateTime(value, timezoneOffset);
                  datetime = true;
                } else {
                  value = Dates.toISODate(value);
                  if (value.length() == 10) {
                    date = true;
                  }
                }
              } catch (ParseException ex) {
                LOGGER.warn("Unparseable date found {}", value);
              }
            }
            xml.openElement("field");
            xml.attribute("name", field.name());
            // Display the correct attributes so that we know we can format the date
            if (date) {
              xml.attribute("date", value);
            } else if (datetime) {
              xml.attribute("datetime", value);
            }
            xml.attribute("boost", Float.toString(field.boost()));
            xml.attribute("omit-norms", Boolean.toString(field.omitNorms()));
            xml.attribute("stored", Boolean.toString(field.store()));
            xml.attribute("tokenized", Boolean.toString(field.tokenize()));
            xml.attribute("term-vectors", Boolean.toString(field.termVector()));
            xml.attribute("term-vector-offsets", Boolean.toString(field.termVectorOffsets()));
            xml.attribute("term-vector-payloads", Boolean.toString(field.termVectorPayloads()));
            xml.attribute("term-vector-positions", Boolean.toString(field.termVectorPositions()));
            xml.attribute("index", field.index().toString().toLowerCase().replace('_', '-'));
            if (value.length() > 100) {
              xml.attribute("truncated", "true");
              xml.writeText(value.substring(0, 100));
            } else {
              xml.writeText(value);
            }
            xml.closeElement();
          }
          // close 'document'
          xml.closeElement();
        }
      } catch (IndexException ex) {
        xml.element("error", "Failed to generate iXML: "+ex.getMessage());
        LOGGER.error("Failed to generate iXML for {}", f, ex);
      } finally {
        xml.closeElement();
      }
    }
    // close root
    xml.closeElement();

  }

  @Override
  public void processMultiple(Collection<IndexMaster> indexes, ContentRequest req, XMLWriter xml) throws IOException {
    GeneratorErrors.error(req, xml, "forbidden", "Can't generate iXML for multiple indexes", ContentStatus.BAD_REQUEST);
  }

}

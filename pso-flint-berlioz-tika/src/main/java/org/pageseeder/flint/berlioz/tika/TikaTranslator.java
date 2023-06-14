package org.pageseeder.flint.berlioz.tika;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.content.Content;
import org.pageseeder.flint.content.ContentTranslator;
import org.pageseeder.xmlwriter.XMLWriter;
import org.pageseeder.xmlwriter.XMLWriterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * This translator uses Tika.
 *
 * @author Jean-Baptiste Reure
 * @version 25 March 2010
 */
public class TikaTranslator implements ContentTranslator {

  /**
   * Logger
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(TikaTranslator.class);

  /**
   * The factory used to produce an output handler
   */
  private final static SAXTransformerFactory factory = (SAXTransformerFactory)SAXTransformerFactory.newInstance();

  /**
   * The object used to parse the
   */
  private final static TikaConfig TIKA_CONFIG = TikaConfig.getDefaultConfig();

  @Override
  public Reader translate(Content content) throws IndexException {
    // check for deleted content
    if (content.isDeleted()) return null;
    try {
      LOGGER.debug("Attempting to translate content {}", content);
      // include metadata
      Metadata metadata = new Metadata();
      String xmlContent = null;
      // create output stream
      TikaInputStream stream = null;
      File f = content.getFile();
      if (f != null) {
        // check max size property
        int maxSize = GlobalSettings.get("flint.index.max-tika-size", TikaTranslatorFactory.MAX_INDEXING_SIZE);
        if (f.length() <= maxSize) {
          stream = TikaInputStream.get(f.toPath());
        }
      } else {
        stream = TikaInputStream.get(content.getSource());
      }
      if (stream != null) {
        ParseContext context = new ParseContext();
        try {
          ByteArrayOutputStream out = new ByteArrayOutputStream();
          new AutoDetectParser(TIKA_CONFIG).parse(stream, getHandler(out), metadata, context);
          xmlContent = out.toString(StandardCharsets.UTF_8);
        } catch (TikaException te) {
          LOGGER.error("Failed to parse content with TIKA", te);
          // should be HTML??
          xmlContent = "<error>"+(te.getMessage() == null ? "Unknown error while reading content in TIKA" : te.getMessage())+"</error>";
        } finally {
          stream.close();
        }
      }
      StringWriter sw = new StringWriter();
      XMLWriter xml = new XMLWriterImpl(sw);
      xml.openElement("content");
      xml.attribute("source", "tika");
      // content
      if (xmlContent != null)
        xml.writeXML(xmlContent.replace(" xmlns=\"http://www.w3.org/1999/xhtml\"", ""));
      // end root
      xml.closeElement();
      // create reader on results
      return new StringReader(sw.toString());
    } catch (Exception ex) {
      LOGGER.error("Failed to translate content {}", content, ex);
      return null;
    }
  }

  /**
   * Create a new handler with an XML output (will be XHTML).
   *
   * @param out the stream to use as output
   * @return the handler used by the Tika parser
   * @throws TransformerConfigurationException if the factory cannot create a new handler.
   */
  private ContentHandler getHandler(OutputStream out) throws TransformerConfigurationException {
    TransformerHandler handler = factory.newTransformerHandler();
    handler.getTransformer().setOutputProperty(OutputKeys.METHOD, "xml");
    handler.getTransformer().setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    handler.getTransformer().setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    handler.setResult(new StreamResult(out));
    return handler;
  }
}
package org.weborganic.flint.util;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.zip.DataFormatException;

import org.apache.lucene.document.CompressionTools;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;

import com.topologi.diffx.xml.XMLWriter;

/**
 * A collection of utility methods to manipulate documents.
 * 
 * @author Christophe Lauret
 * @version 30 July 2010
 */
public final class Documents {


  /**
   * Utility class need no constructor.
   */
  private Documents() {
  }

  /**
   * Returns the XML for a document.
   * 
   * @param xml The XML writer.
   * @param doc Lucene document to serialise as XML.
   * 
   * @throws IOException Any I/O error thrown by the XML writer.
   */
  @Beta
  public static void toXML(XMLWriter xml, Document doc) throws IOException {
    xml.openElement("document", true);
    // display the value of each field
    for (Fieldable f : doc.getFields()) {
      String value = f.stringValue();
      // is it a compressed field?
      if (value == null && f.getBinaryLength() > 0) {
        try {
          value = CompressionTools.decompressString(f.getBinaryValue());
        } catch (DataFormatException ex) {
//            LOGGER.warn("Failed to decompress field value", ex);
          continue;
        }
      }
      // TODO: date formatting

      // unnecessary to return the full value of long fields
      if (value.length() < 100) {
        xml.openElement("field");
        xml.attribute("name", f.name());
        xml.writeText(value);
        xml.closeElement();
      }
    }
    // close 'document'
    xml.closeElement();
  }

}

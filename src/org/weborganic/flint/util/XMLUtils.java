package org.weborganic.flint.util;

import java.io.IOException;

import org.apache.lucene.index.Term;

import com.topologi.diffx.xml.XMLWriter;

/**
 * A collection of utility methods for serialising data as XML.
 * 
 * @author Christophe Lauret
 * @version 21 June 2010
 */
public final class XMLUtils {

  /** Utility class */
  private XMLUtils() {
  }

  /**
   * Returns the XML for a term.
   * 
   * @param t   Term to serialise as XML.
   * @param xml The XML writer.
   * 
   * @throws IOException Any I/O error.
   */
  public static void toXML(Term t, XMLWriter xml) throws IOException {
    xml.openElement("term");
    xml.attribute("field", t.field());
    xml.attribute("text", t.text());
    xml.closeElement();
  }

}

package org.weborganic.flint.util;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.Term;

import com.topologi.diffx.xml.XMLWriter;

/**
 * A collection of utility methods for serialising data as XML.
 * 
 * @author Christophe Lauret
 * @version 23 June 2010
 */
public final class XMLUtils {

  /** Utility class */
  private XMLUtils() {
  }

  /**
   * Returns the XML for a list of terms.
   * 
   * @param terms The list of terms to serialise as XML.
   * @param xml   The XML writer.
   * 
   * @throws IOException Any I/O error.
   * 
   * @deprecated Use {@link Terms#toXML(XMLWriter, List)} instead
   * 
   */
  @Deprecated public static void toXML(List<Term> terms, XMLWriter xml) throws IOException {
    for (Term t : terms) {
      toXML(t, xml);
    }
  }

  /**
   * Returns the XML for a term.
   * 
   * @param t   Term to serialise as XML.
   * @param xml The XML writer.
   * 
   * @throws IOException Any I/O error.
   * 
   * @deprecated Use {@link Terms#toXML(XMLWriter, Term)} instead
   */
  @Deprecated public static void toXML(Term t, XMLWriter xml) throws IOException {
    xml.openElement("term");
    xml.attribute("field", t.field());
    xml.attribute("text", t.text());
    xml.closeElement();
  }

  /**
   * Returns the XML for a term.
   * 
   * @param t         Term to serialise as XML.
   * @param frequency The term document frequency.
   * @param xml       The XML writer.
   * 
   * @throws IOException Any I/O error.
   * 
   * @deprecated Use {@link Terms#toXML(XMLWriter, Term, int)} instead
   */
  @Deprecated public static void toXML(Term t, int frequency, XMLWriter xml) throws IOException {
    xml.openElement("term");
    xml.attribute("field", t.field());
    xml.attribute("text", t.text());
    xml.attribute("frequency", frequency);
    xml.closeElement();
  }

}

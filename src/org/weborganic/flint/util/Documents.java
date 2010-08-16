package org.weborganic.flint.util;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;

import org.apache.lucene.document.CompressionTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.weborganic.flint.search.DocumentCounter;

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
   * Count the number of documents matching the specified query.
   * 
   * @param searcher the index search to use.
   * @param query    the query.
   * 
   * @return the number of documents matching the specified query.
   * 
   * @throws IOException if thrown by the searcher.
   */
  public static int count(IndexSearcher searcher, Query query) throws IOException {
    DocumentCounter counter = new DocumentCounter();
    searcher.search(query, counter);
    return counter.getCount();
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
  public static void toXML(XMLWriter xml, Document doc, List<Terms> terms, int extractLength) throws IOException {
    xml.openElement("document", true);
    // display the value of each field
    for (Fieldable f : doc.getFields()) {
      String value = f.stringValue();
      // is it a compressed field?
      if (value == null && f.getBinaryLength() > 0) {
        try {
          value = CompressionTools.decompressString(f.getBinaryValue());
        } catch (DataFormatException ex) {
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

  /**
   * Returns the extract from the text for the given terms and with the maximum specified length.
   * 
   * <p>This method will include "..." whenever the text was cut (at the beginning or the end).
   * 
   * @param text   the text to search 
   * @param term   the term to find
   * @param length The length of the extract
   * 
   * @return the extract or <code>null</code> if the term could not be found.
   * 
   * @throws IllegalArgumentException If the length of the term is larger than the length of the extract.
   */
  @Beta
  protected static String extract(String text, String term, int length) throws IllegalArgumentException {
    if (term.length() > length) 
      throw new IllegalArgumentException("Term length ("+term.length()+") is larger than requested extract length ("+length+")");
    StringBuilder extract = new StringBuilder();
    final int len = length - term.length();
    Pattern p = Pattern.compile("(?:\\W|^)(\\Q"+term+"\\E)(?:\\W|$)");
    Matcher m = p.matcher(text);
    if (m.find()) {
      int start = m.start(1);
      int end = m.end(1);
      // the entire string can be used 
      if (length > text.length()) {
        extract.append(text.substring(0, start));
        extract.append("<term>").append(m.group(1)).append("</term>");
        extract.append(text.substring(end));

      // 
      } else if (start < len / 2) {
        extract.append("[B]");
        extract.append(text.substring(0, start));
        extract.append("<term>").append(m.group(1)).append("</term>");
        if ((text.length() - end < len - start)) {
          extract.append(text.substring(end));
        } else {
          extract.append(text.substring(end, end+len-start-1)).append("...");
        }

      } else if (text.length() - end < len / 2) {
        extract.append("[C]");
        int x = text.length() - end;
        if (x > start) {
          extract.append(text.substring(0, start));
        } else {
          extract.append("...").append(text.substring(start - x, start));
        }
        extract.append("<term>").append(m.group(1)).append("</term>");
        extract.append(text.substring(end));

      } else {
        extract.append("[D]");
        extract.append("...").append(text.substring(start - (len / 2), start));
        extract.append("<term>").append(m.group(1)).append("</term>");
        extract.append(text.substring(end, end + len / 2)).append("...");
      }

    }

    return extract.toString();
  }

  public static void main(String[] args) {
    System.err.println(extract("This is a very small text.", "very", 12));
    System.err.println(extract("This is a very small text.", "very", 30));
    System.err.println(extract("This is a very small text.", "text", 12));
    System.err.println(extract("This is a very small text.", "This", 12));
    System.err.println(extract("This is a very small text.", "is", 12));
  }

}

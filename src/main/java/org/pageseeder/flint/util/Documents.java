/*
 * Copyright 2015 Allette Systems (Australia)
 * http://www.allette.com.au
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pageseeder.flint.util;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;

import org.apache.lucene.document.CompressionTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.pageseeder.flint.search.DocumentCounter;
import org.pageseeder.xmlwriter.XMLWriter;
import org.pageseeder.xmlwriter.esc.XMLEscapeUTF8;

/**
 * A collection of utility methods to manipulate documents.
 *
 * @author Christophe Lauret
 * @version 5 July 2011
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
   * @param xml   The XML writer.
   * @param doc   Lucene document to serialise as XML.
   * @param terms Unused???
   * @param extractLength
   *
   * @throws IOException Any I/O error thrown by the XML writer.
   */
  @Beta
  public static void toXML(XMLWriter xml, Document doc, List<Terms> terms, int extractLength) throws IOException {
    xml.openElement("document", true);
    // display the value of each field
    for (Fieldable f : doc.getFields()) {
      String value = toValue(f);
      // TODO: date formatting

      // Unnecessary to return the full value of long fields
      if (value != null && value.length() < 100) {
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
      String value = toValue(f);
      // TODO: date formatting

      // unnecessary to return the full value of long fields
      if (value != null && value.length() < 100) {
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
  public static String extract(String text, String term, int length) throws IllegalArgumentException {
    if (text == null) return null;
    if (term.length() > length)
      throw new IllegalArgumentException("Term length ("+term.length()+") is larger than extract length ("+length+")");
    final int len = length - term.length();
    Pattern p = Pattern.compile("(?:\\W|^)(\\Q"+term+"\\E)(?:\\W|$)", Pattern.CASE_INSENSITIVE);
    Matcher m = p.matcher(text);
    if (m.find()) {
      StringBuilder extract = new StringBuilder();
      int start = m.start(1);
      int end = m.end(1);
      // the entire string can be used
      if (length > text.length()) {
        extract.append(asXML(text.substring(0, start)));
        extract.append("<term>").append(asXML(m.group(1))).append("</term>");
        extract.append(asXML(text.substring(end)));

      //
      } else if (start < len / 2) {
        extract.append(asXML(text.substring(0, start)));
        extract.append("<term>").append(asXML(m.group(1))).append("</term>");
        if ((text.length() - end < len - start)) {
          extract.append(asXML(text.substring(end)));
        } else {
          extract.append(asXML(text.substring(end, end+len-start-1))).append("...");
        }

      } else if (text.length() - end < len / 2) {
        int x = text.length() - end;
        if (x > start) {
          extract.append(asXML(text.substring(0, start)));
        } else {
          extract.append("...").append(asXML(text.substring(start - x, start)));
        }
        extract.append("<term>").append(asXML(m.group(1))).append("</term>");
        extract.append(asXML(text.substring(end)));

      } else {
        extract.append("...").append(asXML(text.substring(start - (len / 2), start)));
        extract.append("<term>").append(asXML(m.group(1))).append("</term>");
        extract.append(asXML(text.substring(end, end + len / 2))).append("...");
      }
      return extract.toString();
    }
    return null;
  }

  /**
   * Returns the text as a safe XML text.
   * @param text The to escape for XML.
   * @return the text as a safe XML text.
   */
  private static String asXML(String text) {
    return XMLEscapeUTF8.UTF8_ESCAPE.toElementText(text);
  }

  /**
   * Returns the value of the specified field decompressing it if required.
   *
   * @param f The field
   * @return its value
   */
  private static String toValue(Fieldable f) {
    String value = f.stringValue();
    // is it a compressed field?
    if (value == null && f.getBinaryLength() > 0) {
      try {
        value = CompressionTools.decompressString(f.getBinaryValue());
      } catch (DataFormatException ex) {
        value = null;
      }
    }
    return value;
  }
}

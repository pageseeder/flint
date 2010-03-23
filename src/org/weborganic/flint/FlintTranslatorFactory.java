package org.weborganic.flint;

import java.util.Collections;
import java.util.List;

import org.weborganic.flint.content.ContentTranslator;
import org.weborganic.flint.content.ContentTranslatorFactory;
/**
 * Simple translator factory that only handles XML MIME type by simply forwarding the content without translation.
 * 
 * @author Jean-Baptiste Reure
 * @version 10 March 2010
 */

  public class FlintTranslatorFactory implements ContentTranslatorFactory {
  /**
   * MIME type supported
   */
  protected static String XML_MIME_TYPE = "text/xml";

  public ContentTranslator createTranslator(String mimeType) {
    return new FlintXMLTranslator();
  }
  
  public List<String> getMimeTypesSupported() {
    return Collections.singletonList(XML_MIME_TYPE);
  }
  
}
package org.weborganic.flint;

import java.io.InputStreamReader;
import java.io.Reader;

import org.weborganic.flint.content.Content;
import org.weborganic.flint.content.ContentTranslator;
/**
 * Simple xml translator that handles XML MIME type by simply forwarding the content without translation.
 * 
 * @author Jean-Baptiste Reure
 * @version 10 March 2010
 */
public class FlintXMLTranslator implements ContentTranslator {
  /**
   * private constructor, only the factory can create a new translator
   */
  protected FlintXMLTranslator() {
  }
  
  public Reader translate(Content content) {
    if (content.isDeleted()) return null;
    if (!FlintTranslatorFactory.XML_MIME_TYPE.equals(content.getMimeType())) return null;
    return new InputStreamReader(content.getSource());
  }

}

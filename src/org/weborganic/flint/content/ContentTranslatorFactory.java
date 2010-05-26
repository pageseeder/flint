package org.weborganic.flint.content;

import java.util.List;

/**
 * A Factory to create <code>org.weborganic.flint.content.ContentTranslator</code> objects used to
 * translate <code>org.weborganic.flint.content.Content</code> into XML.
 * 
 * @author Jean-Baptiste Reure
 * 
 * @version 9 March 2010
 */
public interface ContentTranslatorFactory {

  /**
   * Return the list of MIME Types supported
   * 
   * @return the list of MIME Types supported
   */
  public List<String> getMimeTypesSupported();

  /**
   * Return an instance of <code>ContentTranslator</code> used to translate Content with the MIME 
   * Type provided.
   * 
   * @param mimeType the MIME Type of the Content
   * 
   * @return a <code>ContentTranslator</code> object
   */
  public ContentTranslator createTranslator(String mimeType);

}

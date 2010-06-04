package org.weborganic.flint.content;

import java.util.Collection;

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
   * Return the list of MIME Types supported this factory.
   * 
   * @return the list of MIME Types supported
   */
  Collection<String> getMimeTypesSupported();

  /**
   * Return an instance of <code>ContentTranslator</code> used to translate Content with the MIME 
   * Type provided.
   * 
   * @param mimeType the MIME Type of the Content
   * 
   * @return a <code>ContentTranslator</code> instance.
   */
  ContentTranslator createTranslator(String mimeType);

}

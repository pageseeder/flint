package org.weborganic.flint;

import java.util.Collections;
import java.util.List;

import org.weborganic.flint.content.ContentTranslator;
import org.weborganic.flint.content.ContentTranslatorFactory;

/**
 * Simple translator factory that only handles XML MIME type by simply forwarding the content 
 * without translation.
 * 
 * @author Jean-Baptiste Reure
 * @author Christophe Lauret
 * @version 26 May 2010
 */
public class FlintTranslatorFactory implements ContentTranslatorFactory {

  /**
   * MIME type supported.
   */
  protected static final String XML_MIME_TYPE = "text/xml";
  // XXX the correct MIME for XML is be 'application/xml'

  /**
   * The translator for folders: a source forwarder
   */
  private final ContentTranslator xmlTranslator;

  /**
   * Creates a new factory for {@value XML_MIME_TYPE}. 
   */
  public FlintTranslatorFactory() {
    this.xmlTranslator = new SourceForwarder(XML_MIME_TYPE);
  }

  /**
   * Only creates a translator if the specified MIME type matches {@value XML_MIME_TYPE}.
   * 
   * {@inheritDoc}
   */
  public ContentTranslator createTranslator(String mimeType) {
    if (XML_MIME_TYPE.equals(mimeType)) return this.xmlTranslator;
    return null;
  }

  /**
   * Returns a singleton list containing the {@value XML_MIME_TYPE}.
   * 
   * {@inheritDoc}
   */
  public List<String> getMimeTypesSupported() {
    return Collections.singletonList(XML_MIME_TYPE);
  }

}

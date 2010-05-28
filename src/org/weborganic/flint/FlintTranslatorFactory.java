package org.weborganic.flint;

import java.util.ArrayList;
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
   * XML MIME types supported.
   */
  protected static final List<String> XML_MIME_TYPES = new ArrayList<String>();
  static {
    XML_MIME_TYPES.add("text/xml");
    XML_MIME_TYPES.add("application/xml");
    XML_MIME_TYPES.add("application/xhtml+xml");
  }
  /**
   * The translator for xml files: a source forwarder
   */
  private final ContentTranslator xmlTranslator;

  /**
   * <p>Creates a new factory for {@value XML_MIME_TYPES}.</p>
   * <p>See XML MIME Types at 
   * @link <a href="https://www3.tools.ietf.org/html/rfc3023">XML Media Types</a>
   * </p>
   * <p> And XHTML Media Types at
   * @link <a href="http://www.w3.org/TR/xhtml-media-types/">XHTML Media Types</a>
   * </p>
   */
  public FlintTranslatorFactory() {
    this.xmlTranslator = new SourceForwarder(XML_MIME_TYPES);
  }

  /**
   * Only creates a translator if the specified MIME type matches one of {@value XML_MIME_TYPES}.
   * 
   * {@inheritDoc}
   */
  public ContentTranslator createTranslator(String mimeType) {
    if (XML_MIME_TYPES.contains(mimeType)) return this.xmlTranslator;
    return null;
  }

  /**
   * Returns a list containing the {@value XML_MIME_TYPES}.
   * 
   * {@inheritDoc}
   */
  public List<String> getMimeTypesSupported() {
    return Collections.unmodifiableList(XML_MIME_TYPES);
  }

}

/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.weborganic.flint.api.ContentTranslator;
import org.weborganic.flint.api.ContentTranslatorFactory;
import org.weborganic.flint.content.SourceForwarder;

/**
 * Simple translator factory that only handles XML Media types by simply forwarding the content
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
   * The translator for XML files: a source forwarder
   */
  private final ContentTranslator xmlTranslator;

  /**
   * <p>Creates a new factory for {@value XML_MIME_TYPES}.</p>
   *
   * @see <a href="https://www3.tools.ietf.org/html/rfc3023">XML Media Types</a>
   * @see <a href="http://www.w3.org/TR/xhtml-media-types/">XHTML Media Types</a>
   */
  public FlintTranslatorFactory() {
    this.xmlTranslator = new SourceForwarder(XML_MIME_TYPES, "UTF-8");
  }

  /**
   * Only creates a translator if the specified MIME type matches one of {@value XML_MIME_TYPES}.
   *
   * {@inheritDoc}
   */
  @Override
  public ContentTranslator createTranslator(String mediaType) {
    if (XML_MIME_TYPES.contains(mediaType)) return this.xmlTranslator;
    return null;
  }

  /**
   * Returns a list containing the {@value XML_MIME_TYPES}.
   *
   * {@inheritDoc}
   */
  @Override
  public List<String> getMimeTypesSupported() {
    return Collections.unmodifiableList(XML_MIME_TYPES);
  }

}

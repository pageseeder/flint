/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.content;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple translator factory that only handles XML Media types by simply forwarding the content
 * without translation.
 *
 * <p>This translator will automatically add new XML media types.
 *
 * <p>THey include by default:
 * <ul>
 *   <li><code>text/xml</code></li>
 *   <li><code>application/xml</code></li>
 *   <li><code>application/xhtml+xml</code></li>
 * </ul>
 *
 * @see <a href="https://www3.tools.ietf.org/html/rfc3023">XML Media Types</a>
 * @see <a href="http://www.w3.org/TR/xhtml-media-types/">XHTML Media Types</a>
 *
 * @author Jean-Baptiste Reure
 * @author Christophe Lauret
 *
 * @version 18 October 2012
 */
public class AutoXMLTranslatorFactory implements ContentTranslatorFactory {

  /**
   * XML MIME types supported.
   */
  private final List<String> types = new ArrayList<String>();

  /**
   * The translator for XML files: a source forwarder
   */
  private final ContentTranslator xmlTranslator;

  /**
   * Creates a new factory for XML media types.
   */
  public AutoXMLTranslatorFactory() {
    this.xmlTranslator = new SourceForwarder(this.types, "UTF-8");
    this.types.add("text/xml");
    this.types.add("application/xml");
    this.types.add("application/xhtml+xml");
  }

  /**
   * Creates a new factory with the default XML media types and including the specified types.
   *
   * @param include Additional media types to include.
   */
  public AutoXMLTranslatorFactory(List<String> include) {
    this();
    this.types.addAll(include);
  }

  /**
   * Only creates a translator if the specified media type corresponds to XML.
   *
   * <p>If the media type is identified as an XML media type (ending with "+xml"), it is
   * automatically added.
   *
   * {@inheritDoc}
   */
  @Override
  public ContentTranslator createTranslator(String mediaType) {
    if (this.types.contains(mediaType)) return this.xmlTranslator;
    if (mediaType.endsWith("+xml")) {
      this.types.add(mediaType);
      return this.xmlTranslator;
    }
    return null;
  }

  /**
   * Returns a list containing the XML media types recognised by this translator factory.
   *
   * @return a list containing the XML media types recognised by this translator factory.
   */
  @Override
  public List<String> getMimeTypesSupported() {
    return Collections.unmodifiableList(this.types);
  }

}

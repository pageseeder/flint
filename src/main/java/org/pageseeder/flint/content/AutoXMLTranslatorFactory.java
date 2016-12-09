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
package org.pageseeder.flint.content;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple translator factory that only handles XML Media types by simply forwarding the content
 * without translation.
 *
 * <p>This translator will automatically add new XML media types.
 *
 * <p>They include by default:
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
 * @version 27 February 2013
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

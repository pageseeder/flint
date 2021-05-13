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
 * @author Jean-Baptiste Reure
 * @author Christophe Lauret
 * @version 26 May 2010
 */
public class FlintTranslatorFactory implements ContentTranslatorFactory {

  /**
   * XML MIME types supported.
   */
  private static final List<String> XML_MIME_TYPES = new ArrayList<>();
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
   * <p>Creates a new factory for supported XML mime types.</p>
   *
   * @see <a href="https://www3.tools.ietf.org/html/rfc3023">XML Media Types</a>
   * @see <a href="http://www.w3.org/TR/xhtml-media-types/">XHTML Media Types</a>
   */
  public FlintTranslatorFactory() {
    this.xmlTranslator = new SourceForwarder(XML_MIME_TYPES, "UTF-8");
  }

  /**
   * Only creates a translator if the specified MIME type matches one of the supported XML mime types.
   *
   * {@inheritDoc}
   */
  @Override
  public ContentTranslator createTranslator(String mediaType) {
    if (XML_MIME_TYPES.contains(mediaType)) return this.xmlTranslator;
    return null;
  }

  /**
   * Returns a list containing the supported XML mime types.
   *
   * {@inheritDoc}
   */
  @Override
  public List<String> getMimeTypesSupported() {
    return Collections.unmodifiableList(XML_MIME_TYPES);
  }

}

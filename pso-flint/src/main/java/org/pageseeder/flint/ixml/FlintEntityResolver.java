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
package org.pageseeder.flint.ixml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Resolves the public identifiers used by Flint.
 *
 * <p>Flint public identifiers should match the following:
 *
 * <pre>
 *   -//Weborganic//DTD::Flint [name_of_schema]//EN
 * </pre>
 *
 * @author  Christophe Lauret (Weborganic)
 *
 * @version 1 March 2010
 */
public final class FlintEntityResolver implements EntityResolver {

  /**
   * The old prefix used by Flint for all public identifiers.
   */
  public static final String PUBLIC_ID_PREFIX_LEGACY = "-//Weborganic//DTD::Flint Index Documents ";

  /**
   * The prefix used by Flint for all public identifiers.
   * <p>
   * Public identifiers starting with any other prefix than this one or the legacy one will be ignored.
   */
  public static final String PUBLIC_ID_PREFIX = "-//Flint//DTD::Index Documents ";

  /**
   * The suffix used by Flint for all public identifiers.
   */
  private static final String PUBLIC_ID_SUFFIX = "//EN";

  /**
   * This class is a singleton.
   */
  private static final FlintEntityResolver SINGLETON = new FlintEntityResolver();

  /**
   * Creates a new Flint Entity resolver - singleton: keep it private.
   */
  private FlintEntityResolver() {
  }

  /**
   * @see org.xml.sax.EntityResolver#resolveEntity(String, String)
   *
   * @param publicId The public identifier for the entity.
   * @param systemId The system identifier for the entity.
   *
   * @return The entity as an XML input source.
   *
   * @throws SAXException If the library has not been defined.
   */
  @Override
  public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
    // process only public identifiers that are valid for Flint
    String dtd = toFileName(publicId);
    if (dtd != null) {
      // Try to find the resource based on the public ID first.
      InputStream inputStream = FlintEntityResolver.class.getResourceAsStream("/library/"+dtd);
      // Try the System ID if this fails.
      if (inputStream == null) {
        inputStream = toInputStream(systemId);
      }
      // Give up
      if (inputStream == null)
        return null;
      return new InputSource(inputStream);
    // use the default behaviour
    } else return null;
  }

  /**
   * Returns the file name for the specified public ID.
   * <p>
   * Only "-//Weborganic//DTD::Flint"
   *
   * @param publicId the public identifier.
   * @return The corresponding filename.
   */
  private static String toFileName(String publicId) {
    if (publicId == null) return null;
    String version = null;
    if (publicId.startsWith(PUBLIC_ID_PREFIX)) {
      int length = publicId.endsWith(PUBLIC_ID_SUFFIX)? publicId.length() - PUBLIC_ID_SUFFIX.length() : publicId.length();
      if (length <= PUBLIC_ID_PREFIX.length()) return null;
      version = publicId.substring(PUBLIC_ID_PREFIX.length(), length);
    } else if (publicId.startsWith(PUBLIC_ID_PREFIX_LEGACY)) {
      int length = publicId.endsWith(PUBLIC_ID_PREFIX_LEGACY)? publicId.length() - PUBLIC_ID_PREFIX_LEGACY.length() : publicId.length();
      if (length <= PUBLIC_ID_PREFIX_LEGACY.length()) return null;
      version = publicId.substring(PUBLIC_ID_PREFIX_LEGACY.length(), length);
    }
    if (version == null) return null;
    return "index-documents-" + version + ".dtd";
  }

  /**
   * Returns the single instance defined in this class.
   *
   * @return The single instance defined in this class.
   */
  public static FlintEntityResolver getInstance() {
    return SINGLETON;
  }

  // Private helpers
  // ----------------------------------------------------------------------------------------------

  /**
   * Returns the input stream for the specified system ID.
   *
   * @param systemId The system ID
   * @return The corresponding resource or <code>null</code>;
   * @throws SAXException Wrap any IO or malformed URL exception.
   */
  private InputStream toInputStream(String systemId) throws SAXException {
    // Try to use the system ID then
    if (systemId.startsWith("http://")) {
      try {
        URL url = new URL(systemId);
        return url.openStream();
      } catch (IOException ex) {
        throw new SAXException("Unable to resolve entity.", ex);
      }
    } else {
      try {
        return new FileInputStream(systemId);
      } catch (FileNotFoundException ex) {
        throw new SAXException("Unable to resolve entity.", ex);
      }
    }
  }

}

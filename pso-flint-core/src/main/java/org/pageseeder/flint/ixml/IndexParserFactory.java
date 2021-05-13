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

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.pageseeder.flint.IndexException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class is a factory for Index Parser, allows for reusable parser to be produced.
 *
 * @author  Christophe Lauret (Weborganic)
 *
 * @version 15 October 2009
 */
public final class IndexParserFactory extends DefaultHandler {

  /**
   * The logger for this class.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(IndexParserFactory.class);

  /**
   * Generate an index document parser instance that can only be used for XSLT transformation.
   * 
   * @param catalog the catalog to add the fields to (can be <code>null</code>).
   *
   * @return an index parser instance.
   */
  public static IndexParser getInstanceForTransformation(String catalog) {
    return new IndexParser(catalog);
  }

  /**
   * Generate an index document parser instance.
   *
   * @return an index parser instance.
   *
   * @throws IndexException Should any error occur.
   */
  public static IndexParser getInstance() throws IndexException {
    try {
      // get SAX instance and initialise
      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setValidating(true);
      factory.setNamespaceAware(false);
      // also specify the features
      factory.setFeature("http://xml.org/sax/features/validation", true);
      factory.setFeature("http://xml.org/sax/features/namespaces", false);
      factory.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
      // produce a SAX parser instance
      SAXParser parser = factory.newSAXParser();
      XMLReader reader = parser.getXMLReader();
      // use this handler
      return new IndexParser(reader);
      // return the document
    } catch (ParserConfigurationException ex) {
      LOGGER.error("Error while generating index document parser instance.", ex);
      throw new IndexException("An error occurred when trying to generate a parser instance.", ex);
    } catch (SAXException ex) {
      LOGGER.error("Error while generating index document parser instance.", ex);
      throw new IndexException("An error occurred when trying to generate a parser instance.", ex);
    }
  }

}

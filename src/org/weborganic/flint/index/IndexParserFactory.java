/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.index;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.flint.IndexException;
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

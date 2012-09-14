/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.index;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * A ruthless error handler.
 *
 * <p>Any error or warning will throw an exception.
 *
 * @author Jean-Baptiste Reure
 * @version 26 February 2010
 */
public class FlintErrorHandler implements ErrorHandler {

  // TODO: make a little more lenient!

  @Override
  public void error(SAXParseException exc) throws SAXException {
    throw exc;
  }

  @Override
  public void fatalError(SAXParseException exc) throws SAXException {
    throw exc;
  }

  @Override
  public void warning(SAXParseException exc) throws SAXException {
    throw exc;
  }

}

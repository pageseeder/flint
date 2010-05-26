/*
 * This file is part of the Flint library.
 * 
 * For licensing information please see the file license.txt included in the release. A copy of this licence can also be
 * found at http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.content;

/**
 * The ID for a Content.
 * The IndexManager must be able to identify a Content using this ID and its ContentType.
 * 
 * 
 * @author Jean-Baptiste Reure
 * @version 26 February 2010
 */
public interface ContentId {

  /**
   * Turn this ID in a String (must be unique).
   * 
   * @return a String representation of this ID
   */
  String getID();
  // XXX: unique? within the index??

  /**
   * Load the Content Type attached to this ID.
   * 
   * @return the Content Type attached to this ID.
   */
  ContentType getContentType();

}

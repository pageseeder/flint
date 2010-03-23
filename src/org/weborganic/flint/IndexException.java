/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint;

/**
 * An exception dedicated to indexing problems.
 * 
 * <p>Used to wrap other exceptions.
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 9 February 2007
 */
public final class IndexException extends Exception {

  /**
   * The serial version UID as required by the Serializable interface.
   */
  private static final long serialVersionUID = 200702022618001L;

  /**
   * Creates a new IndexException.
   * 
   * @param message The message.
   * @param ex The exception.
   */
  public IndexException(String message, Exception ex) {
    super(message, ex);
  }
}

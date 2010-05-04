/*
 * This file is part of the Flint library.
 * 
 * For licensing information please see the file license.txt included in the release. A copy of this licence can also be
 * found at http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint;


/**
 * A Job Requester.
 * 
 * @author Jean-Baptiste Reure
 * @version 26 February 2010
 */
public interface Requester {

  /**
   * Return the ID for this requester.
   * 
   * @return the ID for this requester.
   */
  public String getRequesterID();

}

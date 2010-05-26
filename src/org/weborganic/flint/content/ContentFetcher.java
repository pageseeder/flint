/*
 * This file is part of the Flint library.
 * 
 * For licensing information please see the file license.txt included in the release. A copy of this licence can also be
 * found at http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.content;


/**
 * This class provides a way for the IndexManager to fetch the content to add to the index.
 * 
 * <p>Content is identified by its ContentID and its ContentType.
 * 
 * @author Jean-Baptiste Reure
 * @version 1 March 2010
 */
public interface ContentFetcher {

  /**
   * Load the Content.
   * 
   * @param id  the ContentID
   * 
   * @return the source where the Content is read from
   */
  Content getContent(ContentId id);

}

/*
 * This file is part of the Flint library.
 * 
 * For licensing information please see the file license.txt included in the release. A copy of this licence can also be
 * found at http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.content;

import java.io.InputStream;

/**
 * This class provides a way for the IndexManager to fetch the content to add to the index.
 * Content is identified by its ContentID and its ContentType.
 * 
 * @author Jean-Baptiste Reure
 * @version 1 March 2010
 */
public interface Content {
  
  /**
   * Load the content as a stream, ready to be translated.
   * 
   * @return the stream where the Content is read from
   */
  public InputStream getSource();
  
  /**
   * Load the MimeType for the content
   * 
   * @return a String representation of the MIME type.
   */
  public String getMimeType();

  /**
   * Return true if the content should be deleted from the index.
   * 
   * @return true if the content should be deleted from the index, false otherwise.
   */
  public boolean isDeleted();
  
  /**
   * Return the config ID to use when transforming the content, null if no ID needed.
   * 
   * @return the config ID to use when transforming the content, null if no ID needed.
   */
  public String getConfigID();
  
  /**
   * Return the rule used to delete the previous content.
   * This is used for updating a document or a simple delete job.
   * 
   * @return a <code>DeleteRule</code> object
   */
  public DeleteRule getDeleteRule();

}

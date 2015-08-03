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
package org.pageseeder.flint.api;

import java.io.InputStream;

import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.content.DeleteRule;

/**
 * This class provides a way for the IndexManager to fetch the content to add to the index.
 *
 * <p>Content is identified by its ContentID and its Media Type.
 *
 * @see <a href="http://tools.ietf.org/html/rfc2046">MIME Part Two: Media Types</a>
 * @see <a href="http://tools.ietf.org/html/rfc3023">XML Media Types</a>
 *
 * @author Jean-Baptiste Reure
 * @author Christophe Lauret
 * @version 29 July 2010
 */
public interface Content {

  /**
   * Returns the content as a stream, ready to be translated.
   *
   * <p>Implementations should buffer large content.
   *
   * @return the stream where the Content is read from
   *
   * @throws IndexException Should any error occur when retrieving the source.
   */
  InputStream getSource() throws IndexException;

  /**
   * Load the media type for the content.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2046">MIME Part Two: Media Types</a>
   * @see <a href="http://tools.ietf.org/html/rfc3023">XML Media Types</a>
   *
   * @return a String representation of the media type.
   *
   * @throws IndexException Should any error occur when retrieving the value.
   */
  String getMediaType() throws IndexException;

  /**
   * Return the config ID to use when transforming the content, null if no ID needed.
   *
   * @return the config ID to use when transforming the content, null if no ID needed.
   */
  String getConfigID();

  /**
   * Return <code>true</code> if the content should be deleted from the index.
   *
   * @return <code>true</code> if the content should be deleted from the index, false otherwise.
   *
   * @throws IndexException Should any error occur when retrieving the value.
   */
  boolean isDeleted() throws IndexException;

  /**
   * Returns the rule used to delete the previous content.
   *
   * <p>This is used for updating a document or a simple delete job.
   *
   * @return a <code>DeleteRule</code> object
   */
  DeleteRule getDeleteRule();

}

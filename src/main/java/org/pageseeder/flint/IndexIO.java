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
package org.pageseeder.flint;

import org.pageseeder.flint.content.DeleteRule;
import org.pageseeder.flint.indexing.FlintDocument;

import java.util.List;

/**
 * Provides a set of utility methods to deal with IO operations on an Index.
 *
 * <p>This class is useful to centralise all operations on an index because it will
 * create one writer and share it with other classes if needed.
 *
 * <p>This is a lower level API.
 *
 * @author Jean-Baptiste Reure
 * @author Christophe Lauret
 *
 * @version 27 February 2013
 */
public interface IndexIO {

  long getLastTimeUsed();

  /**
   * Closes the index.
   *
   * @throws IndexException if closing failed.
   */
  void stop() throws IndexException;

  /**
   * Commit any changes if the state of the index requires it.
   */
  void maybeCommit();

  /**
   * Commit any changes if the state of the index requires it.
   */
  void maybeRefresh();

  /**
   * Clears the index as soon as possible (asynchronously).
   *
   * @return <code>true</code> if the indexed could be scheduled for clearing;
   *         <code>false</code> otherwise.
   * @throws IndexException should any error be thrown by Lucene.
   */
  boolean clearIndex() throws IndexException;

  /**
   * Delete the documents defined in the delete rule as soon as possible
   * (asynchronously).
   *
   * @param rule the rule to identify the items to delete
   * @return <code>true</code> if the item could be be scheduled for deletion;
   *         <code>false</code>
   * @throws IndexException should any error be thrown by Lucene.
   */
  boolean deleteDocuments(DeleteRule rule) throws IndexException;

  /**
   * Update the documents defined in the delete rule as soon as possible
   * (asynchronously).
   *
   * <p>
   * It is not possible to update an item in Lucene, instead it is first deleted
   * then inserted again.
   *
   * @param rule the rule to identify the items to delete before update.
   * @param documents the list of documents to replace with.
   * @return <code>true</code> if the item could be be scheduled for update;
   *         <code>false</code>
   * @throws IndexException should any error be thrown by Lucene
   */
  boolean updateDocuments(DeleteRule rule, List<FlintDocument> documents) throws IndexException;

}

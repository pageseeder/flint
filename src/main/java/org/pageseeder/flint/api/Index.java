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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.store.Directory;

/**
 * An interface to represents a unique index repository.
 *
 * @author Jean-Baptiste Reure
 * @version 26 February 2010
 */
public interface Index {

  /**
   * Return the unique identifier for this index.
   *
   * @return The Index ID.
   */
  String getIndexID();

  /**
   * Return the Index Directory object.
   *
   * @return The Index Directory object
   */
  Directory getIndexDirectory();

  /**
   * Return the Lucene Analyzer.
   *
   * @return the Lucene Analyzer used in this index
   */
  Analyzer getAnalyzer();

}

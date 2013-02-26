/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.api;

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

/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.query;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.weborganic.flint.util.Beta;

/**
 * An interface for defining search queries using flint.
 * 
 * <p>A Flint query typically wraps a Lucene query and provide an XML representation.
 * 
 * <p>Implementations must translate the user parameters into a meaningful query.
 * 
 * @author Christophe Lauret (Weborganic)
 * 
 * @version 13 Augsut 2010
 */
@Beta
public interface FlintQuery extends SearchParameter {

  /**
   * Indicates whether the search query is empty.
   * 
   * <p>A search query is considered empty when it does not contain any parameter that would
   * yield meaningful results with an index search.
   * 
   * @return <code>true</code> if the search query is empty;
   *         <code>false</code> otherwise.
   */
  boolean isEmpty();

  /**
   * Returns the Lucene query instance corresponding to this object.
   * 
   * @return The query object.
   */
  Query toQuery();

  /**
   * Returns the sorting key for the results.
   * 
   * @return The sorting key for the results.
   */
  Sort getSort();

}

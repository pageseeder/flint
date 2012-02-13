/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.query;

import org.apache.lucene.search.Query;

import com.topologi.diffx.xml.XMLWritable;

/**
 * An interface for defining search parameters to be aggregated into a query.
 *
 * <p>
 * Implementations must translate the user parameters into a meaningful query.
 *
 * <p>
 * The search query implementations must provide an XML representation.
 *
 * @author Christophe Lauret (Allette Systems)
 *
 * @version 19 November 2006
 */
public interface SearchParameter extends XMLWritable {

  /**
   * Indicates whether the search query is empty.
   *
   * <p>
   * A search query is considered empty when it does not contain any parameter that would yield
   * meaningful results with an index search.
   *
   * @return <code>true</code> if the search query is empty; <code>false</code> otherwise.
   */
  boolean isEmpty();

  /**
   * Returns the Lucene query instance corresponding to this object.
   *
   * @return The query object
   */
  Query toQuery();

}

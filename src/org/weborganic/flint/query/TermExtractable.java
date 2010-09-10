package org.weborganic.flint.query;

import java.util.Set;

import org.apache.lucene.index.Term;
import org.weborganic.flint.util.Beta;

/**
 * Indicates that terms can be extracted from the query.
 * 
 * <p>Flint Query implementations that implement this interface have better control over the terms
 * which are relevant to generate extracts.
 * 
 * <p>In particular for Lucene Range Queries, where the query have to be rewritten in order to get
 * the relevant parameters.
 * 
 * @author Christophe Lauret
 * @version 10 September 2010
 */
@Beta
public interface TermExtractable {

  /**
   * Adds the terms occurring in this query to the terms set.
   * 
   * <p>This method allows better control over the terms that needs to be extracted from a query.
   * 
   * @param terms terms to collection
   */
  void extractTerms(Set<Term> terms) ;

}

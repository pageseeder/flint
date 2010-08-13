/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.util;

import java.util.regex.Pattern;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;

/**
 * A set of utility methods related to query objects in Lucene.
 *
 * @author  Christophe Lauret (Weborganic)
 * @version 13 August 2010
 */
public final class Queries {

  /**
   * Text that matches this pattern is considered a phrase.
   */
  private static final Pattern IS_A_PHRASE = Pattern.compile("\\\"[^\\\"]+\\\"");

  /**
   * Prevents creation of instances. 
   */
  private Queries() {
  }

  /**
   * Returns the term or phrase query corresponding to the specified text.
   * 
   * <p>If the text is surrounded by double quotes, this method will 
   * return a {@link PhraseQuery} otherwise, it will return a simple {@link TermQuery}. 
   * 
   * <p>Note: Quotation marks are thrown away.
   * 
   * @param field the field to construct the terms.
   * @param text  the text to construct the query from.
   * @return the corresponding query.
   */
  @Beta
  public static Query toTermOrPhraseQuery(String field, String text) {
    if (field == null) throw new NullPointerException("field");
    if (text == null) throw new NullPointerException("text");
    boolean isPhrase = IS_A_PHRASE.matcher(text).matches();
    if (isPhrase) {
      PhraseQuery phrase = new PhraseQuery();
      String[] terms = text.substring(1, text.length()-1).split("\\s+"); 
      for (String t : terms) {
        phrase.add(new Term(field, t));
      }
      return phrase;
    } else {
      return new TermQuery(new Term(field, text));
    }
  }

  /**
   * Returns a boolean query combining all the specified queries in {@link Occur#MUST} clauses
   * as it is were an AND operator.
   * 
   * @param queries the queries to combine with an AND.
   * @return The combined queries.
   */
  public static Query and(Query... queries) {
    BooleanQuery query = new BooleanQuery();
    for (Query q : queries) {
      query.add(q, Occur.MUST);
    }
    return query;
  }

  /**
   * Returns a boolean query combining all the specified queries in {@link Occur#MUST} clauses
   * as it is were an OR operator.
   * 
   * @param queries the queries to combine with an OR.
   * @return The combined queries.
   */
  public static Query or(Query... queries) {
    BooleanQuery query = new BooleanQuery();
    for (Query q : queries) {
      query.add(q, Occur.SHOULD);
    }
    return query;
  }

}

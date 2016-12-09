package org.pageseeder.flint.lucene;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.pageseeder.flint.content.DeleteRule;
import org.pageseeder.flint.lucene.query.SearchQuery;

public class LuceneDeleteRule implements DeleteRule {

  /**
   * The Term used to delete
   */
  private final Term term;

  /**
   * A Lucene query used to delete
   */
  private final Query _query;

  /**
   * Build a rule based on a term.
   *
   * @param fieldname the name of the field
   * @param fieldvalue the value of the field
   */
  public LuceneDeleteRule(String fieldname, String fieldvalue) {
    this.term = new Term(fieldname, fieldvalue);
    this._query = null;
  }

  /**
   * Build a rule based on a Lucene query.
   *
   * @param query the search query to use to identify the file to delete
   */
  public LuceneDeleteRule(SearchQuery query) {
    this._query = query.toQuery();
    this.term = null;
  }

  /**
   * Return the Term used for deleting.
   *
   * @return the Term used for deleting
   */
  public Term toTerm() {
    return this.term;
  }

  /**
   * Return the Query used for deleting.
   *
   * @return the Query used for deleting
   */
  public Query toQuery() {
    return this._query;
  }

  /**
   * Indicates whether the delete rule is defined by an index Term.
   *
   * @return <code>true</code> if a Term defines the rule;
   *         <code>false</code> if a query does
   */
  public boolean useTerm() {
    return this._query == null;
  }

}

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
package org.pageseeder.flint.lucene.query;

/**
 * This class allows for a better control of search results by specifying paging information for the
 * index tools to use.
 *
 * <p>Pages start at 1.
 *
 * <p>Note: This has nothing to do with memory management.
 *
 * @author Christophe Lauret (Weborganic)
 * @version 10 February 2012
 */
public final class SearchPaging {

  /**
   * The default maximum number of hits per page.
   */
  public static final int DEFAULT_PAGE = 1;

  /**
   * The default maximum number of hits per page.
   */
  public static final int DEFAULT_HITS_PER_PAGE = 20;

  /**
   * The requested page of the search results.
   */
  private int _page = DEFAULT_PAGE;

  /**
   * The number of hits per page, default is 20.
   */
  private int _hitsPerPage = DEFAULT_HITS_PER_PAGE;

  /**
   * Creates a new paging configuration using the default values.
   */
  public SearchPaging() {
    this._page = DEFAULT_PAGE;
    this._hitsPerPage = DEFAULT_HITS_PER_PAGE;
  }

  /**
   * Creates a new paging configuration using the specified values.
   *
   * @param page        The page requested.
   * @param hitsPerPage The maximum number of results per page.
   */
  public SearchPaging(int page, int hitsPerPage) {
    this._page = page;
    this._hitsPerPage = hitsPerPage;
  }

  /**
   * Returns the number of hits per page for the searches.
   *
   * @return The number of hits per page for the searches.
   */
  public int getHitsPerPage() {
    return this._hitsPerPage;
  }

  /**
   * Sets the number of hits per page for the searches.
   *
   * <p>A negative value in indicates no limit.
   *
   * @param hitsPerPage The number of hits per page for the searches.
   */
  public void setHitsPerPage(int hitsPerPage) {
    this._hitsPerPage = hitsPerPage;
  }

  /**
   * Returns the requested page number.
   *
   * @return The requested page number.
   */
  public int getPage() {
    return this._page;
  }

  /**
   * Sets the page number.
   *
   * @param page The page number.
   *
   * @throws IllegalArgumentException if the page is less than 1.
   */
  public void setPage(int page) {
    if (page < 1) throw new IllegalArgumentException("Pages must be positive natural integers");
    this._page = page;
  }

  // Methods providing common function for a result set
  // ----------------------------------------------------------------------------------------------

  /**
   * Returns the first hit based on the total number of hits.
   *
   * <p>If hits per page is positive, it is the result of :
   * <pre>
   *   HITS_PER_PAGE x (PAGE - 1) + 1
   * </pre>
   * <p>Otherwise, this method always returns 1;
   *
   * @return the first hit based on the total number of hits (inclusive)
   */
  public int getFirstHit() {
    if (this._hitsPerPage <= 0) return 1;
    return this._hitsPerPage * (this._page - 1) + 1;
  }

  /**
   * Returns the last hit for the current page in the results based on the total number hits.
   *
   * <p>If hits per page is positive, it is the result of :
   * <pre>
   *   MIN( HITS_PER_PAGE x PAGE - 1, TOTAL_HITS)
   * </pre>
   * <p>Otherwise, this method always returns 1;
   *
   * @param totalHits The total number of hits.
   *
   * @return the last hit based on the total number of hits (inclusive).
   */
  public int getLastHit(int totalHits) {
    if (this._hitsPerPage <= 0) return totalHits;
    return Math.min(totalHits, this._hitsPerPage * this._page);
  }

  /**
   * Returns the number of pages in the results based on the total number hits.
   *
   * <p>If hits per page is positive, it is the result of :
   * <pre>
   *   ((TOTAL_HITS - 1) / HITS_PER_PAGE) + 1
   * </pre>
   * <p>Otherwise, this method always returns 1;
   *
   * @param totalHits The total number of hits.
   *
   * @return the last hit based on the total number of hits (inclusive).
   */
  public int getPageCount(int totalHits) {
    if (this._hitsPerPage <= 0) return 1;
    return ((totalHits - 1) / this._hitsPerPage) + 1;
  }

}

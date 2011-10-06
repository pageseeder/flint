/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.query;

/**
 * This class allows for a better control of search results by specifying paging information for the
 * index tools to use.
 * 
 * <p>Pages start at 1.
 * 
 * <p>Note: This has nothing to do with memory management.
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 6 October 2011
 */
public final class SearchPaging {

  /**
   * The default maximum number of hits per page.
   */
  public static final int DEFAULT_HITS_PER_PAGE = 20;

  /**
   * The requested page of the search results.
   */
  private int _page = 1;

  /**
   * The number of hits per page, default is 20.
   */
  private int _hitsPerPage = DEFAULT_HITS_PER_PAGE;

  /**
   * Set mode whether the paging should equally distribute the hits
   */
  private boolean _eqDist = false;

  /**
   * Set the total number of page, to be used with eqDist
   */
  private int _totalpage = 1;

  /**
   * Creates a new paging configuration using the default values.
   */
  public SearchPaging() {
  }

  /**
   * Creates a new paging configuration using the specified values.
   * 
   * @param page        The page requested.
   * @param hitsPerPage The maximum number of results per page.
   */
  public SearchPaging(int page, int hitsPerPage) {
    setPage(page);
    setHitsPerPage(hitsPerPage);
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
   * <p>
   * -1 indicates no limit.
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
    // TODO: If the page number is less then 0
    if (page < 1) throw new IllegalArgumentException("Pages must be greater than 1");
    this._page = page;
  }

  /**
   * Sets the mode to equally distribute hits across the specified page.
   */
  public void setEqDist(int totalpage) {
    // set the eqDist mode to true
    this._eqDist = true;
    this._totalpage = (totalpage > 0) ? totalpage : 1;
  }

  /**
   * Indicates whether its on eqDist mode.
   * 
   * @return <code>true</code> if this eqDist mode is on;
   *         <code>false</code> otherwise.
   */
  public boolean checkEqDist() {
    return this._eqDist;
  }

  /**
   * Get the total number of pages.
   */
  public int getTotalPage() {
    return this._totalpage;
  }
}

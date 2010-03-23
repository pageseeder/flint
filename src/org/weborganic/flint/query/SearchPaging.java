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
 * <p>
 * Note: This has nothing to do with memory management.
 * 
 * @author Christophe Lauret (Weborganic)
 * 
 * @version 22 August 2006
 */
public final class SearchPaging {

  /**
   * The default maximum number of hits per page.
   */
  public static final int DEFAULT_HITS_PER_PAGE = 20;

  /**
   * The requested page of the search results.
   */
  private int page = 1;

  /**
   * The number of hits per page, default is 20.
   */
  private int hitsPerPage = DEFAULT_HITS_PER_PAGE;

  /**
   * Set mode whether the paging should equally distribute the hits
   */
  private boolean eqDist = false;

  /**
   * Set the total number of page, to be used with eqDist
   */
  private int totalpage = 1;

  /**
   * Returns the number of hits per page for the searches.
   * 
   * @return The number of hits per page for the searches.
   */
  public int getHitsPerPage() {
    return this.hitsPerPage;
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
    this.hitsPerPage = hitsPerPage;
  }

  /**
   * Returns the requested page number.
   * 
   * @return The requested page number.
   */
  public int getPage() {
    return this.page;
  }

  /**
   * Sets the page number.
   * 
   * @param page The page number.
   */
  public void setPage(int page) {
    // TODO: If the page number is less then 0
    this.page = page;
  }

  /**
   * Sets the mode to equally distribute hits across the specified page.
   */
  public void setEqDist(int totalpage) {
    // set the eqDist mode to true
    this.eqDist = true;
    this.totalpage = (totalpage > 0) ? totalpage : 1;
  }

  /**
   * Indicates whether its on eqDist mode.
   * 
   * @return <code>true</code> if this eqDist mode is on;
   *         <code>false</code> otherwise.
   */
  public boolean checkEqDist() {
    return this.eqDist;
  }

  /**
   * Get the total number of pages.
   */
  public int getTotalPage() {
    return this.totalpage;
  }
}

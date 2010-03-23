/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.legacy;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.weborganic.flint.query.SearchAssistant;
import org.weborganic.flint.query.SearchPaging;
import org.weborganic.flint.query.SearchQuery;
import org.weborganic.flint.query.SearchResults;

/**
 * Search the specified index using the Lucene search engine.
 * 
 * @author Christophe Lauret (Weborganic)
 * @author Tu Tak Tran (Allette Systems)
 * @author William Liem (Allette Systems)
 * @author Jin Zhou (Allette Systems)
 * 
 * @version 19 November 2009
 */
public final class IndexSearch {

  /**
   * The logger for this class.
   */
  private static final Logger LOGGER = Logger.getLogger(IndexSearch.class);

  /**
   * The index to search.
   */
  private final File _index;
  private final Directory _indexDirectory;

  /**
   * The analyser used by this search engine.
   */
  private final Analyzer _analyzer = new StandardAnalyzer(LegacyLuceneVersion.VERSION);

  /**
   * The analyser used by this search engine.
   */
  private IndexReader _ireader;
  private Searcher _searcher;

  // constructors -------------------------------------------------------------------------------

  /**
   * Creates a new index search.
   * 
   * @param index The directory where the index is located (required).
   * 
   * @throws IllegalArgumentException If there is no index at the specified location or if the index
   *           is <code>null</code>.
   */
  public IndexSearch(File index) throws IllegalArgumentException {
    // Check that the parameter is not null
    if (index == null)
      throw new IllegalArgumentException("The index must be specified.");
    // and check that it corresponds to a real index
    try {
      this._indexDirectory = FSDirectory.open(this._index);
      if (!IndexReader.indexExists(this._indexDirectory))
        throw new IllegalArgumentException("There is no index at the specified location: " + index.toURI());
    } catch (IOException e) {
      throw new IllegalArgumentException("Invalid index location: " + index.toURI());
    }
    this._index = index;
    LOGGER.debug("Creating new search on index " + index.getAbsolutePath());
  }

  // getter and setter ---------------------------------------------------------------------------

  /**
   * Returns the index used for searches.
   * 
   * @return The index used for searches.
   */
  public File getIndex() {
    return this._index;
  }

  // methods ------------------------------------------------------------------------------------

  /**
   * Performs a search using the specified search parameters and writing the results as XML using
   * the <code>PrintWriter</code>.
   * 
   * @param params The search parameters.
   * @param paging The search paging parameters.
   * 
   * @return The search results.
   * 
   * @throws ParseException If the predicate be invalid.
   * @throws IOException If an I/O error occurs whilst reading the index.
   */
  public SearchResults search(SearchQuery params, SearchPaging paging) throws ParseException, IOException {

    // generate the query
    Query query = params.toQuery();
    LOGGER.info("Generated Query    = " + query.toString());
    LOGGER.info("Original Predicate = " + params.getPredicate());

    /*
     * // This block of code seeems to generate a NullPointerException when using wild card queries
     * try { LOGGER.info("Optimized Query    = "+query.rewrite(this.ireader).toString()); } catch
     * (Exception ex) { ex.printStackTrace(); LOGGER.error(ex); }
     */

    // initialise
    if (this._ireader == null)
      this._ireader = IndexReader.open(this._indexDirectory);

    // making the search
    try {
      // make the search
      IndexSearcher searcher = new IndexSearcher(this._ireader);
      TopFieldDocs hits = searcher.search(query, null, Integer.MAX_VALUE, params.getSort());
      if (this._searcher == null)
        this._searcher = searcher;
      LOGGER.info(hits.totalHits + " results found");

      // process results
      SearchResults results = new SearchResults(hits, paging, searcher);
      // searcher.close();
      return results;

    } catch (Exception ex) {
      LOGGER.error("The search generated an error.", ex);
      return null;
    }
  }

  /**
   * Performs a unique group by search using the specified search parameters and writing the results
   * as XML using the <code>PrintWriter</code>.
   * 
   * @param params The search parameters.
   * @param paging The search paging parameters.
   * 
   * @return The search results.
   * 
   * @throws ParseException If the predicate be invalid.
   * @throws IOException If an I/O error occurs while reading the index.
   */
  public SearchResults uniqueSearch(SearchQuery params, SearchPaging paging) throws ParseException, IOException {

    // generate the query
    Query query = params.toQuery();
    LOGGER.info("Generated Query    = " + query.toString());
    LOGGER.info("Original Predicate = " + params.getPredicate());

    // initialise
    if (this._ireader == null) {
      this._ireader = IndexReader.open(this._indexDirectory);
    }
    GroupingHitCollector hcoll = new GroupingHitCollector(paging.getHitsPerPage());
    hcoll.setReader(this._ireader);

    // making the search
    try {
      // make the search
      IndexSearcher searcher = new IndexSearcher(this._ireader);
      searcher.search(query, hcoll);
      if (this._searcher == null)
        this._searcher = searcher;

      // get all hits
      ScoreDoc[] hits = hcoll.getSortedKeyScoreDocs();
      // process results
      SearchResults results = new SearchResults(hits, paging, searcher);
      // searcher.close();
      return results;

    } catch (Exception ex) {
      LOGGER.error("The search generated an error.", ex);
      return null;
    }
  }

  /**
   * Performs a search using the specified search parameters and returns the number of hits found.
   * 
   * @param params The search parameters.
   * 
   * @return The number of search results.
   * 
   * @throws ParseException If the predicate be invalid.
   * @throws IOException If an I/O error occurs while reading the index.
   */
  public int numSearchResults(SearchQuery params) throws ParseException, IOException {

    Query query = params.toQuery();
    LOGGER.info("numSearchResults: Generated Query    = " + query.toString());
    LOGGER.info("numSearchResults: Original Predicate = " + params.getPredicate());

    // initialise
    if (this._ireader == null)
      this._ireader = IndexReader.open(this._indexDirectory);

    // making the search
    try {
      // make the search
      Searcher searcher = new IndexSearcher(this._ireader);
      TopFieldDocs hits = searcher.search(query, null, Integer.MAX_VALUE, params.getSort());
      LOGGER.info(hits.totalHits + " results found");
      return hits.totalHits;

    } catch (Exception ex) {
      LOGGER.error("The search generated an error.", ex);
      return 0;
    }
  }

  /**
   * Performs a search using the specified search parameters and writing the results as XML using
   * the <code>PrintWriter</code>.
   * 
   * @param params The search parameters.
   * 
   * @return The search results.
   * 
   * @throws ParseException If the predicate be invalid.
   * @throws IOException If an I/O error occurs whilst reading the index.
   */
  public SearchAssistant getAssistant(SearchQuery params) throws ParseException, IOException {

    // get the search parameters
    String predicate = params.getPredicate();
    String field = params.getField();

    // generate the query
    QueryParser parser = new QueryParser(LegacyLuceneVersion.VERSION, field, this._analyzer);
    Query query = parser.parse(predicate);

    // initialise
    if (this._ireader == null)
      this._ireader = IndexReader.open(this._indexDirectory);

    // returning the assistant
    return new SearchAssistant(this._ireader, query);
  }

  /**
   * Returns the document corresponding to the specified document ID.
   * 
   * @param id The Id of the document.
   * 
   * @return The corresponding document or <code>null</code> if an error occurred.
   * 
   * @throws CorruptIndexException If thrown by the searcher.
   * @throws IOException           If thrown by the searcher.
   */
  public Document doc(int id) throws CorruptIndexException, IOException {
    try {
      return this._searcher.doc(id);

    // FIXME: these exception are not handled properly.
    } catch (CorruptIndexException ex) {
      LOGGER.error("The search generated CorruptIndexException error.", ex);
      return null;
    } catch (IOException ex) {
      LOGGER.error("The search generated IOException error.", ex);
      return null;
    }
  }

  /**
   * Provide a method for closing the index reader to avoid "too many open files" error.
   */
  public void closeIndex() {
    try {
      this._ireader.close();
    } catch (IOException e) {
      LOGGER.error("Fail to close the index.", e);
    }
  }

}

/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.query;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TimeZone;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopFieldDocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.flint.IndexException;
import org.weborganic.flint.IndexIO;
import org.weborganic.flint.util.Dates;
import org.weborganic.flint.util.Documents;
import org.weborganic.flint.util.Fields;

import com.topologi.diffx.xml.XMLWritable;
import com.topologi.diffx.xml.XMLWriter;

/**
 * A container for search results.
 *
 * <p>Use this class to serialise Lucene Search results as XML.
 *
 * <p>Note: the current implementation is a "throw away" object, once the toXML method has been
 * called, this instance is useless.
 *
 * <p>This class is not synchronized.
 *
 * @author Christophe Lauret (Weborganic)
 * @author Jean-Baptiste Reure (Weborganic)
 * @author William Liem (Allette Systems)
 *
 * @version 10 February 2012
 */
public final class SearchResults implements XMLWritable {

  /**
   * Logger.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(SearchResults.class);

  /**
   * Types of values formatted in the result.
   */
  private static enum ValueType {STRING, DATE, DATETIME};

  /**
   * The maximum length for a field to expand.
   */
  private static final int MAX_FIELD_VALUE_LENGTH = 1000;

  /**
   * One minute in milliseconds.
   */
  private static final int ONE_MINUTE_IN_MS = 60000;

  /**
   * One hour in milliseconds.
   */
  private static final int ONE_HOUR_IN_MS = 3600000;

  /**
   * The actual search results from Lucene.
   */
  private final ScoreDoc[] _scoredocs;

  /**
   * Fields used for sorting.
   */
  private final SortField[] _sortfields;

  /**
   * Indicates the paging information.
   */
  private final SearchPaging _paging;

  /**
   * The query used to produce these results.
   */
  private final SearchQuery _query;

  /**
   * The index searcher used.
   */
  private final IndexSearcher _searcher;

  /**
   * The index I/O.
   */
  private final IndexIO _indexIO;

  /**
   * The total number of results.
   */
  private final int totalNbOfResults;

  // State variables
  // ---------------------------------------------------------------------------------------------

  /**
   * A state variable to indicate whether the search results instance can still be accessed.
   */
  private boolean _terminated = false;

  /**
   * The timezone offset used to adjust the correct date and time.
   */
  private int timezoneOffset;

  // Constructors
  // ---------------------------------------------------------------------------------------------

  /**
   * Creates a new SearchResults.
   *
   * @param query    The search query that was used to produce these results.
   * @param docs     The actual search results from Lucene in TopFieldDocs.
   * @param paging   The paging configuration.
   * @param io       The IndexIO object, used to release the searcher when terminated
   * @param searcher The Lucene searcher.
   *
   * @throws IndexException if the documents could not be retrieved from the Index
   */
  public SearchResults(SearchQuery query, TopFieldDocs docs, SearchPaging paging, IndexIO io, IndexSearcher searcher)
      throws IndexException {
    this(query, docs.scoreDocs, docs.fields, docs.totalHits, paging, io, searcher);
  }

  /**
   * Creates a new SearchResults.
   *
   * @param query    The search query that was used to produce these results.
   * @param docs     The actual search results from Lucene in ScoreDoc.
   * @param paging   The paging configuration.
   * @param io       The IndexIO object, used to release the searcher when terminated
   * @param searcher The Lucene searcher.
   *
   * @throws IndexException if the documents could not be retrieved from the Index
   */
  public SearchResults(SearchQuery query, ScoreDoc[] docs, int totalHits, SearchPaging paging, IndexIO io, IndexSearcher searcher)
      throws IndexException {
    this(query, docs, null, totalHits, paging, io, searcher);
  }

  /**
   * Creates a new SearchResults.
   *
   * @param hits The actual search results from Lucene in ScoreDoc.
   * @param sortf The Field used to sort the results
   * @param paging The paging configuration.
   * @param io The IndexIO object, used to release the searcher when terminated
   * @param searcher The Lucene searcher.
   *
   * @throws IndexException if the documents could not be retrieved from the Index
   */
  private SearchResults(SearchQuery query, ScoreDoc[] hits, SortField[] sortf, int totalResults, SearchPaging paging, IndexIO io,
      IndexSearcher searcher) throws IndexException {
    this._query = query;
    this._scoredocs = hits;
    this._sortfields = sortf;
    this._paging = paging != null? paging : new SearchPaging();
    this._searcher = searcher;
    this._indexIO = io;
    this.totalNbOfResults = totalResults;
    // default timezone is the server's
    TimeZone tz = TimeZone.getDefault();
    this.timezoneOffset = tz.getRawOffset();
    // take daylight savings into account
    if (tz.inDaylightTime(new Date())) this.timezoneOffset += ONE_HOUR_IN_MS;
  }

  // Basic public methods
  // ---------------------------------------------------------------------------------------------

  /**
   * Returns the total number of results.
   *
   * @return the total number of results.
   */
  public int getTotalNbOfResults() {
    return this.totalNbOfResults;
  }

  /**
   * Indicates whether the search results are empty.
   *
   * @return <code>true</code> if the results are empty;
   *         <code>false</code> if there is more than one hit.
   */
  public boolean isEmpty() {
    return this.totalNbOfResults > 0;
  }

  /**
   * Sets the time zone to use when formatting the results as XML.
   *
   * @param timezoneInMinutes the timezone offset in minutes (difference with GMT)
   */
  public void setTimeZone(int timezoneInMinutes) {
    this.timezoneOffset = timezoneInMinutes * ONE_MINUTE_IN_MS;
  }

  /**
   * Serialises the search results as XML.
   *
   * @param xml The XML writer.
   *
   * @throws IOException Should there be any I/O exception while writing the XML.
   */
  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("search-results", true);
    if (this._indexIO != null) xml.attribute("index", this._indexIO.indexID());
    int firsthit = this._paging.getFirstHit();
    int lasthit = this._paging.getLastHit(this.totalNbOfResults);

    // Include query
    if (this._query != null) {
      xml.openElement("query", true);
      xml.attribute("lucene", this._query.toQuery().toString());
      this._query.toXML(xml);
      xml.closeElement();
    }

    // Display some metadata on the search
    toMetadataXML(xml);

    // Returned documents
    xml.openElement("documents", true);

    // Iterate over the hits
    for (int i = firsthit - 1; i < lasthit; i++) {
      xml.openElement("document", true);
      String score = Float.toString(this._scoredocs[i].score);
      xml.element("score", score);
      Document doc = this._searcher.doc(this._scoredocs[i].doc);

      // Find the extract only applies to TermExtractable queries
      if (this._query instanceof TermExtractable) {
        TermExtractable q = (TermExtractable)this._query;
        Set<Term> terms = new HashSet<Term>();
        q.extractTerms(terms);
        for (Fieldable f : doc.getFields()) {
          for (Term t : terms) {
            if (t.field().equals(f.name())) {
              String extract = Documents.extract(Fields.toString(f), t.text(), 200);
              if (extract != null) {
                xml.openElement("extract");
                xml.attribute("from", t.field());
                xml.writeXML(extract);
                xml.closeElement();
              }
            }
          }
        }
      }

      // display the value of each field
      for (Fieldable f : doc.getFields()) {
        // Retrieve the value
        String value = Fields.toString(f);
        ValueType type = ValueType.STRING;
        // format dates using ISO 8601 when possible
        if (value != null && value.length() > 0 && f.name().contains("date") && Dates.isLuceneDate(value)) {
          try {
            if (value.length() > 8) {
              value = Dates.toISODateTime(value, this.timezoneOffset);
              type = ValueType.DATETIME;
            } else {
              value = Dates.toISODate(value);
              if (value.length() == 10) type = ValueType.DATE;
            }
          } catch (ParseException ex) {
            LOGGER.warn("Unparseable date found {}", value);
          }
        }
        // unnecessary to return the full value of long fields
        if (value != null && value.length() < MAX_FIELD_VALUE_LENGTH) {
          xml.openElement("field");
          xml.attribute("name", f.name());
          // Display the correct attributes so that we know we can format the date
          if (type == ValueType.DATE) xml.attribute("date", value);
          else if (type == ValueType.DATETIME) xml.attribute("datetime", value);
          xml.writeText(value);
          xml.closeElement();
        }
      }
      // close 'document'
      xml.closeElement();
    }
    // close 'documents'
    xml.closeElement();

    // close 'results'
    xml.closeElement();

    // close everything
    try {
      terminate();
    } catch (IndexException ex) {
      throw new IOException("Error when terminating Search Results", ex);
    }
  }

  /**
   * Return the actual results.
   *
   * @return the search results.
   *
   * @throws IndexException If the search results have already been terminated.
   */
  public ScoreDoc[] getScoreDoc() throws IndexException {
    if (this._terminated)
      throw new IndexException("Cannot retrieve documents after termination", new IllegalStateException());
    return this._scoredocs;
  }

  /**
   * Load a document from the index.
   *
   * <p>Note this
   *
   * @param id the id of the document
   * @return the document object loaded from the index, could be null
   *
   * @throws IndexException if the index is invalid
   */
  public Document getDocument(int id) throws IndexException {
    if (this._terminated)
      throw new IndexException("Cannot retrieve documents after termination", new IllegalStateException());
    try {
      return this._searcher.doc(id);
    } catch (CorruptIndexException e) {
      LOGGER.error("Failed to retrieve a document because of a corrupted Index", e);
      throw new IndexException("Failed to retrieve a document because of a corrupted Index", e);
    } catch (IOException ioe) {
      LOGGER.error("Failed to retrieve a document because of an I/O problem", ioe);
      throw new IndexException("Failed to retrieve a document because of an I/O problem", ioe);
    }
  }

  /**
   * Release all references to the searcher.
   *
   * <p>Does nothing if the results have already been terminated.
   *
   * @throws IndexException Will wrap any IO error thrown when trying to release the searcher.
   */
  public void terminate() throws IndexException {
    if (this._indexIO == null) return;
    if (this._terminated) return;
    try {
      this._indexIO.releaseSearcher(this._searcher);
      this._terminated = true;
    } catch (IOException ex) {
      String msg = "Failed releasing a Searcher after performing a query on the Index because of an I/O problem";
      LOGGER.error(msg, ex);
      throw new IndexException(msg, ex);
    }
  }

  /**
   * Ensure that the searcher is released when finalizing.
   *
   * @throws Throwable If thrown by super class or <code>terminate</code> method.
   */
  @Override
  protected void finalize() throws Throwable {
    if (!this._terminated) terminate();
    super.finalize();
  }

  /**
   * Provides an iterable class over the Lucene documents.
   *
   * <p>This allows Lucene documents from these results to be iterated over in a for each loop:
   * <pre>
   *   for (Document doc : results.documents()) {
   *     ...
   *   }
   * </pre>
   *
   * @return an iterable class over the Lucene documents.
   *
   * @throws IllegalStateException If these results have been closed (terminated already).
   */
  public Iterable<Document> documents() {
    if (this._terminated)
      throw new IllegalStateException();
    return new DocIterable();
  }

  // Private helpers
  // ----------------------------------------------------------------------------------------------

  /**
   * Write the search results metadata as XML.
   *
   * @param xml     The XML writer
   *
   * @throws IOException Should an error occur while writing the XML
   */
  private void toMetadataXML(XMLWriter xml) throws IOException {
    SearchPaging page = this._paging;
    int total = this.totalNbOfResults;
    // Display some metadata on the search
    xml.openElement("metadata", true);
    xml.openElement("hits", true);
    xml.element("per-page", Integer.toString(page.getHitsPerPage()));
    xml.element("total", Integer.toString(total));
    xml.closeElement();
    xml.openElement("page", true);
    xml.element("first-hit", Integer.toString(page.getFirstHit()));
    xml.element("last-hit",  Integer.toString(page.getLastHit(total)));
    xml.element("current",   Integer.toString(page.getPage()));
    xml.element("last",      Integer.toString(page.getPageCount(total)));
    xml.closeElement();
    if (this._sortfields != null) {
      xml.openElement("sort-fields", true);
      for (SortField field : this._sortfields) {
        xml.element("field", field.getField());
      }
      xml.closeElement();
    }
    xml.closeElement();
  }

  // Private classes
  // ----------------------------------------------------------------------------------------------

  /**
   * An iterable class over the documents in these results.
   *
   * @author christophe Lauret
   * @version 6 October 2011
   */
  private final class DocIterable implements Iterable<Document> {

    /**
     * Provides an iterable class over the Lucene documents.
     *
     * <p>this can be used in a for each loop
     *
     * @return an iterable class over the Lucene documents.
     */
    @Override
    public Iterator<Document> iterator() {
      return new DocIterator();
    }

  }

  /**
   * An iterator over the documents in these results.
   *
   * @author Christophe Lauret
   * @author Jean-Baptiste Reure
   * @version 16 August 2013
   */
  private final class DocIterator implements Iterator<Document> {

    /**
     * The index searcher used.
     */
    private final IndexSearcher searcher = SearchResults.this._searcher;

    /**
     * The actual search results from Lucene.
     */
    private final ScoreDoc[] scoredocs = SearchResults.this._scoredocs;

    /**
     * The current index for this iterator.
     */
    private int index = 0;

    @Override
    public boolean hasNext() {
      return this.index < this.scoredocs.length;
    }

    @Override
    public Document next() {
      if (!hasNext()) throw new NoSuchElementException();
      try {
        return this.searcher.doc(this.scoredocs[this.index++].doc);
      } catch (IOException ex) {
        throw new IllegalStateException("Error retrieving document", ex);
      }
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
    public void remove() {
      throw new UnsupportedOperationException("Cannot remove documents from searc results");
    }
  }

}

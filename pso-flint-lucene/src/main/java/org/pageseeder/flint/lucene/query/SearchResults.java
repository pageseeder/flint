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

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TimeZone;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopFieldDocs;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.lucene.LuceneIndexIO;
import org.pageseeder.flint.lucene.search.Fields;
import org.pageseeder.flint.lucene.util.Dates;
import org.pageseeder.flint.lucene.util.Documents;
import org.pageseeder.xmlwriter.XML.NamespaceAware;
import org.pageseeder.xmlwriter.XMLStringWriter;
import org.pageseeder.xmlwriter.XMLWritable;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private static enum ValueType {STRING, DATE, DATETIME, LONG, DOUBLE, INT, FLOAT};

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
  private final SearchReaders readers;

  /**
   * List of fields to get the extract from.
   */
  private final List<String> extractFields = new ArrayList<>();

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
  public SearchResults(SearchQuery query, TopFieldDocs docs, SearchPaging paging, Map<LuceneIndexIO, IndexReader> readers, IndexSearcher searcher)
      throws IndexException {
    this(query, docs.scoreDocs, docs.fields, docs.totalHits, paging, readers, searcher);
  }

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
  public SearchResults(SearchQuery query, TopFieldDocs docs, SearchPaging paging, LuceneIndexIO io, IndexSearcher searcher)
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
  public SearchResults(SearchQuery query, ScoreDoc[] docs, int totalHits, SearchPaging paging, LuceneIndexIO io, IndexSearcher searcher)
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
  private SearchResults(SearchQuery query, ScoreDoc[] hits, SortField[] sortf, int totalResults,
      SearchPaging paging, LuceneIndexIO io, IndexSearcher searcher) throws IndexException {
    this._query = query;
    this._scoredocs = hits;
    this._sortfields = sortf;
    this._paging = paging != null? paging : new SearchPaging();
    this._searcher = searcher;
    this.readers = new SearchReaders(io);
    this.totalNbOfResults = totalResults;
    // default timezone is the server's
    TimeZone tz = TimeZone.getDefault();
    this.timezoneOffset = tz.getRawOffset();
    // take daylight savings into account
    if (tz.inDaylightTime(new Date())) {
      this.timezoneOffset += ONE_HOUR_IN_MS;
    }
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
  private SearchResults(SearchQuery query, ScoreDoc[] hits, SortField[] sortf, int totalResults,
      SearchPaging paging, Map<LuceneIndexIO, IndexReader> readers, IndexSearcher searcher) throws IndexException {
    this._query = query;
    this._scoredocs = hits;
    this._sortfields = sortf;
    this._paging = paging != null? paging : new SearchPaging();
    this._searcher = searcher;
    this.readers = new SearchReaders(readers);
    this.totalNbOfResults = totalResults;
    // default timezone is the server's
    TimeZone tz = TimeZone.getDefault();
    this.timezoneOffset = tz.getRawOffset();
    // take daylight savings into account
    if (tz.inDaylightTime(new Date())) {
      this.timezoneOffset += ONE_HOUR_IN_MS;
    }
  }

  // Basic public methods
  // ---------------------------------------------------------------------------------------------

  /**
   * Add field names to get extracts from.
   * The order matters here as the first extract found is the one included in the results.
   * 
   * @param fields list of field names
   */
  public void addExtractFields(List<String> fields) {
    this.extractFields.addAll(fields);
  }

  /**
   * Add a field name to get extracts from.
   * The order matters here as the first extract found is the one included in the results.
   * 
   * @param field new field name
   */
  public void addExtractField(String field) {
    this.extractFields.add(field);
  }

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

    // Iterate over the hits to find the extracts
    boolean withExtracts = true;
    for (int i = firsthit - 1; i < lasthit; i++) {
      String score = Float.toString(this._scoredocs[i].score);
      Document doc = this._searcher.doc(this._scoredocs[i].doc);
      String extractXML = null;
      if (withExtracts) {
        Set<Term> terms = new HashSet<Term>();
        try {
          this._searcher.createWeight(this._query.toQuery().rewrite(this._searcher.getIndexReader()), true).extractTerms(terms);
        } catch (Exception ex) {
          // log it
          LOGGER.warn("Computing extract failed, no extracts will be provided", ex);
          // The query provided does not support weight, can't have extracts then
          withExtracts = false;
        }
        bigloop: for (Term t : terms) {
          for (IndexableField f : doc.getFields()) {
            if (t.field().equals(f.name()) &&
               (this.extractFields.isEmpty() || this.extractFields.contains(f.name()))) {
              String extract = Documents.extract(Fields.toString(f), t.text(), 200);
              if (extract != null) {
                XMLStringWriter xsw = new XMLStringWriter(NamespaceAware.No);
                xsw.openElement("extract");
                xsw.attribute("from", t.field());
                xsw.writeXML(extract);
                xsw.closeElement();
                extractXML = xsw.toString();
                break bigloop;
              }
            }
          }
        }
      }
      // document as XML
      documentToXML(doc, extractXML, score, this.timezoneOffset, xml);

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

  public static void documentToXML(Document doc, int timezoneOffset, XMLWriter xml) throws IOException {
    documentToXML(doc, null, null, timezoneOffset, xml);
  }

  private static void documentToXML(Document doc, String extract, String score, int timezoneOffset, XMLWriter xml) throws IOException {
    xml.openElement("document", true);

    if (score != null) xml.element("score", score);
    if (extract != null) xml.writeXML(extract);

    // display the value of each field
    for (IndexableField f : doc.getFields()) {
      // Retrieve the value
      String value = Fields.toString(f);
      ValueType type = ValueType.STRING;
      // check for numeric value
      Number number = f.numericValue();
      if (number != null) {
        if (number instanceof Long)         type = ValueType.LONG;
        else if (number instanceof Double)  type = ValueType.DOUBLE;
        else if (number instanceof Integer) type = ValueType.INT;
        else if (number instanceof Float)   type = ValueType.FLOAT;
      // format dates using ISO 8601 when possible
      } else if (value != null && value.length() > 0 && f.name().contains("date") && Dates.isLuceneDate(value)) {
        try {
          if (value.length() > 8) {
            value = Dates.toISODateTime(value, timezoneOffset);
            type = ValueType.DATETIME;
          } else {
            value = Dates.toISODate(value);
            if (value.length() == 10) {
              type = ValueType.DATE;
            }
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
        if (type == ValueType.DATE) {
          xml.attribute("date", value);
        } else if (type == ValueType.DATETIME) {
          xml.attribute("datetime", value);
        } else if (type == ValueType.LONG) {
          xml.attribute("numeric-type", "long");
        } else if (type == ValueType.DOUBLE) {
          xml.attribute("numeric-type", "double");
        } else if (type == ValueType.FLOAT) {
          xml.attribute("numeric-type", "float");
        } else if (type == ValueType.INT) {
          xml.attribute("numeric-type", "int");
        }
        if (f.binaryValue() != null) xml.attribute("compressed", "true");
        xml.writeText(value);
        xml.closeElement();
      }
    }
    // close 'document'
    xml.closeElement();
    
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
    if (this._terminated) return;
    this.readers.release(this._searcher);
    this._terminated = true;
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
    return new DocIterable(this._paging.getFirstHit() - 1);
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

    private final int _start;
    public DocIterable(int start) {
      this._start = start;
    }

    /**
     * Provides an iterable class over the Lucene documents.
     *
     * <p>this can be used in a for each loop
     *
     * @return an iterable class over the Lucene documents.
     */
    @Override
    public Iterator<Document> iterator() {
      return new DocIterator(this._start);
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
    private int index;

    public DocIterator(int start) {
      this.index = start;
    }

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
      throw new UnsupportedOperationException("Cannot remove documents from search results");
    }
  }

  private static class SearchReaders {
    private final LuceneIndexIO _single;
    private final Map<LuceneIndexIO, IndexReader> _readers = new HashMap<>();
    public SearchReaders(Map<LuceneIndexIO, IndexReader> readers) {
      this._readers.putAll(readers);
      this._single = null;
    }
    public SearchReaders(LuceneIndexIO io) {
      this._single = io;
    }
    public void release(IndexSearcher searcher) {
      if (this._single != null) {
        this._single.releaseSearcher(searcher);
      }
      for (LuceneIndexIO io : this._readers.keySet())
        io.releaseReader(this._readers.get(io));
    }
  }
}

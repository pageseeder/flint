/*
 * This file is part of the Flint library.
 * 
 * For licensing information please see the file license.txt included in the release. A copy of this licence can also be
 * found at http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.query;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.zip.DataFormatException;

import org.apache.log4j.Logger;
import org.apache.lucene.document.CompressionTools;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopFieldDocs;
import org.weborganic.flint.IndexException;
import org.weborganic.flint.IndexIO;

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
 * @author Christophe Lauret (Weborganic)
 * @author Jean-Baptiste Reure (Weborganic)
 * @author William Liem (Allette Systems)
 * 
 * @version 26 May 2009
 */
public final class SearchResults implements XMLWritable {

  /**
   * Logger.
   */
  private static final Logger LOGGER = Logger.getLogger(SearchResults.class);

  /**
   * The ISO 8601 Date and time format
   * 
   * @see <a href="http://www.iso.org/iso/date_and_time_format">ISO: Numeric representation of Dates and Time</a>
   */
  private final String ISO8601_DATETIME = "yyyy-MM-dd'T'HH:mm:ssZ";

  /**
   * The maximum length for a field to expand.
   */
  private static final int MAX_FIELD_VALUE_LENGTH = 1000;

  /**
   * The actual search results from Lucene.
   */
  private final ScoreDoc[] scoredocs;

  private final SortField[] sortfields;

  /**
   * Indicates the paging information.
   */
  private final SearchPaging paging;

  private final IndexSearcher searcher;

  private final IndexIO indexIO;

  private boolean terminated = false;

  private final int totalNbOfResults;

  private int timezoneOffset;

  /**
   * Creates a new SearchResults (legacy constructor).
   * 
   * @param hits The actual search results from Lucene in ScoreDoc.
   * @param paging The paging configuration.
   * @param searcher The Lucene searcher.
   * @deprecated
   * @throws IndexException if the documents could not be retrieved from the Index
   */
  public SearchResults(ScoreDoc[] hits, SearchPaging paging, IndexSearcher searcher) throws IOException, IndexException {
    this(hits, null, hits.length, paging, null, searcher);
  }

  /**
   * Creates a new SearchResults (legacy constructor).
   * 
   * @param fielddocs The actual search results from Lucene in TopFieldDocs.
   * @param paging The paging configuration.
   * @param searcher The Lucene searcher.
   * @deprecated
   * @throws IndexException if the documents could not be retrieved from the Index
   */
  public SearchResults(TopFieldDocs fielddocs, SearchPaging paging, IndexSearcher searcher) throws IOException,
      IndexException {
    this(fielddocs.scoreDocs, fielddocs.fields, fielddocs.totalHits, paging, null, searcher);
  }

  /**
   * Creates a new SearchResults.
   * 
   * @param fielddocs The actual search results from Lucene in TopFieldDocs.
   * @param paging The paging configuration.
   * @param io The IndexIO object, used to release the searcher when terminated
   * @param searcher The Lucene searcher.
   * @throws IndexException if the documents could not be retrieved from the Index
   */
  public SearchResults(TopFieldDocs fielddocs, SearchPaging paging, IndexIO io, IndexSearcher searcher)
      throws IOException, IndexException {
    this(fielddocs.scoreDocs, fielddocs.fields, fielddocs.totalHits, paging, io, searcher);
  }

  /**
   * Creates a new SearchResults.
   * 
   * @param hits The actual search results from Lucene in ScoreDoc.
   * @param paging The paging configuration.
   * @param io The IndexIO object, used to release the searcher when terminated
   * @param searcher The Lucene searcher.
   * @throws IndexException if the documents could not be retrieved from the Index
   */
  public SearchResults(ScoreDoc[] hits, int totalHits, SearchPaging paging, IndexIO io, IndexSearcher searcher)
      throws IndexException {
    this(hits, null, totalHits, paging, io, searcher);
  }

  /**
   * Creates a new SearchResults.
   * 
   * @param hits The actual search results from Lucene in ScoreDoc.
   * @param sortf The Field used to sort the results
   * @param paging The paging configuration.
   * @param io The IndexIO object, used to release the searcher when terminated
   * @param searcher The Lucene searcher.
   * @throws IndexException if the documents could not be retrieved from the Index
   */
  private SearchResults(ScoreDoc[] hits, SortField[] sortf, int totalResults, SearchPaging paging, IndexIO io,
      IndexSearcher search) throws IndexException {
    this.scoredocs = hits;
    this.sortfields = sortf;
    if (paging == null)
      this.paging = new SearchPaging();
    else
      this.paging = paging;
    this.searcher = search;
    this.indexIO = io;
    this.totalNbOfResults = totalResults;
    // default timezone is the server's
    TimeZone tz = TimeZone.getDefault();
    this.timezoneOffset = tz.getRawOffset();
    // take daylight savings into account
    if (tz.inDaylightTime(new Date())) this.timezoneOffset += 3600000;
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
    this.timezoneOffset = timezoneInMinutes * 60000;
  }

  /**
   * Serialises the search results as XML.
   * 
   * @param xml The XML writer.
   * 
   * @throws IOException Should there be any I/O exception while writing the XML.
   */
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("search-results", true);
    if (this.indexIO != null) xml.attribute("index", this.indexIO.indexID());
    // Check whether it's equally distribute mode, if yes then calculate num of hits for each page
    int length = this.totalNbOfResults;
    int hitsperpage = (this.paging.checkEqDist())? ((int)Math.ceil((double)length / (double)this.paging.getTotalPage()))
        : this.paging.getHitsPerPage();
    int firsthit = hitsperpage * (this.paging.getPage() - 1) + 1;
    int lasthit = Math.min(length, firsthit + hitsperpage - 1);

    // Display some metadata on the search
    xml.openElement("metadata", true);
    xml.openElement("hits", true);
    xml.element("per-page", Integer.toString(hitsperpage));
    xml.element("total", Integer.toString(length));
    xml.closeElement();
    xml.openElement("page", true);
    xml.element("first-hit", Integer.toString(firsthit));
    xml.element("last-hit", Integer.toString(lasthit));
    xml.element("current", Integer.toString(this.paging.getPage()));
    xml.element("last", Integer.toString(((length - 1) / hitsperpage) + 1));
    xml.closeElement();
    if (this.sortfields != null) {
      xml.openElement("sort-fields", true);
      for (SortField field : this.sortfields)
        xml.element("field", field.getField());
      xml.closeElement();
    }
    xml.closeElement();

    // Returned documents
    xml.openElement("documents", true);

    // iterate over the hits
    DateFormat dateformat = new SimpleDateFormat(ISO8601_DATETIME);
    for (int i = firsthit - 1; i < lasthit; i++) {
      xml.openElement("document", true);
      String score = Float.toString(this.scoredocs[i].score);
      xml.element("score", score);
      Document doc = this.searcher.doc(this.scoredocs[i].doc);
      // display the value of each field
      for (Fieldable f : doc.getFields()) {
        String value = f.stringValue();
        // is it a compressed field?
        if (value == null && f.getBinaryLength() > 0) try {
          value = CompressionTools.decompressString(f.getBinaryValue());
        } catch (DataFormatException e) {
          // oh well, the field is probably not a date then, we'll keep going with the index value
          LOGGER.error("Failed to decompress field value", e);
          continue;
        }
        if (f.name().contains("date")) {
          if (value == null || "0".equals(value)) value = "";
          if (!"".equals(value)) try {
            Date date = DateTools.stringToDate(value);
            TimeZone tz = TimeZone.getDefault();
            if (tz.inDaylightTime(date))
              tz.setRawOffset(this.timezoneOffset - 3600000);
            else
              tz.setRawOffset(this.timezoneOffset);
            dateformat.setTimeZone(tz);
            value = dateformat.format(date);
          } catch (Exception e) {
            // oh well, the field is probably not a date then, we'll keep going with the index value
            LOGGER.error("Failed to format date field", e);
          }
        }
        // unnecessary to return the full value of long fields
        if (value.length() < MAX_FIELD_VALUE_LENGTH) {
          xml.openElement("field");
          xml.attribute("name", f.name());
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
    } catch (IndexException e) {
      throw new IOException("Error when terminating Search Results", e);
    }
  }

  /**
   * Return the results
   * 
   * @return the search results
   * @throws IndexException
   */
  public ScoreDoc[] getScoreDoc() throws IndexException {
    if (this.terminated)
      throw new IndexException("Cannot retrieve documents after termination", new IllegalStateException());
    return this.scoredocs;
  }

  /**
   * Load a document from the index.
   * 
   * @param id the id of the document
   * @return the document object loaded from the index, could be null
   * @throws IndexException if the index is invalid
   */
  public Document getDocument(int id) throws IndexException {
    if (this.terminated)
      throw new IndexException("Cannot retrieve documents after termination", new IllegalStateException());
    try {
      return this.searcher.doc(id);
    } catch (CorruptIndexException e) {
      LOGGER.error("Failed to retrieve a document because of a corrupted Index", e);
      throw new IndexException("Failed to retrieve a document because of a corrupted Index", e);
    } catch (IOException ioe) {
      LOGGER.error("Failed to retrieve a document because of an I/O problem", ioe);
      throw new IndexException("Failed to retrieve a document because of an I/O problem", ioe);
    }
  }

  /**
   * Release all references to the searcher
   * 
   * @throws IndexException
   */
  public void terminate() throws IndexException {
    if (this.indexIO == null) return;
    try {
      this.indexIO.releaseSearcher(searcher);
      this.terminated = true;
    } catch (IOException ioe) {
      LOGGER.error("Failed releasing a Searcher after performing a query on the Index because of an I/O problem", ioe);
      throw new IndexException(
          "Failed releasing a Searcher after performing a query on the Index because of an I/O problem", ioe);
    }
  }

  @Override
  protected void finalize() throws Throwable {
    if (!this.terminated) terminate();
    super.finalize();
  }
}

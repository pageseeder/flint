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
package org.pageseeder.flint.ixml;

import org.pageseeder.flint.indexing.FlintDocument;
import org.pageseeder.flint.indexing.FlintField;
import org.pageseeder.flint.indexing.FlintField.IndexOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * The handler for the Flint Index Documents format version 2.
 *
 * @see <a href="http://weborganic.org/code/flint/schema/index-documents-2.0.dtd">Index Documents 2.0 Schema</a>
 *
 * @author Christophe Lauret
 * @version 10 September 2010
 */
final class IndexDocumentHandler_2_0 extends DefaultHandler implements IndexDocumentHandler {

  /**
   * Use the GMT time zone.
   */
  private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

  /**
   * The logger for this class.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(IndexDocumentHandler_2_0.class);

  // class attributes
  // -------------------------------------------------------------------------------------------

  /**
   * The time zone to use when parsing dates.
   */
  private TimeZone _timezone = TimeZone.getDefault();

  /**
   * Date parser instances.
   */
  private final Map<String, SimpleDateFormat> dfs = new HashMap<>();

  /**
   * The list of Lucene documents produced by this handler.
   */
  private List<FlintDocument> documents;

  // state variables for documents and fields
  // ----------------------------------------------------------------------------------------------

  /**
   * The current document being processed.
   */
  private FlintDocument _document;

  /**
   * Flag to indicate whether a field is being processed (affects the behaviour of characters())
   */
  private boolean _isField;

  /**
   * The field builder.
   */
  private FlintField field = new FlintField(null);

  /**
   * The characters found within a field.
   */
  private final StringBuilder _value = new StringBuilder();

  // constructors
  // ----------------------------------------------------------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public List<FlintDocument> getDocuments() {
    return this.documents;
  }

  // SAX Methods
  // ----------------------------------------------------------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public void startDocument() {
    LOGGER.debug("Start processing iXML document (version 2.0)");
    this.documents = new ArrayList<>();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void endDocument() {
    LOGGER.debug("End processing iXML document");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) {
    if ("field".equals(qName)) {
      startFieldElement(attributes);
    } else if ("document".equals(qName)) {
      startDocumentElement();
    } else if ("documents".equals(qName)) {
      startDocumentsElement(attributes);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void endElement(String uri, String localName, String qName) {
    if ("field".equals(qName)) {
      endFieldElement();
    } else if ("document".equals(qName)) {
      endDocumentElement();
    }
  }

  /**
   * Receives notification of character data inside an element.
   *
   * <p>Replace the '\n' (newline character) by a space.
   *
   * @param ch     The characters
   * @param start  The start position in the character array.
   * @param length The number of characters to use from the character array.
   */
  @Override
  public void characters(char[] ch, int start, int length) {
    if (this._isField) {
      this._value.append(ch, start, length);
    }
  }

  // private helpers
  // -------------------------------------------------------------------------------------------

  /**
   * Handles the start of a 'documents' element.
   *
   * @param atts The attributes to handles.
   */
  private void startDocumentsElement(Attributes atts) {
    LOGGER.debug("Parsing index document set");
    String timezone = atts.getValue("timezone");
    if (timezone != null) {
      LOGGER.debug("Setting timezone to");
      this._timezone = TimeZone.getTimeZone(timezone);
    } else {
      this._timezone = GMT;
    }
  }

  /**
   * Handles the start of a 'document' element.
   */
  private void startDocumentElement() {
    LOGGER.debug("Parsing index document");
    this._document = new FlintDocument();
  }

  /**
   * Handles the end of a 'document' element.
   */
  private void endDocumentElement() {
    LOGGER.debug("Storing document");
    if (this._document.isEmpty()) {
      LOGGER.warn("This document is empty - will not be stored");
    } else {
      this.documents.add(this._document);
    }
    this._document = null;
  }

  /**
   * Handles the start of a new 'field' element
   *
   * @param atts The attributes to handles.
   */
  private void startFieldElement(Attributes atts) {
    this.field.name(atts.getValue("name"));
    indexAttribute(atts.getValue("index"));
    storeAttribute(atts.getValue("store"));
    // Optional attributes
    termVectorAttribute(atts.getValue("term-vector"));
    // Date handling
    this.field.dateFormat(toDateFormat(atts.getValue("date-format")));
    this.field.resolution(atts.getValue("date-resolution"));
    // Set attributes ready for recording content
    String type = atts.getValue("numeric-type");
    if (type != null) {
      this.field.numeric(type);
      this.field.precisionStep(atts.getValue("precision-step"));
    }
    this._isField = true;
  }

  /**
   * Handles the end of a 'field' element.
   */
  private void endFieldElement() {
    try {
      // set the value
      this.field.value(this._value.toString());
      this._document.add(this.field);

    } catch (IllegalStateException  | IllegalArgumentException ex) {
      LOGGER.warn("Unable to create field: "+this.field.name(), ex);
    }

    // Reset the class attributes involved in this field
    this.field = new FlintField(null);
    this._isField = false;
    this._value.setLength(0);
  }

  /**
   * Returns the date format to use, allowing recycling.
   *
   * <p>Set the current date format to <code>null<code> if the format is <code>null</code>.
   *
   * <p>Otherwise retrieve from map or create an instance if it has never been created.
   *
   * <p>Note: we only set the timezone if the date format includes a time component; otherwise we default to GMT to
   * ensure that Lucene will preserve the date.
   *
   * @param format The date format used.
   * @return the corresponding date format or <code>null</code>.
   */
  private SimpleDateFormat toDateFormat(String format) {
    if (format == null) return null;
    SimpleDateFormat df = this.dfs.get(format);
    // Date format not processed yet
    if (df == null) {
      try {
        df = new SimpleDateFormat(format);
        if (includesTime(format)) {
          df.setTimeZone(this._timezone);
        } else {
          df.setTimeZone(GMT);
        }
        this.dfs.put(format, df);
      } catch (IllegalArgumentException ex) {
        LOGGER.warn("Ignoring unusable date format '"+format+"'", ex);
      }
    }
    return df;
  }

//  /**
//   * Filter the char specified to be printed.
//   *
//   * @param c The char to filter
//   * @return The char to print
//   */
//  private static char filterChar(char c) {
//    // TODO: Check why we need to filter these characters?
//    switch (c) {
//      case '\n' : return ' ';
//      case '\t' : return ' ';
//      default: return c;
//    }
//  }

  /**
   * Indicates whether the format includes a time component.
   *
   * @param format The date format
   * @return <code>true</code> if it includes a time component;
   *         <code>false</code> otherwise.
   */
  private static boolean includesTime(String format) {
    if (format.indexOf('H') >= 0) return true; // Hour in day (0-23)
    else if (format.indexOf('k') >= 0) return true; // Hour in day (1-24)
    else if (format.indexOf('K') >= 0) return true; // Hour in am/pm (0-11)
    else if (format.indexOf('h') >= 0) return true; // Hour in am/pm (1-12)
    else if (format.indexOf('m') >= 0) return true; // Minute in hour
    else if (format.indexOf('s') >= 0) return true; // Second in minute
    else if (format.indexOf('S') >= 0) return true; // Millisecond
    else if (format.indexOf('Z') >= 0) return true; // Time zone
    else if (format.indexOf('z') >= 0) return true; // Time zone
    return false;
  }

  /**
   * Handle the store attribute on fields, support lucene 3 values.
   *
   * @param store the value of the store attribute
   */
  private void storeAttribute(String store) {
    if (store != null) {
      switch (store.toLowerCase()) {
        case "compress":
        case "true":
        case "yes":
          this.field.store(true);
          break;
        case "false":
        case "no":
          this.field.store(false);
          break;
      }
    }
  }

  /**
   * Handle the index attribute on fields, support lucene 3 values.
   *
   * @param index the value of the index attribute
   */
  private void indexAttribute(String index) {
    if (index != null) {
      switch (index.toLowerCase()) {
        case "analyzed":
          this.field.index(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS).omitNorms(false).tokenize(true);
          break;
        case "not-analyzed":
          this.field.index(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS).omitNorms(false).tokenize(false);
          break;
        case "analyzed-no-norms":
          this.field.index(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS).omitNorms(true).tokenize(true);
          break;
        case "not-analyzed-no-norms":
          this.field.index(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS).omitNorms(true).tokenize(false);
          break;
        case "no":
          this.field.index(IndexOptions.NONE).omitNorms(true);
          break;
        default:
          LOGGER.warn("Invalid field index value: {}", index);
      }
    }
  }

  /**
   * Handle the term-vector attribute on fields, support lucene 3 values.
   *
   * @param vector the value of the term-vector attribute
   */

  private void termVectorAttribute(String vector) {
    if (vector != null) {
      switch (vector.toLowerCase()) {
        case "yes":
          this.field.termVector(true).termVectorOffsets(false).termVectorPositions(false);
        case "with-offset":
          this.field.termVector(true).termVectorOffsets(true).termVectorPositions(false);
        case "with-positions":
          this.field.termVector(true).termVectorOffsets(false).termVectorPositions(true);
        case "with-positions-offsets":
          this.field.termVector(true).termVectorOffsets(true).termVectorPositions(true);
        case "no":
          this.field.termVector(false).termVectorOffsets(false).termVectorPositions(false);
        default:
          LOGGER.warn("Invalid term vector value: {}", vector);
      }
    }
  }
}

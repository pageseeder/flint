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
package org.pageseeder.flint.index;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * The handler for the Flint Index Documents format version 5.
 *
 * @see <a href="http://weborganic.org/code/flint/schema/index-documents-5.0.dtd">Index Documents 5.0 Schema</a>
 *
 * @author Jean-Baptiste Reure
 * @version 27 June 2016
 */
final class IndexDocumentHandler_5_0 extends DefaultHandler implements IndexDocumentHandler {

  /**
   * Use the GMT time zone.
   */
  private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

  /**
   * The logger for this class.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(IndexDocumentHandler_5_0.class);

  // class attributes
  // -------------------------------------------------------------------------------------------

  /**
   * The catalog to associate with the fields.
   */
  private final String _catalog;

  /**
   * The time zone to use when parsing dates.
   */
  private TimeZone _timezone = TimeZone.getDefault();

  /**
   * Date parser instances.
   */
  private final Map<String, DateFormat> dfs = new HashMap<String, DateFormat>();

  /**
   * The list of Lucene documents produced by this handler.
   */
  private List<Document> documents;

  // state variables for documents and fields
  // ----------------------------------------------------------------------------------------------

  /**
   * The current document being processed.
   */
  private Document _document;

  /**
   * Flag to indicate whether a field is being processed (affects the behaviour of characters())
   */
  private boolean _isField;

  /**
   * Flag to indicate that the current field should be compressed (may result in two fields).
   */
  private boolean _isCompressed;

  /**
   * The field builder.
   */
  private FieldBuilder builder = null;

  /**
   * The characters found within a field.
   */
  private StringBuilder _value = new StringBuilder();

  // constructors
  // ----------------------------------------------------------------------------------------------

  public IndexDocumentHandler_5_0(String catalog) {
    this._catalog = catalog;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Document> getDocuments() {
    return this.documents;
  }

  // SAX Methods
  // ----------------------------------------------------------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public void startDocument() {
    this.documents = new ArrayList<Document>();
    this.builder = new FieldBuilder(this._catalog);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) {
    if ("field".equals(qName)) {
      startFieldElement(attributes);
    } else if ("document".equals(qName)) {
      startDocumentElement(attributes);
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
   *
   * @throws SAXException Any SAX exception, possibly wrapping another exception.
   */
  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    if (this._isField) {
      for (int i = start; i < (length+start); i++) {
        this._value.append(filterChar(ch[i]));
      }
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
   *
   * @param atts The attributes to handle.
   */
  private void startDocumentElement(Attributes atts) {
    this._document = new Document();
  }

  /**
   * Handles the end of a 'document' element.
   */
  private void endDocumentElement() {
    if (this._document.getFields().isEmpty()) {
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
    // required attributes
    this.builder.name(atts.getValue("name"))
                .index(atts.getValue("index"));
    // handle compression
    if ("compress".equals(atts.getValue("store"))) {
      this._isCompressed = true;
      this.builder.store(false);
    } else {
      this._isCompressed = false;
      this.builder.store(atts.getValue("store"));
    }
    // Numeric type
    String numType = atts.getValue("numeric-type");
    if (numType != null) {
      this.builder.numeric(numType).precisionStep(atts.getValue("precision-step"));
    }
    // Optional attributes
    this.builder.termVector(atts.getValue("term-vector"))
                .termVectorPositions(atts.getValue("term-vector-positions"))
                .termVectorOffsets(atts.getValue("term-vector-offsets"))
                .termVectorPayloads(atts.getValue("term-vector-payloads"))
                .boost(atts.getValue("boost"))
                .tokenize(atts.getValue("tokenize"))
                .docValues(atts.getValue("doc-values"), numType != null);
    // Date handling
    this.builder.dateFormat(toDateFormat(atts.getValue("date-format")))
                .resolution(atts.getValue("date-resolution"));
    this._isField = true;
  }

  /**
   * Handles the end of a 'field' element.
   */
  private void endFieldElement() {
    try {
      // set the value
      this.builder.value(this._value.toString());

      // compressed field
      if (this._isCompressed) {
        // Only include field if it is indexable
        if (this.builder.index() != IndexOptions.NONE) {
          this._document.add(this.builder.build());
        }
        // A compressed field is not indexable
        this._document.add(this.builder.buildCompressed());

      // uncompressed field
      } else  {
        IndexableField docValues = this.builder.buildDocValues();
        if (docValues != null) {
          this._document.add(docValues);
          // add normal field as well
          this._document.add(this.builder.build());
        } else {
          this._document.add(this.builder.build());
        }
      }

    } catch (IllegalStateException ex) {
      LOGGER.warn("Unable to create field: "+this.builder.name(), ex);
    } catch (IllegalArgumentException ex) {
      LOGGER.warn("Unable to create field: "+this.builder.name(), ex);
    }

    // Reset the class attributes involved in this field
    this.builder.reset();
    this._isField = false;
    this._isCompressed = false;
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
  private DateFormat toDateFormat(String format) {
    if (format == null) return null;
    DateFormat df = this.dfs.get(format);
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

  /**
   * Filter the char specified to be printed.
   *
   * @param c The char to filter
   * @return The char to print
   */
  private static char filterChar(char c) {
    // TODO: Check why we need to filter these characters?
    switch (c) {
      case '\n' : return ' ';
      case '\t' : return ' ';
      default: return c;
    }
  }

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

}

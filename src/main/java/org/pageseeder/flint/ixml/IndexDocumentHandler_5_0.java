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

import java.util.ArrayList;
import java.util.List;

import org.pageseeder.flint.indexing.FlintDocument;
import org.pageseeder.flint.indexing.FlintField;
import org.pageseeder.flint.indexing.FlintField.IndexOptions;
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
   * Flag to indicate that the current field should be compressed (may result in two fields).
   */
  private boolean _isCompressed;

  /**
   * The field builder.
   */
  private FlintField field = null;

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
    this.documents = new ArrayList<FlintDocument>();
    this.field = new FlintField(this._catalog);
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
      this._value.append(ch, start, length);
    }
  }

  // private helpers
  // -------------------------------------------------------------------------------------------

  /**
   * Handles the start of a 'document' element.
   *
   * @param atts The attributes to handle.
   */
  private void startDocumentElement(Attributes atts) {
    this._document = new FlintDocument();
  }

  /**
   * Handles the end of a 'document' element.
   */
  private void endDocumentElement() {
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
    // required attributes
    this.field.name(atts.getValue("name"))
                .index(atts.getValue("index"));
    // handle compression
    if ("compress".equals(atts.getValue("store"))) {
      this._isCompressed = true;
      this.field.store(false);
    } else {
      this._isCompressed = false;
      this.field.store(atts.getValue("store"));
    }
    // Numeric type
    String numType = atts.getValue("numeric-type");
    if (numType != null) {
      this.field.numeric(numType).precisionStep(atts.getValue("precision-step"));
    }
    // Optional attributes
    this.field.termVector(atts.getValue("term-vector"))
                .termVectorPositions(atts.getValue("term-vector-positions"))
                .termVectorOffsets(atts.getValue("term-vector-offsets"))
                .termVectorPayloads(atts.getValue("term-vector-payloads"))
                .boost(atts.getValue("boost"))
                .tokenize(atts.getValue("tokenize"))
                .docValues(atts.getValue("doc-values"), numType != null);
    // Date handling
    this.field.dateFormat(atts.getValue("date-format"))
              .resolution(atts.getValue("date-resolution"));
    this._isField = true;
  }

  /**
   * Handles the end of a 'field' element.
   */
  private void endFieldElement() {
    try {
      // set the value
      this.field.value(this._value.toString());

      // compressed field
      if (this._isCompressed) {
        // Only include field if it is indexable
        if (this.field.index() != IndexOptions.NONE) {
          this._document.add(this.field);
        }
        // add compressed field
        this._document.add(this.field.cloneCompressed());

      // uncompressed field
      } else  {
        // doc values
        if (this.field.isDocValues()) {
          // add normal field as well
          this._document.add(this.field.cloneNoDocValues());
        }
        this._document.add(this.field);
      }

    } catch (IllegalStateException ex) {
      LOGGER.warn("Unable to create field: "+this.field.name(), ex);
    } catch (IllegalArgumentException ex) {
      LOGGER.warn("Unable to create field: "+this.field.name(), ex);
    }

    // Reset the class attributes involved in this field
    this.field = new FlintField(this._catalog);
    this._isField = false;
    this._isCompressed = false;
    this._value.setLength(0);
  }

}

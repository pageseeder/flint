package org.weborganic.flint.index;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * The handler for the Flint Index Documents format version 1.
 *
 * @see <a href="http://weborganic.org/code/flint/schema/index-documents-1.0.dtd">Index Documents 1.0 Schema</a>
 * 
 * @author Christophe Lauret
 * @version 2 March 2010
 */
final class IndexDocumentHandler_1_0 extends DefaultHandler implements IndexDocumentHandler {

  /**
   * Use the GMT time zone.
   */
  private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

  /**
   * The logger for this class.
   */
  private static final Logger LOGGER = Logger.getLogger(IndexDocumentHandler_1_0.class);

  // class attributes
  // -------------------------------------------------------------------------------------------

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
  private FieldBuilder builder = new FieldBuilder(); 

  /**
   * The characters found within a field.
   */
  private StringBuilder _value;

  // constructors
  // ----------------------------------------------------------------------------------------------

  /**
   * {@inheritDoc}
   */
  public List<Document> getDocuments() {
    return this.documents;
  }

  // SAX Methods
  // ----------------------------------------------------------------------------------------------

  /**
   * Receives notification of the beginning of the document.
   *
   * <p>Initialise this handler.
   */
  public void startDocument() {
    LOGGER.debug("Start processing iXML documents (version 1.0)");
    this.documents = new ArrayList<Document>();
  }

  /**
   * Receives notification of the end of the document.
   */
  public void endDocument() {
    LOGGER.debug("End processing iXML document");
  }

  /**
   * {@inheritDoc}
   */
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
   * <p>Prints those characters.
   *
   * <p>Replace the '\n' (newline character) by a space.
   *
   * @param ch     The characters
   * @param start  The start position in the character array.
   * @param length The number of characters to use from the character array.
   *
   * @throws SAXException Any SAX exception, possibly wrapping another exception.
   */
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
   * Handles the start of a 'document' element.
   * 
   * @param atts The attributes to handles.
   */
  private void startDocumentElement(Attributes atts) {
    LOGGER.debug("Parsing new index document");
    this._document = new Document();
//    this._document.add(new Field(IndexManager.CONTENT_ID_FIELD, this.contentID, Store.YES, Index.NOT_ANALYZED));
  }

  /**
   * Handles the end of a 'document' element.
   */
  private void endDocumentElement() {
    LOGGER.debug("Storing document");
    if (this._document.getFields().size() == 1) {
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
    this.builder.name(atts.getValue("name"));
    this.builder.index(toFieldIndex(atts.getValue("index")));
    // handle compression    
    if ("compress".equals(atts.getValue("store"))) {
      this._isCompressed = true;
      this.builder.store(Store.NO);
    } else {
      this._isCompressed = false;
      this.builder.store(atts.getValue("store"));
    }
    // Optional attributes
    this.builder.termVector(atts.getValue("term-vector"));
    this.builder.boost(atts.getValue("boost"));
    // Date handling
    this.builder.dateFormat(toDateFormat(atts.getValue("date-format")));
    this.builder.resolution(atts.getValue("date-resolution"));
    // Set attributes ready for recording content
    this._value   = new StringBuilder();
    this._isField = true;
  }

  /**
   * Handles the end of a 'field' element.
   */
  private void endFieldElement() {
    try {
      // construct the field
      this.builder.value(this._value.toString());
      Field field = this.builder.build();
      this._document.add(field);

      // compressed field
      if (this._isCompressed) {
        if (this.builder.index() == Index.NO) {
          this._document.add(this.builder.buildCompressed());
        } else {
          this._document.add(this.builder.build());
          this._document.add(this.builder.buildCompressed());
        }

      // uncompressed fields.
      } else  {
        this._document.add(this.builder.build());
      }

    } catch (IllegalStateException ex) {
      LOGGER.warn("Unable to create field: "+this.builder.name(), ex);
    } catch (IllegalArgumentException ex) {
      LOGGER.warn("Unable to create field: "+this.builder.name(), ex);
    }
    // reset the class attributes involved in this field
    resetField();
  }

  /**
   * Store the date format specified.
   *
   * <p>Set the current date format to <code>null<code> if the format is <code>null</code>.
   *
   * <p>Otherwise retrieve from hashtable or create an instance if it's
   * never been created.
   * 
   * @param format The date format used.
   */
  private DateFormat toDateFormat(String format) {
    if (format == null) return null;
    DateFormat df = this.dfs.get(format);
    // Date format not processed yet
    if (df == null) {
      try {
        df = new SimpleDateFormat(format);
        df.setTimeZone(GMT);
        this.dfs.put(format, df);
      } catch (Exception ex) {
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
    switch (c) {
      case '\n' : return ' ';
      case '\t' : return ' ';
      default: return c;
    }
  }

  /**
   * Reset the fields attributes.
   */
  private void resetField() {
    this._isField = false;
    this.builder.reset();
    this._value = null;
  }

  /**
   * Return the field index values handling legacy Lucene 2 values.
   * 
   * @param index The field index value.
   * @return the Lucene 3 field index values corresponding to the specified string.
   */
  private Field.Index toFieldIndex(String index) {
    if (index == null) return null;
    // Lucene 2 values
    if ("tokenised".equals(index)) return Field.Index.ANALYZED;
    if ("un-tokenised".equals(index)) return Field.Index.NOT_ANALYZED;
    if ("tokenized".equals(index)) return Field.Index.ANALYZED;
    if ("un-tokenized".equals(index)) return Field.Index.NOT_ANALYZED;
    if ("no-norms".equals(index)) return Field.Index.NOT_ANALYZED_NO_NORMS;
    // Accept Lucene 3 values
    return FieldBuilder.toFieldIndex(index);
  }

}
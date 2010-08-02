/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.weborganic.flint.IndexException;
import org.weborganic.flint.util.FlintEntityResolver;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This handler makes a Lucene 2 Document out of a properly formatted XML document.
 *
 * <p>The XML document must validate the Lucene Index Document DTD.
 *
 * <p>For example:
 *
 * <pre>{@code
 * <document>
 *   <field name="modified" store="yes" index="yes" parse-date-as="MMM dd, yy" resolution="day">Jan 12, 02</field>
 *   <field name="path"     store="yes" index="no" >C:\documents\00023.xml</field>
 *   <field name="webpath"  store="yes" index="no" >/documents/doc-23.xml</field>
 *   <field name="text" store="compress" index="tokenised" >
 *     Truly to speak, and with no addition, 
 *     We go to gain a little patch of ground 
 *     That hath in it no profit but the name. 
 *     To pay five ducats, five, I would not farm it; 
 *     Nor will it yield to Norway or the Pole 
 *     A ranker rate, should it be sold in fee.
 *   </field>
 * </document>
 * }</pre>
 * 
 * @see <a href="http://www.weborganic.org/code/flint/schema/index-documents-1.0.dtd">Index Documents 1.0 Schema</a>
 * @see <a href="http://www.weborganic.org/code/flint/schema/index-documents-2.0.dtd">Index Documents 2.0 Schema</a>
 *
 * @author Christophe Lauret (Weborganic)
 *
 * @version 1 March 2010
 */
public final class IndexParser {

  /**
   * THe XML reader to use.
   */
  private final XMLReader _reader;

  /**
   * Creates a new IndexParser.
   *
   * @param reader    The XML reader to use.
   */
  protected IndexParser(XMLReader reader) {
    this._reader = reader;
    this._reader.setEntityResolver(FlintEntityResolver.getInstance());
    this._reader.setErrorHandler(new FlintErrorHandler());
  }

// public static methods -----------------------------------------------------------------------

  /**
   * Returns the Lucene2 Field Store from the attribute value.
   * 
   * @param store The store flag as a string. 
   * 
   * @return The corresponding Lucene2 constant.
   */
  public static Field.Store toFieldStore(String store) {
    return FieldBuilder.toFieldStore(store);
  }

  /**
   * Returns the Lucene2 Field Index from the attribute value.
   * 
   * @param index The index flag as a string. 
   * 
   * @return The corresponding Lucene2 constant. 
   */
  public static Field.Index toFieldIndex(String index) {
    return FieldBuilder.toFieldIndex(index);
  }

  /**
   * Make a collection Lucene documents to be indexed from the XML file given.
   *
   * <p>The XML file must conform to the DTD defined in this class.
   * 
   * <p>Ensure that the reader uses the correct encoding.
   *
   * @param source The source to read.
   *
   * @return A collection of Lucene documents made from the file.
   *
   * @throws IndexException Should an error occur while parsing the file.
   */
  public synchronized List<Document> process(InputSource source) throws IndexException {
    try {
      IndexDocumentHandler handler = new AutoHandler(this._reader);
      this._reader.setContentHandler(handler);
      this._reader.parse(source);
      return handler.getDocuments();
    } catch (SAXException ex) {
      throw new IndexException("An SAX error occurred while parsing source "+source.getSystemId()+": "+ex.getMessage(), ex);
    } catch (IOException ex) {
      ex.printStackTrace();
      throw new IndexException("An I/O error occurred while parsing the file "+source.getSystemId()+": "+ex.getMessage(), ex);
    }
  }

  /**
   * Returns a list of Lucene documents to be indexed from the XML file given.
   *
   * <p>The XML file must conform to the DTD defined in this class.
   *
   * @see #make(java.io.Reader)
   *
   * @param f the file to be read
   *
   * @return A collection of Lucene documents made from the file.
   *
   * @throws IndexException Should an error occur while parsing the file.
   */
  public synchronized List<Document> process(File f) throws IndexException {
    try {
      InputSource source = new InputSource(new InputStreamReader(new FileInputStream(f), "utf-8"));
      source.setSystemId(f.toURI().toURL().toExternalForm());
      return process(source);
    } catch (IOException ex) {
      throw new IndexException("I/O error occurred while generating file input source: "+ex.getMessage(), ex);
    }
  }

  // Inner class to determine which handler to use --------------------------------------------------

  /**
   * A content handler to determine the version used.
   * 
   * @author Christophe Lauret
   * @version 1 March 2010
   */
  private static final class AutoHandler extends DefaultHandler implements IndexDocumentHandler {

    /**
     * The reader in use.
     */
    private final XMLReader _reader;

    /**
     * The handler in use.
     */
    private IndexDocumentHandler _handler;

    /**
     * Create a new auto handler for the specified XML reader.
     * 
     * @param reader   The XML Reader in use.
     */
    public AutoHandler(XMLReader reader) {
      this._reader = reader;
    }

    /**
     * Once element "documents" is matched, the reader is assigned the appropriate handler.
     * 
     * {@inheritDoc}
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
      if ("documents".equals(qName) || "documents".equals(localName)) {
        String version = atts.getValue("version");
        // Version 2.0
        if ("2.0".equals(version)) {
          this._handler = new IndexDocumentHandler_2_0();
        // Assume version 1.0
        } else {
          this._handler = new IndexDocumentHandler_1_0();
        }
        // Start processing the document with the new handler
        this._handler.startDocument();
        this._handler.startElement(uri, localName, qName, atts);
        // Reassign the content handler
        this._reader.setContentHandler(this._handler);
      } else if ("root".equals(qName) || "root".equals(localName)) {
        this._handler = new IndexDocumentHandlerCompatibility();
        // Start processing the document with the new handler
        this._handler.startDocument();
        this._handler.startElement(uri, localName, qName, atts);
        // Reassign the content handler
        this._reader.setContentHandler(this._handler);
      }
    }

    /**
     * {@inheritDoc}
     */
    public List<Document> getDocuments() {
      if (this._handler == null) return Collections.emptyList();
      return this._handler.getDocuments();
    }
  }

}

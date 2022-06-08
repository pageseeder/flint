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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

import javax.xml.transform.sax.SAXResult;

import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.indexing.FlintDocument;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This handler makes a Lucene 5 Document out of a properly formatted XML document.
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
 * @see <a href="http://www.weborganic.org/code/flint/schema/index-documents-5.0.dtd">Index Documents 5.0 Schema</a>
 *
 * @author Christophe Lauret
 * @author Jean-Baptiste Reure
 *
 * @version 1 September 2015
 */
public final class IndexParser {

  /**
   * THe XML reader to use.
   */
  private final XMLReader _reader;

  /**
   * THe XML reader to use.
   */
  private final SAXResult _result;

  /**
   * THe XML reader to use.
   */
  private final IndexDocumentHandler _handler;

  /**
   * Creates a new IndexParser.
   *
   * @param reader    The XML reader to use.
   */
  protected IndexParser(XMLReader reader) {
    this._reader = reader;
    this._reader.setEntityResolver(FlintEntityResolver.getInstance());
    this._reader.setErrorHandler(new FlintErrorHandler());
    this._result = null;
    this._handler = null;
  }

  /**
   * Creates a new IndexParser.
   *
   * @param catalog  The..
   */
  protected IndexParser(String catalog) {
    this._reader = null;
    this._handler = new AutoHandler(catalog);
    this._result = new SAXResult(this._handler);
  }

  //public methods -----------------------------------------------------------------------

  public SAXResult getResult() {
    return this._result;
  }

  public List<FlintDocument> getDocuments() {
    return this._handler == null ? null : this._handler.getDocuments();
  }

  /**
   * Make a collection Lucene documents to be indexed from the XML file given.
   *
   * <p>The XML file must conform to the DTD defined in this class.
   *
   * <p>Ensure that the reader uses the correct encoding.
   *
   * @param source  The source to read.
   * @param catalog The catalog to add the fields to.
   *
   * @return A collection of Lucene documents made from the file.
   *
   * @throws IndexException Should an error occur while parsing the file.
   */
  public synchronized List<FlintDocument> process(InputSource source, String catalog) throws IndexException {
    try {
      IndexDocumentHandler handler = new AutoHandler(catalog);
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
   * @param f       The file to be read.
   * @param catalog The catalog to add the fields to.
   *
   * @return A collection of Lucene documents made from the file.
   *
   * @throws IndexException Should an error occur while parsing the file.
   */
  public synchronized List<FlintDocument> process(File f, String catalog) throws IndexException {
    try {
      InputSource source = new InputSource(new InputStreamReader(new FileInputStream(f), "utf-8"));
      source.setSystemId(f.toURI().toURL().toExternalForm());
      return process(source, catalog);
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
     * The catalog to use.
     */
    private final String _catalog;

    /**
     * The handler in use.
     */
    private IndexDocumentHandler _handler;

    /**
     * @param catalog the catalog to associate the fields with
     */
    public AutoHandler(String catalog) {
      this._catalog = catalog;
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
        // Version 5.0
        if ("5.0".equals(version)) {
          this._handler = new IndexDocumentHandler_5_0(this._catalog);
        // Version 2.0
        } else if ("2.0".equals(version)) {
          this._handler = new IndexDocumentHandler_2_0();
        // Assume version 1.0
        } else {
          throw new SAXException("Unsupported iXML version "+version+", only 2.0 and 5.0 are supported");
        }
        // Start processing the document with the new handler
        this._handler.startDocument();
      }
      if (this._handler != null) {
        this._handler.startElement(uri, localName, qName, atts);
      }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
      if (this._handler != null)
        this._handler.endElement(uri, localName, qName);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
      if (this._handler != null)
        this._handler.characters(ch, start, length);
    }

    @Override
    public void endDocument() throws SAXException {
      if (this._handler != null)
        this._handler.endDocument();
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public List<FlintDocument> getDocuments() {
      if (this._handler == null) return Collections.emptyList();
      return this._handler.getDocuments();
    }
  }

}

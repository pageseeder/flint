package org.weborganic.flint.index;

import java.util.List;

import org.apache.lucene.document.Document;
import org.xml.sax.ContentHandler;

/**
 * All documents handler must be able to return a list of documents after processing.
 * 
 * @author Christophe Lauret
 * @version 1 March 2010
 */
interface IndexDocumentHandler extends ContentHandler {

  /**
   * Return the list of documents which were produced. 
   * 
   * @return the list of documents which were produced. 
   */
  List<Document> getDocuments();

}

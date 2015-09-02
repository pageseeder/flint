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
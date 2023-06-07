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
package org.pageseeder.flint.lucene.search;
import java.io.IOException;
import java.util.Collections;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;


/**
 * Simply counts the number of documents in search results.
 *
 * <p>Typical usage:
 * <pre>
 *  // creates a document counter
 *  DocumentCounter counter = new DocumentCounter();
 *
 *  // make a search
 *  searcher.search(query, counter);
 *
 *  // get the final count
 *  int numberOfDocuments = counter.getCount();
 * </pre>
 *
 * @author Christophe Lauret
 * @version 2 August 2010
 */
public final class FieldDocumentCounter extends SimpleCollector {

  /**
   * The number of documents collected (counted).
   */
  private int count = 0;

  private final String field;

  private LeafReaderContext context = null;

  /**
   * Creates a new document counter.
   */
  public FieldDocumentCounter(String fieldname) {
    this.field = fieldname;
  }

  /**
   * Accept documents out of order - the order is irrelevant when counting.
   * @return always <code>false</code>.
   */
  @Override
  public ScoreMode scoreMode() {
    return ScoreMode.COMPLETE_NO_SCORES;
  }

  @Override
  protected void doSetNextReader(LeafReaderContext ctxt) {
    this.context = ctxt;
  }

  /**
   * Increase the document count.
   *
   * @param doc the position of the Lucene {@link Document} in the index
   */
  @Override
  public void collect(int doc) {
    if (this.context != null) {
      Document d;
      try (LeafReader reader = this.context.reader()) {
        d = reader.storedFields().document(doc, Collections.singleton(this.field));
      } catch (IOException ex) {
        return;
      }
      if (d != null && d.getField(this.field) != null)
        this.count++;
    }
  }

  /**
   * Returns the number of documents counted after a search.
   *
   * @return the  number of documents counted after a search.
   */
  public int getCount() {
    return this.count;
  }

  /**
   * Resets this document counter for reuse by resetting the count to zero.
   */
  public void reset() {
    this.count = 0;
  }
}

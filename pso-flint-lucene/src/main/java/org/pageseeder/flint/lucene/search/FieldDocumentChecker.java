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
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.SimpleCollector;


/**
 * Simply checks if there is at least one document in the search results with the field specified.
 *
 * @author Jean-Baptiste Reure
 * @version 31 July 2017
 */
public final class FieldDocumentChecker extends SimpleCollector {

  /**
   * The flag.
   */
  private boolean fieldFound = false;

  private final String field;

  private LeafReaderContext context = null;

  /**
   * Creates a new document counter.
   */
  public FieldDocumentChecker(String fieldname) {
    this.field = fieldname;
  }

  /**
   * Accept documents out of order - the order is irrelevant when counting.
   * @return always <code>false</code>.
   */
  @Override
  public boolean needsScores() {
    return false;
  }

  @Override
  protected void doSetNextReader(LeafReaderContext ctxt) throws IOException {
    this.context = ctxt;
  }

  /**
   * Check for the field's presence.
   *
   * @param doc the position of the Lucene {@link Document} in the index
   */
  @Override
  public void collect(int doc) {
    if (this.context != null && !this.fieldFound) {
      Document d;
      try {
        d = this.context.reader().document(doc, Collections.singleton(this.field));
      } catch (IOException ex) {
        return;
      }
      if (d != null && d.getField(this.field) != null)
        this.fieldFound = true;
    }
  }

  /**
   * Returns <code>true</code> if there was at least one document with the field specified.
   *
   * @return <code>true</code> if there was at least one document with the field specified.
   */
  public boolean fieldFound() {
    return this.fieldFound;
  }

}

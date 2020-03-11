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
package org.pageseeder.flint.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.pageseeder.flint.Index;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexIO;

import java.io.File;
import java.io.IOException;

/**
 * Provides the details needed to build the data to index from the original content.
 *
 * <p>The path to a valid XSLT script is needed and parameters can be provided as well.
 *
 * <p>The XSLT script should produce valid IndexXML format (see DTD).
 *
 * @author Jean-Baptiste Reure
 * @version 26 February 2010
 */
public class LuceneIndex extends Index {
  
  private final Directory _directory;

  private final Analyzer _analyzer;

  private final IndexIO _io;

  public LuceneIndex(String id, File dir, Analyzer analyzer) throws IOException, IndexException {
    this(id, FSDirectory.open(dir.toPath()), analyzer);
  }

  public LuceneIndex(String id, Directory dir, Analyzer analyzer) throws IndexException {
    super(id);
    this._directory = dir;
    this._analyzer = analyzer;
    this._io = new LuceneIndexIO(this._directory, this._analyzer);
  }

  public Analyzer getAnalyzer() {
    return this._analyzer;
  }

  /**
   * Return the Index Directory object.
   *
   * @return The Index Directory object
   */
  public final Directory getIndexDirectory() {
    return this._directory;
  }

  @Override
  public IndexIO getIndexIO() {
    return this._io;
  }
}

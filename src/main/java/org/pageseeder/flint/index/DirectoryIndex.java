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

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.api.Index;

/**
 * Implementation of Index, uses a File object to build a Lucene index.
 *
 * @author Jean-Baptiste Reure
 * @version 27 February 2013
 */
public class DirectoryIndex implements Index {

  /**
   * The Index Directory object.
   */
  private final Directory indexDirectory;

  /**
   * The analyser used in this default index.
   */
  private static final StandardAnalyzer ANALYSER = new StandardAnalyzer(IndexManager.LUCENE_VERSION);

  /**
   * The unique ID.
   */
  private final String id;

  /**
   * Simple constructor that builds a new Index from the provided directory.
   *
   * <p>Will throw an exception if the folder does not contain a valid Lucene index</p>
   *
   * @param dir the directory containing the Lucene index.
   * @throws IllegalArgumentException is if the specified file cannot be used as an index.
   */
  public DirectoryIndex(File dir) throws IllegalArgumentException {
    try {
      this.indexDirectory = FSDirectory.open(dir);
    } catch (IOException ex) {
      throw new IllegalArgumentException("Unable to use directory " + dir.getAbsolutePath() + " as an index", ex);
    }
    this.id = dir.getAbsolutePath();
  }

  /**
   * Return the Directory used in this Index.
   *
   * @return the directory to use for the Index.
   */
  @Override
  public final Directory getIndexDirectory() {
    return this.indexDirectory;
  }

  /**
   * The Analyzer that the Index should use.
   *
   * @return the Analyzer that the Index should use.
   */
  @Override
  public Analyzer getAnalyzer() {
    return ANALYSER;
  }

  /**
   * This Index's unique ID.
   *
   * @return the unique ID for the Index.
   */
  @Override
  public String getIndexID() {
    return this.id;
  }

}

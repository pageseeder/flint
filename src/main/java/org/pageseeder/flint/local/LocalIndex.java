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
package org.pageseeder.flint.local;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.pageseeder.flint.api.Index;
import org.pageseeder.flint.api.Index.ParametersBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A basic implementation of a local index.
 *
 * @author Christophe Lauret
 * @version 27 February 2013
 */
public final class LocalIndex {

  /**
   * A logger for this class and to provide for Flint.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(LocalIndex.class);

  /**
   * The location of the index.
   */
  private final File _location;

  /**
   * This index.
   */
  private final Index _index;

  /**
   * Create a new local index.
   *
   * @param location The location of the local index.
   * @param analyzer The analyzer of the local index.
   *
   * @throws NullPointerException if the location is <code>null</code>.
   */
  public LocalIndex(File location) {
    this(location, new StandardAnalyzer());
  }

  /**
   * Create a new local index.
   *
   * @param location The location of the local index.
   * @param analyzer The analyzer of the local index.
   *
   * @throws NullPointerException if the location is <code>null</code>.
   */
  public LocalIndex(File location, Analyzer analyzer) {
    if (location == null) throw new NullPointerException("location");
    this._location = location;
    ensureFolderExists(this._location);
    Directory dir;
    try {
      dir = FSDirectory.open(this._location.toPath());
    } catch (IOException ex) {
      throw new IllegalArgumentException("Unable to return a directory on local index "+this._location.getName(), ex);
    }
    this._index = new Index(this._location.getName(), dir, analyzer);
  }

  public void setParameterBuilder(ParametersBuilder builder) {
    this._index.setParametersBuilder(builder);
  }

  public Index getIndex() {
    return this._index;
  }

  // Utility methods for public usage
  // ----------------------------------------------------------------------------------------------

  /**
   * Indicates whether the folder exists and is a valid Lucene index.
   *
   * <p>This method uses the Lucene {@link IndexReader} to check whether the folder corresponds to a valid Lucene Index.
   *
   * @param location the folder where the index is located.
   * @return <code>true</code> if the folder exists and is a Lucene index; <code>false</code> otherwise
   */
  public static boolean exists(File location) {
    if (!location.exists() || !location.isDirectory()) return false;
    boolean exists = false;
    // Get the last modified from the index
    try {
      Directory directory = FSDirectory.open(location.toPath());
      exists = DirectoryReader.indexExists(directory);
      directory.close();
    } catch (IOException ex) {
      LOGGER.error("Unable to retrieve the last modified date of local index", ex);
    }
    return exists;
  }

  // private helpers
  // ----------------------------------------------------------------------------------------------

  /**
   * Ensures that the specified folder exists by creating the folder if it does not.
   *
   * <p>This method will log any creation problem as a warning.
   *
   * @param folder The folder to created.
   */
  private static void ensureFolderExists(File folder) {
    if (!folder.exists()) {
      boolean created = folder.mkdirs();
      if (!created) {
        LOGGER.warn("Unable to create location {}", folder);
      }
    }
  }

}

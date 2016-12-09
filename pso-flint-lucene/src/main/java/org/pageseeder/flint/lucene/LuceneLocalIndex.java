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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexIO;
import org.pageseeder.flint.content.DeleteRule;
import org.pageseeder.flint.indexing.FlintField;
import org.pageseeder.flint.local.LocalIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A basic implementation of a local index.
 *
 * @author Christophe Lauret
 * @version 27 February 2013
 */
public final class LuceneLocalIndex extends LocalIndex {

  /**
   * A logger for this class and to provide for Flint.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(LuceneLocalIndex.class);
  
  private final Directory _directory;

  private final Analyzer _analyzer;

  private final IndexIO _io;

  private final File _contentRoot;

  /**
   * Create a new local index.
   *
   * @param location The location of the local index.
   * @param analyzer The analyzer of the local index.
   *
   * @throws NullPointerException if the location is <code>null</code>.
   */
  public LuceneLocalIndex(File indexLocation, Analyzer analyzer, File contentLocation) {
    this(indexLocation, "lucene", analyzer, contentLocation);
  }

  /**
   * Create a new local index.
   *
   * @param location The location of the local index.
   * @param analyzer The analyzer of the local index.
   *
   * @throws NullPointerException if the location is <code>null</code>.
   */
  public LuceneLocalIndex(File indexLocation, String catalog, Analyzer analyzer, File contentLocation) {
    super(indexLocation.getName(), catalog);
    this._directory = ensureFolderExists(indexLocation);
    this._analyzer = analyzer;
    IndexIO io;
    try {
      io = new LuceneIndexIO(this._directory, this._analyzer);
    } catch (IndexException ex) {
      ex.printStackTrace();
      io = null;
    }
    this._io = io;
    this._contentRoot = contentLocation;
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

  @Override
  public DeleteRule getDeleteRule(File file) {
    return new LuceneDeleteRule("_src", file.getAbsolutePath());
  }

  @Override
  public File getContentLocation() {
    return this._contentRoot;
  }

  @Override
  public Collection<FlintField> getFields(File file) {
    Collection<FlintField> fields = new ArrayList<>();
    if (file.exists()) {
      fields.add(buildField("_src", file.getAbsolutePath()));
      fields.add(buildField("_path", fileToPath(file)));
      fields.add(buildField("_lastmodified", String.valueOf(file.lastModified())));
      fields.add(buildField("_creator", "flint-lucene"));
    }
    return fields;
  }

  @Override
  public Map<String, String> getParameters(File file) {
    HashMap<String, String> params = new HashMap<>();
    if (file.exists()) {
      params.put("_src", file.getAbsolutePath());
      params.put("_path", fileToPath(file));
      params.put("_lastmodified", String.valueOf(file.lastModified()));
      params.put("_filename", file.getName());
    }
    return params;
  }

  public String fileToPath(File f) {
    try {
      String rootPath = this._contentRoot.getCanonicalPath();
      String thisPath = f.getCanonicalPath();
      if (thisPath.startsWith(rootPath))
        return '/' + thisPath.substring(rootPath.length()).replace('\\', '/');
    } catch (IOException ex) {
      LOGGER.error("Failed to compute file relative path", ex);
    }
    return f.getAbsolutePath();
  }
  
  public File pathToFile(String path) {
    return new File(this._contentRoot, path);
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
      LOGGER.error("Unable to check if local index exists", ex);
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
  private static Directory ensureFolderExists(File folder) {
    if (!folder.exists()) {
      folder.mkdirs();
      if (!folder.exists()) {
        LOGGER.warn("Unable to create location {}", folder);
      }
    }
    try {
      return FSDirectory.open(folder.toPath());
    } catch (IOException ex) {
      throw new IllegalArgumentException("Unable to return a directory on local index "+folder.getName(), ex);
    }
  }

  private FlintField buildField(String name, String value) {
    // use filed builder as it will add the fields to the catalog
    return new FlintField(getCatalog()).store(true).tokenize(false).name(name).value(value);
  }

}

/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.local;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.flint.api.Index;

/**
 * A basic implementation of a local index.
 *
 * @author Christophe Lauret
 * @version 27 February 2013
 */
public final class LocalIndex implements Index {

  /**
   * A logger for this class and to provide for Flint.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(LocalIndex.class);

  /**
   * The location of the index.
   */
  private final File _location;

  /**
   * This index's analyser.
   */
  private final Analyzer _analyzer;

  /**
   * This index's directory.
   */
  private volatile Directory _directory;

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
    this._analyzer = analyzer;
  }

  @Override
  public String getIndexID() {
    return this._location.getName();
  }

  @Override
  public synchronized Directory getIndexDirectory() {
    ensureFolderExists(this._location);
    ensureHasDirectory();
    return this._directory;
  }

  /**
   * Returns when this index was last modified.
   *
   * @return When this index was last modified.
   */
  public synchronized long getLastModified() {
    if (!this._location.exists()) return -1;
    ensureHasDirectory();
    long modified = 0;
    try {
      if (IndexReader.indexExists(this._directory))
        modified = IndexReader.lastModified(this._directory);
    } catch (IOException ex) {
      LOGGER.error("Unable to retrieve the last modified date of local index", ex);
    }
    return modified;
  }

  @Override
  public Analyzer getAnalyzer() {
    return this._analyzer;
  }

  @Override
  public String toString() {
    return this.getIndexID();
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
      Directory directory = FSDirectory.open(location);
      exists = IndexReader.indexExists(directory);
      directory.close();
    } catch (IOException ex) {
      LOGGER.error("Unable to retrieve the last modified date of local index", ex);
    }
    return exists;
  }

  /**
   * Returns when an index was last modified.
   *
   * <p>This method uses the Lucene {@link IndexReader} to check whether the folder corresponds to a valid
   * Lucene Index and to read the last modified date stamp from the segments.
   *
   * @param location the folder where the index is located.
   * @return When this index was last modified or -1 if the folder or index does not exist.
   */
  public static long getLastModified(File location) {
    if (!location.exists() || !location.isDirectory()) return -1;
    long modified = 0;
    // Get the last modified from the index
    try {
      Directory directory = FSDirectory.open(location);
      if (IndexReader.indexExists(directory))
        modified = IndexReader.lastModified(directory);
      directory.close();
    } catch (IOException ex) {
      LOGGER.error("Unable to retrieve the last modified date of local index", ex);
    }
    return modified;
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
  private void ensureHasDirectory() {
    try {
      if (this._directory == null)
        this._directory = FSDirectory.open(this._location);
    } catch (IOException ex) {
      LOGGER.error("Unable to return a directory on local index", ex);
    }
  }

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

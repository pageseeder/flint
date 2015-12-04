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
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Map;

import org.pageseeder.flint.IndexJob;
import org.pageseeder.flint.IndexJob.Priority;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.api.ContentType;
import org.pageseeder.flint.api.Requester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A basic implementation of a local index.
 *
 * @author Christophe Lauret
 * @version 27 February 2013
 */
public final class LocalIndexer extends Requester {

  /**
   * A logger for this class and to provide for Flint.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(LocalIndexer.class);
  
  private final IndexManager _manager;

  private final LocalIndex _index;

  private Priority priority = Priority.LOW;
  /**
   * Create a new local index.
   *
   * @param location The location of the local index.
   * @param analyzer The analyzer of the local index.
   *
   * @throws NullPointerException if the location is <code>null</code>.
   */
  public LocalIndexer(IndexManager manager, LocalIndex index) {
    super("Local indexer");
    this._manager = manager;
    this._index = index;
  }

  public void setHighPriority() {
    this.priority = Priority.HIGH;
  }

  public void setLowPriority() {
    this.priority = Priority.LOW;
  }

  public void index(File file) {
    if (file.isFile())
      indexFile(file);
    else if (file.isDirectory())
      indexFolder(file);
  }

  public void indexFolder(File root) {
    indexFolder(root, null);
  }

  public void indexFolder(File root, boolean recursive) {
    indexFolder(root, recursive, null);
  }

  public void indexFolder(File root, FileFilter filter) {
    indexFolder(root, true, filter);
  }

  public int indexFolder(File root, boolean recursive, FileFilter filter) {
    if (root.isDirectory()) {
      // find documents to index
      Map<String, ContentType> files = new HashMap<>();
      collectDocuments(root, recursive, filter, files);
      if (files.isEmpty()) {
        LOGGER.warn("Nothing to index!");
      } else {
        this._manager.indexBatch(files, this._index.getIndex(), this, this.priority);
      }
      return files.size();
    }
    LOGGER.warn("Trying to index file {} as a folder", root.getAbsolutePath());
    return 0;
  }
  
  public void indexFile(File file) {
    if (file.isFile()) {
      this._manager.index(file.getAbsolutePath(), LocalFileContentType.SINGLETON, this._index.getIndex(), this, IndexJob.Priority.HIGH);
    } else {
      LOGGER.warn("Trying to index folder {} as a file", file.getAbsolutePath());
    }
  }

  public void clear() {
    this._manager.clear(this._index.getIndex(), this, Priority.HIGH);
  }

  public File getContentRoot() {
    return this._index.getContentRoot();
  }

  @Override
  public Map<String, String> getParameters(String contentid, ContentType type) {
    HashMap<String, String> params = new HashMap<>();
    File f = new File(contentid);
    if (f.exists() && type == LocalFileContentType.SINGLETON) {
      String path = f.getAbsolutePath();
      String root = this._index.getContentRoot().getAbsolutePath();
      if (path.startsWith(root)) path = path.substring(root.length());
      params.put("_path", path.replace('\\', '/'));
      params.put("_filename", f.getName());
      params.put("_visibility", "private");
      params.put("_lastmodified", String.valueOf(f.lastModified()));
    }
    return params;
  }
  
  // -----------------------------------------------------------------------------------
  // private helpers
  private void collectDocuments(File root, boolean recursive, FileFilter filter, Map<String, ContentType> files) {
    // validate params
    if (root == null) throw new NullPointerException("root");
    if (!root.exists()) return;
    if (!root.isDirectory()) throw new IllegalArgumentException("Not a folder");
    // load children
    File[] children = filter == null ? root.listFiles() : root.listFiles(filter);
    // make sure there are children
    if (children == null) {
      LOGGER.warn("Cannot read files in folder "+root.getAbsolutePath());
      return;
    }
    // loop through them
    for (File child : children) {
      // handle folders
      if (child.isDirectory()) {
        if (recursive) collectDocuments(child, recursive, filter, files);
      } else {
        // add file
        files.put(child.getAbsolutePath(), LocalFileContentType.SINGLETON);
      }
    }
  }

}

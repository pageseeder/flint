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
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
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
public final class LocalIndexer implements FileVisitor<Path> {

  /**
   * A logger for this class and to provide for Flint.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(LocalIndexer.class);

  /**
   * Possible action for each file.
   */
  public enum Action { INSERT, UPDATE, DELETE, IGNORE };

  private final IndexManager _manager;

  private final LocalIndex _index;
  
  private long _indexModifiedDate = -1;

  private Map<File, Long> indexedFiles = null;

  private FileFilter filter = null;

  private final Map<File, Action> batchFiles = new HashMap<>();

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
      indexFolder(file, null);
  }

  public void indexFolder(File root, Map<File, Long> indexed) {
    indexFolder(root, null, indexed);
  }

  public int indexFolder(File root, FileFilter filter, Map<File, Long> indexed) {
    if (root == null) throw new NullPointerException("root");
    if (!root.exists()) return 0;
    if (root.isDirectory()) {
      // get last modif date of index
      this._indexModifiedDate = indexed == null || indexed.isEmpty() ? -1 : this._manager.getLastTimeUsed(this._index);
      // find documents already in the index
      this.indexedFiles = indexed;
      this.filter = filter;
      // find documents to index
      try {
        Files.walkFileTree(root.toPath(), this);
      } catch (IOException ex) {
        LOGGER.warn("Failed to collect files to index from folder {}", root, ex);
      }
      if (this.batchFiles.isEmpty()) {
        LOGGER.warn("Nothing to index!");
      } else {
        // get files
        Map<String, ContentType> toindex = new HashMap<>();
        for (File f : this.batchFiles.keySet()) {
          toindex.put(f.getAbsolutePath(), LocalFileContentType.SINGLETON);
        }
        // files to delete
        if (this.indexedFiles != null) {
          for (File f : this.indexedFiles.keySet()) {
            toindex.put(f.getAbsolutePath(), LocalFileContentType.SINGLETON);
          }
        }
        this._manager.indexBatch(toindex, this._index, new Requester("Local Indexer"), this.priority);
      }
      return this.batchFiles.size();
    }
    LOGGER.warn("Trying to index file {} as a folder", root.getAbsolutePath());
    return 0;
  }
  
  public void indexFile(File file) {
    if (file.isFile()) {
      this._manager.index(file.getAbsolutePath(), LocalFileContentType.SINGLETON, this._index, new Requester("Local Indexer"), IndexJob.Priority.HIGH);
    } else {
      LOGGER.warn("Trying to index folder {} as a file", file.getAbsolutePath());
    }
  }

  public void clear() {
    this._manager.clear(this._index, new Requester("Local Indexer"), Priority.HIGH);
  }

  public File getContentRoot() {
    return this._index.getConfig().getContent();
  }
  
  // -----------------------------------------------------------------------------------
  // private helpers

  @Override
  public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
    File file = path.toFile();
    Long indexModified = this.indexedFiles == null ? null : this.indexedFiles.remove(file);
    // only files updated since last commit
    if (this._indexModifiedDate == -1 || attrs.lastModifiedTime().toMillis() > this._indexModifiedDate) {
      // check for filter
      if (this.filter != null && !this.filter.accept(file))
        return FileVisitResult.CONTINUE;
      // check in the index to know what action to perform
      if (indexModified == null) {
        this.batchFiles.put(file, Action.INSERT);
      } else if (indexModified != file.lastModified()) {
        this.batchFiles.put(file, Action.UPDATE);
      }
    }
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
    LOGGER.error("Failed to collect document {}", file, exc);
    return FileVisitResult.CONTINUE;
  }
}

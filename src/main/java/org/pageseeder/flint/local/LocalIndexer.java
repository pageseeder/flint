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

import org.pageseeder.flint.IndexBatch;
import org.pageseeder.flint.IndexJob;
import org.pageseeder.flint.IndexJob.Priority;
import org.pageseeder.flint.IndexManager;
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

  private IndexBatch batch = null;

  private final Requester _requester = new Requester("Local Indexer");

  private final Map<File, Action> resultFiles = new HashMap<>();

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

  public void setFileFilter(FileFilter filter) {
    this.filter = filter;
  }

  public int indexFolder(File root, Map<File, Long> indexed) {
    if (root == null) throw new NullPointerException("root");
    if (!root.exists()) return 0;
    if (root.isDirectory()) {
      // get last modif date of index
      this._indexModifiedDate = indexed == null || indexed.isEmpty() ? -1 : this._manager.getLastTimeUsed(this._index);
      // create batch object
      this.batch = new IndexBatch(this._index.getIndexID());
      // find documents to modify/add to index
      this.indexedFiles = indexed;
      try {
        Files.walkFileTree(root.toPath(), this);
      } catch (IOException ex) {
        LOGGER.warn("Failed to collect files to index from folder {}", root, ex);
      }
      // get files to remove
      if (this.indexedFiles != null) {
        for (File f : this.indexedFiles.keySet()) {
          this.resultFiles.put(f, Action.DELETE);
          this.batch.increaseTotal();
          this._manager.indexBatch(this.batch, f.getAbsolutePath(), LocalFileContentType.SINGLETON, this._index, this._requester, this.priority);
        }
      }
      this.batch.setComputed();
      return this.resultFiles.size();
    }
    LOGGER.warn("Trying to index file {} as a folder", root.getAbsolutePath());
    return 0;
  }

  public IndexBatch getBatch() {
    return this.batch;
  }

  public Map<File, Action> getIndexedFiles() {
    return this.resultFiles;
  }

  // -----------------------------------------------------------------------------------
  // File walking methods

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
        this.resultFiles.put(file, Action.INSERT);
      } else if (indexModified != file.lastModified()) {
        this.resultFiles.put(file, Action.UPDATE);
      }
      // index
      this.batch.increaseTotal();
      this._manager.indexBatch(this.batch, file.getAbsolutePath(), LocalFileContentType.SINGLETON, this._index, this._requester, IndexJob.Priority.HIGH);
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

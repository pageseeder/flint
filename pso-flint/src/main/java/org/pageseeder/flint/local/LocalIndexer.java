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
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.pageseeder.flint.Index;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.Requester;
import org.pageseeder.flint.indexing.IndexBatch;
import org.pageseeder.flint.indexing.IndexJob;
import org.pageseeder.flint.indexing.IndexJob.Priority;
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
  public enum Action { INSERT, UPDATE, DELETE, IGNORE }

  private final IndexManager _manager;

  private final Index _index;

  private long _indexModifiedDate = -1;

  private Map<String, Long> indexedFiles = null;

  private FileFilter fileFilter = null;

  private FileFilter directoryFilter = null;

  private IndexBatch batch = null;

  private boolean useIndexDate = true;

  private final Requester _requester = new Requester("Local Indexer");

  private final Map<String, Action> resultFiles = new ConcurrentHashMap<>();

  private Priority priority = Priority.LOW;
  /**
   * Create a new local index.
   *
   * @param index The ...
   * @param manager The ...
   *
   * @throws NullPointerException if the location is <code>null</code>.
   */
  public LocalIndexer(IndexManager manager, Index index) {
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
    this.fileFilter = filter;
  }

  public void setDirectoryFilter(FileFilter directoryFilter) {
    this.directoryFilter = directoryFilter;
  }

  /**
   * If the index last modified date is used to select which files to index
   * @param useIndxDate whether to use the index date or not
   */
  public void setUseIndexDate(boolean useIndxDate) {
    this.useIndexDate = useIndxDate;
  }

  public int indexFolder(File root, Map<String, Long> indexed) {
    if (root == null) throw new NullPointerException("root");
    if (!root.exists()) return 0;
    if (root.isDirectory()) {
      // get last modif date of index
      this._indexModifiedDate = indexed == null || indexed.isEmpty() ? -1 : this._index.getIndexIO().getLastTimeUsed();
      // create batch object
      this.batch = new IndexBatch(this._index.getIndexID());
      // find documents to modify/add to index
      this.indexedFiles = indexed;
      try {
        Files.walkFileTree(root.toPath(), Collections.singleton(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, this);
      } catch (IOException ex) {
        LOGGER.warn("Failed to collect files to index from folder {}", root, ex);
      }
      // get files to remove
      if (this.indexedFiles != null) {
        for (String path : this.indexedFiles.keySet()) {
          this.resultFiles.put(path, Action.DELETE);
          this.batch.increaseTotal();
          this._manager.indexBatch(this.batch, path, LocalFileContentType.SINGLETON, this._index, this._requester, this.priority, null);
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

  public Map<String, Action> getIndexedFiles() {
    return this.resultFiles;
  }

  // -----------------------------------------------------------------------------------
  // File walking methods

  @Override
  public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
    File file = path.toFile();
    String aspath = file.getAbsolutePath();
    Long indexModified = this.indexedFiles == null ? null : this.indexedFiles.remove(aspath);
    // only files updated since last commit
    if (!this.useIndexDate || this._indexModifiedDate == -1 || attrs.lastModifiedTime().toMillis() > this._indexModifiedDate) {
      // check for fileFilter
      if (this.fileFilter != null && !this.fileFilter.accept(file))
        return FileVisitResult.CONTINUE;
      // check in the index to know what action to perform
      if (indexModified == null) {
        this.resultFiles.put(aspath, Action.INSERT);
      } else {
        this.resultFiles.put(aspath, Action.UPDATE);
      }
      // index
      this.batch.increaseTotal();
      this._manager.indexBatch(this.batch, aspath, LocalFileContentType.SINGLETON, this._index, this._requester, IndexJob.Priority.HIGH, null);
    }
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
    File file = dir.toFile();
    if (this.directoryFilter != null && !this.directoryFilter.accept(file))
      return FileVisitResult.SKIP_SUBTREE;
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult visitFileFailed(Path file, IOException exc) {
    LOGGER.error("Failed to collect document {}", file, exc);
    return FileVisitResult.CONTINUE;
  }
}

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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.api.Content;
import org.pageseeder.flint.api.ContentType;
import org.pageseeder.flint.content.DeleteRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Content from a file.
 *
 * <p>This class provides a base class implementing of the {@link Content} interface for a local file.
 *
 * <p>Implementations should specify:
 * <ol>
 *   <li>How to resolve the media type for the file.</li>
 *   <li>The delete rule for the file.</li>
 * </ol>
 *
 * @author Christophe Lauret
 * @version 27 February 2013
 */
public class LocalFileContent implements Content {

  /**
   * Logger for this class.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(LocalFileContent.class);

  /**
   * The wrapped file to index or delete.
   */
  private final File _f;
  /**
   * The wrapped file to index or delete.
   */
  private final String _root;

  /**
   * Creates a new content from a given file.
   *
   * @param f The file
   */
  public LocalFileContent(File f) {
    this._f = f;
    this._root = null;
  }

  /**
   * Creates a new content from a given file.
   *
   * @param f The file
   */
  public LocalFileContent(File f, File root) {
    this._f = f;
    this._root = root.getAbsolutePath();
  }

  @Override
  public ContentType getContentType() {
    return LocalFileContentType.SINGLETON;
  }

  @Override
  public String getContentID() {
    return this._f.getAbsolutePath();
  }

  @Override
  public String getMediaType() throws IndexException {
    String name = this._f.getName();
    int lastDot = name.lastIndexOf('.');
    return lastDot == -1 ? "" : name.substring(lastDot+1);
  }

  @Override
  public DeleteRule getDeleteRule() {
    String path = this._f.getAbsolutePath();
    if (this._root != null && path.startsWith(this._root))
      path = path.substring(this._root.length());
    return new DeleteRule("_path", path.replace('\\', '/'));
  }

  /**
   * Returns a new buffered <code>FileInputStream</code>.
   *
   * @return a new buffered <code>FileInputStream</code>.
   */
  @Override
  public final InputStream getSource() {
    if (!this._f.exists()) return null;
    try {
      return new BufferedInputStream(new FileInputStream(this._f));
    } catch (IOException ex) {
      LOGGER.warn("Unable to get input source for ", this._f, ex);
      return null;
    }
  }

  @Override
  public final boolean isDeleted() {
    return !this._f.exists();
  }

  /**
   * Returns the underlying file.
   *
   * @return the underlying file.
   */
  public File file() {
    return this._f;
  }

  public static String getContentID(File f) {
    return f.getAbsolutePath();
  }
}

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

import org.pageseeder.flint.api.ContentId;
import org.pageseeder.flint.api.ContentType;

/**
 * A basic implementation of the Flint's ContentId interface for uses by local system files.
 *
 * <p>The ID for each file is their canonical path.
 *
 * @author Christophe Lauret
 * @version 27 February 2013
 */
public final class LocalFileContentId implements ContentId {

  /**
   * The ID for the file.
   */
  private final String _id;

  /**
   * The file.
   */
  private final File _file;

  /**
   * Creates a new File Content Id for the specified File.
   *
   * @param file The file to index.
   */
  public LocalFileContentId(File file) {
    this._id = toID(file);
    this._file = file;
  }

  @Override
  public ContentType getContentType() {
    return LocalFileContentType.SINGLETON;
  }

  @Override
  public String getID() {
    return this._id;
  }

  /**
   * Returns the file.
   *
   * @return the underlying file.
   */
  public File file() {
    return this._file;
  }

  @Override
  public String toString() {
    return this._id;
  }

  /**
   * Returns a unique identifier for the specified file.
   *
   * @param f The file
   * @return The canonical path as its ID.
   */
  private static String toID(File f) {
    String id = null;
    try {
      id = f.getCanonicalPath();
    } catch (IOException ex) {
      id = f.getAbsolutePath();
    }
    return id;
  }

}

/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.local;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.flint.content.Content;

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
 * @version 18 October 2012
 */
public abstract class LocalFileContent implements Content {

  /**
   * Logger for this class.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(LocalFileContent.class);

  /**
   * The wrapped file to index or delete.
   */
  private final File _f;

  /**
   * Creates a new content from a given file.
   *
   * @param f The file
   */
  public LocalFileContent(File f) {
    this._f = f;
  }

  /**
   * Always <code>null</code>.
   *
   * {@inheritDoc}
   */
  @Override
  public String getConfigID() {
    return null;
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
}

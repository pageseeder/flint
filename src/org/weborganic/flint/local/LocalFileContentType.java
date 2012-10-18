/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.local;

import org.weborganic.flint.content.ContentType;

/**
 * A content type provided for convenience that corresponds to a local file.
 *
 * @author Christophe Lauret
 * @version 18 October 2012
 */
public final class LocalFileContentType implements ContentType {

  /**
   * Sole instance.
   */
  public static final LocalFileContentType SINGLETON = new LocalFileContentType();

  /**
   * Always returns "LocalFile".
   *
   * @return Always "LocalFile".
   */
  @Override public String toString() {
    return "LocalFile";
  }

}

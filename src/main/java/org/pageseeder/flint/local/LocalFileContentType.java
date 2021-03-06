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

import org.pageseeder.flint.content.ContentType;

/**
 * A content type provided for convenience that corresponds to a local file.
 *
 * @author Christophe Lauret
 * @version 27 February 2013
 */
public final class LocalFileContentType implements ContentType {

  /**
   * Sole instance.
   */
  public static final LocalFileContentType SINGLETON = new LocalFileContentType();

  /** Use singleton instance. */
  private LocalFileContentType() {
  }

  /**
   * Always returns "LocalFile".
   *
   * @return Always "LocalFile".
   */
  @Override public String toString() {
    return "LocalFile";
  }

}

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
package org.pageseeder.flint;

/**
 * An exception dedicated to indexing problems.
 *
 * <p>Used to wrap other exceptions.
 *
 * @author Christophe Lauret (Weborganic)
 * @version 9 February 2007
 */
public class IndexException extends Exception {

  /**
   * The serial version UID as required by the Serializable interface.
   */
  private static final long serialVersionUID = 4L;

  /**
   * Creates a new IndexException.
   *
   * @param message The message.
   * @param ex The exception.
   */
  public IndexException(String message, Exception ex) {
    super(message, ex);
  }
}

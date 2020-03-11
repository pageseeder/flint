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
 * An exception when trying to open an index already opened.
 *
 * @author Jean-Baptiste Reure (Weborganic)
 * @version 11 March 2020
 */
public class IndexOpenException extends IndexException {

  /**
   * Creates a new IndexOpenException.
   *
   * @param message The message.
   * @param ex The exception.
   */
  public IndexOpenException(String message, Exception ex) {
    super(message, ex);
  }
}

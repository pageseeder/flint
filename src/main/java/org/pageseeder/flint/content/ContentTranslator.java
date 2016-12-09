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
package org.pageseeder.flint.content;

import java.io.Reader;

import org.pageseeder.flint.IndexException;


/**
 * Translate a <code>Content</code> into an XML Stream, ready to be transformed to produce Index XML.
 *
 * @author Jean-Baptiste Reure
 *
 * @version 9 March 2010
 */
public interface ContentTranslator {

  /**
   * Translate the content provided into an XML Source ready to be transformed by Flint.
   *
   * @param content the content to translate
   *
   * @return the translation as a Reader
   *
   * @throws IndexException Should any error occur during the translation.
   */
  Reader translate(Content content) throws IndexException;

}

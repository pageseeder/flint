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

import java.util.Collection;

/**
 * A Factory to create <code>org.pageseeder.flint.content.ContentTranslator</code> objects used to
 * translate <code>org.pageseeder.flint.content.Content</code> into XML.
 *
 * @author Jean-Baptiste Reure
 *
 * @version 9 March 2010
 */
public interface ContentTranslatorFactory {

  /**
   * Return the list of MIME Types supported this factory.
   *
   * @return the list of MIME Types supported
   */
  Collection<String> getMimeTypesSupported();

  /**
   * Return an instance of <code>ContentTranslator</code> used to translate Content with the MIME
   * Type provided.
   *
   * @param mimeType the MIME Type of the Content
   *
   * @return a <code>ContentTranslator</code> instance.
   */
  ContentTranslator createTranslator(String mimeType);

}

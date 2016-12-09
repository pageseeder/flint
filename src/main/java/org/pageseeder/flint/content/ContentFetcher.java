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

import org.pageseeder.flint.indexing.IndexJob;

/**
 * This class provides a way for the IndexManager to fetch the content to add to the index.
 *
 * <p>Content is identified by its ContentID and its ContentType.
 *
 * @author Jean-Baptiste Reure
 * @version 1 September 2015
 */
public interface ContentFetcher {

  /**
   * Load the Content.
   *
   * @param job   the index job
   *
   * @return the source where the Content is read from
   */
  Content getContent(IndexJob job);

}

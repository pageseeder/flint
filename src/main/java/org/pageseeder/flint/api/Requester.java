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
package org.pageseeder.flint.api;

import java.util.Map;

/**
 * A Job Requester.
 *
 * @author Jean-Baptiste Reure
 * @version 2 September 2015
 */
public class Requester {

  /**
   * The ID for this requester.
   */
  private final String _id;

  /**
   * Create a new requester.
   * 
   * @param id the ID for this requester.
   */
  public Requester(String id) {
    this._id = id;
  }

  /**
   * Return the ID for this requester.
   *
   * @return the ID for this requester.
   */
  public String getRequesterID() {
    return this._id;
  }

  /**
   * Used to define parameters specific to a content.
   * 
   * @param contentid the content ID
   * @param type      the content type
   * 
   * @return the list of parameters, <code>null</code> if none
   */
  public Map<String, String> getParameters(String contentid, ContentType type) {
    return null;
  }
}

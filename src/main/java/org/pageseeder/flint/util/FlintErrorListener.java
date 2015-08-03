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
package org.pageseeder.flint.util;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

import org.pageseeder.flint.IndexJob;
import org.pageseeder.flint.api.IndexListener;

/**
 * Basic error listener for XSLT transformation in flint.
 *
 * @author Jean-Baptiste Reure
 * @author Christophe Lauret
 *
 * @version 8 February 2013
 */
public final class FlintErrorListener implements ErrorListener {

  /**
   * The Flint listener to log the errors to
   */
  private final IndexListener _listener;

  /**
   * The Job the errors belong to
   */
  private final IndexJob _job;

  /**
   * Create a new listener.
   *
   * @param listener the listener to notify
   * @param job      the job being processed
   *
   * @throws NullPointerException if either parameter is <code>null</code>
   */
  public FlintErrorListener(IndexListener listener, IndexJob job) {
    if (listener == null) throw new NullPointerException("listener");
    if (job == null) throw new NullPointerException("job");
    this._listener = listener;
    this._job = job;
  }

  @Override
  public void warning(TransformerException ex) {
    this._listener.warn(this._job, ex.getMessageAndLocation());
  }

  @Override
  public void error(TransformerException ex) {
    this._listener.error(this._job, ex.getMessageAndLocation(), null);
  }

  @Override
  public void fatalError(TransformerException ex) {
    this._listener.error(this._job, ex.getMessageAndLocation(), null);
  }

}

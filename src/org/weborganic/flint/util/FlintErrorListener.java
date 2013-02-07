/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.util;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

import org.weborganic.flint.IndexJob;
import org.weborganic.flint.IndexListener;

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

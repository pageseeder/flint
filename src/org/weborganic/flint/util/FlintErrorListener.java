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
import org.weborganic.flint.log.FlintListener;

/**
 * Basic error listener for XSLT transformation in flint.
 *
 * @author Jean-Baptiste Reure
 * @version 31 May 2010
 */
public class FlintErrorListener implements ErrorListener {

  /**
   * The Flint listener to log the errors to
   */
  private final FlintListener _listener;

  /**
   * The Job the errors belong to
   */
  private final IndexJob _job;

  /**
   * Create a new listener.
   *
   * @param listener the listener to log messages to
   * @param job      the Job the errors belong to
   */
  public FlintErrorListener(FlintListener listener, IndexJob job) {
    this._listener = listener;
    this._job = job;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void error(TransformerException te) throws TransformerException {
    if (this._job != null)
      this._listener.error(this._job, "ERROR: "+te.getMessageAndLocation());
    else
      this._listener.error("ERROR: "+te.getMessageAndLocation());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void fatalError(TransformerException te) throws TransformerException {
    if (this._job != null)
      this._listener.error(this._job, "FATAL: "+te.getMessageAndLocation());
    else
      this._listener.error("FATAL: "+te.getMessageAndLocation());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void warning(TransformerException te) throws TransformerException {
    if (this._job != null)
      this._listener.warn(this._job, "WARNING: "+te.getMessageAndLocation());
    else
      this._listener.warn("WARNING: "+te.getMessageAndLocation());
  }
}

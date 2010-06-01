package org.weborganic.flint.util;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

import org.weborganic.flint.IndexJob;
import org.weborganic.flint.log.FlintListener;

/**
 * Basic error listener for XSLT transformation in flint
 * 
 * @author Jean-Baptiste Reure
 * @version 31 May 2010
 */
public class FlintErrorListener implements ErrorListener {
  /**
   * The Flint listener to log the errors to
   */
  private final FlintListener listener;
  /**
   * The Job the errors belong to
   */
  private final IndexJob job;
  /**
   * Create a new listener
   * 
   * @param listener the listener to log messages to
   * @param job      the Job the errors belong to
   */
  public FlintErrorListener(FlintListener listener, IndexJob job) {
    this.listener = listener;
    this.job = job;
  }
  /**
   * {@inheritDoc}
   */
  public void error(TransformerException te) throws TransformerException {
    if (job != null)
      this.listener.error(job, "ERROR: "+te.getMessageAndLocation());
    else
      this.listener.error("ERROR: "+te.getMessageAndLocation());
  }
  /**
   * {@inheritDoc}
   */
  public void fatalError(TransformerException te) throws TransformerException {
    if (job != null)
      this.listener.error(job, "FATAL: "+te.getMessageAndLocation());
    else
      this.listener.error("FATAL: "+te.getMessageAndLocation());
  }
  /**
   * {@inheritDoc}
   */
  public void warning(TransformerException te) throws TransformerException {
    if (job != null)
      this.listener.warn(job, "WARNING: "+te.getMessageAndLocation());
    else
      this.listener.warn("WARNING: "+te.getMessageAndLocation());
  }
}

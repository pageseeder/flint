package org.weborganic.flint.util;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

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
   * Create a new listener
   * 
   * @param listener the listener to log messages to
   */
  public FlintErrorListener(FlintListener listener) {
    this.listener = listener;
  }
  /**
   * {@inheritDoc}
   */
  public void error(TransformerException te) throws TransformerException {
    this.listener.error("ERROR: "+te.getMessageAndLocation(), te);
  }
  /**
   * {@inheritDoc}
   */
  public void fatalError(TransformerException te) throws TransformerException {
    this.listener.error("FATAL: "+te.getMessageAndLocation(), te);
  }
  /**
   * {@inheritDoc}
   */
  public void warning(TransformerException te) throws TransformerException {
    this.listener.warn("WARNING: "+te.getMessageAndLocation(), te);
  }
}

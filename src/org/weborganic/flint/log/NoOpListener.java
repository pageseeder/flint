package org.weborganic.flint.log;

import org.weborganic.flint.IndexJob;

/**
 * A listener implementation that does nothing.
 * 
 * <p>It will happily ignore anything that is reported to it.
 * 
 * <p>Access the singleton instance with {@link #getInstance()}.
 * 
 * @author Christophe Lauret
 * @version 1 June 2010
 */
public final class NoOpListener implements FlintListener {

  /**
   * Sole instance.
   */
  private static final NoOpListener SINGLETON = new NoOpListener();

  /**
   * Returns the singleton instance.
   * 
   * @return the singleton instance
   */
  public static NoOpListener getInstance() {
    return SINGLETON;
  }

  /**
   * Singleton instance.
   */
  private NoOpListener() {
  }

  /**
   * Do nothing.
   * 
   * {@inheritDoc}
   */
  public void debug(String debug) {
  }

  /**
   * Do nothing.
   * 
   * {@inheritDoc}
   */
  public void error(String error, Throwable throwable) {
  }

  /**
   * Do nothing.
   * 
   * {@inheritDoc}
   */
  public void debug(IndexJob job, String debug) {
  }

  /**
   * Do nothing.
   * 
   * {@inheritDoc}
   */
  public void error(IndexJob job, String error, Throwable throwable) {
  }

  /**
   * Do nothing.
   * 
   * {@inheritDoc}
   */
  public void info(IndexJob job, String info) {
  }

  /**
   * Do nothing.
   * 
   * {@inheritDoc}
   */
  public void warn(IndexJob job, String warning) {
  }

  /**
   * Do nothing.
   * 
   * {@inheritDoc}
   */
  public void info(String info) {
  }

  /**
   * Do nothing.
   * 
   * {@inheritDoc}
   */
  public void warn(String warning) {
  }

  /**
   * Do nothing.
   * 
   * {@inheritDoc}
   */
  public void finishJob(IndexJob job) {
  }

  /**
   * Do nothing.
   * 
   * {@inheritDoc}
   */
  public void startJob(IndexJob job) {
  }

  /**
   * Do nothing.
   * 
   * {@inheritDoc}
   */
  public void debug(IndexJob job, String message, Throwable throwable) {
  }

  /**
   * Do nothing.
   * 
   * {@inheritDoc}
   */
  public void debug(String debug, Throwable throwable) {
  }

  /**
   * Do nothing.
   * 
   * {@inheritDoc}
   */
  public void error(IndexJob job, String message) {
  }

  /**
   * Do nothing.
   * 
   * {@inheritDoc}
   */
  public void error(String error) {
  }

  /**
   * Do nothing.
   * 
   * {@inheritDoc}
   */
  public void info(IndexJob job, String message, Throwable throwable) {
  }

  /**
   * Do nothing.
   * 
   * {@inheritDoc}
   */
  public void info(String info, Throwable throwable) {
  }

  /**
   * Do nothing.
   * 
   * {@inheritDoc}
   */
  public void warn(IndexJob job, String message, Throwable throwable) {
  }

  /**
   * Do nothing.
   * 
   * {@inheritDoc}
   */
  public void warn(String warning, Throwable throwable) {
  }

}

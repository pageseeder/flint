package org.weborganic.flint.log;

import org.weborganic.flint.IndexJob;

/**
 * A logger implementation that remains silent.
 * 
 * <p>It will happily ignore anything that is reported to it.
 * 
 * @author Christophe Lauret
 * @version 26 May 2010
 */
public class SilentListener implements FlintListener {

  /**
   * Sole instance.
   */
  private static final SilentListener SINGLETON = new SilentListener();

  /**
   * Returns the singleton instance.
   * 
   * @return the singleton instance
   */
  public static SilentListener getInstance() {
    return SINGLETON;
  }

  /**
   * Singleton instance.
   */
  private SilentListener() {
  }

  /**
   * {@inheritDoc}
   */
  public void debug(String debug) {
  }

  /**
   * {@inheritDoc}
   */
  public void error(String error, Throwable throwable) {
  }

  /**
   * {@inheritDoc}
   */
  public void debug(IndexJob job, String debug) {
  }

  /**
   * {@inheritDoc}
   */
  public void error(IndexJob job, String error, Throwable throwable) {
  }

  /**
   * {@inheritDoc}
   */
  public void info(IndexJob job, String info) {
  }

  /**
   * {@inheritDoc}
   */
  public void warn(IndexJob job, String warning) {
  }

  /**
   * {@inheritDoc}
   */
  public void info(String info) {
  }

  /**
   * {@inheritDoc}
   */
  public void warn(String warning) {
  }

  /**
   * {@inheritDoc}
   */
  public void finishJob(IndexJob job) {
  }

  /**
   * {@inheritDoc}
   */
  public void startJob(IndexJob job) {
  }

  /**
   * {@inheritDoc}
   */
  public void debug(IndexJob job, String message, Throwable throwable) {
  }

  /**
   * {@inheritDoc}
   */
  public void debug(String debug, Throwable throwable) {
  }

  /**
   * {@inheritDoc}
   */
  public void error(IndexJob job, String message) {    
  }

  /**
   * {@inheritDoc}
   */
  public void error(String error) {
    
  }

  /**
   * {@inheritDoc}
   */
  public void info(IndexJob job, String message, Throwable throwable) {
  }

  /**
   * {@inheritDoc}
   */
  public void info(String info, Throwable throwable) {
  }

  /**
   * {@inheritDoc}
   */
  public void warn(IndexJob job, String message, Throwable throwable) {
  }

  /**
   * {@inheritDoc}
   */
  public void warn(String warning, Throwable throwable) {
  }
  
}

package org.weborganic.flint.log;


import org.slf4j.Logger;
import org.weborganic.flint.IndexJob;

/**
 * A logger implementation that reports events to a <code>SLF4J</code> logger. 
 * 
 * <p>This implementation simply wraps a {@link Logger} instance.
 * 
 * @author Christophe Lauret
 * @version 29 July 2010
 */
public final class SLF4JListener implements FlintListener {

  /**
   * Sole instance.
   */
  private final Logger _logger;

  /**
   * Creates a new logger for the specified Logger.
   * 
   * @param logger The underlying logger to use.
   */
  public SLF4JListener(Logger logger) {
    this._logger = logger;
  }

  /**
   * {@inheritDoc}
   */
  public void debug(String message) {
    this._logger.debug(message);
  }

  /**
   * {@inheritDoc}
   */
  public void debug(String debug, Throwable throwable) {
    this._logger.debug(debug, throwable);
  }

  /**
   * {@inheritDoc}
   */
  public void info(String message) {
    this._logger.info(message);
  }

  /**
   * {@inheritDoc}
   */
  public void info(String info, Throwable throwable) {
    this._logger.info(info, throwable);
  }

  /**
   * {@inheritDoc}
   */
  public void warn(String message) {
    this._logger.warn(message);
  }

  /**
   * {@inheritDoc}
   */
  public void warn(String warn, Throwable throwable) {
    this._logger.warn(warn, throwable);
  }

  /**
   * {@inheritDoc}
   */
  public void error(String message) {
    error(message, null);
  }

  /**
   * {@inheritDoc}
   */
  public void error(String message, Throwable throwable) {
    this._logger.error(message, throwable);
  }

  /**
   * {@inheritDoc}
   */
  public void debug(IndexJob job, String message) {
    this._logger.debug("{} [Job:{}]", message, job.toString());
  }

  /**
   * {@inheritDoc}
   */
  public void debug(IndexJob job, String message, Throwable throwable) {
    this._logger.debug("{} [Job:{}]", message, job.toString());
    this._logger.debug(message, throwable);
  }

  /**
   * {@inheritDoc}
   */
  public void info(IndexJob job, String message) {
    this._logger.info("{} [Job:{}]", message, job.toString());
  }

  /**
   * {@inheritDoc}
   */
  public void info(IndexJob job, String message, Throwable throwable) {
    this._logger.info("{} [Job:{}]", message, job.toString());
    this._logger.info(message, throwable);
  }

  /**
   * {@inheritDoc}
   */
  public void warn(IndexJob job, String message) {
    this._logger.warn("{} [Job:{}]", message, job.toString());
  }

  /**
   * {@inheritDoc}
   */
  public void warn(IndexJob job, String message, Throwable throwable) {
    this._logger.warn("{} [Job:{}]", message, job.toString());
    this._logger.warn(message, throwable);
  }

  /**
   * {@inheritDoc}
   */
  public void error(IndexJob job, String message) {
    this._logger.error("{} [Job:{}]", message, job.toString());
  }

  /**
   * {@inheritDoc}
   */
  public void error(IndexJob job, String message, Throwable throwable) {
    this._logger.error("{} [Job:{}]", message, job.toString());
    this._logger.error(message, throwable);
  }

  /**
   * {@inheritDoc}
   */
  public void finishJob(IndexJob job) {
    this._logger.info("Done! [Job:{}]", job.toString());
  }

  /**
   * {@inheritDoc}
   */
  public void startJob(IndexJob job) {
    this._logger.info("Done! [Job:{}]", job.toString());
  }
}

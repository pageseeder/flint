/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.log;

import org.slf4j.Logger;
import org.weborganic.flint.IndexJob;


/**
 * A logger implementation that reports events to a <code>SLF4J</code> logger.
 *
 * <p>This implementation simply wraps a {@link Logger} instance.
 *
 * @author Christophe Lauret
 * @version 30 January 2012
 */
public final class SLF4JListener implements FlintListener {

  /**
   * The format string used for all SLF4J.
   */
  private static final String FORMAT_STRING = "{} [Job:{}]";

  /**
   * Sole instance.
   */
  private final Logger _logger;

  /**
   * The size of the batch being processes.
   */
  private int _batchSize = 0;

  /**
   * Creates a new logger for the specified Logger.
   *
   * @param logger The underlying logger to use.
   */
  public SLF4JListener(Logger logger) {
    this._logger = logger;
  }

  @Override
  public void debug(String message) {
    this._logger.debug(message);
  }

  @Override
  public void debug(String debug, Throwable throwable) {
    this._logger.debug(debug, throwable);
  }

  @Override
  public void info(String message) {
    this._logger.info(message);
  }

  @Override
  public void info(String info, Throwable throwable) {
    this._logger.info(info, throwable);
  }

  @Override
  public void warn(String message) {
    this._logger.warn(message);
  }

  @Override
  public void warn(String warn, Throwable throwable) {
    this._logger.warn(warn, throwable);
  }

  @Override
  public void error(String message) {
    error(message, null);
  }

  @Override
  public void error(String message, Throwable throwable) {
    this._logger.error(message, throwable);
  }

  @Override
  public void debug(IndexJob job, String message) {
    this._logger.debug(FORMAT_STRING, message, job.toString());
  }

  @Override
  public void debug(IndexJob job, String message, Throwable throwable) {
    this._logger.debug(FORMAT_STRING, message, job.toString());
    this._logger.debug(message, throwable);
  }

  @Override
  public void info(IndexJob job, String message) {
    this._logger.info(FORMAT_STRING, message, job.toString());
  }

  @Override
  public void info(IndexJob job, String message, Throwable throwable) {
    this._logger.info(FORMAT_STRING, message, job.toString());
    this._logger.info(message, throwable);
  }

  @Override
  public void warn(IndexJob job, String message) {
    this._logger.warn(FORMAT_STRING, message, job.toString());
  }

  @Override
  public void warn(IndexJob job, String message, Throwable throwable) {
    this._logger.warn(FORMAT_STRING, message, job.toString());
    this._logger.warn(message, throwable);
  }

  @Override
  public void error(IndexJob job, String message) {
    this._logger.error(FORMAT_STRING, message, job.toString());
  }

  @Override
  public void error(IndexJob job, String message, Throwable throwable) {
    this._logger.error(FORMAT_STRING, message, job.toString());
    this._logger.error(message, throwable);
  }

  @Override
  public void finishJob(IndexJob job) {
    endJob(job);
  }

  @Override
  public void endJob(IndexJob job) {
    this._batchSize++;
    this._logger.debug("Done! [Job:{}]", job.toString());
  }

  @Override
  public void startJob(IndexJob job) {
    this._logger.debug("Starting [Job:{}]", job.toString());
  }

  @Override
  public void startBatch() {
  }

  @Override
  public void endBatch() {
    this._logger.debug("Indexed {} files", this._batchSize);
    this._batchSize = 0;
  }
}

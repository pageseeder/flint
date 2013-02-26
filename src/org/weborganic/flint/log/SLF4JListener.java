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
import org.weborganic.flint.api.IndexListener;


/**
 * A logger implementation that reports events to a <code>SLF4J</code> logger.
 *
 * <p>This implementation simply wraps a {@link Logger} instance.
 *
 * @author Christophe Lauret
 * @version 27 February 2013
 */
public final class SLF4JListener implements IndexListener {

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
  public void warn(IndexJob job, String message) {
    this._logger.warn(FORMAT_STRING, message, job.toString());
  }

  @Override
  public void error(IndexJob job, String message, Throwable throwable) {
    this._logger.error(FORMAT_STRING, message, job.toString());
    this._logger.error(message, throwable);
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

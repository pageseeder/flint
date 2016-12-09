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
package org.pageseeder.flint.log;

import org.pageseeder.flint.indexing.IndexBatch;
import org.pageseeder.flint.indexing.IndexJob;
import org.pageseeder.flint.indexing.IndexListener;
import org.slf4j.Logger;


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
    this._logger.debug("Done! [Job:{}]", job.toString());
  }

  @Override
  public void startJob(IndexJob job) {
    this._logger.debug("Starting [Job:{}]", job.toString());
  }

  @Override
  public void startBatch(IndexBatch batch) {
  }

  @Override
  public void endBatch(IndexBatch batch) {
    this._logger.debug("Indexed {} files", batch.getTotalDocuments());
  }
}

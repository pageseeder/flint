/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.api;

import org.weborganic.flint.IndexJob;

/**
 * A listener to report on indexing events.
 *
 * @author Christophe Lauret
 * @version 8 February 2013
 */
public interface IndexListener {

  // Index job lifecycle
  // ----------------------------------------------------------------------------------------------

  /**
   * Indicates the indexer is about to process a batch of jobs.
   *
   * <p>This event is thrown when at least one new job has been found in the job queue.
   */
  void startBatch();

  /**
   * Indicates that indexer has just finished a batch of jobs.
   *
   * <p>This event is thrown if the queue is empty after a job is finished.
   */
  void endBatch();

  /**
   * When an indexing job is started.
   *
   * @param job the job that just started
   */
  void startJob(IndexJob job);

  /**
   * To log a warning message attached to an indexing job.
   *
   * @param job       the job concerned
   * @param message   the warning message
   */
  void warn(IndexJob job, String message);

  /**
   * When an error occurred during to an indexing job.
   *
   * @param job       the job concerned
   * @param message   the error message
   * @param throwable the exception (may be <code>null</code>)
   */
  void error(IndexJob job, String message, Throwable throwable);

  /**
   * When an indexing job was completed.
   *
   * @param job the job completed
   */
  void endJob(IndexJob job);

}

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
package org.pageseeder.flint.indexing;

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
  void startBatch(IndexBatch batch);

  /**
   * Indicates that indexer has just finished a batch of jobs.
   *
   * <p>This event is thrown if the queue is empty after a job is finished.
   */
  void endBatch(IndexBatch batch);

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

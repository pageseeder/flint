/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The queue containing Index Jobs.
 *
 * @author Jean-Baptiste Reure
 * @version 14 May 2010
 */
public final class IndexJobQueue {

  /**
   * An internal logger.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(IndexJobQueue.class);

  /**
   * Delay between each job poll in milliseconds.
   */
  private final long jobPollDelay;

  /**
   * The actual queue.
   */
  private final PriorityBlockingQueue<IndexJob> _queue;

  /**
   * Simple Constructor.
   *
   * @param pollDelay the poll delay on the queue (in milliseconds)
   */
  public IndexJobQueue(long pollDelay) {
    this.jobPollDelay = pollDelay;
    this._queue = new PriorityBlockingQueue<IndexJob>();
  }

  // public external methods
  // ----------------------------------------------------------------------------------------------

  /**
   * Add a new update job to the indexing queue.
   *
   * @param job The job to add to this queue.
   */
  public void addJob(IndexJob job) {
    LOGGER.debug("Adding Index Job to Queue: {}", job.toString());
    this._queue.put(job);
  }

  /**
   * Returns the list of jobs for the specified requester.
   *
   * <p>Note that by the time each job is checked, they might have run already so the method
   * {@link IndexJob#isFinished()} should be called before parsing the job.
   *
   * <p>The list will never be <code>null</code>.
   *
   * @param r the Requester
   * @return the list of jobs (never <code>null</code>)
   */
  public List<IndexJob> getJobsForRequester(Requester r) {
    if (r == null) return getAllJobs();
    List<IndexJob> jobs = new ArrayList<IndexJob>();
    for (IndexJob job : this._queue) {
      if (job.isForRequester(r)) jobs.add(job);
    }
    return jobs;
  }

  /**
   * Returns the list of jobs for the index provided.
   *
   * <p>Note that by the time each job is checked, they might have run already so the method
   * {@link IndexJob#isFinished()} should be called before parsing the job.
   *
   * <p>The list will never be <code>null</code>.
   *
   * @param i the index
   * @return the list of jobs (never <code>null</code>)
   */
  public List<IndexJob> getJobsForIndex(Index i) {
    if (i == null) return getAllJobs();
    List<IndexJob> jobs = new ArrayList<IndexJob>();
    for (IndexJob job : this._queue) {
      if (job.isForIndex(i)) jobs.add(job);
    }
    return jobs;
  }

  /**
   * Returns the complete list of jobs.
   *
   * <p>Note that by the time each job is checked, they might have run already so the method
   * {@link IndexJob#isFinished()} should be called before parsing the job.
   *
   * <p>The list will never be <code>null</code>.
   *
   * @return the list of jobs waiting (never <code>null</code>)
   */
  public List<IndexJob> getAllJobs() {
    return new ArrayList<IndexJob>(this._queue);
  }

  /**
   * Poll the next job in the queue (<code>null</code> if the queue is currently empty).
   *
   * @return the next job in the queue (<code>null</code> if the queue is currently empty).
   *
   * @throws InterruptedException if the thread was interrupted when waiting for the next job
   */
  public IndexJob nextJob() throws InterruptedException {
    return this._queue.poll(this.jobPollDelay, TimeUnit.MILLISECONDS);
  }

  /**
   * Indicates whether the queue is currently empty.
   *
   * @return <code>true</code> if there are currently no jobs;
   *         <code>false</code> otherwise.
   */
  public boolean isEmpty() {
    return this._queue.isEmpty();
  }

  /**
   * Indicates whether the queue is currently empty.
   *
   * @return <code>true</code> if there are currently no jobs;
   *         <code>false</code> otherwise.
   */
  public int size() {
    return this._queue.size();
  }
}

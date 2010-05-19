/*
 * This file is part of the Flint library.
 * 
 * For licensing information please see the file license.txt included in the release. A copy of this licence can also be
 * found at http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

/**
 * The queue containing Index Jobs
 * 
 * @author Jean-Baptiste Reure
 * @version 14 May 2010
 */
public class IndexJobQueue {
  /**
   * An internal logger 
   */
  private final static Logger logger = Logger.getLogger(IndexJobQueue.class);

  /**
   * Delay between each job poll in milliseconds
   */
  private final long jobPollDelay;

  /**
   * The actual queue
   */
  private final PriorityBlockingQueue<IndexJob> queue;

  /**
   * Simple Constructor
   * 
   * @param poll_delay the poll delay on the queue (in milliseconds)
   */
  public IndexJobQueue(long poll_delay) {
    this.jobPollDelay = poll_delay;
    this.queue = new PriorityBlockingQueue<IndexJob>();
  }

  // public external methods
  // ----------------------------------------------------------------------------------------------

  /**
   * Add a new update job to the indexing queue.
   * 
   * @param ct       the Type of the Content
   * @param id       the ID of the Content
   * @param i        the Index to add the Content to
   * @param config   the Config to use
   * @param r        the Requester calling this method (used for logging)
   * @param p        the Priority of this job
   * @param params   the dynamic XSLt parameters
   */
  public void addJob(IndexJob job) {
    logger.debug("Adding Index Job to Queue: "+job.toString());
    this.queue.put(job);
  }

  /**
   * Returns the list of jobs for the Requester provided.
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
    for (IndexJob job : this.queue) {
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
    for (IndexJob job : this.queue) {
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
    return new ArrayList<IndexJob>(this.queue);
  }
  /**
   * Poll the next job in the queue (null if the queue is currently empty).
   * 
   * @return the next job in the queue (null if the queue is currently empty).
   * 
   * @throws InterruptedException if the thread was interrupted when waiting for the next job
   */
  public IndexJob nextJob() throws InterruptedException {
    return this.queue.poll(jobPollDelay, TimeUnit.MILLISECONDS);
  }
  /**
   * Return true if the queue is currently empty, false otherwise
   * 
   * @return true if the queue is currently empty, false otherwise
   */
  public boolean isEmpty() {
    return this.queue.isEmpty();
  }


  // private methods
  // ----------------------------------------------------------------------------------------------


}

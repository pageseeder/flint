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

import org.pageseeder.flint.Index;
import org.pageseeder.flint.Requester;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * The queue containing index jobs.
 *
 * <p>This class uses a {@link PriorityBlockingQueue} so that when the queue is empty,
 * the calling thread will be delayed.
 *
 * @author Jean-Baptiste Reure
 * @author Christophe Lauret
 *
 * @version 28 February 2013
 */
public final class IndexJobQueue {


  /**
   * The actual queue.
   */
  private final PriorityBlockingQueue<IndexJob> _queue;

  /**
   * The single thread queue.
   */
  private final PriorityBlockingQueue<IndexJob> _singleThreadQueue;

  /**
   * Simple Constructor.
   *
   * @param withSingleThreadQueue if there's only one queue
   */
  public IndexJobQueue(boolean withSingleThreadQueue) {
    this._queue = new PriorityBlockingQueue<IndexJob>();
    this._singleThreadQueue = withSingleThreadQueue ? new PriorityBlockingQueue<IndexJob>() : null;
  }

  // public external methods
  // ----------------------------------------------------------------------------------------------

  /**
   * Add a new update job to the indexing queue.
   *
   * @param job The job to add to this queue.
   */
  public void addSingleThreadJob(IndexJob job) {
    addJob(job, true);
  }

  /**
   * Add a new update job to the indexing queue.
   *
   * @param job The job to add to this queue.
   */
  public void addMultiThreadJob(IndexJob job) {
    addJob(job, false);
  }

  /**
   * Returns the list of jobs for the specified requester.
   *
   * <p>Note that by the time each job is checked, they might have run already so the method
   * {@link IndexJob#isFinished()} should be called before parsing the job.
   *
   * <p>The list will never be <code>null</code>.
   *
   * @param requester the Requester
   * @return the list of jobs (never <code>null</code>)
   */
  public List<IndexJob> getJobsForRequester(Requester requester) {
    if (requester == null) return getAllJobs();
    List<IndexJob> jobs = new ArrayList<IndexJob>();
    for (IndexJob job : this._queue) {
      if (job.isForRequester(requester)) {
        jobs.add(job);
      }
    }
    if (this._singleThreadQueue != null)
    for (IndexJob job : this._singleThreadQueue) {
      if (job.isForRequester(requester)) {
        jobs.add(job);
      }
    }
    return jobs;
  }

  /**
   * Returns the number of jobs for the specified requester.
   *
   * <p>Not that some job have have been processed by the time this method returns.
   *
   * @param requester the requester
   *
   * @return the number of jobs for the specified requester.
   */
  public int countJobsForRequester(Requester requester) {
    if (requester == null) return this._queue.size();
    int count = 0;
    for (IndexJob job : this._queue) {
      if (job.isForRequester(requester)) {
        count++;
      }
    }
    if (this._singleThreadQueue != null)
    for (IndexJob job : this._singleThreadQueue) {
      if (job.isForRequester(requester)) {
        count++;
      }
    }
    return count;
  }

  /**
   * Removes the jobs for the index provided.
   *
   * @param index the index
   */
  public void clearJobsForIndex(Index index) {
    if (index == null) return;
    List<IndexJob> jobs = new ArrayList<IndexJob>();
    for (IndexJob job : this._queue) {
      if (job.isForIndex(index)) {
        jobs.add(job);
      }
    }
    this._queue.removeAll(jobs);
    if (this._singleThreadQueue != null) {
      jobs.clear();
      for (IndexJob job : this._singleThreadQueue) {
        if (job.isForIndex(index)) {
          jobs.add(job);
        }
      }
      this._singleThreadQueue.removeAll(jobs);
    }
  }

  /**
   * Returns the list of jobs for the index provided.
   *
   * <p>Note that by the time each job is checked, they might have run already so the method
   * {@link IndexJob#isFinished()} should be called before parsing the job.
   *
   * <p>The list will never be <code>null</code>.
   *
   * @param index the index
   * @return the list of jobs (never <code>null</code>)
   */
  public List<IndexJob> getJobsForIndex(Index index) {
    if (index == null) return getAllJobs();
    List<IndexJob> jobs = new ArrayList<IndexJob>();
    for (IndexJob job : this._queue) {
      if (job.isForIndex(index)) {
        jobs.add(job);
      }
    }
    if (this._singleThreadQueue != null)
    for (IndexJob job : this._singleThreadQueue) {
      if (job.isForIndex(index)) {
        jobs.add(job);
      }
    }
    return jobs;
  }

  /**
   * <p>Not that some job have have been processed by the time this method returns.
   *
   * @param index the index
   * @return <code>true</code> if there is at least one job for the index provided.
   */
  public boolean hasJobsForIndex(Index index) {
    if (index != null) {
      for (IndexJob job : this._queue) {
        if (job.isForIndex(index)) return true;
      }
      if (this._singleThreadQueue != null)
      for (IndexJob job : this._singleThreadQueue) {
        if (job.isForIndex(index)) return true;
      }
    }
    return false;
  }

  /**
   * Returns the number of jobs for the specified index provided.
   *
   * <p>Not that some job have have been processed by the time this method returns.
   *
   * @param index the index
   * @return the number of jobs for the specified provided.
   */
  public int countJobsForIndex(Index index) {
    if (index == null) return this._queue.size();
    int count = 0;
    for (IndexJob job : this._queue) {
      if (job.isForIndex(index)) {
        count++;
      }
    }
    if (this._singleThreadQueue != null)
    for (IndexJob job : this._singleThreadQueue) {
      if (job.isForIndex(index)) {
        count++;
      }
    }
    return count;
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
    ArrayList<IndexJob> list = new ArrayList<IndexJob>(this._queue);
    if (this._singleThreadQueue != null)
      list.addAll(this._singleThreadQueue);
    return list;
  }

  /**
   * Poll the next job in the queue (<code>null</code> if the queue is currently empty).
   *
   * @return the next job in the queue (<code>null</code> if the queue is currently empty).
   *
   * @throws InterruptedException if the thread was interrupted when waiting for the next job
   */
  public IndexJob nextMultiThreadJob() throws InterruptedException {
    return this._queue.take();
  }

  /**
   * Poll the next job in the queue (<code>null</code> if the queue is currently empty).
   *
   * @return the next job in the queue (<code>null</code> if the queue is currently empty).
   *
   * @throws InterruptedException if the thread was interrupted when waiting for the next job
   */
  public IndexJob nextSingleThreadJob() throws InterruptedException {
    return this._singleThreadQueue != null ? this._singleThreadQueue.take() : null;
  }

  /**
   * Indicates whether the queue is currently empty.
   *
   * @return <code>true</code> if there are currently no jobs;
   *         <code>false</code> otherwise.
   */
  public boolean isMultiThreadsEmpty() {
    return this._queue.isEmpty();
  }

  /**
   * Indicates whether the queue is currently empty.
   *
   * @return <code>true</code> if there are currently no jobs;
   *         <code>false</code> otherwise.
   */
  public boolean isSingleThreadEmpty() {
    return this._singleThreadQueue == null || this._singleThreadQueue.isEmpty();
  }
  
  /**
   * clear all queues
   */
  public void clear() {
    this._queue.clear();
    if (this._singleThreadQueue != null)
      this._singleThreadQueue.clear();
  }

  // -----------------------------------------------------------------------
  // private methods
  // -----------------------------------------------------------------------

  private void addJob(IndexJob job, boolean singleThread) {
    // check if similar job already there
    IndexJob existing;
    synchronized (this._queue) {
      existing = this._queue.stream().filter(ajob -> ajob.isSimilar(job)).findFirst().orElse(null);
    }
    // in the other queue?
    boolean foundInSingleQueue = false;
    if (existing == null && this._singleThreadQueue != null) {
      synchronized (this._singleThreadQueue) {
        existing = this._singleThreadQueue.stream().filter(ajob -> ajob.isSimilar(job)).findFirst().orElse(null);
        foundInSingleQueue = existing != null;
      }
    }

    // if there is one similar, and this one has higher priority, add this one and remove the old one
    if (existing == null || (existing.getPriority() == IndexJob.Priority.LOW && job.getPriority() == IndexJob.Priority.HIGH)) {
      if (singleThread && this._singleThreadQueue != null) {
        synchronized (this._singleThreadQueue) {
          if (existing != null) {
            if (foundInSingleQueue) this._singleThreadQueue.remove(existing);
            else this._queue.remove(existing);
          }
          this._singleThreadQueue.put(job);
        }
      } else {
        synchronized (this._queue) {
          if (existing != null) {
            if (foundInSingleQueue) this._singleThreadQueue.remove(existing);
            else this._queue.remove(existing);
          }
          this._queue.put(job);
        }
      }
    }
  }
}

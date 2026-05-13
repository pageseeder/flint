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
import org.pageseeder.flint.content.ContentType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.stream.Collectors;

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
   * The debounce delay in ms
   */
  private final int _debounceThresholdInMs;

  /**
   * Debounce state (only used when _debounceThresholdInMs > 0).
   */
  private final ConcurrentHashMap<JobKey, PendingDebounce> _debounced = new ConcurrentHashMap<>();

  /**
   * Single-thread scheduler is enough: we only schedule "enqueue this job later" tasks.
   */
  private final ScheduledExecutorService _debounceScheduler;

  /**
   * Internal state flag
   */
  private volatile boolean isShutdown = false;

  /**
   * Simple Constructor.
   *
   * @param withSingleThreadQueue if there's only one queue
   */
  public IndexJobQueue(boolean withSingleThreadQueue, int debounceThresholdInMs) {
    this._queue = new PriorityBlockingQueue<>();
    this._singleThreadQueue = withSingleThreadQueue ? new PriorityBlockingQueue<>() : null;
    this._debounceThresholdInMs = debounceThresholdInMs;

    if (debounceThresholdInMs > 0) {
      ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1, r -> {
        Thread t = new Thread(r, "flint-debounce-indexing");
        t.setDaemon(true);
        return t;
      });
      exec.setRemoveOnCancelPolicy(true);
      this._debounceScheduler = exec;
    } else {
      this._debounceScheduler = null;
    }
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
    List<IndexJob> jobs = this._queue.stream().filter(job -> job.isForRequester(requester)).collect(Collectors.toList());
    if (this._debounceScheduler != null) {
      jobs.addAll(this._debounced.values().stream()
        .filter(pendingDebounce -> pendingDebounce.job.isForRequester(requester))
        .map(pendingDebounce -> pendingDebounce.job)
        .collect(Collectors.toList()));
    }
    if (this._singleThreadQueue != null) {
      jobs.addAll(this._singleThreadQueue.stream().filter(job -> job.isForRequester(requester)).collect(Collectors.toList()));
    }
    return jobs;
  }

  /**
   * Returns the number of jobs for the specified requester.
   *
   * <p>Note that some jobs may have been processed by the time this method returns.
   *
   * @param requester the requester
   *
   * @return the number of jobs for the specified requester.
   */
  public int countJobsForRequester(Requester requester) {
    if (requester == null) return this._queue.size();
    int count = (int) this._queue.stream().filter(job -> job.isForRequester(requester)).count();
    if (this._debounceScheduler != null) {
      count += (int) this._debounced.values().stream()
        .filter(pendingDebounce -> pendingDebounce.job.isForRequester(requester)).count();
    }
    if (this._singleThreadQueue != null) {
      count += (int) this._singleThreadQueue.stream().filter(job -> job.isForRequester(requester)).count();
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

    // Cancel/remove debounced jobs for the index
    if (this._debounceScheduler != null) {
      this._debounced.entrySet().removeIf(e -> {
        PendingDebounce p = e.getValue();
        IndexJob j = p.job;
        if (j != null && j.isForIndex(index)) {
          ScheduledFuture<?> f = p.future;
          if (f != null) f.cancel(false);
          return true;
        }
        return false;
      });
    }
    List<IndexJob> jobs = this._queue.stream().filter(job -> job.isForIndex(index)).collect(Collectors.toList());
    this._queue.removeAll(jobs);
    if (this._singleThreadQueue != null) {
      jobs = this._singleThreadQueue.stream().filter(job -> job.isForIndex(index)).collect(Collectors.toList());
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
    List<IndexJob> jobs = this._queue.stream().filter(job -> job.isForIndex(index)).collect(Collectors.toList());
    if (this._debounceScheduler != null) {
      jobs.addAll(this._debounced.values().stream()
          .filter(pendingDebounce -> pendingDebounce.job.isForIndex(index))
          .map(pendingDebounce -> pendingDebounce.job)
          .collect(Collectors.toList()));
    }
    if (this._singleThreadQueue != null) {
      jobs.addAll(this._singleThreadQueue.stream().filter(job -> job.isForIndex(index)).collect(Collectors.toList()));
    }
    return jobs;
  }

  /**
   * <p>Note that some jobs may have been processed by the time this method returns.
   *
   * @param index the index
   * @return <code>true</code> if there is at least one job for the index provided.
   */
  public boolean hasJobsForIndex(Index index) {
    if (index != null) {
      if (this._queue.stream().anyMatch(job -> job.isForIndex(index)))
        return true;
      if (this._debounceScheduler != null) {
        if (this._debounced.values().stream()
            .anyMatch(pendingDebounce -> pendingDebounce.job.isForIndex(index)))
          return true;
      }
      if (this._singleThreadQueue != null) {
        return this._singleThreadQueue.stream().anyMatch(job -> job.isForIndex(index));
      }
    }
    return false;
  }

  /**
   * Returns the number of jobs for the specified index provided.
   *
   * <p>Note that some jobs may have been processed by the time this method returns.
   *
   * @param index the index
   * @return the number of jobs for the specified provided.
   */
  public int countJobsForIndex(Index index) {
    if (index == null) return this._queue.size();

    int count = (int) this._queue.stream().filter(job -> job.isForIndex(index)).count();
    if (this._debounceScheduler != null) {
      count += (int) this._debounced.values().stream()
        .filter(pendingDebounce -> pendingDebounce.job.isForIndex(index)).count();
    }
    if (this._singleThreadQueue != null) {
      count += (int) this._singleThreadQueue.stream().filter(job -> job.isForIndex(index)).count();
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
    ArrayList<IndexJob> list = new ArrayList<>(this._queue);
    if (this._debounceScheduler != null) {
      list.addAll(this._debounced.values().stream()
          .map(p -> p.job)
          .filter(Objects::nonNull)
          .collect(Collectors.toList()));
    }
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
    while (!this.isShutdown) {
      IndexJob job = this._queue.poll(500, TimeUnit.MILLISECONDS);
      if (job != null) return job;
    }
    return null;
  }

  /**
   * Poll the next job in the queue (<code>null</code> if the queue is currently empty).
   *
   * @return the next job in the queue (<code>null</code> if the queue is currently empty).
   *
   * @throws InterruptedException if the thread was interrupted when waiting for the next job
   */

  public IndexJob nextSingleThreadJob() throws InterruptedException {
    if (this._singleThreadQueue == null) return null;
    while (!this.isShutdown) {
      IndexJob job = this._singleThreadQueue.poll(500, TimeUnit.MILLISECONDS);
      if (job != null) return job;
    }
    return null;
  }

  /**
   * Indicates whether the queue is currently empty.
   *
   * @return <code>true</code> if there are currently no jobs;
   *         <code>false</code> otherwise.
   */
  public boolean isMultiThreadsEmpty() {
    if (!this._queue.isEmpty()) return false;
    // check for debouncing jobs
    if (this._debounceScheduler != null) {
      // allmatch is true for empty lists
      return this._debounced.values().stream().allMatch(pending -> pending.singleThreadTarget);
    }
    return true;
  }

  /**
   * Indicates whether the queue is currently empty.
   *
   * @return <code>true</code> if there are currently no jobs;
   *         <code>false</code> otherwise.
   */
  public boolean isSingleThreadEmpty() {
    if (this._singleThreadQueue == null) return true;
    if (!this._singleThreadQueue.isEmpty()) return false;
    // check for debouncing jobs
    if (this._debounceScheduler != null) {
      return this._debounced.values().stream().noneMatch(pending -> pending.singleThreadTarget);
    }
    return true;
  }

  /**
   * clear all queues (single thread, multi-thread and debounce)
   */
  public void clear() {
    this._queue.clear();

    if (this._singleThreadQueue != null) this._singleThreadQueue.clear();

    if (this._debounceScheduler != null) {
      for (PendingDebounce p : this._debounced.values()) {
        if (p.future != null) p.future.cancel(false);
      }
      this._debounced.clear();
    }
  }

  /**
   * Call when you're done with this queue to release the debounce scheduler thread.
   * This will flush all debounced jobs into the main queues so indexing can finish.
   */
  public void shutdown() {
    // don't accept new jobs
    this.isShutdown = true;
    if (this._debounceScheduler != null) {

      // Cancel scheduled tasks but keep pending jobs
      List<PendingDebounce> pending = new ArrayList<>(this._debounced.values());
      for (PendingDebounce p : pending) {
        ScheduledFuture<?> f = p.future;
        if (f != null) f.cancel(false);
      }
      // Clear debounce map so scheduled tasks won't enqueue
      this._debounced.clear();

      // Add pending jobs immediately
      for (PendingDebounce p : pending) {
        enqueueWithoutDebounce(p.job, p.singleThreadTarget);
      }

      // Stop the scheduler and wait briefly
      this._debounceScheduler.shutdown();
      try {
        if (!this._debounceScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
          this._debounceScheduler.shutdownNow();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        this._debounceScheduler.shutdownNow();
      }
    }
  }

  // -----------------------------------------------------------------------
  // private methods
  // -----------------------------------------------------------------------

  private void addJob(IndexJob job, boolean singleThread) {
    if (job == null || this.isShutdown) return;

    // Debounce
    if (this._debounceScheduler != null) {
      JobKey key = JobKey.from(job);

      this._debounced.compute(key, (k, existing) -> {
        // cancel existing one
        if (existing != null && existing.future != null) {
          existing.future.cancel(false);
          this.removedJob(existing.job);
        }
        // add a new fresh one
        PendingDebounce fresh = new PendingDebounce(job, singleThread);
        fresh.future = this._debounceScheduler.schedule(() -> {
          // Ensure we only enqueue the latest pending for this key
          PendingDebounce current = this._debounced.get(k);
          if (current != null && current == fresh) {
            this._debounced.remove(k, fresh);
            enqueueWithoutDebounce(fresh.job, fresh.singleThreadTarget);
          }
        }, this._debounceThresholdInMs, TimeUnit.MILLISECONDS);

        return fresh;
      });

      return;
    }

    // No debounce: enqueue immediately
    enqueueWithoutDebounce(job, singleThread);
  }

  private void enqueueWithoutDebounce(IndexJob job, boolean singleThread) {
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
    boolean higherPriority = existing != null && existing.getPriority() == IndexJob.Priority.LOW && job.getPriority() == IndexJob.Priority.HIGH;
    // force job if in a batch with a clear job
    boolean force = job.isBatch() && job.getBatch().hasClearJob();
    // don't remove existing if in batch with a clear job
    boolean existingHasPriority = existing != null && existing.isBatch() && existing.getBatch().hasClearJob();

    if (!existingHasPriority && (existing == null || force || higherPriority)) {
      if (existing != null && !force) {
        this.removedJob(existing);
        if (foundInSingleQueue) synchronized (this._singleThreadQueue) { this._singleThreadQueue.remove(existing); }
        else synchronized (this._queue) { this._queue.remove(existing); }
      }
      if (singleThread && this._singleThreadQueue != null) synchronized (this._singleThreadQueue) {
        this._singleThreadQueue.put(job);
      } else synchronized (this._queue) {
        this._queue.put(job);
      }
    } else {
      this.removedJob(job);
    }
  }

  /**
   * Adjust batch counts for the job.
   */
  private void removedJob(IndexJob job) {
    // don't remove last job of batch
    IndexBatch batch = job.getBatch();
    if (batch != null && batch.getCurrentCount() != batch.getTotalDocuments() - 1)
      batch.remove(1);
  }

  // -----------------------------------------------------------------------
  // debounce helper types
  // -----------------------------------------------------------------------

  private static final class PendingDebounce {
    final IndexJob job;
    final boolean singleThreadTarget;
    volatile ScheduledFuture<?> future;

    private PendingDebounce(IndexJob job, boolean singleThreadTarget) {
      this.job = job;
      this.singleThreadTarget = singleThreadTarget;
    }
  }

  private static final class JobKey {
    private final String contentId;
    private final ContentType contentType;
    private final String indexId;
    private final String parameters;

    private final IndexJob.Priority priority;

    private JobKey(String contentId, ContentType contentType,
                   String indexId, String parameters, IndexJob.Priority priority) {
      this.contentId = contentId;
      this.contentType = contentType;
      this.indexId = indexId;
      this.parameters = parameters;
      this.priority = priority;
    }

    static JobKey from(IndexJob job) {
      return new JobKey(
          job.getContentID(),
          job.getContentType(),
          job.getIndex().getIndexID(),
          job.getParamsKey(),
          job.getPriority()
      );
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof JobKey)) return false;
      JobKey other = (JobKey) o;
      return Objects.equals(this.contentId, other.contentId)
          && Objects.equals(this.contentType, other.contentType)
          && Objects.equals(this.indexId, other.indexId)
          && Objects.equals(this.parameters, other.parameters)
          && this.priority == other.priority;
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          this.contentId,
          this.contentType,
          this.indexId,
          this.parameters,
          this.priority
      );
    }
  }
}

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
package org.pageseeder.flint;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.pageseeder.flint.api.ContentType;
import org.pageseeder.flint.api.Index;
import org.pageseeder.flint.api.Requester;

/**
 * A job to run by the IndexManager.
 *
 * <p>Jobs can be of three types: add, update or delete.
 *
 * @author Jean-Baptiste Reure
 * @version 26 February 2010
 */
public class IndexJob implements Comparable<IndexJob> {

  /**
   * Pseudo-type to indicate that the index needs to be cleared.
   */
  private static final ContentType CLEAR_CONTENT_TYPE = new ContentType() {
    @Override
    public String toString() { return "CLEAR"; };
  };
  private static final String CLEAR_CONTENT_ID = "CLEAR";

  /**
   * A list of priorities for IndexJobs.
   */
  public enum Priority {

    /**
     * High priority job (always processed before LOW).
     */
    HIGH,

    /**
     * Low priority job (always processed after HIGH).
     */
    LOW

  };

  private final String _contentid;

  private final ContentType _contenttype;

  /**
   * Job's priority.
   */
  private final Priority _priority;

  /**
   * Index to run the job on.
   */
  private final Index _index;

  /**
   * List of contents to index
   */
  private final Map<String, ContentType> _batchContents;

  /**
   * The initial job's requester.
   */
  private final Requester _requester;

  /**
   * When the job was created.
   */
  private final long _created;

  /**
   * Internal flag to know if the job is finished.
   */
  private boolean finished = false;

  /**
   * The job's ID, generated in the constructor.
   */
  private final String jobId;

  /**
   * Internal flag to know if the job succeeded.
   */
  private boolean success = false;

  /**
   * Private constructor, to build a job, use one of the static methods newAddJob(), newUpdateJob() or newDeleteJob().
   *
   * @param cid     The Content ID
   * @param ctype   The Content type
   * @param i       The Index
   * @param p       The job's priority
   * @param r       the job's requester
   */
  private IndexJob(Map<String, ContentType> contents, Index i, Priority p, Requester r) {
    this._batchContents = new HashMap<>(contents);
    this._contentid = "batch-"+contents.hashCode();
    this._contenttype = null;
    this._priority = p;
    this._requester = r;
    this._index = i;
    this._created = System.nanoTime();
    this.jobId = this._created + "-batch-" + contents.hashCode() + '-' + i.getIndexID() + '-' + r.getRequesterID() + '-' + p.toString();
  }

  /**
   * Private constructor, to build a job, use one of the static methods newAddJob(), newUpdateJob() or newDeleteJob().
   *
   * @param cid     The Content ID
   * @param ctype   The Content type
   * @param i       The Index
   * @param p       The job's priority
   * @param r       the job's requester
   */
  private IndexJob(String cid, ContentType ctype, Index i, Priority p, Requester r) {
    this._batchContents = null;
    this._contentid = cid;
    this._contenttype = ctype;
    this._priority = p;
    this._requester = r;
    this._index = i;
    this._created = System.nanoTime();
    this.jobId = this._created + '-' + cid + '-' + ctype + '-' + i.getIndexID() + '-' + r.getRequesterID() + '-' + p.toString();
  }

  /**
   * Return this job's ID.
   *
   * @return this job's ID.
   */
  protected String getJobID() {
    return this.jobId;
  }

  public boolean isBatch() {
    return this._batchContents != null;
  }

  public String getContentID() {
    return this._contentid;
  }

  public ContentType getContentType() {
    return this._contenttype;
  }

  public Collection<String> getBatchContentIDs() {
    return this._batchContents == null ? null : this._batchContents.keySet();
  }

  public ContentType getBatchContentType(String contentid) {
    return this._batchContents == null ? null : this._batchContents.get(contentid);
  }

  /**
   * Return the Index that this job is to be run on.
   *
   * @return the Index that this job is to be run on.
   */
  public Index getIndex() {
    return this._index;
  }

  /**
   * Return the original job's requester.
   *
   * @return the original job's requester.
   */
  public Requester getRequester() {
    return this._requester;
  }

  /**
   * Return true if this job was launched by the Requester provided.
   *
   * @param request the Requester to check.
   * @return <code>true</code> if this job was launched by the Requester provided;
   *         <code>false</code> otherwise.
   */
  public boolean isForRequester(Requester request) {
    if (this._requester == request) return true;
    return this._requester.getRequesterID().equals(request.getRequesterID());
  }

  /**
   * Return <code>true</code> if this job is running on the provided index.
   *
   * @param index the Index to check
   * @return <code>true</code> if this job is running on the provided index;
   *         <code>false</code> otherwise.
   */
  public boolean isForIndex(Index index) {
    if (this._index == index) return true;
    return this._index != null && this._index.getIndexID().equals(index.getIndexID());
  }

  /**
   * Compare this job to another job.
   *
   * <p>Used to order the jobs by priority in the waiting queue.
   *
   * @param job The job to compare to.
   * @return 0 if both jobs have the same priority;
   *         -1 if this job's priority is HIGH;
   *         1 if this job's priority is LOW;
   */
  @Override
  public int compareTo(IndexJob job) {
    return this._priority == job._priority? Long.compare(this._created, job._created) : this._priority == Priority.HIGH ? -1 : 1;
  }

  /**
   * Set the flag to signify that the job is finished.
   */
  public void finish() {
    this.finished = true;
  }

  /**
   * Indicates whether the job is finished.
   *
   * @return <code>true</code> if the job is finished
   */
  public boolean isFinished() {
    return this.finished;
  }

  /**
   * Set the final status of this job.
   *
   * @param success <code>true</code> if the job succeeded;
   *                <code>false</code> if an error occurred.
   */
  public void setSuccess(boolean success) {
    this.success = success;
  }

  /**
   * Check whether this job was completed successfully or not.
   *
   * @return <code>true</code> if the job was successful;
   *         <code>false</code> otherwise.
   */
  public boolean wasSuccessful() {
    return this.success;
  }

  /**
   * Returns a string with each class attribute value - useful when debugging and logging.
   */
  @Override
  public String toString() {
    return "[IndexJob - contentid:" + this._contentid + " priority:"
        + this._priority + " index:" + this._index + " finished:" + this.finished + " success:" + this.success + "]";
  }

  /**
   * Indicates whether this job is to clear the index.
   *
   * @return <code>true</code> if the content ID for this job is CLEAR;
   *         <code>false</code> otherwise.
   */
  public boolean isClearJob() {
    return CLEAR_CONTENT_ID.equals(this._contentid) && CLEAR_CONTENT_TYPE.equals(this._contenttype);
  }

  // static factory methods ========================================================================

  /**
   * Used to build a new batch job.
   *
   * @param id     The Content ID
   * @param config The Config ID (can be <code>null</code>)
   * @param i      The Index
   * @param p      The job's priority
   * @param r      The job's requester
   *
   * @return the new job
   */
  public static IndexJob newBatchJob(Map<String, ContentType> contents, Index i, Priority p, Requester r) {
    return new IndexJob(contents, i, p, r);
  }

  /**
   * Used to build a new job.
   *
   * @param id     The Content ID
   * @param config The Config ID (can be <code>null</code>)
   * @param i      The Index
   * @param p      The job's priority
   * @param r      The job's requester
   *
   * @return the new job
   */
  public static IndexJob newJob(String contentid, ContentType ctype, Index i, Priority p, Requester r) {
    return new IndexJob(contentid, ctype, i, p, r);
  }

  /**
   * Creates a new job to clear the index.
   *
   * @param index     The Index
   * @param priority  The job's priority
   * @param requester The job's requester
   *
   * @return the new job
   */
  public static IndexJob newClearJob(Index index, Priority priority, Requester requester) {
    return new IndexJob(CLEAR_CONTENT_ID, CLEAR_CONTENT_TYPE, index, priority, requester);
  }

}

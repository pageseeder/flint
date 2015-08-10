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

import java.util.Collections;
import java.util.Map;

import org.pageseeder.flint.api.ContentId;
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
  private static final ContentType CLEAR_CONTENT = new ContentType() {
    @Override
    public String toString() { return "CLEAR"; };
  };

  /**
   * Pseudo content ID used for the index clear job.
   */
  private static final ContentId CLEAR_CONTENT_ID = new ContentId() {
    @Override
    public ContentType getContentType() { return CLEAR_CONTENT; }
    @Override
    public String getID() { return "Clear Index"; }
  };

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

  /**
   * The Content ID.
   */
  private final ContentId _contentID;

  /**
   * The Config.
   */
  private final IndexConfig _config;

  /**
   * Job's priority.
   */
  private final Priority _priority;

  /**
   * Index to run the job on.
   */
  private final Index _index;

  /**
   * Dynamic XSLT parameters
   */
  private final Map<String, String> parameters;

  /**
   * The initial job's requester.
   */
  private final Requester _requester;

  /**
   * When the job was created.
   */
  private final long created;

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
   * @param id      The Content ID
   * @param conf    The Config
   * @param i       The Index
   * @param p       The job's priority
   * @param r       the job's requester
   */
  private IndexJob(ContentId id, IndexConfig conf, Index i, Priority p, Requester r, Map<String, String> params) {
    this._contentID = id;
    this._config = conf;
    this._priority = p;
    this._requester = r;
    this._index = i;
    if (params != null) this.parameters = params;
    else this.parameters = Collections.emptyMap();
    this.created = System.nanoTime();
    this.jobId = this.created + '-' + id.toString() + '-' + (conf == null ? "" : conf.hashCode()) + '-'
        + i.getIndexID() + '-' + r.getRequesterID() + '-' + p.toString();
  }

  /**
   * Return this job's ID.
   *
   * @return this job's ID.
   */
  protected String getJobID() {
    return this.jobId;
  }

  /**
   * Return the content ID used to retrieve the content and the config.
   *
   * @return the content ID.
   */
  public ContentId getContentID() {
    return this._contentID;
  }

  /**
   * Return the config.
   *
   * @return the config
   */
  public IndexConfig getConfig() {
    return this._config;
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
   * Return the dynamic XSLT parameters for this job (unmodifiable list, never <code>null</code>).
   *
   * @return the dynamic XSLT parameters for this job
   */
  public Map<String, String> getParameters() {
    return Collections.unmodifiableMap(this.parameters);
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
    return this._priority == job._priority? Long.compare(this.created, job.created) : this._priority == Priority.HIGH ? -1 : 1;
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
    return "[IndexJob - contentid:" + this._contentID + " priority:"
        + this._priority + " index:" + this._index + " finished:" + this.finished + " success:" + this.success + "]";
  }

  /**
   * Indicates whether this job is to clear the index.
   *
   * @return <code>true</code> if the content ID for this job is CLEAR;
   *         <code>false</code> otherwise.
   */
  public boolean isClearJob() {
    return this.getContentID().equals(CLEAR_CONTENT_ID);
  }

  // static factory methods ========================================================================

  /**
   * Used to build a new job.
   *
   * @param id     The Content ID
   * @param config The Config ID (can be <code>null</code>)
   * @param i      The Index
   * @param p      The job's priority
   * @param r      The job's requester
   * @param params Parameters for use with the job (can be <code>null</code>)
   *
   * @return the new job
   */
  public static IndexJob newJob(ContentId id, IndexConfig config, Index i, Priority p, Requester r, Map<String, String> params) {
    return new IndexJob(id, config, i, p, r, params);
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
    return new IndexJob(CLEAR_CONTENT_ID, null, index, priority, requester, null);
  }

}

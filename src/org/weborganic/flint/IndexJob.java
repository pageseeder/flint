/*
 * This file is part of the Flint library.
 * 
 * For licensing information please see the file license.txt included in the release. A copy of this licence can also be
 * found at http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint;

import java.util.Collections;
import java.util.Map;

import org.weborganic.flint.IndexManager.Priority;
import org.weborganic.flint.content.ContentId;

/**
 * A job to run by the IndexManager. Jobs can be of three types: add, update or delete.
 * 
 * @author Jean-Baptiste Reure
 * @version 26 February 2010
 */
public class IndexJob implements Comparable<IndexJob> {

  /**
   * The Content ID.
   */
  private final ContentId contentID;

  /**
   * The Config.
   */
  private final IndexConfig config;

  /**
   * Job's priority.
   */
  private final Priority priority;

  /**
   * Index to run the job on.
   */
  private final Index index;
  
  /**
   * Dynamic XSLT parameters
   */
  private final Map<String, String> parameters;

  /**
   * The initial job's requester.
   */
  private final Requester requester;

  /**
   * Internal flag to know if the job is finished.
   */
  private boolean finished = false;

  /**
   * The job's ID, generated in the constructor.
   */
  private final String jobId;

  /**
   * Private constructor, to build a job, use one of the static methods newAddJob(), newUpdateJob() or newDeleteJob().
   * 
   * @param t       The Content Type
   * @param id      The Content ID
   * @param conf    The Config
   * @param i       The Index
   * @param p       The job's priority
   * @param r       the job's requester
   * @param thetype the job's type
   */
  @SuppressWarnings("unchecked")
  private IndexJob(ContentId id, IndexConfig conf, Index i, Priority p, Requester r, Map<String, String> params) {
    this.contentID = id;
    this.config = conf;
    this.priority = p;
    this.requester = r;
    this.index = i;
    this.parameters = params == null ? (Map<String, String>) Collections.EMPTY_MAP : params;
    this.jobId = System.currentTimeMillis() + "-" + id.toString() + "-" + conf.hashCode() + "-"
        + i.getIndexID() + "-" + r.getRequesterID() + "-" + p.toString();
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
    return this.contentID;
  }

  /**
   * Return the config
   * 
   * @return the config
   */
  public IndexConfig getConfig() {
    return this.config;
  }

  /**
   * Return the Index that this job is to be run on.
   * 
   * @return the Index that this job is to be run on.
   */
  public Index getIndex() {
    return this.index;
  }

  /**
   * Return the original job's requester.
   * 
   * @return the original job's requester.
   */
  public Requester getRequester() {
    return this.requester;
  }
  
  /**
   * Return the dynamic XSLT parameters for this job (unmodifiable list, never null)
   * 
   * @return the dynamic XSLT parameters for this job
   */
  public Map<String, String> getParameters() {
    return Collections.unmodifiableMap(this.parameters);
  }

  /**
   * Return true if this job was launched by the Requester provided.
   * 
   * @param req the Requester to check.
   * @return <code>true</code> if this job was launched by the Requester provided;
   *         <code>false</code> otherwise.
   */
  public boolean isForRequester(Requester req) {
    return this.requester.getRequesterID().equals(req.getRequesterID());
  }

  /**
   * Return <code>true</code> if this job is running on the provided index.
   * 
   * @param ind the Index to check
   * @return <code>true</code> if this job is running on the provided index;
   *         <code>false</code> otherwise.
   */
  public boolean isForIndex(Index ind) {
    return this.index.getIndexID().equals(ind.getIndexID());
  }

  /**
   * Compare this job to another job.
   * 
   * <p>Used to order the jobs by priority in the waiting queue.
   */
  public int compareTo(IndexJob other) {
    return this.priority == other.priority? 0 : this.priority == Priority.HIGH? 1 : -1;
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
   * Useful when debugging and logging
   */
  @Override
  public String toString() {
    return "[IndexJob - contentid:" + this.contentID + " priority:"
        + this.priority + " index:" + this.index + " finished:" + this.finished + "]";
  }

  /**
   * Used to build a new job.
   * 
   * @param t      The Content Type
   * @param id     The Content ID
   * @param confID The Config ID (can be <code>null</code>)
   * @param i      The Index
   * @param p      The job's priority
   * @param r      The job's requester
   * 
   * @return the new job
   */
  public static IndexJob newJob(ContentId id, IndexConfig config, Index i, Priority p, Requester r, Map<String, String> params) {
    return new IndexJob(id, config, i, p, r, params);
  }

}

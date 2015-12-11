package org.pageseeder.flint;

import java.util.Date;

public class IndexBatch {
  private final String index;
  private int totalCount;
  private boolean computed = false;
  private boolean started = false;
  private int currentCount = 0;
  private long createTime;
  private long startTime;
  private long computeTime;
  private long indexTime;
  private long totalTime;
  public IndexBatch(String idx) {
    this.createTime = System.currentTimeMillis();
    this.index = idx;
    this.totalCount = 0;
  }
  public IndexBatch(String idx, int total) {
    this.createTime = System.currentTimeMillis();
    this.index = idx;
    this.totalCount = total;
    this.computeTime = -1;
    this.computed = true;
  }
  protected synchronized void startIndexing() {
    this.started = true;
    this.startTime = System.currentTimeMillis();
  }
  public synchronized void increaseTotal() {
    this.totalCount++;
  }
  protected synchronized void increaseCurrent() {
    this.currentCount++;
    if (isFinished()) {
      long now = System.currentTimeMillis();
      this.indexTime = now - this.startTime;
      this.totalTime = now - this.createTime;
    }
  }
  public synchronized void setComputed() {
    this.computed = true;
    this.computeTime = System.currentTimeMillis() - this.createTime;
  }
  public synchronized boolean isComputed() {
    return this.computed;
  }
  public synchronized boolean isStarted() {
    return this.started;
  }
  public synchronized boolean isFinished() {
    return this.computed && this.currentCount >= this.totalCount;
  }
  public int getTotalDocuments() {
    return this.totalCount;
  }
  public Date getCreation() {
    return new Date(this.createTime);
  }
  public Date getStart() {
    return new Date(this.startTime);
  }
  public long getIndexingDuration() {
    return this.indexTime;
  }
  public long getComputingDuration() {
    return this.computeTime;
  }
  public long getTotalDuration() {
    return this.totalTime;
  }
  public String getIndex() {
    return this.index;
  }
}
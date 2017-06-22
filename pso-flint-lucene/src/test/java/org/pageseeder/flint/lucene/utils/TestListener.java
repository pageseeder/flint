package org.pageseeder.flint.lucene.utils;

import org.pageseeder.flint.indexing.IndexBatch;
import org.pageseeder.flint.indexing.IndexJob;
import org.pageseeder.flint.indexing.IndexListener;

public class TestListener implements IndexListener {

  @Override
  public void startBatch(IndexBatch batch) {
    System.out.println("Starting batch job");
  }

  @Override
  public void endBatch(IndexBatch batch) {
    System.out.println("Ending batch job");
  }

  @Override
  public void startJob(IndexJob job) {
    System.out.println("Starting job for "+job.getContentID());
  }

  @Override
  public void warn(IndexJob job, String message) {
    System.err.println("Warning with job for "+job.getContentID()+": "+message);
  }

  @Override
  public void error(IndexJob job, String message, Throwable throwable) {
    System.err.println("Error with job for "+(job == null ? "no-job" : job.getContentID())+": "+message);
    if (throwable != null) throwable.printStackTrace();
  }

  @Override
  public void endJob(IndexJob job) {
    System.out.println("Ending job for "+job.getContentID());
  }

}

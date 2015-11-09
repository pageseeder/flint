package org.pageseeder.flint.local;

import org.pageseeder.flint.IndexJob;
import org.pageseeder.flint.api.IndexListener;

public class TestListener implements IndexListener {

  @Override
  public void startBatch() {
    System.out.println("Starting batch job");
  }

  @Override
  public void endBatch() {
    System.out.println("Ending batch job");
  }

  @Override
  public void startJob(IndexJob job) {
    System.out.println("Starting job with content "+job.getContentID());
  }

  @Override
  public void warn(IndexJob job, String message) {
    System.err.println("Warning with job with content "+job.getContentID()+": "+message);
  }

  @Override
  public void error(IndexJob job, String message, Throwable throwable) {
    System.err.println("Error with job with content "+(job == null ? "no-job" : job.getContentID())+": "+message);
    if (throwable != null) throwable.printStackTrace();
  }

  @Override
  public void endJob(IndexJob job) {
    System.out.println("Ending job with content "+job.getContentID());
  }

}

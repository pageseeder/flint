/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.log;

import org.weborganic.flint.IndexJob;
import org.weborganic.flint.api.IndexListener;

/**
 * A listener implementation that does nothing.
 *
 * <p>It will happily ignore anything that is reported to it.
 *
 * <p>Access the singleton instance with {@link #getInstance()}.
 *
 * @author Christophe Lauret
 * @version 27 February 2013
 */
public final class NoOpListener implements IndexListener {

  /**
   * Sole instance.
   */
  private static final NoOpListener SINGLETON = new NoOpListener();

  /**
   * Returns the singleton instance.
   *
   * @return the singleton instance
   */
  public static NoOpListener getInstance() {
    return SINGLETON;
  }

  /**
   * Singleton instance.
   */
  private NoOpListener() {
  }

  @Override
  public void error(IndexJob job, String error, Throwable throwable) {
  }

  @Override
  public void warn(IndexJob job, String warning) {
  }

  @Override
  public void endJob(IndexJob job) {
  }

  @Override
  public void startJob(IndexJob job) {
  }

  @Override
  public void startBatch() {
  }

  @Override
  public void endBatch() {
  }

}

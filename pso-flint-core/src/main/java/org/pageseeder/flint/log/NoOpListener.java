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
package org.pageseeder.flint.log;

import org.pageseeder.flint.indexing.IndexBatch;
import org.pageseeder.flint.indexing.IndexJob;
import org.pageseeder.flint.indexing.IndexListener;

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
  public void startBatch(IndexBatch batch) {
  }

  @Override
  public void endBatch(IndexBatch batch) {
  }

}

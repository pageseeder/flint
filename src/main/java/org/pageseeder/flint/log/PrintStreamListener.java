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

import java.io.PrintStream;

import org.pageseeder.flint.IndexJob;
import org.pageseeder.flint.api.IndexListener;

/**
 * A logger implementation that reports events to a <code>PrintStream</code>.
 *
 * <p>This implementation can be used to report on the standard or error output.
 *
 * <p>For example:
 * <pre>
 *   PrintStreamLogger logger = PrintStreamLogger(System.err);
 * </pre>
 *
 * <p>This logger will generally print each message on one line prefixing them by "[<i>LEVEL</i>]".
 *
 * <p>It will also print the stack trace for each reported error.
 *
 * @author Christophe Lauret
 * @version 27 May 2010
 */
public final class PrintStreamListener implements IndexListener {

  /**
   * Sole instance.
   */
  private final PrintStream _stream;

  /**
   * Creates a new logger on the specified print stream.
   *
   * @param stream Where the logger should print.
   */
  public PrintStreamListener(PrintStream stream) {
    this._stream = stream;
  }

  @Override
  public void warn(IndexJob job, String message) {
    this._stream.println("[WARN ] "+message+' '+job.toString());
  }

  @Override
  public void error(IndexJob job, String message, Throwable throwable) {
    this._stream.println("[ERROR] "+message+' '+job.toString());
    if (throwable != null) {
      throwable.printStackTrace(this._stream);
    }
  }

  @Override
  public void endJob(IndexJob job) {
    this._stream.println("[JOB END] "+job.toString());
  }

  @Override
  public void startJob(IndexJob job) {
    this._stream.println("[JOB START] "+job.toString());
  }

  @Override
  public void startBatch() {
    this._stream.println("[JOB START] ");
  }

  @Override
  public void endBatch() {
    this._stream.println("[BATCH END] ");
  }
}

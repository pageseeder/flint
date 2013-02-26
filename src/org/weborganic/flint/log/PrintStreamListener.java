/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.log;

import java.io.PrintStream;

import org.weborganic.flint.IndexJob;
import org.weborganic.flint.api.IndexListener;

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
    if (throwable != null)
      throwable.printStackTrace(this._stream);
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

/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.log;

import org.weborganic.flint.IndexJob;

/**
 * A listener for indexing job in order to report on the event.
 *
 * @author Jean-Baptiste Reure
 * @version 26 May 2010
 */
public interface FlintListener {

  /**
   * To log an information message.
   *
   * @param info the information message
   */
  void info(String info);

  /**
   * To log an information message.
   *
   * @param info the information message
   * @param throwable the exception
   */
  void info(String info, Throwable throwable);

  /**
   * To log a warning message.
   *
   * @param warning the warning message
   */
  void warn(String warning);

  /**
   * To log a warning message.
   *
   * @param warning the warning message
   * @param throwable the exception
   */
  void warn(String warning, Throwable throwable);

  /**
   * To log a debug message.
   *
   * @param debug the debug message
   */
  void debug(String debug);

  /**
   * To log a debug message.
   *
   * @param debug the debug message
   * @param throwable the exception
   */
  void debug(String debug, Throwable throwable);

  /**
   * When an error occurred.
   *
   * @param error the error message
   */
  void error(String error);

  /**
   * When an error occurred.
   *
   * @param error the error message
   * @param throwable the exception
   */
  void error(String error, Throwable throwable);

  /**
   * To log an information message attached to an indexing job.
   *
   * @param job       the job concerned
   * @param message   the information message
   */
  void info(IndexJob job, String message);

  /**
   * To log an information message attached to an indexing job.
   *
   * @param job       the job concerned
   * @param message   the information message
   * @param throwable an exception
   */
  void info(IndexJob job, String message, Throwable throwable);

  /**
   * To log a warning message attached to an indexing job.
   *
   * @param job       the job concerned
   * @param message   the warning message
   */
  void warn(IndexJob job, String message);

  /**
   * To log a warning message attached to an indexing job.
   *
   * @param job       the job concerned
   * @param message   the warning message
   * @param throwable an exception
   */
  void warn(IndexJob job, String message, Throwable throwable);

  /**
   * To log a debug message attached to an indexing job.
   *
   * @param job       the job concerned
   * @param message   the warning message
   */
  void debug(IndexJob job, String message);

  /**
   * To log a debug message attached to an indexing job.
   *
   * @param job       the job concerned
   * @param message   the warning message
   * @param throwable an exception
   */
  void debug(IndexJob job, String message, Throwable throwable);

  /**
   * When an error occurred during to an indexing job.
   *
   * @param job       the job concerned
   * @param message   the error message
   */
  void error(IndexJob job, String message);

  /**
   * When an error occurred during to an indexing job.
   *
   * @param job       the job concerned
   * @param message   the error message
   * @param throwable the exception
   */
  void error(IndexJob job, String message, Throwable throwable);

  /**
   * When an indexing job is started.
   *
   * @param job the job that just started
   */
  void startJob(IndexJob job);

  /**
   * When an indexing job was completed.
   *
   * @param job the job completed
   */
  void finishJob(IndexJob job);

}

/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.log;

import org.weborganic.flint.IndexJob;
import org.weborganic.flint.IndexListener;

/**
 * A listener for indexing job in order to report on the event.
 *
 * @deprecated This interface will be replaced by the quieter {@link IndexListener}
 *
 * @author Jean-Baptiste Reure
 * @author Christophe Lauret
 *
 * @version 8 February 2013
 */
@Deprecated
public interface FlintListener extends IndexListener {

  /**
   * To log a debug message attached to an indexing job.
   *
   * @deprecated debug messages are logged instead
   *
   * @param job       the job concerned
   * @param message   the warning message
   */
  @Deprecated
  void debug(IndexJob job, String message);

  /**
   * To log a debug message attached to an indexing job.
   *
   * @deprecated debug messages are logged instead
   *
   * @param job       the job concerned
   * @param message   the warning message
   * @param throwable an exception
   */
  @Deprecated
  void debug(IndexJob job, String message, Throwable throwable);

  /**
   * To log a debug message.
   *
   * @deprecated debug messages are logged instead
   *
   * @param debug the debug message
   */
  @Deprecated
  void debug(String debug);

  /**
   * To log a debug message.
   *
   * @deprecated debug messages are logged instead
   *
   * @param debug the debug message
   * @param throwable the exception
   */
  @Deprecated
  void debug(String debug, Throwable throwable);

  /**
   * To log an information message.
   *
   * @deprecated info messages are logged instead
   *
   * @param info the information message
   */
  @Deprecated
  void info(String info);

  /**
   * To log an information message.
   *
   * @deprecated info messages are logged instead
   *
   * @param info the information message
   * @param throwable the exception
   */
  @Deprecated
  void info(String info, Throwable throwable);

  /**
   * To log an information message attached to an indexing job.
   *
   * @deprecated info messages are logged instead
   *
   * @param job       the job concerned
   * @param message   the information message
   */
  @Deprecated
  void info(IndexJob job, String message);

  /**
   * To log an information message attached to an indexing job.
   *
   * @deprecated info messages are logged instead
   *
   * @param job       the job concerned
   * @param message   the information message
   * @param throwable an exception
   */
  @Deprecated
  void info(IndexJob job, String message, Throwable throwable);

  /**
   * To log a warning message.
   *
   * @deprecated Use logger instead
   *
   * @param warning the warning message
   */
  @Deprecated
  void warn(String warning);

  /**
   * To log a warning message.
   *
   * @deprecated Use logger instead
   *
   * @param warning the warning message
   * @param throwable the exception
   */
  @Deprecated
  void warn(String warning, Throwable throwable);

  /**
   * To log a warning message attached to an indexing job.
   *
   * @param job       the job concerned
   * @param message   the warning message
   * @param throwable an exception
   */
  void warn(IndexJob job, String message, Throwable throwable);

  /**
   * When an indexing job was completed.
   *
   * @deprecated replaced endJob
   *
   * @param job the job completed
   */
  @Deprecated
  public void finishJob(IndexJob job);

  /**
   * When an error occurred.
   *
   * @deprecated Use logger instead
   *
   * @param error the error message
   */
  @Deprecated
  void error(String error);

  /**
   * When an error occurred.
   *
   * @deprecated Use logger instead
   *
   * @param error the error message
   * @param throwable the exception
   */
  @Deprecated
  void error(String error, Throwable throwable);

  /**
   * When an error occurred during to an indexing job.
   *
   * @deprecated Use logger or {@link #error(IndexJob, String, Throwable)} instead
   *
   * @param job       the job concerned
   * @param message   the error message
   */
  @Deprecated
  void error(IndexJob job, String message);

}

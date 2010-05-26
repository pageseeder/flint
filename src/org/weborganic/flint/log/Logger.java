package org.weborganic.flint.log;

import org.weborganic.flint.Index;
import org.weborganic.flint.Requester;

/**
 * A listener for indexing job in order to report on the event.
 * 
 * @author Jean-Baptiste Reure
 * @version 26 May 2010
 */
public interface Logger {

  /**
   * To log an information message.
   * 
   * @param info the information message
   */
  void info(String info);

  /**
   * To log a warning message.
   * 
   * @param warning the warning message
   */
  void warn(String warning);

  /**
   * To log a debug message.
   * 
   * @param debug the debug message
   */
  void debug(String debug);

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
   * @param requester the requester
   * @param index     the index concerned
   * @param message   the information message
   */
  void indexInfo(Requester requester, Index index, String message);
  // XXX Why not just name this method info()?

  /**
   * To log a warning message attached to an indexing job.
   * 
   * @param requester the requester
   * @param index     the index concerned
   * @param message   the warning message
   */
  void indexWarn(Requester requester, Index index, String message);
  // XXX Why not just name this method warn()?

  /**
   * To log a debug message attached to an indexing job.
   * 
   * @param requester the requester
   * @param index     the index concerned
   * @param message   the warning message
   */
  void indexDebug(Requester requester, Index index, String message);
  // XXX Why not just name this method debug()?

  /**
   * When an error occurred during to an indexing job.
   * 
   * @param requester the requester
   * @param index     the index concerned
   * @param message   the error message
   * @param throwable the exception
   */
  void indexError(Requester requester, Index index, String message, Throwable throwable);
  // XXX Why not just name this method error()?

}

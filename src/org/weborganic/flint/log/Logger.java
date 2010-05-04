package org.weborganic.flint.log;

import org.weborganic.flint.Index;
import org.weborganic.flint.Requester;

public interface Logger {
  /**
   * To log an information message
   * 
   * @param info the information message
   */
  public void info(String info);
  
  /**
   * To log a warning message
   * 
   * @param warning the warning message
   */
  public void warn(String warning);
  
  /**
   * To log a debug message
   * 
   * @param debug the debug message
   */
  public void debug(String debug);
  
  /**
   * When an error occurred
   * 
   * @param error the error message
   * @param throwable the exception
   */
  public void error(String error, Throwable throwable);
  
  /**
   * To log an information message attached to an indexing job
   * 
   * @param info the information message
   */
  public void indexInfo(Requester r, Index i, String info);
  
  /**
   * To log a warning message attached to an indexing job
   * 
   * @param warning the warning message
   */
  public void indexWarn(Requester r, Index i, String warning);
  
  /**
   * To log a debug message attached to an indexing job
   * 
   * @param debug the debug message
   */
  public void indexDebug(Requester r, Index i, String debug);
  
  /**
   * When an error occurred during to an indexing job
   * 
   * @param error the error message
   * @param throwable the exception
   */
  public void indexError(Requester r, Index i, String error, Throwable throwable);
}

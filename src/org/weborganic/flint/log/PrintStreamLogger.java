package org.weborganic.flint.log;

import java.io.PrintStream;

import org.weborganic.flint.Index;
import org.weborganic.flint.Requester;

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
public final class PrintStreamLogger implements Logger {

  /**
   * Sole instance.
   */
  private final PrintStream _stream;

  /**
   * Creates a new logger on the specified print stream.
   * 
   * @param stream Where the logger should print.
   */
  public PrintStreamLogger(PrintStream stream) {
    this._stream = stream;
  }

  /**
   * {@inheritDoc}
   */
  public void debug(String message) {
    this._stream.println("[DEBUG] "+message);
  }

  /**
   * {@inheritDoc}
   */
  public void info(String message) {
    this._stream.println("[INFO ] "+message);
  }

  /**
   * {@inheritDoc}
   */
  public void warn(String message) {
    this._stream.println("[WARN ] "+message);
  }

  /**
   * {@inheritDoc}
   */
  public void error(String message, Throwable throwable) {
    this._stream.println("[ERROR] "+message);
    if (throwable != null)
      throwable.printStackTrace(this._stream);
  }

  /**
   * {@inheritDoc}
   */
  public void indexDebug(Requester r, Index i, String message) {
    this._stream.println("[ERROR] "+message+" "+toString(r, i));
  }

  /**
   * {@inheritDoc}
   */
  public void indexInfo(Requester r, Index i, String message) {
    this._stream.println("[INFO ] "+message+" "+toString(r, i));
  }

  /**
   * {@inheritDoc}
   */
  public void indexWarn(Requester r, Index i, String message) {
    this._stream.println("[WARN ] "+message+" "+toString(r, i));
  }

  /**
   * {@inheritDoc}
   */
  public void indexError(Requester r, Index i, String message, Throwable throwable) {
    this._stream.println("[ERROR] "+message+" "+toString(r, i));
    if (throwable != null)
      throwable.printStackTrace(this._stream);
  }

  // Private helpers -----------------------------------------------------------------------------

  /**
   * Returns the requester and index as a string handling cases when either of them is 
   * <code>null</code>.
   * 
   * @param requester The requester (may be <code>null</code>).
   * @param index     The index (may be <code>null</code>).
   * 
   * @return The string as <code>"(requester=<i>RequesterID</i>, index=<i>IndexID</i>)"</code>
   */
  private String toString(Requester requester, Index index) {
    return "(requester="+(requester != null? requester.getRequesterID(): "null")
             +", index="+(index != null? index.getIndexID(): "null")+")";
  }
}

package org.weborganic.flint.log;

import org.weborganic.flint.Index;
import org.weborganic.flint.Requester;

/**
 * A logger implementation that remains silent.
 * 
 * <p>It will happily ignore anything that is reported to it.
 * 
 * @author Christophe Lauret
 * @version 26 May 2010
 */
public final class SilentLogger implements Logger {

  /**
   * sole instnace.
   */
  private static final SilentLogger SINGLETON = new SilentLogger();

  /**
   * 
   * @return
   */
  public static SilentLogger getInstance() {
    return SINGLETON;
  }

  /**
   * Singleton instance.
   */
  private SilentLogger(){}
  
  /**
   * {@inheritDoc}
   */
  public void debug(String debug) {
  }

  /**
   * {@inheritDoc}
   */
  public void error(String error, Throwable throwable) {
  }

  /**
   * {@inheritDoc}
   */
  public void indexDebug(Requester r, Index i, String debug) {
  }

  /**
   * {@inheritDoc}
   */
  public void indexError(Requester r, Index i, String error, Throwable throwable) {
  }

  /**
   * {@inheritDoc}
   */
  public void indexInfo(Requester r, Index i, String info) {
  }

  /**
   * {@inheritDoc}
   */
  public void indexWarn(Requester r, Index i, String warning) {
  }

  /**
   * {@inheritDoc}
   */
  public void info(String info) {
  }

  /**
   * {@inheritDoc}
   */
  public void warn(String warning) {
  }

}

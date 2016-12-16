package org.pageseeder.flint.solr;

public class SolrFlintException extends Exception {

  private static final long serialVersionUID = 1L;

  private boolean cannotConnect = false;

  public SolrFlintException() {
  }

  public SolrFlintException(boolean cantConnect) {
    this.cannotConnect = cantConnect;
  }

  public SolrFlintException(String message) {
    super(message);
  }

  public SolrFlintException(Throwable cause) {
    super(cause);
  }

  public SolrFlintException(String message, Throwable cause) {
    super(message, cause);
  }

  public boolean cannotConnect() {
    return this.cannotConnect;
  }
}

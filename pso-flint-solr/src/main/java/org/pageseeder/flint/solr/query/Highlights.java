package org.pageseeder.flint.solr.query;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class Highlights {
  protected final String _field;
  protected int snippet = 2;
  protected int fragsize = 200;
  protected BiConsumer<? super String, ? super Map<String, List<String>>> consumer = null;
  protected List<DocumentHighlight> results = null;
  protected String pre = null;
  protected String post = null;
  protected boolean fieldMustMatch = false;

  public Highlights(String field) {
    this._field = field;
  }

  /**
   * @param snippet the snippet to set
   */
  public Highlights snippet(int snippet) {
    this.snippet = snippet;
    return this;
  }

  /**
   * @param fragsize the fragsize to set
   */
  public Highlights fragsize(int fragsize) {
    this.fragsize = fragsize;
    return this;
  }

  /**
   * @param consumer the consumer to set
   */
  public Highlights consumer(BiConsumer<? super String, ? super Map<String, List<String>>> consumer) {
    this.consumer = consumer;
    return this;
  }

  /**
   * @param results the results to set
   */
  public Highlights results(List<DocumentHighlight> results) {
    this.results = results;
    return this;
  }

  /**
   * @param pre the pre to set
   */
  public Highlights pre(String pre) {
    this.pre = pre;
    return this;
  }

  /**
   * @param post the post to set
   */
  public Highlights post(String post) {
    this.post = post;
    return this;
  }

  /**
   * @param fieldMustMatch the fieldMustMatch to set
   */
  public Highlights fieldMustMatch(boolean fieldMustMatch) {
    this.fieldMustMatch = fieldMustMatch;
    return this;
  }
  
}
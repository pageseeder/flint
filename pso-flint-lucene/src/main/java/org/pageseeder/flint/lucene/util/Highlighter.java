package org.pageseeder.flint.lucene.util;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Class used to create highlighted extract for a search.
 * By default, the match is wrapped in a {@code <term>} element.
 */
public class Highlighter {

  private final static Logger LOGGER = LoggerFactory.getLogger(org.apache.lucene.search.highlight.Highlighter.class);

  private final Query _query;
  private final IndexReader _reader;
  private final Analyzer _analyzer;

  private String markerName = "term";

  private boolean escape = true;

  public Highlighter(Query query, Analyzer analyzer) {
    this(query, null, analyzer);
  }

  public Highlighter(Query query, IndexReader reader, Analyzer analyzer) {
    this._query = query;
    this._reader = reader;
    this._analyzer = analyzer;
  }

  public void setMarkerTag(String name) {
    this.markerName = name;
  }

  /**
   * @param esc if the highlighted text should be XML escaped
   */
  public void setEscape(boolean esc) {
    this.escape = esc;
  }

  public String highlight(String field, String text, int length) {
    try {
      String h = createHighlighter(field, length).getBestFragment(this._analyzer, field, text);
      return h == null ? null : h.trim();
    } catch (IOException | InvalidTokenOffsetsException ex) {
      LOGGER.error("Failed to highlight content for field {}", field, ex);
    }
    return null;
  }

  public String[] highlights(String field, String text, int nbHighlihghts, int length) {
    try {
      return createHighlighter(field, length).getBestFragments(this._analyzer, field, text, nbHighlihghts);
    } catch (IOException | InvalidTokenOffsetsException ex) {
      LOGGER.error("Failed to highlight content for field {}", field, ex);
    }
    return null;
  }

  private org.apache.lucene.search.highlight.Highlighter createHighlighter(String field, int length) {
    QueryScorer scorer = new QueryScorer(this._query, this._reader, field);
    Formatter formatter = new SimpleHTMLFormatter("<"+this.markerName+">", "</"+this.markerName+">");
    org.apache.lucene.search.highlight.Highlighter highlighter;
    if (this.escape)
      highlighter = new org.apache.lucene.search.highlight.Highlighter(formatter, new SimpleHTMLEncoder(), scorer);
    else
      highlighter = new org.apache.lucene.search.highlight.Highlighter(formatter, scorer);
    highlighter.setTextFragmenter(new SimpleSpanFragmenter(scorer, length));
    highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);
    return highlighter;
  }
}

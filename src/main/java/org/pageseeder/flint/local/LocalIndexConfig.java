package org.pageseeder.flint.local;

import java.io.File;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.pageseeder.flint.content.DeleteRule;

public abstract class LocalIndexConfig {

  public abstract File getContent();
  public abstract File getIndexLocation();
  public Map<String, String> getParameters(File file) {
    return null;
  };
  public DeleteRule getDeleteRule(File file) {
    return null;
  }
  public Analyzer getAnalyzer() {
    return new StandardAnalyzer();
  }
}

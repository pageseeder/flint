package org.pageseeder.flint.berlioz.model;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;

public class DefaultAnalyzerFactory implements AnalyzerFactory {
  @Override
  public Analyzer getAnalyzer() {
    return getAnalyzer(null);
  }

  @Override
  public Analyzer getAnalyzer(IndexDefinition definition) {
    if (definition == null || definition.withStopWords()) return new StandardAnalyzer();
    return new StandardAnalyzer(CharArraySet.EMPTY_SET);
  }

}

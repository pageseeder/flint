/*
 * This file is part of the Flint library.
 * 
 * For licensing information please see the file license.txt included in the release. A copy of this licence can also be
 * found at http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordTokenizer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * Implementation of Index, uses a File object to build a Lucene index.
 * 
 * @author Jean-Baptiste Reure
 * @version 26 February 2010
 * 
 * @deprecated {@link org.weborganic.flint.index.DirectoryIndex}
 * 
 */
public class KeywordsIndex implements Index {

  /**
   * The Index Directory object.
   */
  private final Directory indexDirectory;

  /**
   * The analyser used in this default index.
   */
  private PerFieldAnalyzerWrapper analyser = null;

  /**
   * The unique ID.
   */
  private final String id;

  /**
   * List of keywords.
   */
  private final List<String> keywords;

  /**
   * Simple constructor that builds a new Index from the provided directory.
   * 
   * <p>Will throw an exception if the folder does not contain a valid Lucene index
   * 
   * @param dir the directory containing the Lucene index.
   */
  public KeywordsIndex(File dir) {
    this(dir, null);
  }

  /**
   * Simple constructor that builds a new Index from the provided directory. 
   * 
   * <p>Will throw an exception if the folder does not contain a valid Lucene index
   * 
   * @param dir the directory containing the Lucene index
   * @param keywordsList list of keywords (can be null)
   */
  public KeywordsIndex(File dir, List<String> keywordsList) {
    try {
      this.indexDirectory = FSDirectory.open(dir);
    } catch (IOException e) {
      throw new IllegalArgumentException("Unable to use directory " + dir.getAbsolutePath() + " as an Index: "
          + e.getMessage());
    }
    this.id = dir.getAbsolutePath();
    if (keywordsList == null)
      this.keywords = new ArrayList<String>();
    else
      this.keywords = new ArrayList<String>(keywordsList);
  }

  /**
   * Define a new keyword for this index
   * 
   * @param keyword the name of the field which is a keyword
   */
  public void addKeyword(String keyword) {
    if (!this.keywords.contains(keyword)) {
      this.keywords.add(keyword);
      this.analyser.addAnalyzer(keyword, new Analyzer() {
        @Override
        public TokenStream tokenStream(String field, Reader reader) {
          return new LowerCaseFilter(new KeywordTokenizer(reader));
        }
      });
    }
  }

  public Directory getIndexDirectory() {
    return this.indexDirectory;
  }

  public PerFieldAnalyzerWrapper getAnalyzer() {
    if (this.analyser == null) {
      // TODO make a static variable for the version??
      this.analyser = new PerFieldAnalyzerWrapper(new StandardAnalyzer(IndexManager.LUCENE_VERSION));
    }
    return this.analyser;
  }

  public String getIndexID() {
    return this.id;
  }

}

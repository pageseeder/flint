package org.pageseeder.flint.lucene.query;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.search.Query;
import org.junit.Assert;
import org.junit.Test;

public class QueriesTest {

  @Test
  public void testToQuery() throws IOException {
    Analyzer analyzer = new WhitespaceAnalyzer(); // include stopwords so AND is actually a valid search word

    // simple one word
    Query q = Queries.parseToQuery("field", "value", analyzer);
    Assert.assertEquals("field:value", q.toString());

    // two words
    q = Queries.parseToQuery("field", "value1 value2", analyzer);
    Assert.assertEquals("field:value1 field:value2", q.toString());

    // phrase
    q = Queries.parseToQuery("field", "\"value1 value2\"", analyzer);
    Assert.assertEquals("field:\"value1 value2\"", q.toString());

    // AND
    q = Queries.parseToQuery("field", "value1 AND value2", analyzer);
    Assert.assertEquals("+field:value1 +field:value2", q.toString());

    // OR
    q = Queries.parseToQuery("field", "value1 OR value2", analyzer);
    Assert.assertEquals("field:value1 field:value2", q.toString());

    // parenthesis
    q = Queries.parseToQuery("field", "value1 AND (value2 OR value3)", analyzer);
    Assert.assertEquals("+field:value1 +(field:value2 field:value3)", q.toString());

    // parenthesis
    q = Queries.parseToQuery("field", "value1 AND (value2 value3)", analyzer);
    Assert.assertEquals("+field:value1 +(field:value2 field:value3)", q.toString());

    // mix
    q = Queries.parseToQuery("field", "value1 AND (value2 AND value3)", analyzer);
    Assert.assertEquals("+field:value1 +(+field:value2 +field:value3)", q.toString());

    // mix
    q = Queries.parseToQuery("field", "value1 OR (value2 AND value3)", analyzer);
    Assert.assertEquals("field:value1 (+field:value2 +field:value3)", q.toString());

    // mix
    q = Queries.parseToQuery("field", "\"value1 AND value2\"", analyzer);
    Assert.assertEquals("field:\"value1 AND value2\"", q.toString()); 
  }

  @Test
  public void testToQuery2() {
    Analyzer analyzer = new TestAnalyzer();

    // tokenized
    Query q = Queries.parseToQuery("field", "value1 value2", analyzer);
    Assert.assertEquals("field:value1 value2", q.toString());

    // tokenized
    q = Queries.parseToQuery("field", "\"value1 value2\"", analyzer);
    Assert.assertEquals("field:\"value1 value2\"", q.toString());

    // not tokenized
    q = Queries.parseToQuery("field-tokenized", "value1 value2", analyzer);
    Assert.assertEquals("field-tokenized:value1 field-tokenized:value2", q.toString());

    // not tokenized
    q = Queries.parseToQuery("field-tokenized", "\"value1 value2\"", analyzer);
    Assert.assertEquals("field-tokenized:\"value1 value2\"", q.toString());

  }

  /**
   * A simple analyzer that uses a KeywordTokenizer filtered with a LowerCaseFilter
   *
   * @author Jean-Baptiste Reure
   * @version 13 April 2011
   */
  private static final class TestAnalyzer extends Analyzer {

    public TestAnalyzer() {
      super(new ReuseStrategy() {
        @Override
        public void setReusableComponents(Analyzer analyzer, String fieldName, TokenStreamComponents components) {
        }
        @Override
        public TokenStreamComponents getReusableComponents(Analyzer analyzer, String fieldName) {
          return null;
        }
      });
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
      // tokenizer
      Tokenizer source = fieldName.endsWith("-tokenized") ? new StandardTokenizer() : new KeywordTokenizer();
      // lower case
      TokenStream results = new LowerCaseFilter(source);
      // standard
      results = new StandardFilter(results);
      return new TokenStreamComponents(source, results);
    }
    
  }
}

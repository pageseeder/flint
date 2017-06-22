package org.pageseeder.flint.lucene.query;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
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
}

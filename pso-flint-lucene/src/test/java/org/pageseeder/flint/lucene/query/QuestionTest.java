package org.pageseeder.flint.lucene.query;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.hamcrest.CoreMatchers;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.Requester;
import org.pageseeder.flint.catalog.Catalog;
import org.pageseeder.flint.catalog.Catalogs;
import org.pageseeder.flint.content.Content;
import org.pageseeder.flint.content.ContentFetcher;
import org.pageseeder.flint.content.SourceForwarder;
import org.pageseeder.flint.indexing.IndexJob;
import org.pageseeder.flint.indexing.IndexJob.Priority;
import org.pageseeder.flint.lucene.LuceneIndex;
import org.pageseeder.flint.lucene.LuceneIndexQueries;
import org.pageseeder.flint.lucene.utils.TestListener;
import org.pageseeder.flint.lucene.utils.TestUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class QuestionTest {

  private static File template  = new File("src/test/resources/template.xsl");

  private static LuceneIndex index;
  private static IndexManager manager;

  @BeforeClass
  public static void init() {
    try {
      index = new LuceneIndex("QuestionTest", new ByteBuffersDirectory(), new StandardAnalyzer());
      index.setTemplates(TestUtils.TYPE, TestUtils.MEDIA_TYPE, template.toURI());
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    manager = new IndexManager(new ContentFetcher() {
      @Override
      public Content getContent(IndexJob job) {
        // add all documents in one go
        String xml = "<documents version='5.0'>\n"+
                       "<document>\n"+
                         "<field name='"+TestUtils.ID_FIELD+"' tokenize='false'>doc1</field>\n"+
                         "<field name='term1'>value1</field>\n"+
                       "</document>\n"+
                       "<document>\n"+
                         "<field name='"+TestUtils.ID_FIELD+"' tokenize='false'>doc2</field>\n"+
                         "<field name='term1'>value2</field>\n"+
                       "</document>\n"+
                       "<document>\n"+
                         "<field name='"+TestUtils.ID_FIELD+"' tokenize='false'>doc3</field>\n"+
                         "<field name='term1'>value3</field>\n"+
                       "</document>\n"+
                     "</documents>";
        return new TestUtils.TestContent(job.getContentID(), xml);
      }
    }, new TestListener());
    manager.setDefaultTranslator(new SourceForwarder("xml", "UTF-8"));
    System.out.println("Starting manager!");
    manager.index("content", TestUtils.TYPE, index, new Requester(TermParameterTest.class.getName()), Priority.HIGH, null);
    System.out.println("Documents indexed");
    // wait a bit
    TestUtils.wait(1);
  }

  @AfterClass
  public static void after() {
    // stop index
    System.out.println("Stopping manager!");
    manager.stop();
    System.out.println("-----------------------------------");
  }

  @Test
  public void testSimilar() throws IOException, IndexException {
    Question q = new Question.Builder().field("term1").question("value").build();
    IndexReader reader = LuceneIndexQueries.grabReader(index);
    List<Question> qs = q.similar(reader);
    Assert.assertEquals(3, qs.size());
    Assert.assertThat(qs.get(0).question(), CoreMatchers.anyOf(CoreMatchers.equalTo("value1"), CoreMatchers.equalTo("value2"), CoreMatchers.equalTo("value3")));
    Assert.assertThat(qs.get(1).question(), CoreMatchers.anyOf(CoreMatchers.equalTo("value1"), CoreMatchers.equalTo("value2"), CoreMatchers.equalTo("value3")));
    Assert.assertThat(qs.get(2).question(), CoreMatchers.anyOf(CoreMatchers.equalTo("value1"), CoreMatchers.equalTo("value2"), CoreMatchers.equalTo("value3")));
    LuceneIndexQueries.releaseQuietly(index, reader);

  }

  @Test
  public void testSimilarBig() throws IOException, IndexException {
    Question q = new Question.Builder().field("term1").question(
        "value value value value value value value value value value value value value value value value value value value value "+
        "value value value value value value value value value value value value value value value value value value value value "+
        "value value value value value value value value value value value value value value value value value value value value "+
        "value value value value value value value value value value value value value value value value value value value value "+
        "value value value value value value value value value value value value value value value value value value value value "+
        "value value value value value value value value value value value value value value value value value value value value "+
        "value value value value value value value value value value value value value value value value value value value value "+
        "value value value value value value value value value value value value value value value value value value value value "+
        "value value value value value value value value value value value value value value value value value value value value "+
        "value value value value value value value value value value value value value value value value value value value value "+
        "value value value value value value value value value value value value value value value value value value value value "+
        "value value value value value value value value value value value value value value value value value value value value "+
        "value value value value value value value value value value value value value value value value value value value value "+
        "value value value value value value value value value value value value value value value value value value value value "+
        "value value value value value value value value value value value value value value value value value value value value "+
        "value value value value value value value value value value value value value value value value value value value value "+
        "value value value value value value value value value value value value value value value value value value value value "+
        "value value value value value value value value value value value value value value value value value value value value "+
        "value value value value value value value value value value value value value value value value value value value value "+
        "value value value value value value value value value value value value value value value value value value value value "+
        "value value value value value value value value value value value value value value value value value value value value "+
        "value value value value value value value value value value value value value value value value value value value value "+
        "value value value value value value value value value value value value value value value value value value value value "+
        "value value value value value value value value value value value value value value value value value value value value "+
        "value value value value value value value value value value value value value value value value value value value value "+
        "value value value value value value value value value value value value value value value value value value value value "+
        "value value value value value value value value value value value value value value value value value value value value "+
        "value value value value value value value value value value value value value value value value value value value value "+
        "value value value value value value value value value value").build();
    IndexReader reader = LuceneIndexQueries.grabReader(index);
    List<Question> qs = q.similar(reader, 5);
    Assert.assertEquals(5, qs.size());
    LuceneIndexQueries.releaseQuietly(index, reader);

  }

  @Test
  public void testOperators() {
    Question q = new Question.Builder()
        .analyzerAndCatalog(new KeywordAnalyzer(), null)
        .field("term1").question("value1 value2").build();
    Assert.assertEquals("term1:value1 value2", q.toQuery().toString());
    q = new Question.Builder()
        .analyzerAndCatalog(new KeywordAnalyzer(), null)
        .field("term1").question("value1 OR value2").build();
    Assert.assertEquals("(term1:value1 term1:value2)", q.toQuery().toString());
    q = new Question.Builder()
        .analyzerAndCatalog(new KeywordAnalyzer(), null)
        .field("term1").question("\"value1 value2\"").build();
    Assert.assertEquals("term1:\"value1 value2\"", q.toQuery().toString());
    q = new Question.Builder()
        .analyzerAndCatalog(new KeywordAnalyzer(), null)
        .field("term1").question("\"value1 OR value2\"").build();
    Assert.assertEquals("term1:\"value1 OR value2\"", q.toQuery().toString());
  }
}

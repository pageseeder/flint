package org.pageseeder.flint.lucene.query;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.RAMDirectory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.Requester;
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

public class TermParameterTest {

  private static File template  = new File("src/test/resources/template.xsl");

  private static LuceneIndex index;
  private static IndexManager manager;
  
  @BeforeClass
  public static void init() {
    try {
      index = new LuceneIndex(TermParameterTest.class.getName(), new RAMDirectory(), new StandardAnalyzer());
      index.setTemplates(TestUtils.TYPE, TestUtils.MEDIA_TYPE, template.toURI());
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    manager = new IndexManager(new ContentFetcher() {
      @Override
      public Content getContent(IndexJob job) {
        // delete?
        if (job.getContentID().startsWith("delete-")) {
          return new TestUtils.TestContent(job.getContentID().substring(7), null);
        }
        // add all documents in one go
        String xml = "<documents version='5.0'>\n"+
                       "<document>\n"+
                         "<field name='"+TestUtils.ID_FIELD+"' tokenize='false'>doc1</field>\n"+
                         "<field name='term1' tokenize='false'>value1</field>\n"+
                       "</document>\n"+
                       "<document>\n"+
                         "<field name='"+TestUtils.ID_FIELD+"' tokenize='false'>doc2</field>\n"+
                         "<field name='term1' tokenize='false'>value2</field>\n"+
                       "</document>\n"+
                       "<document>\n"+
                         "<field name='"+TestUtils.ID_FIELD+"' tokenize='false'>doc3</field>\n"+
                         "<field name='term1' tokenize='false'>value3</field>\n"+
                       "</document>\n"+
                       "<document>\n"+
                         "<field name='"+TestUtils.ID_FIELD+"' tokenize='false'>doc4</field>\n"+
                         "<field name='term1' tokenize='false'></field>\n"+
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
  public void testTermParameter1() {
    try {
      TermParameter param = new TermParameter("term1", "value1");
      SearchResults results = LuceneIndexQueries.query(index, BasicQuery.newBasicQuery(param));
      Assert.assertEquals(1, results.getTotalNbOfResults());
      Assert.assertEquals("doc1", results.documents().iterator().next().get(TestUtils.ID_FIELD));
    } catch (Exception ex) {
      ex.printStackTrace();
      Assert.fail(ex.getMessage());
    }
  }

  @Test
  public void testEmptyTermParameter() {
    try {
      TermParameter param = new TermParameter("term1", "");
      SearchResults results = LuceneIndexQueries.query(index, BasicQuery.newBasicQuery(param));
      Assert.assertEquals(1, results.getTotalNbOfResults());
      Assert.assertEquals("doc4", results.documents().iterator().next().get(TestUtils.ID_FIELD));
    } catch (Exception ex) {
      ex.printStackTrace();
      Assert.fail(ex.getMessage());
    }
  }

}

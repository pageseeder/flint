package org.pageseeder.flint.query;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import javax.xml.transform.TransformerException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.document.Document;
import org.apache.lucene.store.RAMDirectory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pageseeder.flint.IndexJob;
import org.pageseeder.flint.IndexJob.Priority;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.api.Content;
import org.pageseeder.flint.api.ContentFetcher;
import org.pageseeder.flint.api.Index;
import org.pageseeder.flint.api.Requester;
import org.pageseeder.flint.content.SourceForwarder;
import org.pageseeder.flint.utils.TestListener;
import org.pageseeder.flint.utils.TestUtils;

public class DateParameterTest {

  private static SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
  private static File template  = new File("src/test/resources/template.xsl");

  private static Index index;
  private static IndexManager manager;
  
  @BeforeClass
  public static void init() {
    index = new Index("DateParameterTest", new RAMDirectory(), new StandardAnalyzer());
    try {
      index.setTemplates(TestUtils.TYPE, TestUtils.MEDIA_TYPE, template.toURI());
    } catch (TransformerException ex) {
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
                         "<field name='date1' date-format='yyyy-MM-dd' date-resolution='day'>2015-05-12</field>\n"+
                         "<field name='date2' date-format='yyyy-MM-dd' date-resolution='second' numeric-type='long'>2015-05-12</field>\n"+
                       "</document>\n"+
                       "<document>\n"+
                         "<field name='"+TestUtils.ID_FIELD+"' tokenize='false'>doc2</field>\n"+
                         "<field name='date1' date-format='yyyy-MM-dd' date-resolution='day'>2014-11-22</field>\n"+
                         "<field name='date2' date-format='yyyy-MM-dd' date-resolution='second' numeric-type='long'>2014-11-22</field>\n"+
                       "</document>\n"+
                       "<document>\n"+
                         "<field name='"+TestUtils.ID_FIELD+"' tokenize='false'>doc3</field>\n"+
                         "<field name='date1' date-format='yyyy-MM-dd' date-resolution='day'>2015-07-04</field>\n"+
                         "<field name='date2' date-format='yyyy-MM-dd' date-resolution='second' numeric-type='long'>2015-07-04</field>\n"+
                       "</document>\n"+
                     "</documents>";
        return new TestUtils.TestContent(job.getContentID(), xml);
      }
    }, new TestListener());
    manager.setDefaultTranslator(new SourceForwarder("xml", "UTF-8"));
    System.out.println("Starting manager!");
    manager.index("content", TestUtils.TYPE, index, new Requester("DateParameter"), Priority.HIGH, null);
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
  public void testDateParameter1() {
    try {
      Date from = DAY_FORMAT.parse("2015-01-01");
      Date to   = DAY_FORMAT.parse("2016-01-01");
      // test non numeric
      DateParameter param = new DateParameter("date1", from, to, Resolution.DAY, true);
      SearchResults results = manager.query(index, BasicQuery.newBasicQuery(param));
      Assert.assertEquals(0, results.getTotalNbOfResults());
      // correct
      param = new DateParameter("date1", from, to, Resolution.DAY, false);
      results = manager.query(index, BasicQuery.newBasicQuery(param));
      Assert.assertEquals(2, results.getTotalNbOfResults());
      Iterator<Document> docs = results.documents().iterator();
      Assert.assertEquals("doc1", docs.next().get(TestUtils.ID_FIELD));
      Assert.assertEquals("doc3", docs.next().get(TestUtils.ID_FIELD));
    } catch (Exception ex) {
      ex.printStackTrace();
      Assert.fail(ex.getMessage());
    }
  }

  @Test
  public void testDateParameter2() {
    try {
      Date from = DAY_FORMAT.parse("2015-01-01");
      Date to   = DAY_FORMAT.parse("2016-01-01");
      // test not numeric
      DateParameter param = new DateParameter("date2", from, to, Resolution.DAY, false);
      SearchResults results = manager.query(index, BasicQuery.newBasicQuery(param));
      Assert.assertEquals(0, results.getTotalNbOfResults());
      // test wrong resolution
      param = new DateParameter("date2", from, to, Resolution.SECOND, false);
      results = manager.query(index, BasicQuery.newBasicQuery(param));
      Assert.assertEquals(0, results.getTotalNbOfResults());
      // then correct
      param = new DateParameter("date2", from, to, Resolution.SECOND, true);
      results = manager.query(index, BasicQuery.newBasicQuery(param));
      Assert.assertEquals(2, results.getTotalNbOfResults());
      Iterator<Document> docs = results.documents().iterator();
      Assert.assertEquals("doc1", docs.next().get(TestUtils.ID_FIELD));
      Assert.assertEquals("doc3", docs.next().get(TestUtils.ID_FIELD));
    } catch (Exception ex) {
      ex.printStackTrace();
      Assert.fail(ex.getMessage());
    }
  }

}

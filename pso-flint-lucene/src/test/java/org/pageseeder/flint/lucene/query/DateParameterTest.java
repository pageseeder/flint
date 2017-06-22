package org.pageseeder.flint.lucene.query;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;

import javax.xml.transform.TransformerException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.document.Document;
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
import org.pageseeder.flint.lucene.query.BasicQuery;
import org.pageseeder.flint.lucene.query.DateParameter;
import org.pageseeder.flint.lucene.query.SearchResults;
import org.pageseeder.flint.lucene.utils.TestListener;
import org.pageseeder.flint.lucene.utils.TestUtils;

public class DateParameterTest {

  private static File template  = new File("src/test/resources/template.xsl");

  private static LuceneIndex index;
  private static IndexManager manager;
  
  @BeforeClass
  public static void init() {
    index = new LuceneIndex("DateParameterTest", new RAMDirectory(), new StandardAnalyzer());
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
                         "<field name='date2' date-format='yyyy-MM-dd-hh-mm-ss' date-resolution='second' numeric-type='long'>2015-05-12-17-15-33</field>\n"+
                         "<field name='date3' date-format='yyyy-MM-dd-hh-mm-ss' date-resolution='second'>2015-05-12-17-15-33</field>\n"+
                         "<field name='date4' date-format='yyyy-MM-dd' date-resolution='day' numeric-type='int'>2015-05-12</field>\n"+
                       "</document>\n"+
                       "<document>\n"+
                         "<field name='"+TestUtils.ID_FIELD+"' tokenize='false'>doc2</field>\n"+
                         "<field name='date1' date-format='yyyy-MM-dd' date-resolution='day'>2014-11-22</field>\n"+
                         "<field name='date2' date-format='yyyy-MM-dd-hh-mm-ss' date-resolution='second' numeric-type='long'>2014-11-22-10-02-23</field>\n"+
                         "<field name='date3' date-format='yyyy-MM-dd-hh-mm-ss' date-resolution='second'>2014-11-22-10-02-23</field>\n"+
                         "<field name='date4' date-format='yyyy-MM-dd' date-resolution='day' numeric-type='int'>2014-11-22</field>\n"+
                       "</document>\n"+
                       "<document>\n"+
                         "<field name='"+TestUtils.ID_FIELD+"' tokenize='false'>doc3</field>\n"+
                         "<field name='date1' date-format='yyyy-MM-dd' date-resolution='day'>2015-07-04</field>\n"+
                         "<field name='date2' date-format='yyyy-MM-dd-hh-mm-ss' date-resolution='second' numeric-type='long'>2015-07-04-21-11-55</field>\n"+
                         "<field name='date3' date-format='yyyy-MM-dd-hh-mm-ss' date-resolution='second'>2015-07-04-21-11-55</field>\n"+
                         "<field name='date4' date-format='yyyy-MM-dd' date-resolution='day' numeric-type='int'>2015-07-04</field>\n"+
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
  public void testResolutionDayString() {
    try {
      Date from = buildDate(2015, 1, 1);
      Date to   = buildDate(2016, 1, 1);
      // test numeric
      DateParameter param = new DateParameter("date1", from, to, Resolution.DAY, true);
      SearchResults results = LuceneIndexQueries.query(index, BasicQuery.newBasicQuery(param));
      Assert.assertEquals(0, results.getTotalNbOfResults());
      // correct
      param = new DateParameter("date1", from, to, Resolution.DAY, false);
      results = LuceneIndexQueries.query(index, BasicQuery.newBasicQuery(param));
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
  public void testResolutionDayNumeric() {
    try {
      Date from = buildDate(2015, 1, 1);
      Date to   = buildDate(2016, 1, 1);
      // test non numeric
      DateParameter param = new DateParameter("date4", from, to, Resolution.DAY, false);
      SearchResults results = LuceneIndexQueries.query(index, BasicQuery.newBasicQuery(param));
      Assert.assertEquals(0, results.getTotalNbOfResults());
      // correct
      param = new DateParameter("date4", from, to, Resolution.DAY, true);
      results = LuceneIndexQueries.query(index, BasicQuery.newBasicQuery(param));
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
  public void testResolutionSecondString() {
    try {
      Date from = buildDate(2015, 1, 1);
      Date to   = buildDate(2016, 1, 1);
      // test numeric
      DateParameter param = new DateParameter("date3", from, to, Resolution.DAY, true);
      SearchResults results = LuceneIndexQueries.query(index, BasicQuery.newBasicQuery(param));
      Assert.assertEquals(0, results.getTotalNbOfResults());
      // test wrong resolution
      param = new DateParameter("date3", from, to, Resolution.SECOND, true);
      results = LuceneIndexQueries.query(index, BasicQuery.newBasicQuery(param));
      Assert.assertEquals(0, results.getTotalNbOfResults());
      // then correct
      param = new DateParameter("date3", from, to, Resolution.SECOND, false);
      results = LuceneIndexQueries.query(index, BasicQuery.newBasicQuery(param));
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
  public void testResolutionSecondNumeric() {
    try {
      Date from = buildDate(2015, 1, 1);
      Date to   = buildDate(2016, 1, 1);
      // test not numeric
      DateParameter param = new DateParameter("date2", from, to, Resolution.DAY, false);
      SearchResults results = LuceneIndexQueries.query(index, BasicQuery.newBasicQuery(param));
      Assert.assertEquals(0, results.getTotalNbOfResults());
      // test wrong resolution
      param = new DateParameter("date2", from, to, Resolution.SECOND, false);
      results = LuceneIndexQueries.query(index, BasicQuery.newBasicQuery(param));
      Assert.assertEquals(0, results.getTotalNbOfResults());
      // then correct
      param = new DateParameter("date2", from, to, Resolution.SECOND, true);
      results = LuceneIndexQueries.query(index, BasicQuery.newBasicQuery(param));
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
  public void testExactMatch() {
    try {
      Date exact1 = buildDate(2015, 5, 12);
      Date exact2 = buildDate(2015, 5, 12, 17, 15, 33);
      // test non numeric day
      DateParameter param = new DateParameter("date1", exact1, Resolution.DAY, false);
      SearchResults results = LuceneIndexQueries.query(index, BasicQuery.newBasicQuery(param));
      Assert.assertEquals(1, results.getTotalNbOfResults());
      Iterator<Document> docs = results.documents().iterator();
      Assert.assertEquals("doc1", docs.next().get(TestUtils.ID_FIELD));
      // test non numeric second
      param = new DateParameter("date3", exact2, Resolution.SECOND, false);
      results = LuceneIndexQueries.query(index, BasicQuery.newBasicQuery(param));
      Assert.assertEquals(1, results.getTotalNbOfResults());
      docs = results.documents().iterator();
      Assert.assertEquals("doc1", docs.next().get(TestUtils.ID_FIELD));
      // test numeric second
      param = new DateParameter("date2", exact2, Resolution.SECOND, true);
      results = LuceneIndexQueries.query(index, BasicQuery.newBasicQuery(param));
      Assert.assertEquals(1, results.getTotalNbOfResults());
      docs = results.documents().iterator();
      Assert.assertEquals("doc1", docs.next().get(TestUtils.ID_FIELD));
      // test numeric day
      param = new DateParameter("date4", exact1, Resolution.DAY, true);
      results = LuceneIndexQueries.query(index, BasicQuery.newBasicQuery(param));
      Assert.assertEquals(1, results.getTotalNbOfResults());
      docs = results.documents().iterator();
      Assert.assertEquals("doc1", docs.next().get(TestUtils.ID_FIELD));
    } catch (Exception ex) {
      ex.printStackTrace();
      Assert.fail(ex.getMessage());
    }
  }

  @Test
  public void testEdges() {
    try {
      Date fromday = buildDate(2015, 5, 12);
      Date today   = buildDate(2015, 7, 4);
      Date fromsec = buildDate(2015, 5, 12, 17, 15, 33);
      Date tosec   = buildDate(2015, 7, 4, 21, 11, 55);
      // test non numeric day
      DateParameter param = new DateParameter("date1", fromday, today, Resolution.DAY, false);
      SearchResults results = LuceneIndexQueries.query(index, BasicQuery.newBasicQuery(param));
      Assert.assertEquals(2, results.getTotalNbOfResults());
      Iterator<Document> docs = results.documents().iterator();
      Assert.assertEquals("doc1", docs.next().get(TestUtils.ID_FIELD));
      Assert.assertEquals("doc3", docs.next().get(TestUtils.ID_FIELD));
      // test non numeric second
      param = new DateParameter("date3", fromsec, tosec, Resolution.SECOND, false);
      results = LuceneIndexQueries.query(index, BasicQuery.newBasicQuery(param));
      Assert.assertEquals(2, results.getTotalNbOfResults());
      docs = results.documents().iterator();
      Assert.assertEquals("doc1", docs.next().get(TestUtils.ID_FIELD));
      Assert.assertEquals("doc3", docs.next().get(TestUtils.ID_FIELD));
      // test numeric second
      param = new DateParameter("date2", fromsec, tosec, Resolution.SECOND, true);
      results = LuceneIndexQueries.query(index, BasicQuery.newBasicQuery(param));
      Assert.assertEquals(2, results.getTotalNbOfResults());
      docs = results.documents().iterator();
      Assert.assertEquals("doc1", docs.next().get(TestUtils.ID_FIELD));
      Assert.assertEquals("doc3", docs.next().get(TestUtils.ID_FIELD));
      // test numeric day
      param = new DateParameter("date4", fromday, today, Resolution.DAY, true);
      results = LuceneIndexQueries.query(index, BasicQuery.newBasicQuery(param));
      Assert.assertEquals(2, results.getTotalNbOfResults());
      docs = results.documents().iterator();
      Assert.assertEquals("doc1", docs.next().get(TestUtils.ID_FIELD));
      Assert.assertEquals("doc3", docs.next().get(TestUtils.ID_FIELD));
    } catch (Exception ex) {
      ex.printStackTrace();
      Assert.fail(ex.getMessage());
    }
  }

  private static Date buildDate(int y, int m, int d) {
    return buildDate(y, m, d, 0, 0, 0);
  }

  private static Date buildDate(int y, int m, int d, int h, int mn, int s) {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    cal.set(Calendar.YEAR,   y);
    cal.set(Calendar.MONTH,  m-1);
    cal.set(Calendar.DATE,   d);
    cal.set(Calendar.HOUR,   h);
    cal.set(Calendar.MINUTE, mn);
    cal.set(Calendar.SECOND, s);
    return cal.getTime();
  }
}

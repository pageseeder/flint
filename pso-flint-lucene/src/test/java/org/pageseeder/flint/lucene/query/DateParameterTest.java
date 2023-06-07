package org.pageseeder.flint.lucene.query;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.document.Document;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.Requester;
import org.pageseeder.flint.content.SourceForwarder;
import org.pageseeder.flint.indexing.IndexJob.Priority;
import org.pageseeder.flint.lucene.LuceneIndex;
import org.pageseeder.flint.lucene.LuceneIndexQueries;
import org.pageseeder.flint.lucene.utils.TestListener;
import org.pageseeder.flint.lucene.utils.TestUtils;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Iterator;

public class DateParameterTest {

  private static final File template  = new File("src/test/resources/template.xsl");

  private static LuceneIndex index;
  private static IndexManager manager;

  @BeforeClass
  public static void init() {
    try {
      index = new LuceneIndex("DateParameterTest", new ByteBuffersDirectory(), new StandardAnalyzer());
      index.setTemplates(TestUtils.TYPE, TestUtils.MEDIA_TYPE, template.toURI());
    } catch (Exception ex) {
      LoggerFactory.getLogger(TestUtils.class).error("Something went wrong", ex);
    }

    manager = new IndexManager(job -> {
      // delete?
      if (job.getContentID().startsWith("delete-")) {
        return new TestUtils.TestContent(job.getContentID().substring(7), null);
      }
      // add all documents in one go
      String xml = "<documents version='5.0'>\n"+
                     "<document>\n"+
                       "<field name='"+TestUtils.ID_FIELD+"' tokenize='false'>doc1</field>\n"+
                       "<field tokenize='false' name='date1' date-format='yyyy-MM-dd' date-resolution='day'>2015-05-12</field>\n"+
                       "<field tokenize='false' name='date2' date-format='yyyy-MM-dd-hh-mm-ss' date-resolution='second' numeric-type='long'>2015-05-12-17-15-33</field>\n"+
                       "<field tokenize='false' name='date3' date-format='yyyy-MM-dd-hh-mm-ss' date-resolution='second'>2015-05-12-17-15-33</field>\n"+
                       "<field tokenize='false' name='date4' date-format='yyyy-MM-dd' date-resolution='day' numeric-type='int'>2015-05-12</field>\n"+
                     "</document>\n"+
                     "<document>\n"+
                       "<field name='"+TestUtils.ID_FIELD+"' tokenize='false'>doc2</field>\n"+
                       "<field tokenize='false' name='date1' date-format='yyyy-MM-dd' date-resolution='day'>2014-11-22</field>\n"+
                       "<field tokenize='false' name='date2' date-format='yyyy-MM-dd-hh-mm-ss' date-resolution='second' numeric-type='long'>2014-11-22-10-02-23</field>\n"+
                       "<field tokenize='false' name='date3' date-format='yyyy-MM-dd-hh-mm-ss' date-resolution='second'>2014-11-22-10-02-23</field>\n"+
                       "<field tokenize='false' name='date4' date-format='yyyy-MM-dd' date-resolution='day' numeric-type='int'>2014-11-22</field>\n"+
                     "</document>\n"+
                     "<document>\n"+
                       "<field name='"+TestUtils.ID_FIELD+"' tokenize='false'>doc3</field>\n"+
                       "<field tokenize='false' name='date1' date-format='yyyy-MM-dd' date-resolution='day'>2015-07-04</field>\n"+
                       "<field tokenize='false' name='date2' date-format='yyyy-MM-dd-hh-mm-ss' date-resolution='second' numeric-type='long'>2015-07-04-21-11-55</field>\n"+
                       "<field tokenize='false' name='date3' date-format='yyyy-MM-dd-hh-mm-ss' date-resolution='second'>2015-07-04-21-11-55</field>\n"+
                       "<field tokenize='false' name='date4' date-format='yyyy-MM-dd' date-resolution='day' numeric-type='int'>2015-07-04</field>\n"+
                     "</document>\n"+
                     "<document>\n"+
                       "<field name='"+TestUtils.ID_FIELD+"' tokenize='false'>doc4</field>\n"+
                       "<field tokenize='false' name='date1' date-format='yyyy-MM-dd' date-resolution='day'></field>\n"+
                       "<field tokenize='false' name='date3' date-format='yyyy-MM-dd-hh-mm-ss' date-resolution='second'></field>\n"+
                     "</document>\n"+
                   "</documents>";
      return new TestUtils.TestContent(job.getContentID(), xml);
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
      OffsetDateTime from = OffsetDateTime.of(2015, 1, 1, 0,  0,  0,  0, ZoneOffset.UTC);
      OffsetDateTime to   = OffsetDateTime.of(2016, 1, 1, 0,  0,  0,  0, ZoneOffset.UTC);
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
      LoggerFactory.getLogger(TestUtils.class).error("Something went wrong", ex);
      Assert.fail(ex.getMessage());
    }
  }

  @Test
  public void testResolutionDayNumeric() {
    try {
      OffsetDateTime from = OffsetDateTime.of(2015, 1, 1, 0,  0,  0,  0, ZoneOffset.UTC);
      OffsetDateTime to   = OffsetDateTime.of(2016, 1, 1, 0,  0,  0,  0, ZoneOffset.UTC);
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
      LoggerFactory.getLogger(TestUtils.class).error("Something went wrong", ex);
      Assert.fail(ex.getMessage());
    }
  }

  @Test
  public void testResolutionSecondString() {
    try {
      OffsetDateTime from = OffsetDateTime.of(2015, 1, 1, 0,  0,  0,  0, ZoneOffset.UTC);
      OffsetDateTime to   = OffsetDateTime.of(2016, 1, 1, 0,  0,  0,  0, ZoneOffset.UTC);
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
      LoggerFactory.getLogger(TestUtils.class).error("Something went wrong", ex);
      Assert.fail(ex.getMessage());
    }
  }

  @Test
  public void testResolutionSecondNumeric() {
    try {
      OffsetDateTime from = OffsetDateTime.of(2015, 1, 1, 0,  0,  0,  0, ZoneOffset.UTC);
      OffsetDateTime to   = OffsetDateTime.of(2016, 1, 1, 0,  0,  0,  0, ZoneOffset.UTC);
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
      LoggerFactory.getLogger(TestUtils.class).error("Something went wrong", ex);
      Assert.fail(ex.getMessage());
    }
  }


  /**
   * current dates in index:
   * doc2  2014/11/22 10:02:23
   * doc1  2015/05/12 17:15:33
   * doc3  2015/07/04 21:11:55
   */
  @Test
  public void testExactMatch() {
    try {
      OffsetDateTime exact1 = OffsetDateTime.of(2015, 5, 12, 0,  0,  0,  0, ZoneOffset.UTC);
      OffsetDateTime exact2 = OffsetDateTime.of(2015, 5, 12, 17, 15, 33, 0, ZoneOffset.UTC);
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
      LoggerFactory.getLogger(TestUtils.class).error("Something went wrong", ex);
      Assert.fail(ex.getMessage());
    }
  }

  /**
   * current dates in index:
   * doc2  2014/11/22 10:02:23
   * doc1  2015/05/12 17:15:33
   * doc3  2015/07/04 21:11:55
   */
  @Test
  public void testEdges() {
    try {
      OffsetDateTime fromday = OffsetDateTime.of(2015, 5, 12, 0,  0,  0,  0, ZoneOffset.UTC);
      OffsetDateTime today   = OffsetDateTime.of(2015, 7, 4,  0,  0,  0,  0, ZoneOffset.UTC);
      OffsetDateTime fromsec = OffsetDateTime.of(2015, 5, 12, 17, 15, 33, 0, ZoneOffset.UTC);
      OffsetDateTime tosec   = OffsetDateTime.of(2015, 7, 4,  21, 11, 55, 0, ZoneOffset.UTC);
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
      LoggerFactory.getLogger(TestUtils.class).error("Something went wrong", ex);
      Assert.fail(ex.getMessage());
    }
  }

  @Test
  public void testEmpty() {
    try {
      // test non numeric day
      DateParameter param = new DateParameter("date1", Resolution.DAY, false);
      SearchResults results = LuceneIndexQueries.query(index, BasicQuery.newBasicQuery(param));
      Assert.assertEquals(1, results.getTotalNbOfResults());
      Iterator<Document> docs = results.documents().iterator();
      Assert.assertEquals("doc4", docs.next().get(TestUtils.ID_FIELD));
      // test non numeric second
      param = new DateParameter("date3", (Date) null, Resolution.SECOND, false);
      results = LuceneIndexQueries.query(index, BasicQuery.newBasicQuery(param));
      Assert.assertEquals(1, results.getTotalNbOfResults());
      docs = results.documents().iterator();
      Assert.assertEquals("doc4", docs.next().get(TestUtils.ID_FIELD));
    } catch (Exception ex) {
      LoggerFactory.getLogger(TestUtils.class).error("Something went wrong", ex);
      Assert.fail(ex.getMessage());
    }
  }
//
//  private static Date buildDate(int y, int m, int d) {
//    return buildDate(y, m, d, 0, 0, 0);
//  }
//
//  private static Date buildDate(int y, int m, int d, int h, int mn, int s) {
//    Calendar cal = Calendar.getInstance();
//    cal.set(Calendar.YEAR,   y);
//    cal.set(Calendar.MONTH,  m-1);
//    cal.set(Calendar.DATE,   d);
//    cal.set(Calendar.HOUR,   h);
//    cal.set(Calendar.MINUTE, mn);
//    cal.set(Calendar.SECOND, s);
//    cal.setTimeZone(TimeZone.getTimeZone("GMT"));
//    return cal.getTime();
//  }

}

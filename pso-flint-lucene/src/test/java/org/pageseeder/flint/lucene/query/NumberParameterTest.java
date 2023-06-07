package org.pageseeder.flint.lucene.query;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
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

public class NumberParameterTest {

  private static final File template  = new File("src/test/resources/template.xsl");

  private static LuceneIndex index;
  private static IndexManager manager;

  @BeforeClass
  public static void init() {
    try {
      index = new LuceneIndex(NumberParameterTest.class.getName(), new ByteBuffersDirectory(), new StandardAnalyzer());
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
                       "<field name='int-field'    numeric-type='int'>11</field>\n"+
                       "<field name='long-field'   numeric-type='long'>22</field>\n"+
                       "<field name='float-field'  numeric-type='float'>33.33</field>\n"+
                       "<field name='double-field' numeric-type='double'>44.44</field>\n"+
                     "</document>\n"+
                   "</documents>";
      return new TestUtils.TestContent(job.getContentID(), xml);
    }, new TestListener());
    manager.setDefaultTranslator(new SourceForwarder("xml", "UTF-8"));
    System.out.println("Starting manager!");
    manager.index("content", TestUtils.TYPE, index, new Requester(NumberParameterTest.class.getName()), Priority.HIGH, null);
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
  public void testInteger() {
    try {
      NumberParameter<Integer> param = new NumberParameter<>("int-field", 11);
      SearchResults results = LuceneIndexQueries.query(index, BasicQuery.newBasicQuery(param));
      Assert.assertEquals(1, results.getTotalNbOfResults());
      Assert.assertEquals("doc1", results.documents().iterator().next().get(TestUtils.ID_FIELD));
      // test wrong type
      NumberParameter<Long> param2 = new NumberParameter<>("int-field", 11L);
      results = LuceneIndexQueries.query(index, BasicQuery.newBasicQuery(param2));
      Assert.assertEquals(0, results.getTotalNbOfResults());
    } catch (Exception ex) {
      LoggerFactory.getLogger(TestUtils.class).error("Something went wrong", ex);
      Assert.fail(ex.getMessage());
    }
  }

  @Test
  public void testLong() {
    try {
      NumberParameter<Long> param = new NumberParameter<Long>("long-field", 22L);
      SearchResults results = LuceneIndexQueries.query(index, BasicQuery.newBasicQuery(param));
      Assert.assertEquals(1, results.getTotalNbOfResults());
      Assert.assertEquals("doc1", results.documents().iterator().next().get(TestUtils.ID_FIELD));
      // test wrong type
      NumberParameter<Integer> param2 = new NumberParameter<Integer>("long-field", 22);
      results = LuceneIndexQueries.query(index, BasicQuery.newBasicQuery(param2));
      Assert.assertEquals(0, results.getTotalNbOfResults());
    } catch (Exception ex) {
      LoggerFactory.getLogger(TestUtils.class).error("Something went wrong", ex);
      Assert.fail(ex.getMessage());
    }
  }

  @Test
  public void testFloat() {
    try {
      NumberParameter<Float> param = new NumberParameter<Float>("float-field", 33.33f);
      SearchResults results = LuceneIndexQueries.query(index, BasicQuery.newBasicQuery(param));
      Assert.assertEquals(1, results.getTotalNbOfResults());
      Assert.assertEquals("doc1", results.documents().iterator().next().get(TestUtils.ID_FIELD));
      // test wrong type
      NumberParameter<Double> param2 = new NumberParameter<Double>("float-field", 33.33d);
      results = LuceneIndexQueries.query(index, BasicQuery.newBasicQuery(param2));
      Assert.assertEquals(0, results.getTotalNbOfResults());
    } catch (Exception ex) {
      LoggerFactory.getLogger(TestUtils.class).error("Something went wrong", ex);
      Assert.fail(ex.getMessage());
    }
  }

  @Test
  public void testDouble() {
    try {
      NumberParameter<Double> param = new NumberParameter<Double>("double-field", 44.44d);
      SearchResults results = LuceneIndexQueries.query(index, BasicQuery.newBasicQuery(param));
      Assert.assertEquals(1, results.getTotalNbOfResults());
      Assert.assertEquals("doc1", results.documents().iterator().next().get(TestUtils.ID_FIELD));
      // test wrong type
      NumberParameter<Float> param2 = new NumberParameter<Float>("double-field", 44.44f);
      results = LuceneIndexQueries.query(index, BasicQuery.newBasicQuery(param2));
      Assert.assertEquals(0, results.getTotalNbOfResults());
    } catch (Exception ex) {
      LoggerFactory.getLogger(TestUtils.class).error("Something went wrong", ex);
      Assert.fail(ex.getMessage());
    }
  }

}

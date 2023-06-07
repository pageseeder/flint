package org.pageseeder.flint.lucene.local;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pageseeder.flint.local.LocalIndexManager;
import org.pageseeder.flint.local.LocalIndexManagerFactory;
import org.pageseeder.flint.lucene.LuceneIndexQueries;
import org.pageseeder.flint.lucene.LuceneLocalIndex;
import org.pageseeder.flint.lucene.utils.TestListener;
import org.pageseeder.flint.lucene.utils.TestUtils;
import org.slf4j.LoggerFactory;

import java.io.File;

@RunWith(TestUtils.ParallelRunner.class)
public class ParallelIndexerTest {

  private static final File template     = new File("src/test/resources/template.xsl");
  private static final File templatePSML = new File("src/test/resources/psml-template.xsl");
  private static final File indexing     = new File("src/test/resources/indexing");
  private static final File indexRoot    = new File("tmp/index-parallel");

  private static LuceneLocalIndex index = null;
  private static LocalIndexManager manager = null;

  public synchronized static void init() {
    if (!indexRoot.exists()) indexRoot.mkdir();
    if (index == null) {
      try {
        index = new LuceneLocalIndex(indexRoot, "parallel", new StandardAnalyzer(), indexing);
        index.setTemplate("xml", template.toURI());
        index.setTemplate("psml", templatePSML.toURI());
      } catch (Exception ex) {
        LoggerFactory.getLogger(TestUtils.class).error("Something went wrong", ex);
      }
    }
    if (manager == null) {
      manager = LocalIndexManagerFactory.createMultiThreads(5, new TestListener());
      System.out.println("Starting manager!");
    }
  }

  @AfterClass
  public static void after() {
    // stop index
    System.out.println("Stopping manager!");
    manager.shutdown();
    System.out.println("-----------------------------------");
  }

  @Test
  public void testIndexing1() {
    testIndexing(1);
  }
  @Test
  public void testIndexing2() {
    testIndexing(2);
  }
/*  @Test
  public void testIndexing3() throws IndexException {
    testIndexing(3);
  }
  @Test
  public void testIndexing4() throws IndexException {
    testIndexing(4);
  }
  @Test
  public void testIndexing5() throws IndexException {
    testIndexing(5);
  }*/

  private void testIndexing(int nb) {
    System.out.println("Indexing "+nb+" start");
    init();
    manager.indexNewContent(index, indexing);
    // wait a bit
    int DELAY = 2;
    TestUtils.wait(DELAY);
    IndexReader reader;
    try {
      reader = LuceneIndexQueries.grabReader(index);
      Assert.assertNotNull(reader);
      Assert.assertEquals(30, reader.numDocs());
      LuceneIndexQueries.release(index, reader);
    } finally {
      System.out.println("Indexing "+nb+" end");
    }
  }

}

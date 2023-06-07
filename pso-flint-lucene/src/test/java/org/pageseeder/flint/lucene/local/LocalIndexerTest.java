package org.pageseeder.flint.lucene.local;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.local.LocalIndexManager;
import org.pageseeder.flint.local.LocalIndexManagerFactory;
import org.pageseeder.flint.lucene.LuceneIndexQueries;
import org.pageseeder.flint.lucene.LuceneLocalIndex;
import org.pageseeder.flint.lucene.utils.TestListener;
import org.pageseeder.flint.lucene.utils.TestUtils;
import org.slf4j.LoggerFactory;

import java.io.File;

public class LocalIndexerTest {

  private static File template     = new File("src/test/resources/template.xsl");
  private static File templatePSML = new File("src/test/resources/psml-template.xsl");
  private static File indexing     = new File("src/test/resources/indexing");
  private static File indexRoot1   = new File("tmp/index1");
  private static File indexRoot2   = new File("tmp/index2");

  private static int DELAY = 2;
  private static LuceneLocalIndex index1;
  private static LuceneLocalIndex index2;
  private static LocalIndexManager manager;

//  @Before
  public void init1() {
    if (!indexRoot1.exists()) indexRoot1.mkdir();
    // clean up previous test's data
    for (File f : indexRoot1.listFiles()) f.delete();
    indexRoot1.delete();
    try {
      index1 = new LuceneLocalIndex(indexRoot1, "local", new StandardAnalyzer(), indexing);
      index1.setTemplate("xml", template.toURI());
      index1.setTemplate("psml", templatePSML.toURI());
    } catch (Exception ex) {
      LoggerFactory.getLogger(TestUtils.class).error("Something went wrong", ex);
    }
    manager = LocalIndexManagerFactory.createMultiThreads(5, new TestListener());
    System.out.println("Starting manager!");
  }
  public void init2() {
    if (!indexRoot2.exists()) indexRoot2.mkdir();
    // clean up previous test's data
    for (File f : indexRoot2.listFiles()) f.delete();
    indexRoot2.delete();
    try {
      index2 = new LuceneLocalIndex(indexRoot2, "local", new StandardAnalyzer(), indexing);
      index2.setTemplate("xml", template.toURI());
      index2.setTemplate("psml", templatePSML.toURI());
    } catch (Exception ex) {
      LoggerFactory.getLogger(TestUtils.class).error("Something went wrong", ex);
    }
  }

//  @After
//  public void after() {
//     // stop index
//    System.out.println("Clearing index!");
//    manager.clear(index);
//    // wait a bit
//    TestUtils.wait(1);
//    System.out.println("-----------------------------------");
//  }

  @AfterClass
  public static void after() {
    // stop index
    System.out.println("Stopping manager!");
    manager.shutdown();
    System.out.println("-----------------------------------");
  }

  /**
   * @throws IndexException
   */
  @Test
  public void testIndexing1() throws IndexException {
    init1();
    manager.indexNewContent(index1, indexing);
    // wait a bit
    TestUtils.wait(DELAY);
    IndexReader reader;
    reader = LuceneIndexQueries.grabReader(index1);
    Assert.assertEquals(30, reader.numDocs());
    LuceneIndexQueries.release(index1, reader);
  }

  /**
   * @throws IndexException
   */
  @Test
  public void testIndexing2() throws IndexException {
    init2();
    for (File f : indexing.listFiles()) {
      manager.indexFile(index2, f);
    }
    // wait a bit
    TestUtils.wait(DELAY);
    IndexReader reader;
    reader = LuceneIndexQueries.grabReader(index2);
    Assert.assertEquals(30, reader.numDocs());
    LuceneIndexQueries.release(index2, reader);
  }
}

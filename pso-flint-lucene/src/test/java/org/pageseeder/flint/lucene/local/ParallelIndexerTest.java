package org.pageseeder.flint.lucene.local;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.local.LocalIndexManager;
import org.pageseeder.flint.local.LocalIndexManagerFactory;
import org.pageseeder.flint.lucene.LuceneIndexQueries;
import org.pageseeder.flint.lucene.LuceneLocalIndex;
import org.pageseeder.flint.lucene.utils.TestListener;
import org.pageseeder.flint.lucene.utils.TestUtils;

import java.io.File;

@RunWith(TestUtils.ParallelRunner.class)
public class ParallelIndexerTest {

  private static File template     = new File("src/test/resources/template.xsl");
  private static File templatePSML = new File("src/test/resources/psml-template.xsl");
  private static File indexing     = new File("src/test/resources/indexing");
  private static File indexRoot    = new File("tmp/index-parallel");

  private static int DELAY = 2;
  private static LuceneLocalIndex index = null;
  private static LocalIndexManager manager = null;
  private static Boolean creating = false;

  public synchronized static void init() {
    if (!indexRoot.exists()) indexRoot.mkdir();
    if (index == null) {
        try {
          index = new LuceneLocalIndex(indexRoot, new StandardAnalyzer(), indexing);
          index.setTemplate("xml", template.toURI());
          index.setTemplate("psml", templatePSML.toURI());
        } catch (Exception ex) {
          ex.printStackTrace();
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
  public void testIndexing1() throws IndexException {
    testIndexing(1);
  }
  @Test
  public void testIndexing2() throws IndexException {
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

  private void testIndexing(int nb) throws IndexException {
    System.out.println("Indexing "+nb+" start");
    init();
    manager.indexNewContent(index, indexing);
    // wait a bit
    TestUtils.wait(DELAY);
    IndexReader reader;
    try {
      reader = LuceneIndexQueries.grabReader(index);
      Assert.assertEquals(30, reader.numDocs());
      LuceneIndexQueries.release(index, reader);
    } catch (IndexException ex) {
      ex.printStackTrace();
      Assert.fail();
    } finally {
      System.out.println("Indexing "+nb+" end");
    }
  }

}

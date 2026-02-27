package org.pageseeder.flint.lucene.local;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.pageseeder.flint.local.LocalIndexManager;
import org.pageseeder.flint.local.LocalIndexManagerFactory;
import org.pageseeder.flint.lucene.LuceneIndexQueries;
import org.pageseeder.flint.lucene.LuceneLocalIndex;
import org.pageseeder.flint.lucene.utils.TestListener;
import org.pageseeder.flint.lucene.utils.TestUtils;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;

public class DebounceIndexerTest {

  private static final File template  = new File("src/test/resources/template.xsl");
  private static final File indexing  = new File("src/test/resources/indexing");
  private static final File indexRootNormal = new File("tmp/index-normal");
  private static final File indexRoot = new File("tmp/index-debounce");

  private static final int delayBetweenIndexing = 300;
  private static final int DELAY = 2;
  private static LuceneLocalIndex index;
  private static LuceneLocalIndex indexNormal;
  private static LocalIndexManager manager;
  private static LocalIndexManager managerNormal;
  private static TestListener listener;

  public void initNormal() {
    if (listener == null) listener = new TestListener();
    managerNormal = LocalIndexManagerFactory.createMultiThreads(5, listener, Collections.singletonList("xml"), 0);
    System.out.println("Starting normal manager!");
    if (!indexRootNormal.exists()) indexRootNormal.mkdir();
    // clean up previous test's data
    for (File f : indexRootNormal.listFiles()) f.delete();
    indexRootNormal.delete();
    try {
      indexNormal = new LuceneLocalIndex(indexRootNormal, "local", new StandardAnalyzer(), indexing);
      indexNormal.setTemplate("xml", template.toURI());
    } catch (Exception ex) {
      LoggerFactory.getLogger(TestUtils.class).error("Something went wrong", ex);
    }
  }

  public void initDebounce() {
    if (listener == null) listener = new TestListener();
    manager = LocalIndexManagerFactory.createMultiThreads(5, listener, Collections.singletonList("xml"), delayBetweenIndexing + 100);
    System.out.println("Starting debounce manager!");
    if (!indexRoot.exists()) indexRoot.mkdir();
    // clean up previous test's data
    for (File f : indexRoot.listFiles()) f.delete();
    indexRoot.delete();
    try {
      index = new LuceneLocalIndex(indexRoot, "local", new StandardAnalyzer(), indexing);
      index.setTemplate("xml", template.toURI());
    } catch (Exception ex) {
      LoggerFactory.getLogger(TestUtils.class).error("Something went wrong", ex);
    }
  }

  @AfterClass
  public static void after() {
    // stop index
    System.out.println("Stopping managers!");
    if (manager != null) manager.shutdown();
    if (managerNormal != null) managerNormal.shutdown();
    System.out.println("-----------------------------------");
  }

  @Test
  public void testNoDebounce() {
    initNormal();
    listener.resetCount();
    int nbFiles = indexing.listFiles().length;
    for (File f : indexing.listFiles()) {
      managerNormal.indexFile(indexNormal, f);
      TestUtils.waitMs(delayBetweenIndexing);
      managerNormal.indexFile(indexNormal, f);
      TestUtils.waitMs(delayBetweenIndexing);
      managerNormal.indexFile(indexNormal, f);
    }
    // wait a bit
    TestUtils.wait(DELAY);
    IndexReader reader = LuceneIndexQueries.grabReader(indexNormal);
    Assert.assertEquals(30, reader.numDocs());
    Assert.assertEquals(listener.getJobsCount(), nbFiles * 3);
    LuceneIndexQueries.release(indexNormal, reader);
  }

  @Test
  public void testDebounce() {
    initDebounce();
    listener.resetCount();
    int nbFiles = indexing.listFiles().length;
    for (File f : indexing.listFiles()) {
      manager.indexFile(index, f);
      TestUtils.waitMs(delayBetweenIndexing);
      manager.indexFile(index, f);
      TestUtils.waitMs(delayBetweenIndexing);
      manager.indexFile(index, f);
    }
    // wait a bit
    TestUtils.wait(DELAY);
    IndexReader reader = LuceneIndexQueries.grabReader(index);
    Assert.assertEquals(30, reader.numDocs());
    Assert.assertEquals(listener.getJobsCount(), nbFiles);
    LuceneIndexQueries.release(index, reader);
  }
}

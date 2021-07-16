package org.pageseeder.flint.lucene;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.pageseeder.flint.Index;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.local.LocalIndexManager;
import org.pageseeder.flint.local.LocalIndexManagerFactory;
import org.pageseeder.flint.lucene.query.*;
import org.pageseeder.flint.lucene.search.Terms;
import org.pageseeder.flint.lucene.utils.TestListener;
import org.pageseeder.flint.lucene.utils.TestUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultipleIndexReaderTest {

  private static File template     = new File("src/test/resources/template.xsl");
  private static File templatePSML = new File("src/test/resources/psml-template.xsl");
  private static File indexing     = new File("src/test/resources/indexing");
  private static File indexRoot   = new File("tmp/indexes");

  private static int NB_INDEXES = 1000;
  private static Map<String, LuceneLocalIndex> indexes = new HashMap();
  private static LocalIndexManager manager;

  private final static ExecutorService threads = Executors.newCachedThreadPool();

  @Before
  public void init() {
    indexRoot.mkdir();
    for (int i = 1; i <= NB_INDEXES; i++) init(i);
    manager = LocalIndexManagerFactory.createMultiThreads(5, new TestListener());
    System.out.println("Starting manager!");
    for (LuceneLocalIndex index : indexes.values())
      manager.indexNewContent(index, indexing);
    // wait a bit
    while (manager.isIndexing()) {
      TestUtils.wait(1);
    }
    TestUtils.wait(2);
    System.out.println("Indexing done!");
  }

  public void init(int nb) {
    File myRoot = new File(indexRoot, "index"+nb);
    if (!myRoot.exists()) myRoot.mkdir();
    // clean up previous test's data
    for (File f : myRoot.listFiles()) f.delete();
    myRoot.delete();
    try {
      LuceneLocalIndex index = new LuceneLocalIndex(myRoot, new StandardAnalyzer(), indexing);
      index.setTemplate("xml", template.toURI());
      index.setTemplate("psml", templatePSML.toURI());
      indexes.put("index"+nb, index);
    } catch (Exception ex) {
      ex.printStackTrace();
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
  public void test1() throws IndexException, IOException {
    MultipleIndexReader multi1 = LuceneIndexQueries.getMultipleIndexReader(new ArrayList<>(indexes.values()));
    IndexReader reader1 = multi1.grab();
    Assert.assertEquals(30 * NB_INDEXES, reader1.numDocs());
    List<Term> terms = Terms.terms(reader1, "field1");
    Assert.assertEquals(13, terms.size());
    System.out.println("Searched multi indexes 1");

    int[] searchers1 = randoms();
    for (int i : searchers1) {
      if (!indexes.containsKey("index"+i))
        System.err.println("Invalid index 2 index"+i);
      else
        threads.execute(new ParallelSearcher(indexes.get("index"+i)));
    }
    // close a few indexes
    int[] toclose = randoms();
    for (int i : toclose) {
      if (!indexes.containsKey("index"+i))
        System.err.println("Invalid index 2 index"+i);
      else
        indexes.get("index"+i).close();
    }
    SearchResults results = LuceneIndexQueries.query(new ArrayList<>(indexes.values()), new PredicateSearchQuery("+field1:doc5 +field1:value2"));
    Assert.assertEquals(NB_INDEXES, results.getTotalNbOfResults());
    results.terminate();
    MultipleIndexReader multi2 = LuceneIndexQueries.getMultipleIndexReader(new ArrayList<>(indexes.values()));
    IndexReader reader2 = multi2.grab();
    Assert.assertEquals(30 * NB_INDEXES, reader2.numDocs());
    List<Term> terms2 = Terms.terms(reader2, "field1");
    Assert.assertEquals(13, terms2.size());
    System.out.println("Searched multi indexes 2");
    int[] searchers2 = randoms();
    for (int i : searchers2) {
      if (!indexes.containsKey("index"+i))
        System.err.println("Invalid index 3 index"+i);
      else
        threads.execute(new ParallelSearcher(indexes.get("index"+i)));
    }

    while (ParallelSearcher.searching > 0) {
      TestUtils.wait(1);
    }
    // release multi readers
    multi1.releaseSilently();
    multi2.releaseSilently();
  }

  private int[] randoms() {
    int nb = Math.round(NB_INDEXES / 3);
    Random r = new Random(System.nanoTime());
    int[] rands = new int[nb];
    for (int i = 0; i < nb; i++) {
      rands[i] = r.nextInt(NB_INDEXES) + 1; // avoid 0
    }
    return rands;
  }

  private static class ParallelSearcher implements Runnable {
    private static int searching = 0;
    private final Index index;
    ParallelSearcher(Index idx) {
      this.index = idx;
    }
    @Override
    public void run() {
      searching++;
      // read a single index at the same time
      if (index == null) System.out.println("null index");
      else try {
        IndexReader reader = LuceneIndexQueries.grabReader(index);
        if (reader == null) System.out.println("null reader for "+index.getIndexID());
        Assert.assertEquals(30, reader.numDocs());
        List<Term> terms = Terms.terms(reader, "field1");
        Assert.assertEquals(13, terms.size());
        SearchResults results = LuceneIndexQueries.query(index, new PredicateSearchQuery("+field1:doc8 +field1:value1"));
        Assert.assertEquals(1, results.getTotalNbOfResults());
        results.terminate();
        LuceneIndexQueries.releaseQuietly(index, reader);
        System.out.println("Searched index "+index.getIndexID());
      } catch (IndexException | IOException ex) {
        System.out.println(ex);
      }
      searching--;
    }
  }
}

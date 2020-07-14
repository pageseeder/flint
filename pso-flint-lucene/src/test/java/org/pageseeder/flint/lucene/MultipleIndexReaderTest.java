package org.pageseeder.flint.lucene;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.TermQuery;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.pageseeder.flint.Index;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.local.LocalIndexManager;
import org.pageseeder.flint.local.LocalIndexManagerFactory;
import org.pageseeder.flint.lucene.util.Highlighter;
import org.pageseeder.flint.lucene.utils.TestListener;
import org.pageseeder.flint.lucene.utils.TestUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MultipleIndexReaderTest {

  private static File template     = new File("src/test/resources/template.xsl");
  private static File templatePSML = new File("src/test/resources/psml-template.xsl");
  private static File indexing     = new File("src/test/resources/indexing");
  private static File indexRoot   = new File("tmp/indexes");

  private static int DELAY = 3;
  private static int NB_INDEXES = 100;
  private static Map<String, LuceneLocalIndex> indexes = new HashMap();
  private static LuceneLocalIndex index2;
  private static LocalIndexManager manager;

  @Before
  public void init() {
    indexRoot.mkdir();
    for (int i = 1; i <= NB_INDEXES; i++) init(i);
    manager = LocalIndexManagerFactory.createMultiThreads(5, new TestListener());
    System.out.println("Starting manager!");
    for (LuceneLocalIndex index : indexes.values())
      manager.indexNewContent(index, indexing);
    // wait a bit
    TestUtils.wait(DELAY);
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
  public void test1() throws IndexException {
    MultipleIndexReader multi = LuceneIndexQueries.getMultipleIndexReader(new ArrayList<>(indexes.values()));
    IndexReader reader = multi.grab();
    Assert.assertEquals(30 * indexes.size(), reader.numDocs());

    // read a single index at the same time
    Index index = indexes.get("index12");
    IndexReader single = LuceneIndexQueries.grabReader(index);
    Assert.assertEquals(30, single.numDocs());
    LuceneIndexQueries.releaseQuietly(index, single);

    // release multi reader
    multi.releaseSilently();
  }

}

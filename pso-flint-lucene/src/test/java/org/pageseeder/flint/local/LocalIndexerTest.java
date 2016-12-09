package org.pageseeder.flint.local;

import java.io.File;

import javax.xml.transform.TransformerException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.lucene.LuceneIndexQueries;
import org.pageseeder.flint.lucene.LuceneLocalIndex;
import org.pageseeder.flint.utils.TestListener;
import org.pageseeder.flint.utils.TestUtils;

public class LocalIndexerTest {

  private static File template     = new File("src/test/resources/template.xsl");
  private static File templatePSML = new File("src/test/resources/psml-template.xsl");
  private static File indexing     = new File("src/test/resources/indexing");
  private static File indexRoot    = new File("tmp/index");

  private LuceneLocalIndex index;
  private LocalIndexManager manager;
  
  @Before
  public void init() {
    // clean up previous test's data
    for (File f : indexRoot.listFiles()) f.delete();
    indexRoot.delete();
    index = new LuceneLocalIndex(indexRoot, new StandardAnalyzer(), indexing);
    try {
      this.index.setTemplate("xml", template.toURI());
      this.index.setTemplate("psml", templatePSML.toURI());
    } catch (TransformerException ex) {
      ex.printStackTrace();
    }
    manager = LocalIndexManagerFactory.createMultiThreads(new TestListener());
    System.out.println("Starting manager!");
  }

  @After
  public void after() {
    // stop index
    System.out.println("Stopping manager!");
    this.manager.shutdown();
    System.out.println("-----------------------------------");
  }

  /**
   * Tests the {BerliozEntityResolver#toFileName} method.
   * @throws IndexException 
   */
  @Test
  public void testIndexing1() throws IndexException {
    this.manager.indexNewContent(index, indexing);
    // wait a bit
    TestUtils.wait(1);
    IndexReader reader;
    try {
      reader = LuceneIndexQueries.grabReader(this.index);
      Assert.assertEquals(30, reader.maxDoc());
      LuceneIndexQueries.release(this.index, reader);
    } catch (IndexException ex) {
      ex.printStackTrace();
      Assert.fail();
    }
  }

  /**
   * Tests the {BerliozEntityResolver#toFileName} method.
   * @throws IndexException 
   */
  @Test
  public void testIndexing2() throws IndexException {
    for (File f : indexing.listFiles()) {
      this.manager.indexFile(index, f);
    }
    // wait a bit
    TestUtils.wait(1);
    IndexReader reader;
    try {
      reader = LuceneIndexQueries.grabReader(this.index);
      Assert.assertEquals(30, reader.maxDoc());
      LuceneIndexQueries.release(this.index, reader);
    } catch (IndexException ex) {
      ex.printStackTrace();
      Assert.fail();
    }
  }
}

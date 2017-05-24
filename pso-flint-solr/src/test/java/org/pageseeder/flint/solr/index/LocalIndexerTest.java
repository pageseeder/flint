package org.pageseeder.flint.solr.index;

import java.io.File;

import javax.xml.transform.TransformerException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.local.LocalIndexManager;
import org.pageseeder.flint.local.LocalIndexManagerFactory;
import org.pageseeder.flint.solr.SolrFlintException;
import org.pageseeder.flint.solr.query.TestUtils;

public class LocalIndexerTest {

  private static File template     = new File("src/test/resources/template.xsl");
  private static File templatePSML = new File("src/test/resources/psml-template.xsl");
  private static File indexing     = new File("src/test/resources/indexing");
  private static File indexRoot    = new File("tmp/index");

  private SolrLocalIndex index;
  private LocalIndexManager manager;
  
  @Before
  public void init() throws SolrFlintException {
    indexRoot.mkdirs();
    // clean up previous test's data
    for (File f : indexRoot.listFiles()) f.delete();
    indexRoot.delete();
    index = new SolrLocalIndex("test-solr-index", TestUtils.CATALOG, indexing);
    try {
      this.index.setTemplate("xml", template.toURI());
      this.index.setTemplate("psml", templatePSML.toURI());
    } catch (TransformerException ex) {
      ex.printStackTrace();
    }
    this.manager = LocalIndexManagerFactory.createMultiThreads(new TestListener());
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
    wait(2);
    // TODO
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
    wait(2);
    // TODO
  }

  private void wait(int time) {
    try {
      Thread.sleep(time * 1000);
    } catch (InterruptedException ex) {
      ex.printStackTrace();
      Assert.fail();
    }
  }
}

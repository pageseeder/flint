package org.pageseeder.flint.local;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.apache.lucene.index.IndexReader;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexJob.Priority;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.api.Requester;
import org.pageseeder.flint.content.SourceForwarder;
import org.pageseeder.flint.utils.TestListener;
import org.pageseeder.flint.utils.TestLocalIndexConfig;

public class LocalIndexerTest {

  private static File template     = new File("src/test/resources/template.xsl");
  private static File templatePSML = new File("src/test/resources/psml-template.xsl");
  private static File indexing     = new File("src/test/resources/indexing");
  private static File indexRoot    = new File("tmp/index");

  private LocalIndex index;
  private IndexManager manager;
  
  @Before
  public void init() {
    this.index = new LocalIndex(new TestLocalIndexConfig(indexRoot, indexing));
    try {
      this.index.setTemplate("xml", template.toURI());
      this.index.setTemplate("psml", templatePSML.toURI());
    } catch (TransformerException ex) {
      ex.printStackTrace();
    }
    this.manager = new IndexManager(new LocalFileContentFetcher(), new TestListener(), 3);
    List<String> types = new ArrayList<>();
    types.add("xml");
    types.add("psml");
    this.manager.setDefaultTranslator(new SourceForwarder(types, "UTF-8"));
    System.out.println("Starting manager!");
  }

  @After
  public void after() {
    // stop index
    System.out.println("Stopping manager!");
    this.manager.stop();
    // clean up
    for (File f : indexRoot.listFiles()) f.delete();
    indexRoot.delete();
    System.out.println("-----------------------------------");
  }

  /**
   * Tests the {BerliozEntityResolver#toFileName} method.
   * @throws IndexException 
   */
  @Test
  public void testIndexing1() throws IndexException {
    LocalIndexer indexer = new LocalIndexer(this.manager, this.index);
    indexer.indexFolder(indexing, null);
    // wait a bit
    wait(2);
    IndexReader reader;
    try {
      reader = manager.grabReader(this.index);
      Assert.assertEquals(30, reader.maxDoc());
      manager.release(this.index, reader);
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
    Requester requester = new Requester("Local indexer tester");
    for (File f : indexing.listFiles()) {
      this.manager.index(f.getAbsolutePath(), LocalFileContentType.SINGLETON, this.index, requester, Priority.HIGH, null);
    }
    // wait a bit
    wait(2);
    IndexReader reader;
    try {
      reader = manager.grabReader(this.index);
      Assert.assertEquals(30, reader.maxDoc());
      manager.release(this.index, reader);
    } catch (IndexException ex) {
      ex.printStackTrace();
      Assert.fail();
    }
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

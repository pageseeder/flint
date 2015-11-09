package org.pageseeder.flint.local;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.content.SourceForwarder;
import org.pageseeder.flint.util.Terms;

public class LocalIndexerTest {

  private static File template  = new File("src/test/resources/template.xsl");
  private static File documents = new File("src/test/resources/documents");
  private static File terms     = new File("src/test/resources/terms");
  private static File indexRoot = new File("tmp/index");

  private LocalIndex index;
  private IndexManager manager;
  
  @Before
  public void init() {
    this.index = new LocalIndex(indexRoot);
    this.index.setTemplate("xml", template.toURI());
    this.manager = new IndexManager(new LocalFileContentFetcher(), new TestListener());
    this.manager.setDefaultTranslator(new SourceForwarder("xml", "UTF-8"));
    this.manager.start();
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

//  /**
//   * Tests the {BerliozEntityResolver#toFileName} method.
//   */
//  @Test
//  public void testClear() {
//    LocalIndexer indexer = new LocalIndexer(this.manager, this.index);
//    // ad docs
//    indexer.indexDocuments(documents);
//    // wait a bit
//    wait(2);
//    // clear
//    indexer.clear();
//    // wait a bit
//    wait(2);
//    try {
//      IndexReader reader = manager.grabReader(this.index.getIndex());
//      Assert.assertEquals(1, reader.numDeletedDocs());
//      Assert.assertEquals(0, reader.numDocs());
//      manager.release(this.index.getIndex(), reader);
//    } catch (IndexException ex) {
//      ex.printStackTrace();
//      Assert.fail();
//    }
//  }
//
//  /**
//   * Tests the {BerliozEntityResolver#toFileName} method.
//   */
//  @Test
//  public void testIndexDocuments() {
//    LocalIndexer indexer = new LocalIndexer(this.manager, this.index);
//    indexer.indexDocuments(documents);
//    // wait a bit
//    wait(2);
//    IndexReader reader;
//    try {
//      reader = manager.grabReader(this.index.getIndex());
//      Assert.assertEquals(documents.listFiles().length, reader.numDocs());
//      manager.release(this.index.getIndex(), reader);
//    } catch (IndexException ex) {
//      ex.printStackTrace();
//      Assert.fail();
//    }
//  }

  /**
   * Tests the {BerliozEntityResolver#toFileName} method.
   * @throws IndexException 
   */
  @Test
  public void testTerms() throws IndexException {
    LocalIndexer indexer = new LocalIndexer(this.manager, this.index);
    indexer.indexDocuments(terms);
    // wait a bit
    wait(2);
    IndexReader reader;
    try {
      reader = manager.grabReader(this.index.getIndex());
      List<Term> terms = Terms.terms(reader, "field1");
      List<String> values = new ArrayList<>();
      for (Term t : terms) values.add(t.bytes().utf8ToString());
      Assert.assertArrayEquals(values.toArray(), new String[] {"value1", "value2", "value3", "value4", "value5", "value6"});
      manager.release(this.index.getIndex(), reader);
    } catch (IndexException | IOException ex) {
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

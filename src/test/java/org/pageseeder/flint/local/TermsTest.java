package org.pageseeder.flint.local;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.content.SourceForwarder;
import org.pageseeder.flint.util.Terms;
import org.pageseeder.flint.utils.TestUtils;

public class TermsTest {

  private static File template  = new File("src/test/resources/template.xsl");
  private static File documents = new File("src/test/resources/terms");
  private static File indexRoot = new File("tmp/index");

  private static LocalIndex index;
  private static IndexManager manager;
  
  @BeforeClass
  public static void init() {
    index = new LocalIndex(indexRoot);
    index.setTemplate("xml", template.toURI());
    manager = new IndexManager(new LocalFileContentFetcher(), new TestListener());
    manager.setDefaultTranslator(new SourceForwarder("xml", "UTF-8"));
    manager.start();
    System.out.println("Starting manager!");
    LocalIndexer indexer = new LocalIndexer(manager, index);
    indexer.indexDocuments(documents);
    System.out.println("Documents indexed!");
    // wait a bit
    TestUtils.wait(2);
  }

  @AfterClass
  public static void after() {
    // stop index
    System.out.println("Stopping manager!");
    manager.stop();
    // clean up
    for (File f : indexRoot.listFiles()) f.delete();
    indexRoot.delete();
    System.out.println("-----------------------------------");
  }

  @Test
  public void testValues() throws IndexException {
    IndexReader reader;
    try {
      reader = manager.grabReader(index.getIndex());
      List<String> values = Terms.values(reader, "field1");
      Assert.assertArrayEquals(values.toArray(), new String[] {"value1", "value2", "value3", "value4", "value5", "value6"});
      manager.release(index.getIndex(), reader);
    } catch (IndexException | IOException ex) {
      ex.printStackTrace();
      Assert.fail();
    }
  }

  @Test
  public void testTerms() throws IndexException {
    IndexReader reader;
    try {
      reader = manager.grabReader(index.getIndex());
      List<String> values = Terms.values(reader, "field2");
      Assert.assertArrayEquals(values.toArray(), new String[] {"value1", "value2", "value3", "value4", "value5"});
      Assert.assertEquals(1, reader.docFreq(new Term("field2", "value1")));
      Assert.assertEquals(2, reader.docFreq(new Term("field2", "value2")));
      Assert.assertEquals(2, reader.docFreq(new Term("field2", "value3")));
      Assert.assertEquals(2, reader.docFreq(new Term("field2", "value4")));
      Assert.assertEquals(1, reader.docFreq(new Term("field2", "value5")));
      manager.release(index.getIndex(), reader);
    } catch (IndexException | IOException ex) {
      ex.printStackTrace();
      Assert.fail();
    }
  }

  @Test
  public void testFuzzy1() throws IndexException {
    IndexReader reader;
    try {
      reader = manager.grabReader(index.getIndex());
      List<String> values = Terms.fuzzy(index.getIndex(), reader, new Term("fuzzy1", "clove"));
      Assert.assertTrue(values.contains("clove"));
      Assert.assertTrue(values.contains("close"));
      Assert.assertTrue(values.contains("clone"));
      Assert.assertTrue(values.contains("glove"));
      manager.release(index.getIndex(), reader);
    } catch (IndexException | IOException ex) {
      ex.printStackTrace();
      Assert.fail();
    }
  }

}

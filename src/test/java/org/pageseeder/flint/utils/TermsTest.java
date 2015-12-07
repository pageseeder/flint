package org.pageseeder.flint.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.content.SourceForwarder;
import org.pageseeder.flint.local.LocalFileContentFetcher;
import org.pageseeder.flint.local.LocalIndex;
import org.pageseeder.flint.local.LocalIndexer;
import org.pageseeder.flint.util.Bucket;
import org.pageseeder.flint.util.Terms;

public class TermsTest {

  private static File template  = new File("src/test/resources/template.xsl");
  private static File documents = new File("src/test/resources/terms");
  private static File indexRoot = new File("tmp/index");

  private static LocalIndex index;
  private static IndexManager manager;
  
  @BeforeClass
  public static void init() {
    index = new LocalIndex(new TestLocalIndexConfig(indexRoot, documents));
    try {
      index.setTemplate("xml", template.toURI());
    } catch (TransformerException ex) {
      ex.printStackTrace();
    }
    manager = new IndexManager(new LocalFileContentFetcher(), new TestListener());
    manager.setDefaultTranslator(new SourceForwarder("xml", "UTF-8"));
    System.out.println("Starting manager!");
    LocalIndexer indexer = new LocalIndexer(manager, index);
    indexer.index(documents);
    System.out.println("Documents indexed!");
    // wait a bit
    TestUtils.wait(1);
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
  public void testFields() throws IndexException {
    IndexReader reader;
    try {
      reader = manager.grabReader(index);
      List<String> fields = Terms.fields(reader);
      Assert.assertArrayEquals(fields.toArray(), new String[] {"field1", "field2", "fuzzy1", "prefix1", "prefix2"});
      manager.release(index, reader);
    } catch (IndexException | IOException ex) {
      ex.printStackTrace();
      Assert.fail();
    }
  }

  @Test
  public void testValues() throws IndexException {
    IndexReader reader;
    try {
      reader = manager.grabReader(index);
      List<String> values = Terms.values(reader, "field1");
      Assert.assertArrayEquals(values.toArray(), new String[] {"value1", "value2", "value3", "value4", "value5", "value6"});
      manager.release(index, reader);
    } catch (IndexException | IOException ex) {
      ex.printStackTrace();
      Assert.fail();
    }
  }

  @Test
  public void testTerms() throws IndexException {
    IndexReader reader;
    try {
      reader = manager.grabReader(index);
      List<String> values = Terms.values(reader, "field2");
      Assert.assertArrayEquals(values.toArray(), new String[] {"value1", "value2", "value3", "value4", "value5"});
      Assert.assertEquals(1, reader.docFreq(new Term("field2", "value1")));
      Assert.assertEquals(2, reader.docFreq(new Term("field2", "value2")));
      Assert.assertEquals(2, reader.docFreq(new Term("field2", "value3")));
      Assert.assertEquals(2, reader.docFreq(new Term("field2", "value4")));
      Assert.assertEquals(1, reader.docFreq(new Term("field2", "value5")));
      manager.release(index, reader);
    } catch (IndexException | IOException ex) {
      ex.printStackTrace();
      Assert.fail();
    }
  }

  @Test
  public void testPrefixValues1() throws IndexException {
    IndexReader reader;
    try {
      reader = manager.grabReader(index);
      List<String> values = Terms.prefix(index, reader, new Term("prefix1", "pre"));
      Assert.assertTrue(values.contains("president"));
      Assert.assertTrue(values.contains("pretense"));
      Assert.assertTrue(values.contains("pretentious"));
      Assert.assertTrue(values.contains("preference"));
      Assert.assertTrue(values.contains("prepare"));
      Assert.assertTrue(values.contains("pretext"));
      Assert.assertTrue(values.contains("pressing"));
      values = Terms.prefix(index, reader, new Term("prefix1", "pret"));
      Assert.assertTrue(values.contains("pretense"));
      Assert.assertTrue(values.contains("pretentious"));
      Assert.assertTrue(values.contains("pretext"));
      values = Terms.prefix(index, reader, new Term("prefix1", "preten"));
      Assert.assertTrue(values.contains("pretense"));
      Assert.assertTrue(values.contains("pretentious"));
      manager.release(index, reader);
    } catch (IndexException | IOException ex) {
      ex.printStackTrace();
      Assert.fail();
    }
  }

  @Test
  public void testPrefixValues2() throws IndexException {
    IndexReader reader;
    try {
      reader = manager.grabReader(index);
      List<String> values = new ArrayList<>();
      Terms.prefix(index, reader, values, Collections.singletonList("prefix2"), "fro");
      Assert.assertTrue(values.contains("frog"));
      Assert.assertTrue(values.contains("fromage"));
      manager.release(index, reader);
    } catch (IndexException | IOException ex) {
      ex.printStackTrace();
      Assert.fail();
    }
  }

  @Test
  public void testFuzzyValues() throws IndexException {
    IndexReader reader;
    try {
      reader = manager.grabReader(index);
      List<String> values = Terms.fuzzy(index, reader, new Term("fuzzy1", "clove"));
      Assert.assertTrue(values.contains("close"));
      Assert.assertTrue(values.contains("clone"));
      Assert.assertTrue(values.contains("glove"));
      manager.release(index, reader);
    } catch (IndexException | IOException ex) {
      ex.printStackTrace();
      Assert.fail();
    }
  }

  @Test
  public void testFuzzyTerms() throws IndexException {
    IndexReader reader;
    try {
      reader = manager.grabReader(index);
      Bucket<Term> terms = new Bucket<>(6);
      Terms.fuzzy(index, reader, terms, new Term("fuzzy1", "clove"));
      Assert.assertEquals(1, terms.count(new Term("fuzzy1", "close")));
      Assert.assertEquals(1, terms.count(new Term("fuzzy1", "clone")));
      Assert.assertEquals(2, terms.count(new Term("fuzzy1", "glove")));
      manager.release(index, reader);
    } catch (IndexException | IOException ex) {
      ex.printStackTrace();
      Assert.fail();
    }
  }

}

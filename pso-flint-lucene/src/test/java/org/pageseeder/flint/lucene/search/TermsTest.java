package org.pageseeder.flint.lucene.search;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.local.LocalIndexManager;
import org.pageseeder.flint.local.LocalIndexManagerFactory;
import org.pageseeder.flint.lucene.LuceneIndexQueries;
import org.pageseeder.flint.lucene.LuceneLocalIndex;
import org.pageseeder.flint.lucene.util.Bucket;
import org.pageseeder.flint.lucene.utils.TestListener;
import org.pageseeder.flint.lucene.utils.TestUtils;

public class TermsTest {

  private static File template  = new File("src/test/resources/template.xsl");
  private static File documents = new File("src/test/resources/terms");
  private static File indexRoot = new File("tmp/index");

  private static LuceneLocalIndex index;
  private static LocalIndexManager manager;
  
  @BeforeClass
  public static void init() {
    // clean up last test's data
    for (File f : indexRoot.listFiles()) f.delete();
    indexRoot.delete();
    // create new
    index = new LuceneLocalIndex(indexRoot, new StandardAnalyzer(), documents);
    try {
      index.setTemplate("xml", template.toURI());
    } catch (TransformerException ex) {
      ex.printStackTrace();
    }
    manager = LocalIndexManagerFactory.createMultiThreads(new TestListener());
    System.out.println("Starting manager!");
    manager.indexNewContent(index, documents);
    System.out.println("Documents indexed!");
    // wait a bit
    TestUtils.wait(1);
  }

  @AfterClass
  public static void after() {
    // stop index
    System.out.println("Stopping manager!");
    index.close();
    manager.shutdown();
    System.out.println("-----------------------------------");
  }

  @Test
  public void testFields() throws IndexException {
    IndexReader reader;
    try {
      reader = LuceneIndexQueries.grabReader(index);
      List<String> fields = Terms.fields(reader);
System.out.println(fields);
      Assert.assertEquals(9, fields.size());
      Assert.assertTrue(fields.contains("_src"));
      Assert.assertTrue(fields.contains("_path"));
      Assert.assertTrue(fields.contains("_lastmodified"));
      Assert.assertTrue(fields.contains("_creator"));
      Assert.assertTrue(fields.contains("field1"));
      Assert.assertTrue(fields.contains("field2"));
      Assert.assertTrue(fields.contains("fuzzy1"));
      Assert.assertTrue(fields.contains("prefix1"));
      Assert.assertTrue(fields.contains("prefix2"));
      Assert.assertEquals(2, reader.getDocCount("_src"));
      Assert.assertEquals(2, reader.getDocCount("_path"));
      Assert.assertEquals(2, reader.getDocCount("_lastmodified"));
      Assert.assertEquals(2, reader.getDocCount("_creator"));
      Assert.assertEquals(2, reader.getDocCount("field1"));
      Assert.assertEquals(2, reader.getDocCount("field2"));
      Assert.assertEquals(2, reader.getDocCount("fuzzy1"));
      Assert.assertEquals(2, reader.getDocCount("prefix1"));
      Assert.assertEquals(2, reader.getDocCount("prefix2"));
      LuceneIndexQueries.release(index, reader);
    } catch (IndexException | IOException ex) {
      ex.printStackTrace();
      Assert.fail();
    }
  }

  @Test
  public void testFields2() throws IndexException {
    IndexReader reader;
    File doc3 = null;
    try {
      // index new doc
      doc3 = TestUtils.createFile(documents, "doc3.xml", "<documents version=\"5.0\"><document><field name=\"field3\">value3</field></document></documents>");
      manager.indexFile(index, doc3);
      // wait a bit
      TestUtils.wait(1);
      // check fields
      reader = LuceneIndexQueries.grabReader(index);
      List<String> fields = Terms.fields(reader);
      Assert.assertEquals(10, fields.size());
      Assert.assertTrue(fields.contains("_src"));
      Assert.assertTrue(fields.contains("_path"));
      Assert.assertTrue(fields.contains("_lastmodified"));
      Assert.assertTrue(fields.contains("_creator"));
      Assert.assertTrue(fields.contains("field1"));
      Assert.assertTrue(fields.contains("field2"));
      Assert.assertTrue(fields.contains("field3"));
      Assert.assertTrue(fields.contains("fuzzy1"));
      Assert.assertTrue(fields.contains("prefix1"));
      Assert.assertTrue(fields.contains("prefix2"));
      Assert.assertEquals(3, reader.getDocCount("_src"));
      Assert.assertEquals(3, reader.getDocCount("_path"));
      Assert.assertEquals(3, reader.getDocCount("_lastmodified"));
      Assert.assertEquals(3, reader.getDocCount("_creator"));
      Assert.assertEquals(2, reader.getDocCount("field1"));
      Assert.assertEquals(2, reader.getDocCount("field2"));
      Assert.assertEquals(1, reader.getDocCount("field3"));
      Assert.assertEquals(2, reader.getDocCount("fuzzy1"));
      Assert.assertEquals(2, reader.getDocCount("prefix1"));
      Assert.assertEquals(2, reader.getDocCount("prefix2"));
      LuceneIndexQueries.release(index, reader);
      // delete doc3
      doc3.delete();
      manager.indexFile(index, doc3);
      // wait a bit
      TestUtils.wait(1);
      // check fields
      reader = LuceneIndexQueries.grabReader(index);
      fields = Terms.fields(reader);
      Assert.assertEquals(9, fields.size());
      Assert.assertTrue(fields.contains("_src"));
      Assert.assertTrue(fields.contains("_path"));
      Assert.assertTrue(fields.contains("_lastmodified"));
      Assert.assertTrue(fields.contains("_creator"));
      Assert.assertTrue(fields.contains("field1"));
      Assert.assertTrue(fields.contains("field2"));
      Assert.assertTrue(fields.contains("fuzzy1"));
      Assert.assertTrue(fields.contains("prefix1"));
      Assert.assertTrue(fields.contains("prefix2"));
      Assert.assertEquals(2, reader.getDocCount("_path"));
      Assert.assertEquals(2, reader.getDocCount("field1"));
      Assert.assertEquals(2, reader.getDocCount("field2"));
      Assert.assertEquals(0, reader.getDocCount("field3"));
      Assert.assertEquals(2, reader.getDocCount("fuzzy1"));
      Assert.assertEquals(2, reader.getDocCount("prefix1"));
      Assert.assertEquals(2, reader.getDocCount("prefix2"));
      LuceneIndexQueries.release(index, reader);
    } catch (IndexException | IOException ex) {
      ex.printStackTrace();
      Assert.fail();
    } finally {
      // cleanup
      if (doc3 != null && doc3.exists()) doc3.delete();
    }
  }

  @Test
  public void testValues() throws IndexException {
    IndexReader reader;
    try {
      reader = LuceneIndexQueries.grabReader(index);
      List<String> values = Terms.values(reader, "field1");
      Assert.assertArrayEquals(values.toArray(), new String[] {"value1", "value2", "value3", "value4", "value5", "value6"});
      LuceneIndexQueries.release(index, reader);
    } catch (IndexException | IOException ex) {
      ex.printStackTrace();
      Assert.fail();
    }
  }

  @Test
  public void testTerms() throws IndexException {
    IndexReader reader;
    try {
      reader = LuceneIndexQueries.grabReader(index);
      List<String> values = Terms.values(reader, "field2");
      Assert.assertArrayEquals(values.toArray(), new String[] {"value1", "value2", "value3", "value4", "value5"});
      Assert.assertEquals(1, reader.docFreq(new Term("field2", "value1")));
      Assert.assertEquals(2, reader.docFreq(new Term("field2", "value2")));
      Assert.assertEquals(2, reader.docFreq(new Term("field2", "value3")));
      Assert.assertEquals(2, reader.docFreq(new Term("field2", "value4")));
      Assert.assertEquals(1, reader.docFreq(new Term("field2", "value5")));
      LuceneIndexQueries.release(index, reader);
    } catch (IndexException | IOException ex) {
      ex.printStackTrace();
      Assert.fail();
    }
  }

  @Test
  public void testTerms2() throws IndexException {
    IndexReader reader;
    File doc3 = null;
    try {
      // index new doc
      doc3 = TestUtils.createFile(documents, "doc3.xml", "<documents version=\"5.0\"><document><field name=\"field2\">value3 value5</field></document></documents>");
      manager.indexFile(index, doc3);
      // wait a bit
      TestUtils.wait(1);
      // check terms
      reader = LuceneIndexQueries.grabReader(index);
      try {
        List<String> values = Terms.values(reader, "field2");
        Assert.assertArrayEquals(values.toArray(), new String[] {"value1", "value2", "value3", "value4", "value5"});
        Assert.assertEquals(1, reader.docFreq(new Term("field2", "value1")));
        Assert.assertEquals(2, reader.docFreq(new Term("field2", "value2")));
        Assert.assertEquals(3, reader.docFreq(new Term("field2", "value3")));
        Assert.assertEquals(2, reader.docFreq(new Term("field2", "value4")));
        Assert.assertEquals(2, reader.docFreq(new Term("field2", "value5")));
      } finally {
        LuceneIndexQueries.release(index, reader);
      }
      // delete doc3
      doc3.delete();
      manager.indexFile(index, doc3);
      // wait a bit
      TestUtils.wait(1);
      // check terms
      reader = LuceneIndexQueries.grabReader(index);
      try {
        List<String> values = Terms.values(reader, "field2");
        Assert.assertArrayEquals(values.toArray(), new String[] {"value1", "value2", "value3", "value4", "value5"});
        Assert.assertEquals(1, reader.docFreq(new Term("field2", "value1")));
        Assert.assertEquals(2, reader.docFreq(new Term("field2", "value2")));
        Assert.assertEquals(2, reader.docFreq(new Term("field2", "value3")));
        Assert.assertEquals(2, reader.docFreq(new Term("field2", "value4")));
        Assert.assertEquals(1, reader.docFreq(new Term("field2", "value5")));
      } finally {
        LuceneIndexQueries.release(index, reader);
      }
    } catch (IndexException | IOException ex) {
      ex.printStackTrace();
      Assert.fail();
    } finally {
      // cleanup
      if (doc3 != null && doc3.exists()) doc3.delete();
    }
  }

  @Test
  public void testPrefixValues1() throws IndexException {
    IndexReader reader = null;
    try {
      reader = LuceneIndexQueries.grabReader(index);
      List<String> values = Terms.prefix(reader, new Term("prefix1", "pre"));
      Assert.assertFalse(values.contains("pre"));
      Assert.assertTrue(values.contains("president"));
      Assert.assertTrue(values.contains("pretense"));
      Assert.assertTrue(values.contains("pretentious"));
      Assert.assertTrue(values.contains("preference"));
      Assert.assertTrue(values.contains("prepare"));
      Assert.assertTrue(values.contains("pretext"));
      Assert.assertTrue(values.contains("pressing"));
      values = Terms.prefix(reader, new Term("prefix1", "pret"));
      Assert.assertFalse(values.contains("pret"));
      Assert.assertTrue(values.contains("pretense"));
      Assert.assertTrue(values.contains("pretentious"));
      Assert.assertTrue(values.contains("pretext"));
      values = Terms.prefix(reader, new Term("prefix1", "preten"));
      Assert.assertFalse(values.contains("preten"));
      Assert.assertTrue(values.contains("pretense"));
      Assert.assertTrue(values.contains("pretentious"));
    } catch (IndexException | IOException ex) {
      ex.printStackTrace();
      Assert.fail();
    } finally {
      LuceneIndexQueries.releaseQuietly(index, reader);
    }
  }

//  @Test
//  public void testPrefixValues2() throws IndexException {
//    IndexReader reader;
//    try {
//      reader = LuceneIndexQueries.grabReader(index);
//      List<String> values = new ArrayList<>();
//      Terms.prefix(index, reader, values, Collections.singletonList("prefix2"), "fro");
//      Assert.assertTrue(values.contains("frog"));
//      Assert.assertTrue(values.contains("fromage"));
//      LuceneIndexQueries.release(index, reader);
//    } catch (IndexException | IOException ex) {
//      ex.printStackTrace();
//      Assert.fail();
//    }
//  }

  @Test
  public void testFuzzyValues() throws IndexException {
    IndexReader reader;
    try {
      reader = LuceneIndexQueries.grabReader(index);
      List<String> values = Terms.fuzzy(reader, new Term("fuzzy1", "clove"));
      Assert.assertTrue(values.contains("close"));
      Assert.assertTrue(values.contains("clone"));
      Assert.assertTrue(values.contains("glove"));
      LuceneIndexQueries.release(index, reader);
    } catch (IndexException | IOException ex) {
      ex.printStackTrace();
      Assert.fail();
    }
  }

  @Test
  public void testFuzzyTerms() throws IndexException {
    IndexReader reader;
    try {
      reader = LuceneIndexQueries.grabReader(index);
      Bucket<Term> terms = new Bucket<>(6);
      Terms.fuzzy(reader, terms, new Term("fuzzy1", "clove"));
      Assert.assertEquals(1, terms.count(new Term("fuzzy1", "close")));
      Assert.assertEquals(1, terms.count(new Term("fuzzy1", "clone")));
      Assert.assertEquals(2, terms.count(new Term("fuzzy1", "glove")));
      LuceneIndexQueries.release(index, reader);
    } catch (IndexException | IOException ex) {
      ex.printStackTrace();
      Assert.fail();
    }
  }
}

package org.pageseeder.flint.solr.query;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.local.LocalIndexManager;
import org.pageseeder.flint.local.LocalIndexManagerFactory;
import org.pageseeder.flint.solr.index.SolrLocalIndex;
import org.pageseeder.flint.solr.index.TestListener;
import org.pageseeder.flint.solr.query.AutoSuggest.Suggestion;

public class AutoSuggestTest {

  private final static File template  = new File("src/test/resources/template.xsl");
  private final static File documents = new File("src/test/resources/autosuggest");
  private final static File indexRoot = new File("tmp/index");
  private static final String CATALOG_AUTOSUGGEST = "testing_autosuggest";
  private final static String AUTOSUGGEST_NAME = "test_autosuggest";
  private final static String AUTOSUGGEST_CONTEXT_NAME = "test_autosuggest_context";
  private final static String AUTOSUGGEST_WEIGHTED1_NAME = "test_autosuggest_weight1";
  private final static String AUTOSUGGEST_WEIGHTED2_NAME = "test_autosuggest_weight2";
  private final static String AUTOSUGGEST_WEIGHTED3_NAME = "test_autosuggest_weight3";

  private SolrLocalIndex index;
  private LocalIndexManager manager;
  
  @Before
  public void init() {
    indexRoot.mkdirs();
    // clean up previous test's data
    for (File f : indexRoot.listFiles()) f.delete();
    indexRoot.delete();
    index = new SolrLocalIndex("test-solr-autosuggest", CATALOG_AUTOSUGGEST, documents);
    try {
      index.setTemplate("xml", template.toURI());
    } catch (TransformerException ex) {
      ex.printStackTrace();
    }
    // just in case
    try {
      index.getIndexIO().clearIndex();
    } catch (IndexException ex) {
      ex.printStackTrace();
    }
    manager = LocalIndexManagerFactory.createMultiThreads(new TestListener());
    System.out.println("Starting manager!");
    manager.indexNewContent(index);
    System.out.println("Documents indexed!");
    // wait a bit
    TestUtils.wait(1);
  }

  @After
  public void after() {
    // stop index
    System.out.println("Stopping manager!");
    manager.shutdown();
    System.out.println("-----------------------------------");
  }

  @Test
  public void testAutoSuggestReuseFields() throws IOException {
    AutoSuggest as = new AutoSuggest(index, AUTOSUGGEST_NAME);
    List<Suggestion> suggestions = as.suggest("elec", 5);
    Assert.assertEquals(2, suggestions.size());
    for (Suggestion sug : suggestions) {
      Assert.assertTrue(sug.text.equals("electric guitar") ||
                        sug.text.equals("electric train"));
    }
    // index new doc
    File doc5 = TestUtils.createFile(documents, "doc5.xml", "<documents version='5.0'><document>"
                                                            + "<field name='test_text_autosuggest'>electra doll</field>"
                                                          + "</document></documents>");
    manager.indexFile(index, doc5);
    // wait a bit
    TestUtils.wait(1);
    // try again
    try {
      suggestions = as.suggest("elec", 5);
      Assert.assertEquals(3, suggestions.size());
      for (Suggestion sug : suggestions) {
        Assert.assertTrue(sug.text.equals("electric guitar") ||
                          sug.text.equals("electric train") ||
                          sug.text.equals("electra doll"));
      }
    } finally {
      // delete doc5
      if (!doc5.delete())
        System.err.println("Failed to delete document "+doc5.getAbsolutePath());
      // reindex
      manager.indexFile(index, doc5);
      // wait a bit
      TestUtils.wait(1);
    }
    // check again
    suggestions = as.suggest("elec", 5);
    Assert.assertEquals(2, suggestions.size());
    for (Suggestion sug : suggestions) {
      Assert.assertTrue(sug.text.equals("electric guitar") ||
                        sug.text.equals("electric train"));
    }
  }

  @Test
  public void testAutoSuggestFields() {
    AutoSuggest as = new AutoSuggest(index, AUTOSUGGEST_NAME);
    List<Suggestion> suggestions = as.suggest("elec", 5);
    Assert.assertEquals(2, suggestions.size());
    for (Suggestion sug : suggestions) {
      Assert.assertTrue(sug.text.equals("electric guitar") ||
                        sug.text.equals("electric train"));
    }
    suggestions = as.suggest("gui", 5);
    Assert.assertEquals(2, suggestions.size());
    for (Suggestion sug : suggestions) {
      Assert.assertTrue(sug.text.equals("electric guitar") ||
                        sug.text.equals("acoustic guitar"));
    }
  }

  @Test
  public void testAutoSuggestFieldsWithContext() {
    AutoSuggest as = new AutoSuggest(index, AUTOSUGGEST_CONTEXT_NAME);
    List<Suggestion> suggestions = as.suggest("elec", "blue", 5);
    Assert.assertEquals(2, suggestions.size());
    for (Suggestion sug : suggestions) {
      Assert.assertTrue(sug.text.equals("electric guitar") ||
                        sug.text.equals("electric train"));
    }
    suggestions = as.suggest("elec", "yellow", 5);
    Assert.assertEquals(1, suggestions.size());
    Suggestion sug = suggestions.get(0);
    Assert.assertEquals(sug.text, "electric train");
    suggestions = as.suggest("elec", "red", 5);
    Assert.assertEquals(1, suggestions.size());
    sug = suggestions.get(0);
    Assert.assertEquals(sug.text, "electric guitar");
    suggestions = as.suggest("gui", "blue", 5);
    Assert.assertEquals(1, suggestions.size());
    sug = suggestions.get(0);
    Assert.assertEquals(sug.text, "electric guitar");
  }

  @Test
  public void testAutoSuggestWeights() {
    AutoSuggest as = new AutoSuggest(index, AUTOSUGGEST_WEIGHTED1_NAME);
    List<Suggestion> suggestions = as.suggest("weig", 5);
    Assert.assertEquals(4, suggestions.size());
    Assert.assertEquals(10, suggestions.get(0).weight);
    Assert.assertEquals(3,  suggestions.get(1).weight);
    Assert.assertEquals(1,  suggestions.get(2).weight);
    Assert.assertEquals(0,  suggestions.get(3).weight);
    Assert.assertEquals("weight 10 1",  suggestions.get(0).text);// 10
    Assert.assertEquals("weight 3",     suggestions.get(1).text);// 3
    Assert.assertEquals("weight 1 2",   suggestions.get(2).text);// 1
    Assert.assertEquals("weight",       suggestions.get(3).text);// 0
    // use weight1 and weight2
    as = new AutoSuggest(index, AUTOSUGGEST_WEIGHTED2_NAME);
    suggestions = as.suggest("weig", 5);
    Assert.assertEquals(4, suggestions.size());
    Assert.assertEquals(20,   suggestions.get(0).weight);
    Assert.assertEquals(15, suggestions.get(1).weight);
    Assert.assertEquals(1,  suggestions.get(2).weight);
    Assert.assertEquals(0,    suggestions.get(3).weight);
    Assert.assertEquals("weight 1 2",   suggestions.get(0).text);// 20
    Assert.assertEquals("weight 10 1",  suggestions.get(1).text);// 15
    Assert.assertEquals("weight 3",     suggestions.get(2).text);// 1
    Assert.assertEquals("weight",       suggestions.get(3).text);// 0
    // use weight2 only
    as = new AutoSuggest(index, AUTOSUGGEST_WEIGHTED3_NAME);
    suggestions = as.suggest("weig", 5);
    Assert.assertEquals(4, suggestions.size());
    Assert.assertEquals(8, suggestions.get(0).weight);
    Assert.assertEquals(4, suggestions.get(1).weight);
    Assert.assertEquals(0, suggestions.get(2).weight);
    Assert.assertEquals(0, suggestions.get(3).weight);
    Assert.assertEquals("weight 1 2",   suggestions.get(0).text);// 8
    Assert.assertEquals("weight 10 1",  suggestions.get(1).text);// 4
    Assert.assertTrue("weight 3".equals(suggestions.get(2).text) || "weight".equals(suggestions.get(2).text));// 0
    Assert.assertTrue("weight 3".equals(suggestions.get(3).text) || "weight".equals(suggestions.get(3).text));// 0
  }
//
//  @Test
//  public void testAutoSuggestObjects() throws IndexException {
//    IndexReader reader;
//    try {
//      AutoSuggest as = new AutoSuggest.Builder().index(index).useTerms(false).build();
//      reader = LuceneIndexQueries.grabReader(index);
//      as.addSearchField("name");
//      as.addResultField("name");
//      as.addResultField("color");
//      as.build(reader);
//      LuceneIndexQueries.release(index, reader);
//      List<Suggestion> suggestions = as.suggest("elec", 5);
//      Assert.assertEquals(3, suggestions.size());
//      for (Suggestion sug : suggestions) {
//        Assert.assertTrue(sug.text.equals("electric guitar") ||
//                          sug.text.equals("electric train"));
//        Assert.assertTrue(sug.highlight.equals("electric guitar") ||
//                          sug.highlight.equals("electric train"));
//        Assert.assertTrue(sug.document.containsKey("name"));
//        Assert.assertTrue(sug.document.containsKey("color"));
//        Assert.assertEquals(1, sug.document.get("name").length);
//        String name = sug.document.get("name")[0];
//        if ("electric train".equals(name)) {
//          Assert.assertEquals(1, sug.document.get("color").length);
//          String color = sug.document.get("color")[0];
//          Assert.assertTrue("blue".equals(color) || "yellow".equals(color));
//        } else if ("electric guitar".equals(name)) {
//          Assert.assertArrayEquals(new String[] {"red", "blue"}, sug.document.get("color"));
//        } else {
//          Assert.fail("Found invalid suggestion with name "+name);
//        }
//      }
//      suggestions = as.suggest("gui", 5);
//      Assert.assertEquals(2, suggestions.size());
//      for (Suggestion sug : suggestions) {
//        Assert.assertTrue(sug.text.equals("electric guitar") ||
//                          sug.text.equals("acoustic guitar"));
//        Assert.assertTrue(sug.highlight.equals("electric guitar") ||
//                          sug.highlight.equals("acoustic guitar"));
//        Assert.assertTrue(sug.document.containsKey("name"));
//        Assert.assertTrue(sug.document.containsKey("color"));
//        Assert.assertEquals(1, sug.document.get("name").length);
//        String name = sug.document.get("name")[0];
//        if ("acoustic guitar".equals(name)) {
//          Assert.assertArrayEquals(new String[] {"green"}, sug.document.get("color"));
//        } else if ("electric guitar".equals(name)) {
//          Assert.assertArrayEquals(new String[] {"red", "blue"}, sug.document.get("color"));
//        } else {
//          Assert.fail("Found invalid suggestion with name "+name);
//        }
//      }
//      as.close();
//    } catch (IndexException ex) {
//      ex.printStackTrace();
//      Assert.fail();
//    }
//  }
//
//  public static final class Toy implements Serializable {
//    private static final long serialVersionUID = 1L;
//    String name;
//    String colors;
//    public Toy(String n, String c) {
//      this.name = n;
//      this.colors = c;
//    }
//    @Override
//    public boolean equals(Object obj) {
//      if (!(obj instanceof Toy)) return false;
//      Toy t = (Toy) obj;
//      return this.name.equals(t.name) && this.colors.equals(t.colors);
//    }
//    @Override
//    public int hashCode() {
//      return this.name.hashCode() * 3 + this.colors.hashCode() * 11;
//    }
//  };
}

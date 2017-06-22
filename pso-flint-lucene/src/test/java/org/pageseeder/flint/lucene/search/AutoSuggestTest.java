package org.pageseeder.flint.search;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.Requester;
import org.pageseeder.flint.content.SourceForwarder;
import org.pageseeder.flint.indexing.IndexJob.Priority;
import org.pageseeder.flint.local.LocalFileContentFetcher;
import org.pageseeder.flint.local.LocalFileContentType;
import org.pageseeder.flint.local.LocalIndexer;
import org.pageseeder.flint.lucene.LuceneIndexQueries;
import org.pageseeder.flint.lucene.LuceneLocalIndex;
import org.pageseeder.flint.lucene.search.AutoSuggest;
import org.pageseeder.flint.lucene.search.AutoSuggest.Suggestion;
import org.pageseeder.flint.utils.TestListener;
import org.pageseeder.flint.utils.TestUtils;

public class AutoSuggestTest {

  private static File template  = new File("src/test/resources/template.xsl");
  private static File documents = new File("src/test/resources/autosuggest");
  private static File indexRoot = new File("tmp/index");

  private static LuceneLocalIndex index;
  private static IndexManager manager;
  
  @BeforeClass
  public static void init() {
    // clean up previous test's data
    for (File f : indexRoot.listFiles()) f.delete();
    indexRoot.delete();
    index = new LuceneLocalIndex(indexRoot, new StandardAnalyzer(), documents);
    try {
      index.setTemplate("xml", template.toURI());
    } catch (TransformerException ex) {
      ex.printStackTrace();
    }
    manager = new IndexManager(new LocalFileContentFetcher(), new TestListener());
    manager.setDefaultTranslator(new SourceForwarder("xml", "UTF-8"));
    System.out.println("Starting manager!");
    LocalIndexer indexer = new LocalIndexer(manager, index);
    indexer.indexFolder(documents, null);
    System.out.println("Documents indexed!");
    // wait a bit
    TestUtils.wait(1);
  }

  @AfterClass
  public static void after() {
    // stop index
    System.out.println("Stopping manager!");
    manager.stop();
    System.out.println("-----------------------------------");
  }

  @Test
  public void testAutoSuggestTerms() throws IndexException {
    IndexReader reader;
    try {
      AutoSuggest as = new AutoSuggest.Builder().index(index).useTerms(true).build();
      reader = LuceneIndexQueries.grabReader(index);
      as.addSearchField("fulltext");
      as.build(reader);
      List<Suggestion> suggestions = as.suggest("fro", 5);
      Assert.assertEquals(3, suggestions.size());
      for (Suggestion sug : suggestions) {
        Assert.assertTrue(sug.text.equals("frog") ||
                          sug.text.equals("fromage") ||
                          sug.text.equals("front"));
        Assert.assertTrue(sug.highlight.equals("<b>fro</b>g") ||
                          sug.highlight.equals("<b>fro</b>mage") ||
                          sug.highlight.equals("<b>fro</b>nt"));
      }
      LuceneIndexQueries.release(index, reader);
    } catch (IndexException ex) {
      ex.printStackTrace();
      Assert.fail();
    }
  }

  @Test
  public void testAutoSuggestReuseFields() throws IndexException {
    File tempRoot = new File("tmp/index-as");
    IndexReader reader;
    try {
      AutoSuggest as = new AutoSuggest.Builder().index(index).useTerms(false).directory(FSDirectory.open(tempRoot.toPath())).build();
      File doc5 = null;
      try {
        reader = LuceneIndexQueries.grabReader(index);
        as.addSearchField("name");
        as.build(reader);
        LuceneIndexQueries.release(index, reader);
        List<Suggestion> suggestions = as.suggest("elec", 5);
        Assert.assertEquals(2, suggestions.size());
        for (Suggestion sug : suggestions) {
          Assert.assertTrue(sug.text.equals("electric guitar") ||
                            sug.text.equals("electric train"));
          Assert.assertTrue(sug.highlight.equals("<b>elec</b>tric guitar") ||
                            sug.highlight.equals("<b>elec</b>tric train"));
        }
        // index new doc
        doc5 = TestUtils.createFile(documents, "doc5.xml", "<documents version=\"5.0\"><document><field name=\"name\">electra doll</field><field name=\"color\">pink</field></document></documents>");
        manager.index(doc5.getAbsolutePath(), LocalFileContentType.SINGLETON, index, new Requester("doc5 indexing"), Priority.HIGH, null);
        // wait a bit
        TestUtils.wait(1);
        // test current
        Assert.assertFalse(as.isCurrent(manager));
      } finally {
        as.close();
        // clean up
        for (File f : tempRoot.listFiles()) f.delete();
        tempRoot.delete();
      }
      // try again
      as = new AutoSuggest.Builder().index(index).useTerms(false).directory(FSDirectory.open(tempRoot.toPath())).build();
      as.addSearchField("name");
      try {
        // rebuild autosuggest
        reader = LuceneIndexQueries.grabReader(index);
        as.build(reader);
        LuceneIndexQueries.release(index, reader);
        List<Suggestion> suggestions = as.suggest("elec", 5);
        Assert.assertEquals(3, suggestions.size());
        for (Suggestion sug : suggestions) {
          Assert.assertTrue(sug.text.equals("electric guitar") ||
                            sug.text.equals("electric train") ||
                            sug.text.equals("electra doll"));
          Assert.assertTrue(sug.highlight.equals("<b>elec</b>tric guitar") ||
                            sug.highlight.equals("<b>elec</b>tric train") ||
                            sug.highlight.equals("<b>elec</b>tra doll"));
        }
      } finally {
        // delete doc3
        if (!doc5.delete())
          System.err.println("Failed to delete document "+doc5.getAbsolutePath());
        manager.index(doc5.getAbsolutePath(), LocalFileContentType.SINGLETON, index, new Requester("doc5 deleting"), Priority.HIGH, null);
        as.close();
        // clean up
        for (File f : tempRoot.listFiles()) f.delete();
        tempRoot.delete();
      }
      // wait a bit
      TestUtils.wait(1);
      // test current
      Assert.assertFalse(as.isCurrent(manager));
      // try again
      as = new AutoSuggest.Builder().index(index).useTerms(false).directory(FSDirectory.open(tempRoot.toPath())).build();
      as.addSearchField("name");
      try {
        // check again
        reader = LuceneIndexQueries.grabReader(index);
        as.build(reader);
        LuceneIndexQueries.release(index, reader);
        List<Suggestion> suggestions = as.suggest("elec", 5);
        Assert.assertEquals(2, suggestions.size());
        for (Suggestion sug : suggestions) {
          Assert.assertTrue(sug.text.equals("electric guitar") ||
                            sug.text.equals("electric train"));
          Assert.assertTrue(sug.highlight.equals("<b>elec</b>tric guitar") ||
                            sug.highlight.equals("<b>elec</b>tric train"));
        }
      } finally {
        as.close();
        // clean up
        for (File f : tempRoot.listFiles()) f.delete();
        tempRoot.delete();
      }
    } catch (IndexException | IOException ex) {
      ex.printStackTrace();
      Assert.fail();
    }
  }

  @Test
  public void testAutoSuggestFields() throws IndexException {
    IndexReader reader;
    try {
      AutoSuggest as = new AutoSuggest.Builder().index(index).useTerms(false).build();
      reader = LuceneIndexQueries.grabReader(index);
      as.addSearchField("name");
      as.build(reader);
      LuceneIndexQueries.release(index, reader);
      List<Suggestion> suggestions = as.suggest("elec", 5);
      Assert.assertEquals(2, suggestions.size());
      for (Suggestion sug : suggestions) {
        Assert.assertTrue(sug.text.equals("electric guitar") ||
                          sug.text.equals("electric train"));
        Assert.assertTrue(sug.highlight.equals("<b>elec</b>tric guitar") ||
                          sug.highlight.equals("<b>elec</b>tric train"));
      }
      suggestions = as.suggest("gui", 5);
      Assert.assertEquals(2, suggestions.size());
      for (Suggestion sug : suggestions) {
        Assert.assertTrue(sug.text.equals("electric guitar") ||
                          sug.text.equals("acoustic guitar"));
        Assert.assertTrue(sug.highlight.equals("electric <b>gui</b>tar") ||
                          sug.highlight.equals("acoustic <b>gui</b>tar"));
      }
      as.close();
    } catch (IndexException ex) {
      ex.printStackTrace();
      Assert.fail();
    }
  }

  @Test
  public void testAutoSuggestFieldsWithCriteria() throws IndexException {
    IndexReader reader;
    try {
      AutoSuggest as = new AutoSuggest.Builder().index(index).useTerms(false).build();
      reader = LuceneIndexQueries.grabReader(index);
      as.addSearchField("name");
      as.setCriteriaField("color");
      as.build(reader);
      LuceneIndexQueries.release(index, reader);
      List<Suggestion> suggestions = as.suggest("elec", "blue", 5);
      Assert.assertEquals(2, suggestions.size());
      for (Suggestion sug : suggestions) {
        Assert.assertTrue(sug.text.equals("electric guitar") ||
                          sug.text.equals("electric train"));
        Assert.assertTrue(sug.highlight.equals("<b>elec</b>tric guitar") ||
                          sug.highlight.equals("<b>elec</b>tric train"));
      }
      suggestions = as.suggest("elec", "yellow", 5);
      Assert.assertEquals(1, suggestions.size());
      Suggestion sug = suggestions.get(0);
      Assert.assertEquals(sug.text, "electric train");
      Assert.assertEquals(sug.highlight, "<b>elec</b>tric train");
      suggestions = as.suggest("elec", "red", 5);
      Assert.assertEquals(1, suggestions.size());
      sug = suggestions.get(0);
      Assert.assertEquals(sug.text, "electric guitar");
      Assert.assertEquals(sug.highlight, "<b>elec</b>tric guitar");
      suggestions = as.suggest("gui", "blue", 5);
      Assert.assertEquals(1, suggestions.size());
      sug = suggestions.get(0);
      Assert.assertEquals(sug.text, "electric guitar");
      Assert.assertEquals(sug.highlight, "electric <b>gui</b>tar");
      as.close();
    } catch (IndexException ex) {
      ex.printStackTrace();
      Assert.fail();
    }
  }

  @Test
  public void testAutoSuggestWeights() throws IndexException {
    // search document without weighted values
    IndexReader reader = null;
    // then search document with weighted values
    try {
      reader = LuceneIndexQueries.grabReader(index);
      // use weight1 only
      AutoSuggest as = new AutoSuggest.Builder().index(index).useTerms(false).build();
      as.addSearchField("weighted-search");
      as.setWeight("weight1", 1);
      as.build(reader);
      List<Suggestion> suggestions = as.suggest("weig", 5);
      Assert.assertEquals(4, suggestions.size());
      Assert.assertEquals(1000, suggestions.get(0).weight);
      Assert.assertEquals(300,  suggestions.get(1).weight);
      Assert.assertEquals(100,  suggestions.get(2).weight);
      Assert.assertEquals(50,   suggestions.get(3).weight);
      Assert.assertEquals("weight 10 0.25",  suggestions.get(0).text);// 10
      Assert.assertEquals("weight 3",        suggestions.get(1).text);// 3
      Assert.assertEquals("weight",          suggestions.get(2).text);// 1
      Assert.assertEquals("weight 0.5 2",    suggestions.get(3).text);// 0.5
      as.close();
      // use weight1 and weight2
      as = new AutoSuggest.Builder().index(index).useTerms(false).build();
      as.addSearchField("weighted-search");
      as.setWeights("weight1:0.5,weight2:10");
      as.build(reader);
      suggestions = as.suggest("weig", 5);
      Assert.assertEquals(4, suggestions.size());
      Assert.assertEquals(2025,  suggestions.get(0).weight);
      Assert.assertEquals(1150,  suggestions.get(1).weight);
      Assert.assertEquals(1050,  suggestions.get(2).weight);
      Assert.assertEquals(750,   suggestions.get(3).weight);
      Assert.assertEquals("weight 0.5 2",    suggestions.get(0).text);// 20.25
      Assert.assertEquals("weight 3",        suggestions.get(1).text);// 11.5
      Assert.assertEquals("weight",          suggestions.get(2).text);// 10.5
      Assert.assertEquals("weight 10 0.25",  suggestions.get(3).text);// 7.5
      as.close();
      // use weight2 only
      as = new AutoSuggest.Builder().index(index).useTerms(false).build();
      as.addSearchField("weighted-search");
      as.setWeight("weight1", 0);
      as.setWeight("weight2", 4);
      as.build(reader);
      suggestions = as.suggest("weig", 5);
      Assert.assertEquals(4, suggestions.size());
      Assert.assertEquals(800, suggestions.get(0).weight);
      Assert.assertEquals(400, suggestions.get(1).weight);
      Assert.assertEquals(400, suggestions.get(2).weight);
      Assert.assertEquals(100, suggestions.get(3).weight);
      Assert.assertEquals("weight 0.5 2",    suggestions.get(0).text);// 8
      Assert.assertTrue("weight 3".equals(suggestions.get(1).text) || "weight"  .equals(suggestions.get(1).text));// 4
      Assert.assertTrue("weight"  .equals(suggestions.get(2).text) || "weight 3".equals(suggestions.get(2).text));// 4
      Assert.assertEquals("weight 10 0.25",  suggestions.get(3).text);// 1
    } catch (IndexException ex) {
      ex.printStackTrace();
      Assert.fail();
    } finally {
      LuceneIndexQueries.release(index, reader);
    }
  }

  @Test
  public void testAutoSuggestObjects() throws IndexException {
    IndexReader reader;
    try {
      AutoSuggest as = new AutoSuggest.Builder().index(index).useTerms(false).build();
      reader = LuceneIndexQueries.grabReader(index);
      as.addSearchField("name");
      as.addResultField("name");
      as.addResultField("color");
      as.build(reader);
      LuceneIndexQueries.release(index, reader);
      List<Suggestion> suggestions = as.suggest("elec", 5);
      Assert.assertEquals(3, suggestions.size());
      for (Suggestion sug : suggestions) {
        Assert.assertTrue(sug.text.equals("electric guitar") ||
                          sug.text.equals("electric train"));
        Assert.assertTrue(sug.highlight.equals("<b>elec</b>tric guitar") ||
                          sug.highlight.equals("<b>elec</b>tric train"));
        Assert.assertTrue(sug.document.containsKey("name"));
        Assert.assertTrue(sug.document.containsKey("color"));
        Assert.assertEquals(1, sug.document.get("name").length);
        String name = sug.document.get("name")[0];
        if ("electric train".equals(name)) {
          Assert.assertEquals(1, sug.document.get("color").length);
          String color = sug.document.get("color")[0];
          Assert.assertTrue("blue".equals(color) || "yellow".equals(color));
        } else if ("electric guitar".equals(name)) {
          Assert.assertArrayEquals(new String[] {"red", "blue"}, sug.document.get("color"));
        } else {
          Assert.fail("Found invalid suggestion with name "+name);
        }
      }
      suggestions = as.suggest("gui", 5);
      Assert.assertEquals(2, suggestions.size());
      for (Suggestion sug : suggestions) {
        Assert.assertTrue(sug.text.equals("electric guitar") ||
                          sug.text.equals("acoustic guitar"));
        Assert.assertTrue(sug.highlight.equals("electric <b>gui</b>tar") ||
                          sug.highlight.equals("acoustic <b>gui</b>tar"));
        Assert.assertTrue(sug.document.containsKey("name"));
        Assert.assertTrue(sug.document.containsKey("color"));
        Assert.assertEquals(1, sug.document.get("name").length);
        String name = sug.document.get("name")[0];
        if ("acoustic guitar".equals(name)) {
          Assert.assertArrayEquals(new String[] {"green"}, sug.document.get("color"));
        } else if ("electric guitar".equals(name)) {
          Assert.assertArrayEquals(new String[] {"red", "blue"}, sug.document.get("color"));
        } else {
          Assert.fail("Found invalid suggestion with name "+name);
        }
      }
      as.close();
    } catch (IndexException ex) {
      ex.printStackTrace();
      Assert.fail();
    }
  }

  public static final class Toy implements Serializable {
    private static final long serialVersionUID = 1L;
    String name;
    String colors;
    public Toy(String n, String c) {
      this.name = n;
      this.colors = c;
    }
    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Toy)) return false;
      Toy t = (Toy) obj;
      return this.name.equals(t.name) && this.colors.equals(t.colors);
    }
    @Override
    public int hashCode() {
      return this.name.hashCode() * 3 + this.colors.hashCode() * 11;
    }
  };
}

package org.pageseeder.flint.utils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexJob.Priority;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.api.Requester;
import org.pageseeder.flint.content.SourceForwarder;
import org.pageseeder.flint.local.LocalFileContentFetcher;
import org.pageseeder.flint.local.LocalFileContentType;
import org.pageseeder.flint.local.LocalIndex;
import org.pageseeder.flint.local.LocalIndexer;
import org.pageseeder.flint.util.AutoSuggest;
import org.pageseeder.flint.util.AutoSuggest.Suggestion;

public class AutoSuggestTest {

  private static File template  = new File("src/test/resources/template.xsl");
  private static File documents = new File("src/test/resources/autosuggest");
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
    // clean up
    for (File f : indexRoot.listFiles()) f.delete();
    indexRoot.delete();
    System.out.println("-----------------------------------");
  }

  @Test
  public void testAutoSuggestTerms() throws IndexException {
    IndexReader reader;
    try {
      AutoSuggest as = new AutoSuggest.Builder().index(index).useTerms(true).build();
      reader = manager.grabReader(index);
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
      manager.release(index, reader);
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
        reader = manager.grabReader(index);
        as.addSearchField("name");
        as.build(reader);
        manager.release(index, reader);
        List<Suggestion> suggestions = as.suggest("elec", 5);
        Assert.assertEquals(2, suggestions.size());
        for (Suggestion sug : suggestions) {
          Assert.assertTrue(sug.text.equals("electric guitar") ||
                            sug.text.equals("electric train"));
          Assert.assertTrue(sug.highlight.equals("<b>elec</b>tric guitar") ||
                            sug.highlight.equals("<b>elec</b>tric train"));
        }
        // index new doc
        doc5 = TestUtils.createFile(documents, "doc5.xml", "<documents version=\"3.0\"><document><field name=\"name\">electra doll</field><field name=\"color\">pink</field></document></documents>");
        manager.index(doc5.getAbsolutePath(), LocalFileContentType.SINGLETON, index, new Requester("doc5 indexing"), Priority.HIGH);
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
        reader = manager.grabReader(index);
        as.build(reader);
        manager.release(index, reader);
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
        doc5.delete();
        manager.index(doc5.getAbsolutePath(), LocalFileContentType.SINGLETON, index, new Requester("doc5 deleting"), Priority.HIGH);
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
        reader = manager.grabReader(index);
        as.build(reader);
        manager.release(index, reader);
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
      reader = manager.grabReader(index);
      as.addSearchField("name");
      as.build(reader);
      manager.release(index, reader);
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
      reader = manager.grabReader(index);
      as.addSearchField("name");
      as.setCriteriaField("color");
      as.build(reader);
      manager.release(index, reader);
      List<Suggestion> suggestions = as.suggest("elec", "blue", 5);
      Assert.assertEquals(2, suggestions.size());
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
  public void testAutoSuggestObjects() throws IndexException {
    IndexReader reader;
    try {
      AutoSuggest as = new AutoSuggest.Builder().index(index).useTerms(false).build();
      reader = manager.grabReader(index);
      as.addSearchField("name");
      as.addResultField("name");
      as.addResultField("color");
      as.build(reader);
      manager.release(index, reader);
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

package org.pageseeder.flint.utils;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
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
import org.pageseeder.flint.local.TestListener;
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
    index = new LocalIndex(indexRoot);
    index.setTemplate("xml", template.toURI());
    manager = new IndexManager(new LocalFileContentFetcher(), new TestListener());
    manager.setDefaultTranslator(new SourceForwarder("xml", "UTF-8"));
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
  public void testAutoSuggestTerms() throws IndexException {
    IndexReader reader;
    try {
      AutoSuggest as = AutoSuggest.terms(index.getIndex());
      reader = manager.grabReader(index.getIndex());
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
      manager.release(index.getIndex(), reader);
    } catch (IndexException ex) {
      ex.printStackTrace();
      Assert.fail();
    }
  }

  @Test
  public void testAutoSuggestFields() throws IndexException {
    IndexReader reader;
    try {
      AutoSuggest as = AutoSuggest.fields(index.getIndex());
      reader = manager.grabReader(index.getIndex());
      as.addSearchField("name");
      as.build(reader);
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
      manager.release(index.getIndex(), reader);
    } catch (IndexException ex) {
      ex.printStackTrace();
      Assert.fail();
    }
  }

  @Test
  public void testAutoSuggestFieldsWithCriteria() throws IndexException {
    IndexReader reader;
    try {
      AutoSuggest as = AutoSuggest.fields(index.getIndex());
      reader = manager.grabReader(index.getIndex());
      as.addSearchField("name");
      as.setCriteriaField("color");
      as.build(reader);
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
      manager.release(index.getIndex(), reader);
    } catch (IndexException ex) {
      ex.printStackTrace();
      Assert.fail();
    }
  }

  @Test
  public void testAutoSuggestObjects() throws IndexException {
    IndexReader reader;
    try {
      AutoSuggest as = AutoSuggest.documents(index.getIndex(), TOY_BUILDER);
      reader = manager.grabReader(index.getIndex());
      as.addSearchField("name");
      as.build(reader);
      List<Suggestion> suggestions = as.suggest("elec", 5);
      Assert.assertEquals(3, suggestions.size());
      for (Suggestion sug : suggestions) {
        Assert.assertTrue(sug.text.equals("electric guitar") ||
                          sug.text.equals("electric train"));
        Assert.assertTrue(sug.highlight.equals("<b>elec</b>tric guitar") ||
                          sug.highlight.equals("<b>elec</b>tric train"));
        Assert.assertTrue(sug.object.equals(new Toy("electric train", "blue")) ||
                          sug.object.equals(new Toy("electric train", "yellow")) ||
                          sug.object.equals(new Toy("electric guitar", "red,blue")));
      }
      suggestions = as.suggest("gui", 5);
      Assert.assertEquals(2, suggestions.size());
      for (Suggestion sug : suggestions) {
        Assert.assertTrue(sug.text.equals("electric guitar") ||
                          sug.text.equals("acoustic guitar"));
        Assert.assertTrue(sug.highlight.equals("electric <b>gui</b>tar") ||
                          sug.highlight.equals("acoustic <b>gui</b>tar"));
        Assert.assertTrue(sug.object.equals(new Toy("acoustic guitar", "green")) ||
                          sug.object.equals(new Toy("electric guitar", "red,blue")));
      }
      manager.release(index.getIndex(), reader);
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

  private AutoSuggest.ObjectBuilder TOY_BUILDER = new AutoSuggest.ObjectBuilder() {
    @Override
    public Toy documentToObject(Document document) {
      String[] colors = document.getValues("color");
      StringBuilder cs = new StringBuilder();
      for (int i = 0; i < colors.length; i++)
        cs.append(i == 0 ? "" : ",").append(colors[i]);
      return new Toy(document.get("name"), cs.toString());
    }
  };
}

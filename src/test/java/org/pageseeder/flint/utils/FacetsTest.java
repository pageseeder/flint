package org.pageseeder.flint.utils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.xml.transform.TransformerException;

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
import org.pageseeder.flint.search.FieldFacet;
import org.pageseeder.flint.util.Bucket;
import org.pageseeder.flint.util.Facets;

public class FacetsTest {

  private static File template  = new File("src/test/resources/template.xsl");
  private static File documents = new File("src/test/resources/facets");
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
    System.out.println("Documents indexed");
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
  public void testAllFacets() throws IndexException {
    try {
      List<String> fields  = Arrays.asList(new String[] {"field1", "field2", "field3"});
      List<String> values1 = Arrays.asList(new String[] {"value10", "value11", "value12", "value13", "value14", "value15"});
      List<String> values2 = Arrays.asList(new String[] {"value20", "value21", "value22", "value23", "value24", "value25"});
      List<String> values3 = Arrays.asList(new String[] {"value30"});
      List<FieldFacet> facets = Facets.getFacets(manager, 20, index);
      Assert.assertEquals(fields.size(), facets.size());
      for (FieldFacet f : facets) {
        Assert.assertTrue(fields.contains(f.name()));
        Bucket<Term> terms = f.getValues();
        List<String> values = "field1".equals(f.name()) ? values1 :
                              "field2".equals(f.name()) ? values2 : values3;
        Assert.assertEquals(values.size(), terms.items().size());
        for (String v : values) {
          Assert.assertTrue(terms.items().contains(new Term(f.name(), v)));
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }
  }

  @Test
  public void testMaxFacets() throws IndexException {
    try {
      List<FieldFacet> facets = Facets.getFacets(manager, 4, index);
      Assert.assertEquals(1, facets.size());
      FieldFacet facet = facets.iterator().next();
      Assert.assertEquals("field3", facet.name());
      Assert.assertEquals(1, facet.getValues().items().size());
      Assert.assertEquals(new Term("field3", "value30"), facet.getValues().iterator().next());
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }
  }

}
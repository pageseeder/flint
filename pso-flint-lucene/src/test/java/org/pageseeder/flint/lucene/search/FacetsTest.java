package org.pageseeder.flint.lucene.search;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.local.LocalIndexManager;
import org.pageseeder.flint.local.LocalIndexManagerFactory;
import org.pageseeder.flint.lucene.LuceneLocalIndex;
import org.pageseeder.flint.lucene.facet.FlexibleFieldFacet;
import org.pageseeder.flint.lucene.util.Bucket;
import org.pageseeder.flint.lucene.utils.TestListener;
import org.pageseeder.flint.lucene.utils.TestUtils;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class FacetsTest {

  private static final File template  = new File("src/test/resources/template.xsl");
  private static final File documents = new File("src/test/resources/facets");
  private static final FileFilter filter = file -> "facets.xml".equals(file.getName());
  private static final File indexRoot = new File("tmp/index");

  private static LuceneLocalIndex index;
  private static LocalIndexManager manager;

  @BeforeClass
  public static void init() {
    // clean up previous test's data
    if (indexRoot.exists()) for (File f : indexRoot.listFiles()) f.delete();
    indexRoot.delete();
    try {
      index = new LuceneLocalIndex(indexRoot, "facets", new StandardAnalyzer(), documents);
      index.setTemplate("xml", template.toURI());
    } catch (Exception ex) {
      LoggerFactory.getLogger(TestUtils.class).error("Something went wrong", ex);
    }
    manager = LocalIndexManagerFactory.createMultiThreads(new TestListener());
    System.out.println("Starting manager!");
    manager.indexNewContent(index, filter, documents);
    System.out.println("Documents indexed");
    // wait a bit
    TestUtils.wait(1);
  }

  @AfterClass
  public static void after() {
    // stop index
    System.out.println("Stopping manager!");
    manager.shutdown();
    System.out.println("-----------------------------------");
  }


  @Test
  public void testAllFacets() throws IndexException {
    try {
      List<String> fields  = Arrays.asList("field1", "field2", "field3");
      List<String> values1 = Arrays.asList("value10", "value11", "value12", "value13", "value14");
      List<String> values2 = Arrays.asList("value20", "value21", "value22", "value23", "value24");
      List<String> values3 = List.of("value30");
      List<FlexibleFieldFacet> facets = Facets.getFlexibleFacets(null, 20, index);
      Assert.assertEquals(fields.size(), facets.size());
      for (FlexibleFieldFacet f : facets) {
        Assert.assertTrue(fields.contains(f.name()));
        Bucket<String> terms = f.getValues();
        List<String> values = "field1".equals(f.name()) ? values1 :
                              "field2".equals(f.name()) ? values2 : values3;
        Assert.assertEquals(values.size(), terms.items().size());
        for (String v : values) {
          Assert.assertTrue(terms.items().contains(v));
        }
      }
    } catch (IOException ex) {
      LoggerFactory.getLogger(TestUtils.class).error("Something went wrong", ex);
      Assert.fail();
    }
  }

  @Test
  public void testMaxFacets() throws IndexException {
    try {
      List<String> fields  = Arrays.asList("field1", "field2", "field3");
      List<String> values1 = Arrays.asList("value11", "value12");
      List<String> values2 = Arrays.asList("value23", "value24");
      List<String> values3 = List.of("value30");
      List<FlexibleFieldFacet> facets = Facets.getFlexibleFacets(null, 2, index);
      Assert.assertEquals(fields.size(), facets.size());
      for (FlexibleFieldFacet f : facets) {
        Assert.assertTrue(fields.contains(f.name()));
        Bucket<String> terms = f.getValues();
        List<String> values = "field1".equals(f.name()) ? values1 :
                              "field2".equals(f.name()) ? values2 : values3;
        Assert.assertEquals(values.size(), terms.items().size());
        for (String v : values) {
          Assert.assertTrue(terms.items().contains(v));
        }
      }
    } catch (IOException ex) {
      LoggerFactory.getLogger(TestUtils.class).error("Something went wrong", ex);
      Assert.fail();
    }
  }

}
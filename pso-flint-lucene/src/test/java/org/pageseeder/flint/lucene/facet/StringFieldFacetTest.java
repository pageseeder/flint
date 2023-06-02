package org.pageseeder.flint.lucene.facet;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.local.LocalIndexManager;
import org.pageseeder.flint.local.LocalIndexManagerFactory;
import org.pageseeder.flint.lucene.LuceneIndexQueries;
import org.pageseeder.flint.lucene.LuceneLocalIndex;
import org.pageseeder.flint.lucene.search.Filter;
import org.pageseeder.flint.lucene.search.StringTermFilter;
import org.pageseeder.flint.lucene.util.Bucket;
import org.pageseeder.flint.lucene.utils.TestListener;
import org.pageseeder.flint.lucene.utils.TestUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class StringFieldFacetTest {

  private static File template  = new File("src/test/resources/template.xsl");
  private static File documents = new File("src/test/resources/facets");
  private static FileFilter filter = new FileFilter() { public boolean accept(File file) { return "stringfieldfacet.xml".equals(file.getName()); } };
  private static File indexRoot = new File("tmp/index");

  private static LuceneLocalIndex index;
  private static LocalIndexManager manager;
  private static IndexSearcher searcher;

  @BeforeClass
  public static void init() {
    // clean up previous test's data
    if (indexRoot.listFiles() != null) for (File f : indexRoot.listFiles()) f.delete();
    indexRoot.delete();
    try {
      index = new LuceneLocalIndex(indexRoot, "string", new StandardAnalyzer(), documents);
      index.setTemplate("xml", template.toURI());
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    manager = LocalIndexManagerFactory.createMultiThreads(new TestListener());
    System.out.println("Starting manager!");
    manager.indexNewContent(index, filter, documents);
    System.out.println("Documents indexed");
    // wait a bit
    TestUtils.wait(1);
    // prepare base query
    try {
      searcher = LuceneIndexQueries.grabSearcher(index);
    } catch (IndexException ex) {
      ex.printStackTrace();
    }
  }

  @AfterClass
  public static void after() {
    // close searcher
    try {
      LuceneIndexQueries.release(index, searcher);
    } catch (IndexException ex) {
      ex.printStackTrace();
    }
    // stop index
    System.out.println("Stopping manager!");
    manager.shutdown();
    System.out.println("-----------------------------------");
  }

  @Test
  public void testFacetsNoQuery() throws IndexException, IOException {
    StringFieldFacet facet = StringFieldFacet.newFacet("facet1");
    facet.compute(searcher, null);
    Assert.assertEquals(6, facet.getTotalTerms());
    Bucket<String> values = facet.getValues();
    Assert.assertEquals(6, values.items().size());
    Assert.assertEquals(1, values.count("value10"));
    Assert.assertEquals(2, values.count("value11"));
    Assert.assertEquals(1, values.count("value12"));
    Assert.assertEquals(1, values.count("value13"));
    Assert.assertEquals(1, values.count("value14"));
    Assert.assertEquals(1, values.count("value15"));
    // facets 2
    facet = StringFieldFacet.newFacet("facet2");
    facet.compute(searcher, null);
    Assert.assertEquals(6, facet.getTotalTerms());
    values = facet.getValues();
    Assert.assertEquals(6, values.items().size());
    Assert.assertEquals(1, values.count("value20"));
    Assert.assertEquals(1, values.count("value21"));
    Assert.assertEquals(1, values.count("value22"));
    Assert.assertEquals(1, values.count("value23"));
    Assert.assertEquals(2, values.count("value24"));
    Assert.assertEquals(1, values.count("value25"));
    // facets 3
    facet = StringFieldFacet.newFacet("facet3");
    facet.compute(searcher, null);
    Assert.assertEquals(2, facet.getTotalTerms());
    values = facet.getValues();
    Assert.assertEquals(2, values.items().size());
    Assert.assertEquals(6, values.count("value30"));
    Assert.assertEquals(1, values.count("value31"));
  }

  @Test
  public void testFacetsQueryNoTerms() throws IndexException, IOException {
    Query base = new TermQuery(new Term("facet3", "value30"));
    StringFieldFacet facet = StringFieldFacet.newFacet("facet1");
    facet.compute(searcher, base, 0);
    Assert.assertTrue(facet.hasResults());
    Assert.assertNull(facet.getValues());
    // facets 2
    facet = StringFieldFacet.newFacet("facet2");
    facet.compute(searcher, base, 0);
    Assert.assertTrue(facet.hasResults());
    Assert.assertNull(facet.getValues());
    // facets 3
    facet = StringFieldFacet.newFacet("facet3");
    facet.compute(searcher, base, 0);
    Assert.assertTrue(facet.hasResults());
    Assert.assertNull(facet.getValues());
    // facets 4
    facet = StringFieldFacet.newFacet("facet4");
    facet.compute(searcher, base, 0);
    Assert.assertFalse(facet.hasResults());
    Assert.assertNull(facet.getValues());
  }

  @Test
  public void testFacetsQuery() throws IndexException, IOException {
    Query base = new TermQuery(new Term("facet3", "value30"));
    StringFieldFacet facet = StringFieldFacet.newFacet("facet1");
    facet.compute(searcher, base);
    Assert.assertEquals(5, facet.getTotalTerms());
    Bucket<String> values = facet.getValues();
    Assert.assertEquals(5, values.items().size());
    Assert.assertEquals(1, values.count("value10"));
    Assert.assertEquals(2, values.count("value11"));
    Assert.assertEquals(1, values.count("value12"));
    Assert.assertEquals(1, values.count("value13"));
    Assert.assertEquals(1, values.count("value14"));
    // facets 2
    facet = StringFieldFacet.newFacet("facet2");
    facet.compute(searcher, base);
    Assert.assertEquals(5, facet.getTotalTerms());
    values = facet.getValues();
    Assert.assertEquals(5, values.items().size());
    Assert.assertEquals(1, values.count("value20"));
    Assert.assertEquals(1, values.count("value21"));
    Assert.assertEquals(1, values.count("value22"));
    Assert.assertEquals(1, values.count("value23"));
    Assert.assertEquals(2, values.count("value24"));
    // facets 3
    facet = StringFieldFacet.newFacet("facet3");
    facet.compute(searcher, base);
    Assert.assertEquals(1, facet.getTotalTerms());
    values = facet.getValues();
    Assert.assertEquals(1, values.items().size());
    Assert.assertEquals(6, values.count("value30"));
  }

  @Test
  public void testFlexibleFacetsQuery() throws IndexException, IOException {
    List<Filter> filters = Collections.singletonList(StringTermFilter.newFilter("facet3", "value30"));
    Query base = new TermQuery(new Term("field", "value"));
    StringFieldFacet facet = StringFieldFacet.newFacet("facet1");
    facet.compute(searcher, base, filters);
    Assert.assertEquals(5, facet.getTotalTerms());
    Bucket<String> values = facet.getValues();
    Assert.assertEquals(5, values.items().size());
    Assert.assertEquals(1, values.count("value10"));
    Assert.assertEquals(2, values.count("value11"));
    Assert.assertEquals(1, values.count("value12"));
    Assert.assertEquals(1, values.count("value13"));
    Assert.assertEquals(1, values.count("value14"));
    // facets 2
    facet = StringFieldFacet.newFacet("facet2");
    facet.compute(searcher, base, filters);
    Assert.assertEquals(5, facet.getTotalTerms());
    values = facet.getValues();
    Assert.assertEquals(5, values.items().size());
    Assert.assertEquals(1, values.count("value20"));
    Assert.assertEquals(1, values.count("value21"));
    Assert.assertEquals(1, values.count("value22"));
    Assert.assertEquals(1, values.count("value23"));
    Assert.assertEquals(2, values.count("value24"));
    // facets 3
    facet = StringFieldFacet.newFacet("facet3");
    facet.compute(searcher, base, filters);
    Assert.assertEquals(2, facet.getTotalTerms());
    values = facet.getValues();
    Assert.assertEquals(2, values.items().size());
    Assert.assertEquals(6, values.count("value30"));
    Assert.assertEquals(1, values.count("value31"));
  }

  @Test
  public void testFlexibleFacetsQueryNoTerms() throws IndexException, IOException {
    List<Filter> filters = Collections.singletonList(StringTermFilter.newFilter("facet3", "value30"));
    Query base = new TermQuery(new Term("field", "value"));
    StringFieldFacet facet = StringFieldFacet.newFacet("facet1");
    facet.compute(searcher, base, filters, 0);
    Assert.assertTrue(facet.hasResults());
    Assert.assertNull(facet.getValues());
    // facets 2
    facet = StringFieldFacet.newFacet("facet2");
    facet.compute(searcher, base, 0);
    Assert.assertTrue(facet.hasResults());
    Assert.assertNull(facet.getValues());
    // facets 3
    facet = StringFieldFacet.newFacet("facet3");
    facet.compute(searcher, base, 0);
    Assert.assertTrue(facet.hasResults());
    Assert.assertNull(facet.getValues());
    // facets 4
    facet = StringFieldFacet.newFacet("facet4");
    facet.compute(searcher, base, 0);
    Assert.assertTrue(facet.hasResults());
    Assert.assertNull(facet.getValues());
  }

}
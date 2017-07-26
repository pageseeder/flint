package org.pageseeder.flint.lucene.facet;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.xml.transform.TransformerException;

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
import org.pageseeder.flint.indexing.FlintField.NumericType;
import org.pageseeder.flint.local.LocalIndexManager;
import org.pageseeder.flint.local.LocalIndexManagerFactory;
import org.pageseeder.flint.lucene.LuceneIndexQueries;
import org.pageseeder.flint.lucene.LuceneLocalIndex;
import org.pageseeder.flint.lucene.facet.FlexibleRangeFacet.Range;
import org.pageseeder.flint.lucene.query.NumberParameter;
import org.pageseeder.flint.lucene.search.Filter;
import org.pageseeder.flint.lucene.search.NumericTermFilter;
import org.pageseeder.flint.lucene.util.Bucket;
import org.pageseeder.flint.lucene.utils.TestListener;
import org.pageseeder.flint.lucene.utils.TestUtils;

public class NumericRangeFacetTest {

  private static File template  = new File("src/test/resources/template.xsl");
  private static File documents = new File("src/test/resources/facets");
  private static FileFilter filter = new FileFilter() { public boolean accept(File file) { return "numericrangefacet.xml".equals(file.getName()); } };
  private static File indexRoot = new File("tmp/index");

  private static LuceneLocalIndex index;
  private static LocalIndexManager manager;
  private static IndexSearcher searcher;

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
    NumericRangeFacet facet = new NumericRangeFacet.Builder().name("facet1").numeric(NumericType.INT)
        .addRange(10, 12)
        .addRange(13, 15)
        .addRange(16, 20).build();
    facet.compute(searcher, null);
    Assert.assertEquals(0, facet.getTotalResults());
    Assert.assertEquals(2, facet.getTotalRanges());
    Bucket<Range> ranges = facet.getValues();
    Assert.assertEquals(2, ranges.items().size());
    Assert.assertEquals(4, ranges.count(Range.numericRange(10, 12)));
    Assert.assertEquals(3, ranges.count(Range.numericRange(13, 15)));
    // facets 2
    facet = new NumericRangeFacet.Builder().name("facet2").numeric(NumericType.INT)
        .addRange(20, 22)
        .addRange(23, 24)
        .addRange(25, 29).build();
    facet.compute(searcher, null);
    Assert.assertEquals(0, facet.getTotalResults());
    Assert.assertEquals(3, facet.getTotalRanges());
    ranges = facet.getValues();
    Assert.assertEquals(3, ranges.items().size());
    Assert.assertEquals(3, ranges.count(Range.numericRange(20, 22)));
    Assert.assertEquals(3, ranges.count(Range.numericRange(23, 24)));
    Assert.assertEquals(1, ranges.count(Range.numericRange(25, 29)));
    // facets 3
    facet = new NumericRangeFacet.Builder().name("facet3").numeric(NumericType.INT)
        .addRange(30, 32)
        .addRange(33, 38).build();
    facet.compute(searcher, null);
    Assert.assertEquals(0, facet.getTotalResults());
    Assert.assertEquals(1, facet.getTotalRanges());
    ranges = facet.getValues();
    Assert.assertEquals(1, ranges.items().size());
    Assert.assertEquals(7, ranges.count(Range.numericRange(30, 32)));
  }

  @Test
  public void testFacetsQuery() throws IndexException, IOException {
    Query base = NumberParameter.newIntParameter("facet3", 30).toQuery();
    NumericRangeFacet facet = new NumericRangeFacet.Builder().name("facet1").numeric(NumericType.INT)
        .addRange(10, 12)
        .addRange(13, 15)
        .addRange(16, 20).build();
    facet.compute(searcher, base);
    Assert.assertEquals(6, facet.getTotalResults());
    Assert.assertEquals(2, facet.getTotalRanges());
    Bucket<Range> ranges = facet.getValues();
    Assert.assertEquals(2, ranges.items().size());
    Assert.assertEquals(4, ranges.count(Range.numericRange(10, 12)));
    Assert.assertEquals(2, ranges.count(Range.numericRange(13, 15)));
    // facets 2
    facet = new NumericRangeFacet.Builder().name("facet2").numeric(NumericType.INT)
        .addRange(20, 22)
        .addRange(23, 24)
        .addRange(25, 29).build();
    facet.compute(searcher, base);
    Assert.assertEquals(6, facet.getTotalResults());
    Assert.assertEquals(2, facet.getTotalRanges());
    ranges = facet.getValues();
    Assert.assertEquals(2, ranges.items().size());
    Assert.assertEquals(3, ranges.count(Range.numericRange(20, 22)));
    Assert.assertEquals(3, ranges.count(Range.numericRange(23, 24)));
    // facets 3
    facet = new NumericRangeFacet.Builder().name("facet3").numeric(NumericType.INT)
        .addRange(30, 32)
        .addRange(33, 38).build();
    facet.compute(searcher, base);
    Assert.assertEquals(6, facet.getTotalResults());
    Assert.assertEquals(1, facet.getTotalRanges());
    ranges = facet.getValues();
    Assert.assertEquals(1, ranges.items().size());
    Assert.assertEquals(6, ranges.count(Range.numericRange(30, 32)));
  }

  @Test
  public void testFlexibleFacetsQuery() throws IndexException, IOException {
    List<Filter> filters = Collections.singletonList(NumericTermFilter.newFilter("facet3", 30));
    Query base = new TermQuery(new Term("field", "value"));
    NumericRangeFacet facet = new NumericRangeFacet.Builder().name("facet1").numeric(NumericType.INT)
        .addRange(10, 12)
        .addRange(13, 15)
        .addRange(16, 20).build();
    facet.compute(searcher, base, filters);
    Assert.assertEquals(6, facet.getTotalResults());
    Assert.assertEquals(2, facet.getTotalRanges());
    Bucket<Range> ranges = facet.getValues();
    Assert.assertEquals(2, ranges.items().size());
    Assert.assertEquals(4, ranges.count(Range.numericRange(10, 12)));
    Assert.assertEquals(2, ranges.count(Range.numericRange(13, 15)));
    // facets 2
    facet = new NumericRangeFacet.Builder().name("facet2").numeric(NumericType.INT)
        .addRange(20, 22)
        .addRange(23, 24)
        .addRange(25, 29).build();
    facet.compute(searcher, base, filters);
    Assert.assertEquals(6, facet.getTotalResults());
    Assert.assertEquals(2, facet.getTotalRanges());
    ranges = facet.getValues();
    Assert.assertEquals(2, ranges.items().size());
    Assert.assertEquals(3, ranges.count(Range.numericRange(20, 22)));
    Assert.assertEquals(3, ranges.count(Range.numericRange(23, 24)));
    // facets 3
    facet = new NumericRangeFacet.Builder().name("facet3").numeric(NumericType.INT)
        .addRange(30, 32)
        .addRange(33, 38).build();
    facet.compute(searcher, base, filters);
    Assert.assertEquals(7, facet.getTotalResults());
    Assert.assertEquals(1, facet.getTotalRanges());
    ranges = facet.getValues();
    Assert.assertEquals(1, ranges.items().size());
    Assert.assertEquals(7, ranges.count(Range.numericRange(30, 32)));

  }

}
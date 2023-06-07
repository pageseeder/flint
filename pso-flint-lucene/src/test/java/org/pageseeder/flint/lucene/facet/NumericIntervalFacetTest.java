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
import org.pageseeder.flint.indexing.FlintField.NumericType;
import org.pageseeder.flint.local.LocalIndexManager;
import org.pageseeder.flint.local.LocalIndexManagerFactory;
import org.pageseeder.flint.lucene.LuceneIndexQueries;
import org.pageseeder.flint.lucene.LuceneLocalIndex;
import org.pageseeder.flint.lucene.facet.FlexibleIntervalFacet.Interval;
import org.pageseeder.flint.lucene.query.NumberParameter;
import org.pageseeder.flint.lucene.search.Filter;
import org.pageseeder.flint.lucene.search.NumericTermFilter;
import org.pageseeder.flint.lucene.util.Bucket;
import org.pageseeder.flint.lucene.utils.TestListener;
import org.pageseeder.flint.lucene.utils.TestUtils;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;

public class NumericIntervalFacetTest {

  private static File template  = new File("src/test/resources/template.xsl");
  private static File documents = new File("src/test/resources/facets");
  private static FileFilter filter = new FileFilter() { public boolean accept(File file) { return "numericintervalfacet.xml".equals(file.getName()); } };
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
      index = new LuceneLocalIndex(indexRoot, "numericinterval", new StandardAnalyzer(), documents);
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
    // prepare base query
    searcher = LuceneIndexQueries.grabSearcher(index);
  }

  @AfterClass
  public static void after() {
    // close searcher
    LuceneIndexQueries.release(index, searcher);
    // stop index
    System.out.println("Stopping manager!");
    manager.shutdown();
    System.out.println("-----------------------------------");
  }

  @Test
  public void testFacetsNoQuery() throws IndexException, IOException, ParseException {
    NumericIntervalFacet facet = new NumericIntervalFacet.Builder().name("facet1").numeric(NumericType.INT).start(10).end(20).intervalLength(1).build();
    facet.compute(searcher);
    Bucket<Interval> intervals = facet.getValues();
    System.out.println(intervals);
    Assert.assertEquals(6, facet.getTotalIntervals());
    Assert.assertEquals(6, intervals.items().size());
    Assert.assertEquals(1, intervals.count(Interval.numericInterval(10, 11)));
    Assert.assertEquals(2, intervals.count(Interval.numericInterval(11, 12)));
    Assert.assertEquals(1, intervals.count(Interval.numericInterval(12, 13)));
    Assert.assertEquals(1, intervals.count(Interval.numericInterval(13, 14)));
    Assert.assertEquals(1, intervals.count(Interval.numericInterval(14, 15)));
    Assert.assertEquals(1, intervals.count(Interval.numericInterval(15, 16)));
    facet = new NumericIntervalFacet.Builder().name("facet1").numeric(NumericType.INT).start(12).end(14).intervalLength(1).build();
    facet.compute(searcher);
    intervals = facet.getValues();
    System.out.println(intervals);
    Assert.assertEquals(2, facet.getTotalIntervals());
    Assert.assertEquals(2, intervals.items().size());
    Assert.assertEquals(1, intervals.count(Interval.numericInterval(12, 13)));
    Assert.assertEquals(2, intervals.count(Interval.numericInterval(13, true, 14, true)));
    facet = new NumericIntervalFacet.Builder().name("facet2").numeric(NumericType.INT).start(18).end(28).intervalLength(3).build();
    facet.compute(searcher);
    intervals = facet.getValues();
    System.out.println(intervals);
    Assert.assertEquals(3, facet.getTotalIntervals());
    Assert.assertEquals(3, intervals.items().size());
    Assert.assertEquals(1, intervals.count(Interval.numericInterval(18, 21)));
    Assert.assertEquals(3, intervals.count(Interval.numericInterval(21, 24)));
    Assert.assertEquals(3, intervals.count(Interval.numericInterval(24, 27)));
    facet = new NumericIntervalFacet.Builder().name("facet3").numeric(NumericType.INT).start(25).end(40).intervalLength(10).build();
    facet.compute(searcher);
    intervals = facet.getValues();
    System.out.println(intervals);
    Assert.assertEquals(1, facet.getTotalIntervals());
    Assert.assertEquals(1, intervals.items().size());
    Assert.assertEquals(7, intervals.count(Interval.numericInterval(25, 35)));
  }

  @Test
  public void testFacetsQuery() throws IndexException, IOException, ParseException {
    Query base = NumberParameter.newIntParameter("facet3", 30).toQuery();
    NumericIntervalFacet facet = new NumericIntervalFacet.Builder().name("facet1").numeric(NumericType.INT).start(10).end(20).intervalLength(1).build();
    facet.compute(searcher, base);
    Bucket<Interval> intervals = facet.getValues();
    System.out.println(intervals);
    Assert.assertEquals(5, facet.getTotalIntervals());
    Assert.assertEquals(5, intervals.items().size());
    Assert.assertEquals(1, intervals.count(Interval.numericInterval(10, 11)));
    Assert.assertEquals(2, intervals.count(Interval.numericInterval(11, 12)));
    Assert.assertEquals(1, intervals.count(Interval.numericInterval(12, 13)));
    Assert.assertEquals(1, intervals.count(Interval.numericInterval(13, 14)));
    Assert.assertEquals(1, intervals.count(Interval.numericInterval(14, 15)));
    facet = new NumericIntervalFacet.Builder().name("facet1").numeric(NumericType.INT).start(12).end(14).intervalLength(1).build();
    facet.compute(searcher, base);
    intervals = facet.getValues();
    System.out.println(intervals);
    Assert.assertEquals(2, facet.getTotalIntervals());
    Assert.assertEquals(2, intervals.items().size());
    Assert.assertEquals(1, intervals.count(Interval.numericInterval(12, 13)));
    Assert.assertEquals(2, intervals.count(Interval.numericInterval(13, true, 14, true)));
    facet = new NumericIntervalFacet.Builder().name("facet2").numeric(NumericType.INT).start(18).end(28).intervalLength(3).build();
    facet.compute(searcher, base);
    intervals = facet.getValues();
    System.out.println(intervals);
    Assert.assertEquals(3, facet.getTotalIntervals());
    Assert.assertEquals(3, intervals.items().size());
    Assert.assertEquals(1, intervals.count(Interval.numericInterval(18, 21)));
    Assert.assertEquals(3, intervals.count(Interval.numericInterval(21, 24)));
    Assert.assertEquals(2, intervals.count(Interval.numericInterval(24, 27)));
    facet = new NumericIntervalFacet.Builder().name("facet3").numeric(NumericType.INT).start(25).end(40).intervalLength(10).build();
    facet.compute(searcher, base);
    intervals = facet.getValues();
    System.out.println(intervals);
    Assert.assertEquals(1, facet.getTotalIntervals());
    Assert.assertEquals(1, intervals.items().size());
    Assert.assertEquals(6, intervals.count(Interval.numericInterval(25, 35)));
  }

  @Test
  public void testFlexibleFacetsQuery() throws IndexException, IOException, ParseException {
    List<Filter> filters = Collections.singletonList(NumericTermFilter.newFilter("facet3", 30));
    Query base = new TermQuery(new Term("field", "value"));
    NumericIntervalFacet facet = new NumericIntervalFacet.Builder().numeric(NumericType.INT).name("facet1").start(10).end(20).intervalLength(1).build();
    facet.compute(searcher, base, filters);
    Bucket<Interval> intervals = facet.getValues();
    System.out.println(intervals);
    Assert.assertEquals(5, facet.getTotalIntervals());
    Assert.assertEquals(5, intervals.items().size());
    Assert.assertEquals(1, intervals.count(Interval.numericInterval(10, 11)));
    Assert.assertEquals(2, intervals.count(Interval.numericInterval(11, 12)));
    Assert.assertEquals(1, intervals.count(Interval.numericInterval(12, 13)));
    Assert.assertEquals(1, intervals.count(Interval.numericInterval(13, 14)));
    Assert.assertEquals(1, intervals.count(Interval.numericInterval(14, 15)));
    facet = new NumericIntervalFacet.Builder().name("facet1").numeric(NumericType.INT).start(12).end(14).intervalLength(1).build();
    facet.compute(searcher, base, filters);
    intervals = facet.getValues();
    System.out.println(intervals);
    Assert.assertEquals(2, facet.getTotalIntervals());
    Assert.assertEquals(2, intervals.items().size());
    Assert.assertEquals(1, intervals.count(Interval.numericInterval(12, 13)));
    Assert.assertEquals(2, intervals.count(Interval.numericInterval(13, true, 14, true)));
    facet = new NumericIntervalFacet.Builder().name("facet2").numeric(NumericType.INT).start(18).end(28).intervalLength(3).build();
    facet.compute(searcher, base, filters);
    intervals = facet.getValues();
    System.out.println(intervals);
    Assert.assertEquals(3, facet.getTotalIntervals());
    Assert.assertEquals(3, intervals.items().size());
    Assert.assertEquals(1, intervals.count(Interval.numericInterval(18, 21)));
    Assert.assertEquals(3, intervals.count(Interval.numericInterval(21, 24)));
    Assert.assertEquals(2, intervals.count(Interval.numericInterval(24, 27)));
    facet = new NumericIntervalFacet.Builder().name("facet3").numeric(NumericType.INT).start(25).end(40).intervalLength(10).build();
    facet.compute(searcher, base, filters);
    intervals = facet.getValues();
    System.out.println(intervals);
    Assert.assertEquals(1, facet.getTotalIntervals());
    Assert.assertEquals(1, intervals.items().size());
    Assert.assertEquals(7, intervals.count(Interval.numericInterval(25, 35)));
  }

}
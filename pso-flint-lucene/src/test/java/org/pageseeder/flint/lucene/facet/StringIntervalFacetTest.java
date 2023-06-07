package org.pageseeder.flint.lucene.facet;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.local.LocalIndexManager;
import org.pageseeder.flint.local.LocalIndexManagerFactory;
import org.pageseeder.flint.lucene.LuceneIndexQueries;
import org.pageseeder.flint.lucene.LuceneLocalIndex;
import org.pageseeder.flint.lucene.facet.FlexibleIntervalFacet.Interval;
import org.pageseeder.flint.lucene.query.TermParameter;
import org.pageseeder.flint.lucene.search.Filter;
import org.pageseeder.flint.lucene.search.StringTermFilter;
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

public class StringIntervalFacetTest {

  private static final File template  = new File("src/test/resources/template.xsl");
  private static final File documents = new File("src/test/resources/facets");
  private static final FileFilter filter = file -> "stringintervalfacet.xml".equals(file.getName());
  private static final File indexRoot = new File("tmp/index");

  private static LuceneLocalIndex index;
  private static LocalIndexManager manager;
  private static IndexSearcher searcher;

  @BeforeClass
  public static void init() {
    // clean up previous test's data
    if (indexRoot.listFiles() != null) for (File f : indexRoot.listFiles()) f.delete();
    indexRoot.delete();
    try {
      index = new LuceneLocalIndex(indexRoot, "stringinterval", new StandardAnalyzer(), documents);
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
    StringIntervalFacet facet = new StringIntervalFacet.Builder().name("facet1").start("a").intervalLength(1).build();
    facet.compute(searcher);
    Bucket<Interval> intervals = facet.getValues();
    System.out.println(intervals);
    Assert.assertEquals(5, facet.getTotalIntervals());
    Assert.assertEquals(5, intervals.items().size());
    Assert.assertEquals(2, intervals.count(Interval.stringInterval("a", "b")));
    Assert.assertEquals(2, intervals.count(Interval.stringInterval("b", "c")));
    Assert.assertEquals(1, intervals.count(Interval.stringInterval("c", "d")));
    Assert.assertEquals(1, intervals.count(Interval.stringInterval("d", "e")));
    Assert.assertEquals(1, intervals.count(Interval.stringInterval("e", "f")));
    facet = new StringIntervalFacet.Builder().name("facet1").start("c").end("eeeeee").intervalLength(1).build();
    facet.compute(searcher);
    intervals = facet.getValues();
    System.out.println(intervals);
    Assert.assertEquals(3, facet.getTotalIntervals());
    Assert.assertEquals(3, intervals.items().size());
    Assert.assertEquals(1, intervals.count(Interval.stringInterval("c", "d")));
    Assert.assertEquals(1, intervals.count(Interval.stringInterval("d", "e")));
    Assert.assertEquals(1, intervals.count(Interval.stringInterval("e", true, "eeeeee", true)));
    facet = new StringIntervalFacet.Builder().name("facet2").start("aaaaa").intervalLength(4).build();
    facet.compute(searcher);
    intervals = facet.getValues();
    System.out.println(intervals);
    Assert.assertEquals(5, facet.getTotalIntervals());
    Assert.assertEquals(5, intervals.items().size());
    Assert.assertEquals(2, intervals.count(Interval.stringInterval("aaaaa", "eaaaa")));
    Assert.assertEquals(1, intervals.count(Interval.stringInterval("eaaaa", "iaaaa")));
    Assert.assertEquals(2, intervals.count(Interval.stringInterval("maaaa", "qaaaa")));
    Assert.assertEquals(1, intervals.count(Interval.stringInterval("qaaaa", "uaaaa")));
    Assert.assertEquals(1, intervals.count(Interval.stringInterval("yaaaa", "{aaaa")));
    facet = new StringIntervalFacet.Builder().name("facet3").start("m").intervalLength(11).build();
    facet.compute(searcher);
    intervals = facet.getValues();
    System.out.println(intervals);
    Assert.assertEquals(4, facet.getTotalIntervals());
    Assert.assertEquals(4, intervals.items().size());
    Assert.assertEquals(1, intervals.count(Interval.stringInterval("/", "6")));
    Assert.assertEquals(1, intervals.count(Interval.stringInterval("L", "W")));
    Assert.assertEquals(1, intervals.count(Interval.stringInterval("b", "m")));
    Assert.assertEquals(4, intervals.count(Interval.stringInterval("m", "x")));
  }

  @Test
  public void testFacetsQuery() throws IndexException, IOException, ParseException {
    Query base = new TermParameter("field", "value1").toQuery();
    StringIntervalFacet facet = new StringIntervalFacet.Builder().name("facet1").start("a").intervalLength(1).build();
    facet.compute(searcher, base);
    Assert.assertEquals(4, facet.getTotalIntervals());
    Bucket<Interval> intervals = facet.getValues();
    Assert.assertEquals(4, intervals.items().size());
    Assert.assertEquals(2, intervals.count(Interval.stringInterval("a", "b")));
    Assert.assertEquals(2, intervals.count(Interval.stringInterval("b", "c")));
    Assert.assertEquals(1, intervals.count(Interval.stringInterval("c", "d")));
    Assert.assertEquals(1, intervals.count(Interval.stringInterval("d", "e")));
    facet = new StringIntervalFacet.Builder().name("facet2").start("aaaaa").intervalLength(4).build();
    facet.compute(searcher, base);
    Assert.assertEquals(4, facet.getTotalIntervals());
    intervals = facet.getValues();
    Assert.assertEquals(4, intervals.items().size());
    Assert.assertEquals(2, intervals.count(Interval.stringInterval("aaaaa", "eaaaa")));
    Assert.assertEquals(1, intervals.count(Interval.stringInterval("eaaaa", "iaaaa")));
    Assert.assertEquals(2, intervals.count(Interval.stringInterval("maaaa", "qaaaa")));
    Assert.assertEquals(1, intervals.count(Interval.stringInterval("qaaaa", "uaaaa")));
    facet = new StringIntervalFacet.Builder().name("facet3").start("m").intervalLength(11).build();
    facet.compute(searcher, base);
    Assert.assertEquals(3, facet.getTotalIntervals());
    intervals = facet.getValues();
    Assert.assertEquals(3, intervals.items().size());
    Assert.assertEquals(1, intervals.count(Interval.stringInterval("/", "6")));
    Assert.assertEquals(1, intervals.count(Interval.stringInterval("L", "W")));
    Assert.assertEquals(4, intervals.count(Interval.stringInterval("m", "x")));
  }

  @Test
  public void testFlexibleFacetsQuery() throws IndexException, IOException, ParseException {
    List<Filter> filters = Collections.singletonList(StringTermFilter.newFilter("facet3", "something"));
    Query base = new TermParameter("field", "value1").toQuery();
    StringIntervalFacet facet = new StringIntervalFacet.Builder().name("facet1").start("a").intervalLength(1).build();
    facet.compute(searcher, base, filters);
    Assert.assertEquals(2, facet.getTotalIntervals());
    Bucket<Interval> intervals = facet.getValues();
    Assert.assertEquals(2, intervals.items().size());
    Assert.assertEquals(1, intervals.count(Interval.stringInterval("a", "b")));
    Assert.assertEquals(1, intervals.count(Interval.stringInterval("d", "e")));
    facet = new StringIntervalFacet.Builder().name("facet2").start("aaaaa").intervalLength(4).build();
    facet.compute(searcher, base, filters);
    Assert.assertEquals(2, facet.getTotalIntervals());
    intervals = facet.getValues();
    Assert.assertEquals(2, intervals.items().size());
    Assert.assertEquals(1, intervals.count(Interval.stringInterval("aaaaa", "eaaaa")));
    Assert.assertEquals(1, intervals.count(Interval.stringInterval("qaaaa", "uaaaa")));
    facet = new StringIntervalFacet.Builder().name("facet3").start("m").intervalLength(11).build();
    facet.compute(searcher, base, filters);
    Assert.assertEquals(3, facet.getTotalIntervals());
    intervals = facet.getValues();
    Assert.assertEquals(3, intervals.items().size());
    Assert.assertEquals(1, intervals.count(Interval.stringInterval("/", "6")));
    Assert.assertEquals(1, intervals.count(Interval.stringInterval("L", "W")));
    Assert.assertEquals(4, intervals.count(Interval.stringInterval("m", "x")));
  }

}
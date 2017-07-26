package org.pageseeder.flint.lucene.facet;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Period;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import javax.xml.transform.TransformerException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DateTools.Resolution;
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
import org.pageseeder.flint.lucene.facet.FlexibleIntervalFacet.Interval;
import org.pageseeder.flint.lucene.query.DateParameter;
import org.pageseeder.flint.lucene.search.DateTermFilter;
import org.pageseeder.flint.lucene.search.Filter;
import org.pageseeder.flint.lucene.util.Bucket;
import org.pageseeder.flint.lucene.utils.TestListener;
import org.pageseeder.flint.lucene.utils.TestUtils;

public class DateIntervalFacetTest {

  private static File template  = new File("src/test/resources/template.xsl");
  private static File documents = new File("src/test/resources/facets");
  private static FileFilter filter = new FileFilter() { public boolean accept(File file) { return "dateintervalfacet.xml".equals(file.getName()); } };
  private static File indexRoot = new File("tmp/index");

  private static LuceneLocalIndex index;
  private static LocalIndexManager manager;
  private static IndexSearcher searcher;
  private static final Resolution day_resolution = Resolution.DAY;
  private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
  private static final ZoneId GMT = ZoneId.of("GMT");

  private static final Period ONE_MONTHS = Period.ofMonths(1);
  private static final Period TWO_MONTHS = Period.ofMonths(2);
  private static final Period SIX_DAYS   = Period.ofDays(6);

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
    format.setTimeZone(TimeZone.getTimeZone(GMT));
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
  public void testFacetsNoQuery() throws IndexException, IOException, ParseException {
    DateIntervalFacet facet = new DateIntervalFacet.Builder().name("facet1").resolution(day_resolution).intervalDate(ONE_MONTHS).start(format.parse("2017-05-05")).build();
    facet.compute(searcher, null);
    Assert.assertEquals(0, facet.getTotalResults());
    Assert.assertEquals(6, facet.getTotalIntervals());
    Bucket<Interval> intervals = facet.getValues();
    Assert.assertEquals(6, intervals.items().size());
    Assert.assertEquals(1, intervals.count(Interval.dateInterval(format.parse("2016-12-05"), format.parse("2017-01-05"), day_resolution)));
    Assert.assertEquals(1, intervals.count(Interval.dateInterval(format.parse("2017-01-05"), format.parse("2017-02-05"), day_resolution)));
    Assert.assertEquals(1, intervals.count(Interval.dateInterval(format.parse("2017-02-05"), format.parse("2017-03-05"), day_resolution)));
    Assert.assertEquals(2, intervals.count(Interval.dateInterval(format.parse("2017-03-05"), format.parse("2017-04-05"), day_resolution)));
    Assert.assertEquals(1, intervals.count(Interval.dateInterval(format.parse("2017-04-05"), format.parse("2017-05-05"), day_resolution)));
    Assert.assertEquals(1, intervals.count(Interval.dateInterval(format.parse("2017-05-05"), format.parse("2017-06-05"), day_resolution)));
    // facets 2
    facet = new DateIntervalFacet.Builder().name("facet2").resolution(day_resolution).intervalDate(TWO_MONTHS).start(format.parse("2016-05-24")).build();
    facet.compute(searcher, null);
    Assert.assertEquals(0, facet.getTotalResults());
    Assert.assertEquals(3, facet.getTotalIntervals());
    intervals = facet.getValues();
    Assert.assertEquals(3, intervals.items().size());
    Assert.assertEquals(1, intervals.count(Interval.dateInterval(format.parse("2015-11-24"), format.parse("2016-01-24"), day_resolution)));
    Assert.assertEquals(4, intervals.count(Interval.dateInterval(format.parse("2016-01-24"), format.parse("2016-03-24"), day_resolution)));
    Assert.assertEquals(2, intervals.count(Interval.dateInterval(format.parse("2016-03-24"), format.parse("2016-05-24"), day_resolution)));
    // facets 3
    facet = new DateIntervalFacet.Builder().name("facet3").resolution(day_resolution).intervalDate(SIX_DAYS).start(format.parse("2015-01-10")).build();
    facet.compute(searcher, null);
    Assert.assertEquals(0, facet.getTotalResults());
    Assert.assertEquals(2, facet.getTotalIntervals());
    intervals = facet.getValues();
    Assert.assertEquals(2, intervals.items().size());
    Assert.assertEquals(6, intervals.count(Interval.dateInterval(format.parse("2014-12-29"), format.parse("2015-01-04"), day_resolution)));
    Assert.assertEquals(1, intervals.count(Interval.dateInterval(format.parse("2015-01-28"), format.parse("2015-02-03"), day_resolution)));
  }

  @Test
  public void testFacetsQuery() throws IndexException, IOException, ParseException {
    DateParameter base = new DateParameter("facet3", format.parse("2015-01-01"), day_resolution, false);
    DateIntervalFacet facet = new DateIntervalFacet.Builder().name("facet1").resolution(day_resolution).intervalDate(ONE_MONTHS).start(format.parse("2017-05-05")).build();
    facet.compute(searcher, base.toQuery());
    Assert.assertEquals(6, facet.getTotalResults());
    Assert.assertEquals(5, facet.getTotalIntervals());
    Bucket<Interval> intervals = facet.getValues();
    Assert.assertEquals(5, intervals.items().size());
    Assert.assertEquals(1, intervals.count(Interval.dateInterval(format.parse("2016-12-05"), format.parse("2017-01-05"), day_resolution)));
    Assert.assertEquals(1, intervals.count(Interval.dateInterval(format.parse("2017-01-05"), format.parse("2017-02-05"), day_resolution)));
    Assert.assertEquals(1, intervals.count(Interval.dateInterval(format.parse("2017-02-05"), format.parse("2017-03-05"), day_resolution)));
    Assert.assertEquals(2, intervals.count(Interval.dateInterval(format.parse("2017-03-05"), format.parse("2017-04-05"), day_resolution)));
    Assert.assertEquals(1, intervals.count(Interval.dateInterval(format.parse("2017-04-05"), format.parse("2017-05-05"), day_resolution)));
    // facets 2
    facet = new DateIntervalFacet.Builder().name("facet2").resolution(day_resolution).intervalDate(TWO_MONTHS).start(format.parse("2016-05-24")).build();
    facet.compute(searcher, base.toQuery());
    Assert.assertEquals(6, facet.getTotalResults());
    Assert.assertEquals(3, facet.getTotalIntervals());
    intervals = facet.getValues();
    Assert.assertEquals(3, intervals.items().size());
    Assert.assertEquals(1, intervals.count(Interval.dateInterval(format.parse("2015-11-24"), format.parse("2016-01-24"), day_resolution)));
    Assert.assertEquals(4, intervals.count(Interval.dateInterval(format.parse("2016-01-24"), format.parse("2016-03-24"), day_resolution)));
    Assert.assertEquals(1, intervals.count(Interval.dateInterval(format.parse("2016-03-24"), format.parse("2016-05-24"), day_resolution)));
    // facets 3
    facet = new DateIntervalFacet.Builder().name("facet3").resolution(day_resolution).intervalDate(SIX_DAYS).start(format.parse("2015-01-10")).build();
    facet.compute(searcher, base.toQuery());
    Assert.assertEquals(6, facet.getTotalResults());
    Assert.assertEquals(1, facet.getTotalIntervals());
    intervals = facet.getValues();
    Assert.assertEquals(1, intervals.items().size());
    Assert.assertEquals(6, intervals.count(Interval.dateInterval(format.parse("2014-12-29"), format.parse("2015-01-04"), day_resolution)));
  }

  @Test
  public void testFlexibleFacetsQuery() throws IndexException, IOException, ParseException {
    List<Filter> filters = Collections.singletonList(DateTermFilter.newFilter("facet3", format.parse("2015-01-01"), day_resolution));
    Query base = new TermQuery(new Term("field", "value"));
    DateIntervalFacet facet = new DateIntervalFacet.Builder().name("facet1").resolution(day_resolution).intervalDate(ONE_MONTHS).start(format.parse("2017-05-05")).build();
    facet.compute(searcher, base, filters);
    Assert.assertEquals(6, facet.getTotalResults());
    Assert.assertEquals(5, facet.getTotalIntervals());
    Bucket<Interval> intervals = facet.getValues();
    Assert.assertEquals(5, intervals.items().size());
    Assert.assertEquals(1, intervals.count(Interval.dateInterval(format.parse("2016-12-05"), format.parse("2017-01-05"), day_resolution)));
    Assert.assertEquals(1, intervals.count(Interval.dateInterval(format.parse("2017-01-05"), format.parse("2017-02-05"), day_resolution)));
    Assert.assertEquals(1, intervals.count(Interval.dateInterval(format.parse("2017-02-05"), format.parse("2017-03-05"), day_resolution)));
    Assert.assertEquals(2, intervals.count(Interval.dateInterval(format.parse("2017-03-05"), format.parse("2017-04-05"), day_resolution)));
    Assert.assertEquals(1, intervals.count(Interval.dateInterval(format.parse("2017-04-05"), format.parse("2017-05-05"), day_resolution)));
    // facets 2
    facet = new DateIntervalFacet.Builder().name("facet2").resolution(day_resolution).intervalDate(TWO_MONTHS).start(format.parse("2016-05-24")).build();
    facet.compute(searcher, base, filters);
    Assert.assertEquals(6, facet.getTotalResults());
    Assert.assertEquals(3, facet.getTotalIntervals());
    intervals = facet.getValues();
    Assert.assertEquals(3, intervals.items().size());
    Assert.assertEquals(1, intervals.count(Interval.dateInterval(format.parse("2015-11-24"), format.parse("2016-01-24"), day_resolution)));
    Assert.assertEquals(4, intervals.count(Interval.dateInterval(format.parse("2016-01-24"), format.parse("2016-03-24"), day_resolution)));
    Assert.assertEquals(1, intervals.count(Interval.dateInterval(format.parse("2016-03-24"), format.parse("2016-05-24"), day_resolution)));
    // facets 3
    facet = new DateIntervalFacet.Builder().name("facet3").resolution(day_resolution).intervalDate(SIX_DAYS).start(format.parse("2015-01-10")).build();
    facet.compute(searcher, base, filters);
    Assert.assertEquals(7, facet.getTotalResults());
    Assert.assertEquals(2, facet.getTotalIntervals());
    intervals = facet.getValues();
    Assert.assertEquals(2, intervals.items().size());
    Assert.assertEquals(6, intervals.count(Interval.dateInterval(format.parse("2014-12-29"), format.parse("2015-01-04"), day_resolution)));
    Assert.assertEquals(1, intervals.count(Interval.dateInterval(format.parse("2015-01-28"), format.parse("2015-02-03"), day_resolution)));

  }
}
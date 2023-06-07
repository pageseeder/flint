package org.pageseeder.flint.lucene.facet;

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
import org.pageseeder.flint.lucene.facet.FlexibleRangeFacet.Range;
import org.pageseeder.flint.lucene.query.DateParameter;
import org.pageseeder.flint.lucene.search.DateTermFilter;
import org.pageseeder.flint.lucene.search.Filter;
import org.pageseeder.flint.lucene.util.Bucket;
import org.pageseeder.flint.lucene.utils.TestListener;
import org.pageseeder.flint.lucene.utils.TestUtils;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

public class DateRangeFacetTest {

  private static final File template  = new File("src/test/resources/template.xsl");
  private static final File documents = new File("src/test/resources/facets");
  private static final FileFilter filter = file -> "daterangefacet.xml".equals(file.getName());
  private static final File indexRoot = new File("tmp/index");

  private static LuceneLocalIndex index;
  private static LocalIndexManager manager;
  private static IndexSearcher searcher;
  private static final Resolution second_resolution = Resolution.SECOND;
  private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");

  @BeforeClass
  public static void init() {
    // clean up previous test's data
    if (indexRoot.listFiles() != null) for (File f : indexRoot.listFiles()) f.delete();
    indexRoot.delete();
    try {
      index = new LuceneLocalIndex(indexRoot, "daterange", new StandardAnalyzer(), documents);
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
    // set GMT as indexed dates
    format.setTimeZone(TimeZone.getTimeZone("GMT"));
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
    DateRangeFacet facet = new DateRangeFacet.Builder().name("facet1").resolution(second_resolution)
        .addRange(format.parse("2017-01-01_12:00:00"), format.parse("2017-01-03_12:00:00"))
        .addRange(format.parse("2017-01-04_12:00:00"), format.parse("2017-01-06_12:00:00"))
        .addRange(format.parse("2017-01-07_12:00:00"), format.parse("2017-01-10_12:00:00")).build();
    facet.compute(searcher);
    Assert.assertEquals(3, facet.getTotalRanges());
    Bucket<Range> ranges = facet.getValues();
    Assert.assertEquals(3, ranges.items().size());
    Assert.assertEquals(3, ranges.count(Range.dateRange(format.parse("2017-01-01_12:00:00"), format.parse("2017-01-03_12:00:00"), second_resolution)));
    Assert.assertEquals(4, ranges.count(Range.dateRange(format.parse("2017-01-04_12:00:00"), format.parse("2017-01-06_12:00:00"), second_resolution)));
    Assert.assertEquals(1, ranges.count(FlexibleRangeFacet.OTHER));
    // facets 2
    facet = new DateRangeFacet.Builder().name("facet2").resolution(second_resolution)
        .addRange(format.parse("2017-02-01_12:00:00"), format.parse("2017-02-03_12:00:00"))
        .addRange(format.parse("2017-02-04_12:00:00"), format.parse("2017-02-05_12:00:00"))
        .addRange(format.parse("2017-02-06_12:00:00"), format.parse("2017-02-10_12:00:00")).build();
    facet.compute(searcher);
    Assert.assertEquals(4, facet.getTotalRanges());
    ranges = facet.getValues();
    Assert.assertEquals(4, ranges.items().size());
    Assert.assertEquals(4, ranges.count(Range.dateRange(format.parse("2017-02-01_12:00:00"), format.parse("2017-02-03_12:00:00"), second_resolution)));
    Assert.assertEquals(2, ranges.count(Range.dateRange(format.parse("2017-02-04_12:00:00"), format.parse("2017-02-05_12:00:00"), second_resolution)));
    Assert.assertEquals(1, ranges.count(Range.dateRange(format.parse("2017-02-06_12:00:00"), format.parse("2017-02-10_12:00:00"), second_resolution)));
    Assert.assertEquals(1, ranges.count(FlexibleRangeFacet.OTHER));
    // facets 3
    facet = new DateRangeFacet.Builder().name("facet3").resolution(second_resolution)
        .addRange(format.parse("2017-03-01_12:00:00"), format.parse("2017-03-03_12:00:00"))
        .addRange(format.parse("2017-03-06_12:00:00"), format.parse("2017-03-10_12:00:00")).build();
    facet.compute(searcher);
    Assert.assertEquals(2, facet.getTotalRanges());
    ranges = facet.getValues();
    Assert.assertEquals(2, ranges.items().size());
    Assert.assertEquals(7, ranges.count(Range.dateRange(format.parse("2017-03-01_12:00:00"), format.parse("2017-03-03_12:00:00"), second_resolution)));
    Assert.assertEquals(1, ranges.count(FlexibleRangeFacet.OTHER));
  }

  @Test
  public void testFacetsQuery() throws IndexException, IOException, ParseException {
    Query base = new DateParameter("facet3", format.parse("2017-03-01_12:00:00"), second_resolution, false).toQuery();
    DateRangeFacet facet = new DateRangeFacet.Builder().name("facet1").resolution(second_resolution)
        .addRange(format.parse("2017-01-01_12:00:00"), format.parse("2017-01-03_12:00:00"))
        .addRange(format.parse("2017-01-04_12:00:00"), format.parse("2017-01-06_12:00:00"))
        .addRange(format.parse("2017-01-07_12:00:00"), format.parse("2017-01-10_12:00:00")).build();
    facet.compute(searcher, base);
    Assert.assertEquals(2, facet.getTotalRanges());
    Bucket<Range> ranges = facet.getValues();
    Assert.assertEquals(2, ranges.items().size());
    Assert.assertEquals(3, ranges.count(Range.dateRange(format.parse("2017-01-01_12:00:00"), format.parse("2017-01-03_12:00:00"), second_resolution)));
    Assert.assertEquals(3, ranges.count(Range.dateRange(format.parse("2017-01-04_12:00:00"), format.parse("2017-01-06_12:00:00"), second_resolution)));
    // facets 2
    facet = new DateRangeFacet.Builder().name("facet2").resolution(second_resolution)
        .addRange(format.parse("2017-02-01_12:00:00"), format.parse("2017-02-03_12:00:00"))
        .addRange(format.parse("2017-02-04_12:00:00"), format.parse("2017-02-05_12:00:00"))
        .addRange(format.parse("2017-02-06_12:00:00"), format.parse("2017-02-10_12:00:00")).build();
    facet.compute(searcher, base);
    Assert.assertEquals(2, facet.getTotalRanges());
    ranges = facet.getValues();
    Assert.assertEquals(2, ranges.items().size());
    Assert.assertEquals(4, ranges.count(Range.dateRange(format.parse("2017-02-01_12:00:00"), format.parse("2017-02-03_12:00:00"), second_resolution)));
    Assert.assertEquals(2, ranges.count(Range.dateRange(format.parse("2017-02-04_12:00:00"), format.parse("2017-02-05_12:00:00"), second_resolution)));
    // facets 3
    facet = new DateRangeFacet.Builder().name("facet3").resolution(second_resolution)
        .addRange(format.parse("2017-03-01_12:00:00"), format.parse("2017-03-03_12:00:00"))
        .addRange(format.parse("2017-03-06_12:00:00"), format.parse("2017-03-10_12:00:00")).build();
    facet.compute(searcher, base);
    Assert.assertEquals(1, facet.getTotalRanges());
    ranges = facet.getValues();
    Assert.assertEquals(1, ranges.items().size());
    Assert.assertEquals(6, ranges.count(Range.dateRange(format.parse("2017-03-01_12:00:00"), format.parse("2017-03-03_12:00:00"), second_resolution)));
  }

  @Test
  public void testFlexibleFacetsQuery() throws IndexException, IOException, ParseException {
    List<Filter> filters = Collections.singletonList(DateTermFilter.newFilter("facet3", format.parse("2017-03-01_12:00:00"), second_resolution));
    Query base = new TermQuery(new Term("field", "value"));
    DateRangeFacet facet = new DateRangeFacet.Builder().name("facet1").resolution(second_resolution)
        .addRange(format.parse("2017-01-01_12:00:00"), format.parse("2017-01-03_12:00:00"))
        .addRange(format.parse("2017-01-04_12:00:00"), format.parse("2017-01-06_12:00:00"))
        .addRange(format.parse("2017-01-07_12:00:00"), format.parse("2017-01-10_12:00:00")).build();
    facet.compute(searcher, base, filters);
    Assert.assertEquals(2, facet.getTotalRanges());
    Bucket<Range> ranges = facet.getValues();
    Assert.assertEquals(2, ranges.items().size());
    Assert.assertEquals(3, ranges.count(Range.dateRange(format.parse("2017-01-01_12:00:00"), format.parse("2017-01-03_12:00:00"), second_resolution)));
    Assert.assertEquals(3, ranges.count(Range.dateRange(format.parse("2017-01-04_12:00:00"), format.parse("2017-01-06_12:00:00"), second_resolution)));
    // facets 2
    facet = new DateRangeFacet.Builder().name("facet2").resolution(second_resolution)
        .addRange(format.parse("2017-02-01_12:00:00"), format.parse("2017-02-03_12:00:00"))
        .addRange(format.parse("2017-02-04_12:00:00"), format.parse("2017-02-05_12:00:00"))
        .addRange(format.parse("2017-02-06_12:00:00"), format.parse("2017-02-10_12:00:00")).build();
    facet.compute(searcher, base, filters);
    Assert.assertEquals(2, facet.getTotalRanges());
    ranges = facet.getValues();
    Assert.assertEquals(2, ranges.items().size());
    Assert.assertEquals(4, ranges.count(Range.dateRange(format.parse("2017-02-01_12:00:00"), format.parse("2017-02-03_12:00:00"), second_resolution)));
    Assert.assertEquals(2, ranges.count(Range.dateRange(format.parse("2017-02-04_12:00:00"), format.parse("2017-02-05_12:00:00"), second_resolution)));
    // facets 3
    facet = new DateRangeFacet.Builder().name("facet3").resolution(second_resolution)
        .addRange(format.parse("2017-03-01_12:00:00"), format.parse("2017-03-03_12:00:00"))
        .addRange(format.parse("2017-03-06_12:00:00"), format.parse("2017-03-10_12:00:00")).build();
    facet.compute(searcher, base, filters);
    Assert.assertEquals(2, facet.getTotalRanges());
    ranges = facet.getValues();
    Assert.assertEquals(2, ranges.items().size());
    Assert.assertEquals(7, ranges.count(Range.dateRange(format.parse("2017-03-01_12:00:00"), format.parse("2017-03-03_12:00:00"), second_resolution)));
    Assert.assertEquals(1, ranges.count(FlexibleRangeFacet.OTHER));

  }
}
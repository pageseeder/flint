package org.pageseeder.flint.lucene.facet;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
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
import org.pageseeder.flint.lucene.query.DateParameter;
import org.pageseeder.flint.lucene.search.DateTermFilter;
import org.pageseeder.flint.lucene.search.Filter;
import org.pageseeder.flint.lucene.util.Bucket;
import org.pageseeder.flint.lucene.util.Dates;
import org.pageseeder.flint.lucene.utils.TestListener;
import org.pageseeder.flint.lucene.utils.TestUtils;

public class DateFieldFacetTest {

  private static File template  = new File("src/test/resources/template.xsl");
  private static File documents = new File("src/test/resources/facets");
  private static FileFilter filter = new FileFilter() { public boolean accept(File file) { return "datefieldfacet.xml".equals(file.getName()); } };
  private static File indexRoot = new File("tmp/index");

  private static LuceneLocalIndex index;
  private static LocalIndexManager manager;
  private static IndexSearcher searcher;
  private static final Resolution second_resolution = Resolution.SECOND;
  private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");

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
    // set GMT as indexed dates
    format.setTimeZone(TimeZone.getTimeZone("GMT"));
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
    DateFieldFacet facet = DateFieldFacet.newFacet("facet1", second_resolution);
    facet.compute(searcher, null);
    Assert.assertEquals(0, facet.getTotalResults());// 2017-01-01_12:00:00
    Assert.assertEquals(6, facet.getTotalTerms());
    Bucket<String> values = facet.getValues();
    Assert.assertEquals(6, values.items().size());
    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-01-01_12:00:00"), second_resolution)));
    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-01-02_12:00:00"), second_resolution)));
    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-01-03_12:00:00"), second_resolution)));
    Assert.assertEquals(2, values.count(Dates.toString(format.parse("2017-01-04_12:00:00"), second_resolution)));
    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-01-05_12:00:00"), second_resolution)));
    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-01-06_12:00:00"), second_resolution)));
    // facets 2
    facet = DateFieldFacet.newFacet("facet2", Resolution.SECOND);
    facet.compute(searcher, null);
    Assert.assertEquals(0, facet.getTotalResults());
    Assert.assertEquals(6, facet.getTotalTerms());
    values = facet.getValues();
    Assert.assertEquals(6, values.items().size());
    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-02-01_12:00:00"), second_resolution)));
    Assert.assertEquals(2, values.count(Dates.toString(format.parse("2017-02-02_12:00:00"), second_resolution)));
    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-02-03_12:00:00"), second_resolution)));
    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-02-04_12:00:00"), second_resolution)));
    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-02-05_12:00:00"), second_resolution)));
    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-02-06_12:00:00"), second_resolution)));
    // facets 3
    facet = DateFieldFacet.newFacet("facet3", Resolution.SECOND);
    facet.compute(searcher, null);
    Assert.assertEquals(0, facet.getTotalResults());
    Assert.assertEquals(2, facet.getTotalTerms());
    values = facet.getValues();
    Assert.assertEquals(2, values.items().size());
    Assert.assertEquals(6, values.count(Dates.toString(format.parse("2017-03-01_12:00:00"), second_resolution)));
    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-03-02_12:00:00"), second_resolution)));
  }

  @Test
  public void testFacetsQuery() throws IndexException, IOException, ParseException {
    Date d = format.parse("2017-03-01_12:00:00");
    Query base = new DateParameter("facet3", d, second_resolution, false).toQuery();
    DateFieldFacet facet = DateFieldFacet.newFacet("facet1", second_resolution);
    facet.compute(searcher, base);
    Assert.assertEquals(6, facet.getTotalResults());
    Assert.assertEquals(5, facet.getTotalTerms());
    Bucket<String> values = facet.getValues();
    Assert.assertEquals(5, values.items().size());
    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-01-01_12:00:00"), second_resolution)));
    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-01-02_12:00:00"), second_resolution)));
    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-01-03_12:00:00"), second_resolution)));
    Assert.assertEquals(2, values.count(Dates.toString(format.parse("2017-01-04_12:00:00"), second_resolution)));
    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-01-05_12:00:00"), second_resolution)));
    // facets 2
    facet = DateFieldFacet.newFacet("facet2", Resolution.SECOND);
    facet.compute(searcher, base);
    Assert.assertEquals(6, facet.getTotalResults());
    Assert.assertEquals(5, facet.getTotalTerms());
    values = facet.getValues();
    Assert.assertEquals(5, values.items().size());
    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-02-01_12:00:00"), second_resolution)));
    Assert.assertEquals(2, values.count(Dates.toString(format.parse("2017-02-02_12:00:00"), second_resolution)));
    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-02-03_12:00:00"), second_resolution)));
    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-02-04_12:00:00"), second_resolution)));
    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-02-05_12:00:00"), second_resolution)));
    // facets 3
    facet = DateFieldFacet.newFacet("facet3", Resolution.SECOND);
    facet.compute(searcher, base);
    Assert.assertEquals(6, facet.getTotalResults());
    Assert.assertEquals(1, facet.getTotalTerms());
    values = facet.getValues();
    Assert.assertEquals(1, values.items().size());
    Assert.assertEquals(6, values.count(Dates.toString(format.parse("2017-03-01_12:00:00"), second_resolution)));
  }

//  @Test
//  public void testFacetsQuerySearchResults() throws IndexException, IOException, ParseException {
//    Date d = format.parse("2017-03-01_12:00:00");
//    SearchQuery base = BasicQuery.newBasicQuery(new DateParameter("facet3", d, second_resolution, false));
//    // run search
//    SearchResults results = LuceneIndexQueries.query(index, base);
//    DateFieldFacet facet = DateFieldFacet.newFacet("facet1", second_resolution);
//    facet.compute(results);
//    Assert.assertEquals(6, facet.getTotalResults());
//    Assert.assertEquals(5, facet.getTotalTerms());
//    Bucket<String> values = facet.getValues();
//    Assert.assertEquals(5, values.items().size());
//    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-01-01_12:00:00"), second_resolution)));
//    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-01-02_12:00:00"), second_resolution)));
//    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-01-03_12:00:00"), second_resolution)));
//    Assert.assertEquals(2, values.count(Dates.toString(format.parse("2017-01-04_12:00:00"), second_resolution)));
//    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-01-05_12:00:00"), second_resolution)));
//    // facets 2
//    facet = DateFieldFacet.newFacet("facet2", Resolution.SECOND);
//    facet.compute(results);
//    Assert.assertEquals(6, facet.getTotalResults());
//    Assert.assertEquals(5, facet.getTotalTerms());
//    values = facet.getValues();
//    Assert.assertEquals(5, values.items().size());
//    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-02-01_12:00:00"), second_resolution)));
//    Assert.assertEquals(2, values.count(Dates.toString(format.parse("2017-02-02_12:00:00"), second_resolution)));
//    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-02-03_12:00:00"), second_resolution)));
//    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-02-04_12:00:00"), second_resolution)));
//    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-02-05_12:00:00"), second_resolution)));
//    // facets 3
//    facet = DateFieldFacet.newFacet("facet3", Resolution.SECOND);
//    facet.compute(results);
//    Assert.assertEquals(6, facet.getTotalResults());
//    Assert.assertEquals(1, facet.getTotalTerms());
//    values = facet.getValues();
//    Assert.assertEquals(1, values.items().size());
//    Assert.assertEquals(6, values.count(Dates.toString(format.parse("2017-03-01_12:00:00"), second_resolution)));
//  }

  @Test
  public void testFlexibleFacetsQuery() throws IndexException, IOException, ParseException {
    Date d = format.parse("2017-03-01_12:00:00");
    List<Filter> filters = Collections.singletonList(DateTermFilter.newFilter("facet3", d, second_resolution));
    Query base = new TermQuery(new Term("field", "value"));
    DateFieldFacet facet = DateFieldFacet.newFacet("facet1", second_resolution);
    facet.compute(searcher, base, filters);
    Assert.assertEquals(6, facet.getTotalResults());
    Assert.assertEquals(5, facet.getTotalTerms());
    Bucket<String> values = facet.getValues();
    Assert.assertEquals(5, values.items().size());
    Assert.assertEquals(5, values.items().size());
    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-01-01_12:00:00"), second_resolution)));
    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-01-02_12:00:00"), second_resolution)));
    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-01-03_12:00:00"), second_resolution)));
    Assert.assertEquals(2, values.count(Dates.toString(format.parse("2017-01-04_12:00:00"), second_resolution)));
    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-01-05_12:00:00"), second_resolution)));
    // facets 2
    facet = DateFieldFacet.newFacet("facet2", Resolution.SECOND);
    facet.compute(searcher, base, filters);
    Assert.assertEquals(6, facet.getTotalResults());
    Assert.assertEquals(5, facet.getTotalTerms());
    values = facet.getValues();
    Assert.assertEquals(5, values.items().size());
    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-02-01_12:00:00"), second_resolution)));
    Assert.assertEquals(2, values.count(Dates.toString(format.parse("2017-02-02_12:00:00"), second_resolution)));
    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-02-03_12:00:00"), second_resolution)));
    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-02-04_12:00:00"), second_resolution)));
    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-02-05_12:00:00"), second_resolution)));
    // facets 3
    facet = DateFieldFacet.newFacet("facet3", Resolution.SECOND);
    facet.compute(searcher, base, filters);
    Assert.assertEquals(7, facet.getTotalResults());
    Assert.assertEquals(2, facet.getTotalTerms());
    values = facet.getValues();
    Assert.assertEquals(2, values.items().size());
    Assert.assertEquals(6, values.count(Dates.toString(format.parse("2017-03-01_12:00:00"), second_resolution)));
    Assert.assertEquals(1, values.count(Dates.toString(format.parse("2017-03-02_12:00:00"), second_resolution)));
  }

}
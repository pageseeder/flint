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
import org.pageseeder.flint.local.LocalIndexManager;
import org.pageseeder.flint.local.LocalIndexManagerFactory;
import org.pageseeder.flint.lucene.LuceneIndexQueries;
import org.pageseeder.flint.lucene.LuceneLocalIndex;
import org.pageseeder.flint.lucene.facet.StringRangeFacet;
import org.pageseeder.flint.lucene.facet.FlexibleRangeFacet.Range;
import org.pageseeder.flint.lucene.search.Filter;
import org.pageseeder.flint.lucene.search.StringTermFilter;
import org.pageseeder.flint.lucene.util.Bucket;
import org.pageseeder.flint.lucene.utils.TestListener;
import org.pageseeder.flint.lucene.utils.TestUtils;

public class StringRangeFacetTest {

  private static File template  = new File("src/test/resources/template.xsl");
  private static File documents = new File("src/test/resources/facets");
  private static FileFilter filter = new FileFilter() { public boolean accept(File file) { return "stringrangefacet.xml".equals(file.getName()); } };
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
    StringRangeFacet facet = new StringRangeFacet.Builder().name("facet1")
        .addRange("value10", "value12")
        .addRange("value13", "value15")
        .addRange("value16", "value20").build();
    facet.compute(searcher, null);
    Assert.assertEquals(0, facet.getTotalResults());
    Assert.assertEquals(2, facet.getTotalRanges());
    Bucket<Range> ranges = facet.getValues();
    Assert.assertEquals(2, ranges.items().size());
    Assert.assertEquals(4, ranges.count(Range.stringRange("value10", "value12")));
    Assert.assertEquals(3, ranges.count(Range.stringRange("value13", "value15")));
    // facets 2
    facet = new StringRangeFacet.Builder().name("facet2")
        .addRange("value20", "value22")
        .addRange("value23", "value24")
        .addRange("value25", "value29").build();
    facet.compute(searcher, null);
    Assert.assertEquals(0, facet.getTotalResults());
    Assert.assertEquals(3, facet.getTotalRanges());
    ranges = facet.getValues();
    Assert.assertEquals(3, ranges.items().size());
    Assert.assertEquals(3, ranges.count(Range.stringRange("value20", "value22")));
    Assert.assertEquals(3, ranges.count(Range.stringRange("value23", "value24")));
    Assert.assertEquals(1, ranges.count(Range.stringRange("value25", "value29")));
    // facets 3
    facet = new StringRangeFacet.Builder().name("facet3")
        .addRange("value30", "value32")
        .addRange("value33", "value38").build();
    facet.compute(searcher, null);
    Assert.assertEquals(0, facet.getTotalResults());
    Assert.assertEquals(1, facet.getTotalRanges());
    ranges = facet.getValues();
    Assert.assertEquals(1, ranges.items().size());
    Assert.assertEquals(7, ranges.count(Range.stringRange("value30", "value32")));
  }

  @Test
  public void testFacetsQuery() throws IndexException, IOException {
    Query base = new TermQuery(new Term("facet3", "value30"));
    StringRangeFacet facet = new StringRangeFacet.Builder().name("facet1")
        .addRange("value10", "value12")
        .addRange("value13", "value15")
        .addRange("value16", "value20").build();
    facet.compute(searcher, base);
    Assert.assertEquals(6, facet.getTotalResults());
    Assert.assertEquals(2, facet.getTotalRanges());
    Bucket<Range> ranges = facet.getValues();
    Assert.assertEquals(2, ranges.items().size());
    Assert.assertEquals(4, ranges.count(Range.stringRange("value10", "value12")));
    Assert.assertEquals(2, ranges.count(Range.stringRange("value13", "value15")));
    // facets 2
    facet = new StringRangeFacet.Builder().name("facet2")
        .addRange("value20", "value22")
        .addRange("value23", "value24")
        .addRange("value25", "value29").build();
    facet.compute(searcher, base);
    Assert.assertEquals(6, facet.getTotalResults());
    Assert.assertEquals(2, facet.getTotalRanges());
    ranges = facet.getValues();
    Assert.assertEquals(2, ranges.items().size());
    Assert.assertEquals(3, ranges.count(Range.stringRange("value20", "value22")));
    Assert.assertEquals(3, ranges.count(Range.stringRange("value23", "value24")));
    // facets 3
    facet = new StringRangeFacet.Builder().name("facet3")
        .addRange("value30", "value32")
        .addRange("value33", "value38").build();
    facet.compute(searcher, base);
    Assert.assertEquals(6, facet.getTotalResults());
    Assert.assertEquals(1, facet.getTotalRanges());
    ranges = facet.getValues();
    Assert.assertEquals(1, ranges.items().size());
    Assert.assertEquals(6, ranges.count(Range.stringRange("value30", "value32")));
  }

  @Test
  public void testFlexibleFacetsQuery() throws IndexException, IOException {
    List<Filter> filters = Collections.singletonList(StringTermFilter.newFilter("facet3", "value30"));
    Query base = new TermQuery(new Term("field", "value"));
    StringRangeFacet facet = new StringRangeFacet.Builder().name("facet1")
        .addRange("value10", "value12")
        .addRange("value13", "value15")
        .addRange("value16", "value20").build();
    facet.compute(searcher, base, filters);
    Assert.assertEquals(6, facet.getTotalResults());
    Assert.assertEquals(2, facet.getTotalRanges());
    Bucket<Range> ranges = facet.getValues();
    Assert.assertEquals(2, ranges.items().size());
    Assert.assertEquals(4, ranges.count(Range.stringRange("value10", "value12")));
    Assert.assertEquals(2, ranges.count(Range.stringRange("value13", "value15")));
    // facets 2
    facet = new StringRangeFacet.Builder().name("facet2")
        .addRange("value20", "value22")
        .addRange("value23", "value24")
        .addRange("value25", "value29").build();
    facet.compute(searcher, base, filters);
    Assert.assertEquals(6, facet.getTotalResults());
    Assert.assertEquals(2, facet.getTotalRanges());
    ranges = facet.getValues();
    Assert.assertEquals(2, ranges.items().size());
    Assert.assertEquals(3, ranges.count(Range.stringRange("value20", "value22")));
    Assert.assertEquals(3, ranges.count(Range.stringRange("value23", "value24")));
    // facets 3
    facet = new StringRangeFacet.Builder().name("facet3")
        .addRange("value30", "value32")
        .addRange("value33", "value38").build();
    facet.compute(searcher, base, filters);
    Assert.assertEquals(7, facet.getTotalResults());
    Assert.assertEquals(1, facet.getTotalRanges());
    ranges = facet.getValues();
    Assert.assertEquals(1, ranges.items().size());
    Assert.assertEquals(7, ranges.count(Range.stringRange("value30", "value32")));

  }

}
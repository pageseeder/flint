package org.pageseeder.flint.lucene.search;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;

import javax.xml.transform.TransformerException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.Requester;
import org.pageseeder.flint.content.Content;
import org.pageseeder.flint.content.ContentFetcher;
import org.pageseeder.flint.content.ContentType;
import org.pageseeder.flint.content.DeleteRule;
import org.pageseeder.flint.content.SourceForwarder;
import org.pageseeder.flint.indexing.IndexBatch;
import org.pageseeder.flint.indexing.IndexJob;
import org.pageseeder.flint.indexing.IndexJob.Priority;
import org.pageseeder.flint.lucene.LuceneIndexQueries;
import org.pageseeder.flint.lucene.LuceneLocalIndex;
import org.pageseeder.flint.lucene.query.BasicQuery;
import org.pageseeder.flint.lucene.query.NumericRange;
import org.pageseeder.flint.lucene.query.PredicateSearchQuery;
import org.pageseeder.flint.lucene.query.SearchQuery;
import org.pageseeder.flint.lucene.query.SearchResults;
import org.pageseeder.flint.lucene.query.TermRange;
import org.pageseeder.flint.lucene.util.Dates;
import org.pageseeder.flint.lucene.utils.TestListener;
import org.pageseeder.flint.lucene.utils.TestUtils;
import org.pageseeder.xmlwriter.XML.NamespaceAware;
import org.pageseeder.xmlwriter.XMLStringWriter;

public class DatesTest {

  private static File template  = new File("src/test/resources/template.xsl");
  private static File documents = new File("src/test/resources/dates");
  private static File indexNumericRoot = new File("tmp/dates-numeric-index");
  private static File indexStringRoot = new File("tmp/dates-string-index");

  private static LuceneLocalIndex indexNumeric;
  private static LuceneLocalIndex indexString;

  private static IndexManager manager;

  @BeforeClass
  public static void init() {
    // clean up previous test data
//    for (File f : indexNumericRoot.listFiles()) f.delete();
//    for (File f : indexStringRoot.listFiles()) f.delete();
    indexNumeric = new LuceneLocalIndex(indexNumericRoot, new StandardAnalyzer(), documents);
    try {
      indexNumeric.setTemplates(TestUtils.TYPE, TestUtils.MEDIA_TYPE, template.toURI());
    } catch (TransformerException ex) {
      ex.printStackTrace();
    }
    indexString = new LuceneLocalIndex(indexStringRoot, new StandardAnalyzer(), documents);
    try {
      indexString.setTemplates(TestUtils.TYPE, TestUtils.MEDIA_TYPE, template.toURI());
    } catch (TransformerException ex) {
      ex.printStackTrace();
    }
    manager = new IndexManager(new DatesContentFetcher(), new TestListener(), 10, false);
    manager.setDefaultTranslator(new SourceForwarder(Collections.singletonList(TestUtils.MEDIA_TYPE), "UTF-8"));
    System.out.println("Starting manager!");
//    indexContent();
  }

  @AfterClass
  public static void after() {
    // stop index
    System.out.println("Stopping manager!");
    manager.stop();
    System.out.println("-----------------------------------");
  }

  @Test
  public void testSorting1() throws IndexException, IOException {
    long before = System.currentTimeMillis();
    // run searches
    SearchQuery query = new PredicateSearchQuery("field:value", new Sort(new SortedNumericSortField("date-numeric", SortField.Type.LONG)));
    SearchResults results = LuceneIndexQueries.query(indexNumeric, query);
    results.toXML(new XMLStringWriter(NamespaceAware.No));
    results.terminate();
    System.out.println("Time sorting numeric: "+(System.currentTimeMillis()-before));
    // second search
    before = System.currentTimeMillis();
    // run searches
    query = new PredicateSearchQuery("field:value", "date-string");
    results = LuceneIndexQueries.query(indexString, query);
    results.toXML(new XMLStringWriter(NamespaceAware.No));
    results.terminate();
    System.out.println("Time sorting string: "+(System.currentTimeMillis()-before));
  }

  @Test
  public void testSearch() throws IndexException, IOException {
    long before = System.currentTimeMillis();
    Date d = new Date(before);
    // run searches
    SearchQuery query = BasicQuery.newBasicQuery(NumericRange.newLongRange("date-numeric",
        (Long)Dates.toNumber(d, Resolution.MINUTE), (Long) Dates.toNumber(d, Resolution.MINUTE), true, true));
    SearchResults results = LuceneIndexQueries.query(indexNumeric, query);
    results.toXML(new XMLStringWriter(NamespaceAware.No));
    results.terminate();
    System.out.println("Time search numeric: "+(System.currentTimeMillis()-before));
    // second search
    before = System.currentTimeMillis();
    // run searches
    query = new PredicateSearchQuery("date-string:"+Dates.toString(d, Resolution.MINUTE));
    results = LuceneIndexQueries.query(indexString, query);
    results.toXML(new XMLStringWriter(NamespaceAware.No));
    results.terminate();
    System.out.println("Time search string: "+(System.currentTimeMillis()-before));
  }

  @Test
  public void testRanges() throws IndexException, IOException {
    long before = System.currentTimeMillis();
    Date min = new Date(before - 4 * 3600 * 1000);
    Date max = new Date(before + 4 * 3600 * 1000);
    // run searches
    SearchQuery query = BasicQuery.newBasicQuery(NumericRange.newLongRange("date-numeric",
        (Long)Dates.toNumber(min, Resolution.MINUTE), (Long) Dates.toNumber(max, Resolution.MINUTE), true, true));
    SearchResults results = LuceneIndexQueries.query(indexNumeric, query);
    results.toXML(new XMLStringWriter(NamespaceAware.No));
    results.terminate();
    System.out.println("Time range numeric: "+(System.currentTimeMillis()-before));
    // second search
    before = System.currentTimeMillis();
    // run searches
    query = BasicQuery.newBasicQuery(TermRange.newRange("date-string",
        Dates.toString(min, Resolution.MINUTE), Dates.toString(max, Resolution.MINUTE), true, true));
    results = LuceneIndexQueries.query(indexString, query);
    results.toXML(new XMLStringWriter(NamespaceAware.No));
    results.terminate();
    System.out.println("Time range string: "+(System.currentTimeMillis()-before));
  }

  private static void indexContent() {
    int total = 100000;
    IndexBatch stringBatch = new IndexBatch("string", total);
    Requester requester = new Requester("batch");
    for (int i = 0; i < total; i++) {
      stringBatch.setComputed();
      manager.indexBatch(stringBatch, "string-dates"+i, TestUtils.TYPE, indexString, requester, Priority.HIGH, null);
    }
    System.out.println("Numeric documents indexed");
    IndexBatch numericBatch = new IndexBatch("numeric", total);
    for (int i = 0; i < total; i++) {
      manager.indexBatch(numericBatch, "numeric-dates"+i, TestUtils.TYPE, indexNumeric, requester, Priority.HIGH, null);
    }
    System.out.println("Numeric documents indexed");
    while (!manager.getStatus().isEmpty()) {
      // wait a bit
      TestUtils.wait(1);
    }
    // wait more for last threads to complete
    TestUtils.wait(3);
    System.out.println("Indexing time numeric: "+numericBatch.getTotalDuration());
    System.out.println("Indexing time string: "+stringBatch.getTotalDuration());
  }

  public static class DatesContentFetcher implements ContentFetcher {
    @Override
    public Content getContent(IndexJob job) {
      return new DatesContent(job.getContentID());
    }
  }

  public static class DatesContent implements Content {
    private String id;
    private boolean numeric;
    private final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    public DatesContent(String i) {
      this.id = i;
      this.numeric = i.startsWith("numeric-");
    }
    @Override
    public boolean isDeleted() throws IndexException {
      return false;
    }
    @Override
    public InputStream getSource() throws IndexException {
      String ixml = "<documents version='5.0'>";
      LocalDateTime now = LocalDateTime.now();
      for (int i = -50; i < 50; i++) {
        ixml += document(now.plusMinutes(i));
      }
      ixml += "</documents>";
      return new ByteArrayInputStream(ixml.getBytes(StandardCharsets.UTF_8));
    }
    @Override
    public String getMediaType() throws IndexException {
      return TestUtils.MEDIA_TYPE;
    }
    @Override
    public File getFile() throws IndexException {
      return null;
    }
    @Override
    public DeleteRule getDeleteRule() {
      return null;
    }
    @Override
    public ContentType getContentType() {
      return TestUtils.TYPE;
    }
    @Override
    public String getContentID() {
      return id;
    }
    private String document(LocalDateTime date) {
      String d = "  <document>"
               + "    <field name='"+TestUtils.ID_FIELD+"' tokenize='false'>doc-"+this.id+"</field>"
               + "    <field name='field' tokenize='false'>value</field>";
      if (this.numeric) {
        d += "    <field name='date-numeric' tokenize='false' index='docs-and-freqs-and-positions-and-offsets' "
           + "           date-format='yyyyMMddHHmmss' date-resolution='minute' numeric-type='long' doc-values='sorted'>"+formatter.format(date)+"</field>";
      } else {
        d += "    <field name='date-string' tokenize='false' index='docs-and-freqs-and-positions-and-offsets' "
           + "           date-format='yyyyMMddHHmmss' date-resolution='second' doc-values='sorted'>"+formatter.format(date)+"</field>";
      }
      d += "  </document>";
      return d;
    }
  }
}

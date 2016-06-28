package org.pageseeder.flint.utils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.xml.transform.TransformerException;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.apache.lucene.search.SortedNumericSortField;
import org.apache.lucene.search.SortedSetSortField;
import org.apache.lucene.search.TopFieldCollector;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.content.SourceForwarder;
import org.pageseeder.flint.local.LocalFileContentFetcher;
import org.pageseeder.flint.local.LocalIndex;
import org.pageseeder.flint.local.LocalIndexer;
import org.pageseeder.flint.query.BasicQuery;
import org.pageseeder.flint.query.PredicateSearchQuery;
import org.pageseeder.flint.query.SearchQuery;
import org.pageseeder.flint.query.SearchResults;
import org.pageseeder.flint.query.TermParameter;
import org.pageseeder.flint.util.Queries;

public class QueryTest {

  private static File template  = new File("src/test/resources/template.xsl");
  private static File documents = new File("src/test/resources/query");
  private static File indexRoot = new File("tmp/index");

  private static LocalIndex index;
  private static IndexManager manager;
  
  @BeforeClass
  public static void init() {
    index = new LocalIndex(new TestLocalIndexConfig(indexRoot, documents));
    try {
      index.setTemplate("xml", template.toURI());
    } catch (TransformerException ex) {
      ex.printStackTrace();
    }
    manager = new IndexManager(new LocalFileContentFetcher(), new TestListener());
    manager.setDefaultTranslator(new SourceForwarder("xml", "UTF-8"));
    System.out.println("Starting manager!");
    LocalIndexer indexer = new LocalIndexer(manager, index);
    indexer.indexFolder(documents, null);
    System.out.println("Documents indexed");
    // wait a bit
    TestUtils.wait(1);
  }

  @AfterClass
  public static void after() {
    // stop index
    System.out.println("Stopping manager!");
    manager.stop();
    // clean up
    for (File f : indexRoot.listFiles()) f.delete();
    indexRoot.delete();
    System.out.println("-----------------------------------");
  }


  @Test
  public void testQuery1() throws IndexException {
    // run searches
    SearchQuery query = new PredicateSearchQuery("field1:value0");
    SearchResults results = manager.query(index, query);
    Assert.assertEquals(0, results.getTotalNbOfResults());
    results.terminate();
  }
  @Test
  public void testQuery2() throws IndexException, IOException {
    SearchQuery query = new PredicateSearchQuery("field1:value1");
    SearchResults results = manager.query(index, query);
    Assert.assertEquals(4, results.getTotalNbOfResults());
    results.terminate();
  }

  @Test
  public void testQuery3() throws IndexException, IOException {
    SearchQuery query = new PredicateSearchQuery("field1:value2");
    SearchResults results = manager.query(index, query);
    Assert.assertEquals(3, results.getTotalNbOfResults());
    results.terminate();
  }

  @Test
  public void testQuery4() throws IndexException {
    SearchQuery query = new PredicateSearchQuery("field1:value3");
    SearchResults results = manager.query(index, query);
    Assert.assertEquals(1, results.getTotalNbOfResults());
    results.terminate();
  }

  @Test
  public void testQuery5() throws IndexException {
    SearchQuery query = new PredicateSearchQuery("field1:value4");
    SearchResults results = manager.query(index, query);
    Assert.assertEquals(1, results.getTotalNbOfResults());
    results.terminate();
  }

  @Test
  public void testQuery6() throws IndexException {
    Query query = Queries.toTermOrPhraseQuery("field1", "value4");
    TopFieldCollector results;
    try {
      results = TopFieldCollector.create(Sort.RELEVANCE, 10, true, true, false);
      manager.query(index, query, results);
      Assert.assertEquals(1, results.getTotalHits());
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Test
  public void testSorting1() throws IndexException {
    BasicQuery<TermParameter> query = BasicQuery.newBasicQuery(new TermParameter("field2", "value2"));
    // sort by sorting1, order is 4, 2, 1, 3
    query.setSort(new Sort(new SortField("sorting1", SortField.Type.STRING)));
    SearchResults results = manager.query(index, query);
    Assert.assertEquals(4, results.getTotalNbOfResults());
    Iterator<Document> docs = results.documents().iterator();
    Assert.assertEquals("doc4", docs.next().get("id"));
    Assert.assertEquals("doc2", docs.next().get("id"));
    Assert.assertEquals("doc1", docs.next().get("id"));
    Assert.assertEquals("doc3", docs.next().get("id"));
    results.terminate();
  }

  @Test
  public void testSorting2() throws IndexException {
    BasicQuery<TermParameter> query = BasicQuery.newBasicQuery(new TermParameter("field2", "value2"));
    // sort by sorting2, order is 2, 1, 4, 3
    query.setSort(new Sort(new SortedSetSortField("sorting2", false)));
    SearchResults results = manager.query(index, query);
    Assert.assertEquals(4, results.getTotalNbOfResults());
    Iterator<Document> docs = results.documents().iterator();
    Assert.assertEquals("doc2", docs.next().get("id"));
    Assert.assertEquals("doc1", docs.next().get("id"));
    Assert.assertEquals("doc4", docs.next().get("id"));
    Assert.assertEquals("doc3", docs.next().get("id"));
    // sort by sorting2, reverse order is 3, 4, 1, 2
    query.setSort(new Sort(new SortedSetSortField("sorting2", true)));
    results = manager.query(index, query);
    Assert.assertEquals(4, results.getTotalNbOfResults());
    docs = results.documents().iterator();
    Assert.assertEquals("doc3", docs.next().get("id"));
    Assert.assertEquals("doc4", docs.next().get("id"));
    Assert.assertEquals("doc1", docs.next().get("id"));
    Assert.assertEquals("doc2", docs.next().get("id"));
    results.terminate();
  }

  @Test
  public void testSorting3() throws IndexException {
    BasicQuery<TermParameter> query = BasicQuery.newBasicQuery(new TermParameter("field2", "value2"));
    // sort by sorting3, order is 4, 1, 3, 2
    query.setSort(new Sort(new SortedNumericSortField("sorting3", Type.INT)));
    SearchResults results = manager.query(index, query);
    Assert.assertEquals(4, results.getTotalNbOfResults());
    Iterator<Document> docs = results.documents().iterator();
    Assert.assertEquals("doc4", docs.next().get("id"));
    Assert.assertEquals("doc1", docs.next().get("id"));
    Assert.assertEquals("doc3", docs.next().get("id"));
    Assert.assertEquals("doc2", docs.next().get("id"));
    results.terminate();
  }

  @Test
  public void testSorting4() throws IndexException {
    BasicQuery<TermParameter> query = BasicQuery.newBasicQuery(new TermParameter("field2", "value2"));
    // sort by sorting4, order is 1, 2, 4, 3
    query.setSort(new Sort(new SortedNumericSortField("sorting4", SortField.Type.LONG)));
    SearchResults results = manager.query(index, query);
    Assert.assertEquals(4, results.getTotalNbOfResults());
    Iterator<Document> docs = results.documents().iterator();
    Assert.assertEquals("doc1", docs.next().get("id"));
    Assert.assertEquals("doc2", docs.next().get("id"));
    Assert.assertEquals("doc4", docs.next().get("id"));
    Assert.assertEquals("doc3", docs.next().get("id"));
    results.terminate();
  }

//  private void outputResults(SearchResults results) throws IOException {
//    XMLWriter xml = new XMLWriterImpl(new PrintWriter(System.out));
//    results.toXML(xml);
//    xml.close();
//  }
}

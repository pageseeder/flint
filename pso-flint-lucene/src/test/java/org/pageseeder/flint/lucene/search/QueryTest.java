package org.pageseeder.flint.lucene.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.*;
import org.apache.lucene.search.SortField.Type;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.local.LocalIndexManager;
import org.pageseeder.flint.local.LocalIndexManagerFactory;
import org.pageseeder.flint.lucene.LuceneIndexQueries;
import org.pageseeder.flint.lucene.LuceneLocalIndex;
import org.pageseeder.flint.lucene.query.*;
import org.pageseeder.flint.lucene.utils.TestListener;
import org.pageseeder.flint.lucene.utils.TestUtils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class QueryTest {

  private static File template  = new File("src/test/resources/template.xsl");
  private static File documents = new File("src/test/resources/query");
  private static File indexRoot = new File("tmp/index");

  private static LuceneLocalIndex index;
  private static LocalIndexManager manager;
  private static Analyzer analyser = new StandardAnalyzer();

  @BeforeClass
  public static void init() {
    // clean up previous test's data
    if (indexRoot.exists()) for (File f : indexRoot.listFiles()) f.delete();
    indexRoot.delete();
    try {
      index = new LuceneLocalIndex(indexRoot, "query", new StandardAnalyzer(), documents);
      index.setTemplate("xml", template.toURI());
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    manager = LocalIndexManagerFactory.createMultiThreads(new TestListener());
    System.out.println("Starting manager!");
    manager.indexNewContent(index, documents);
    System.out.println("Documents indexed");
    // wait a bit
    TestUtils.wait(1);
  }

  @AfterClass
  public static void after() {
    // stop index
    System.out.println("Stopping manager!");
    manager.shutdown();
    System.out.println("-----------------------------------");
  }


  @Test
  public void testQuery1() throws IndexException {
    // run searches
    SearchQuery query = new PredicateSearchQuery("field1:value0");
    SearchResults results = LuceneIndexQueries.query(index, query);
    Assert.assertEquals(0, results.getTotalNbOfResults());
    results.terminate();
  }
  @Test
  public void testQuery2() throws IndexException, IOException {
    SearchQuery query = new PredicateSearchQuery("field1:value1");
    SearchResults results = LuceneIndexQueries.query(index, query);
    Assert.assertEquals(4, results.getTotalNbOfResults());
    results.terminate();
  }

  @Test
  public void testQuery3() throws IndexException, IOException {
    SearchQuery query = new PredicateSearchQuery("field1:value2");
    SearchResults results = LuceneIndexQueries.query(index, query);
    Assert.assertEquals(3, results.getTotalNbOfResults());
    results.terminate();
  }

  @Test
  public void testQuery4() throws IndexException {
    SearchQuery query = new PredicateSearchQuery("field1:value3");
    SearchResults results = LuceneIndexQueries.query(index, query);
    Assert.assertEquals(1, results.getTotalNbOfResults());
    results.terminate();
  }

  @Test
  public void testQuery5() throws IndexException {
    SearchQuery query = new PredicateSearchQuery("field1:value4");
    SearchResults results = LuceneIndexQueries.query(index, query);
    Assert.assertEquals(1, results.getTotalNbOfResults());
    results.terminate();
  }

  @Test
  public void testQuery6() throws IndexException {
    Query query = Queries.toTermOrPhraseQuery("field1", "value4");
    TopFieldCollector results = TopFieldCollector.create(Sort.RELEVANCE, 10, 200);
    LuceneIndexQueries.query(index, query, results);
    Assert.assertEquals(1, results.getTotalHits());
  }
  @Test
  public void testNumericField() throws IndexException {
    SearchQuery query = BasicQuery.newBasicQuery(new NumberParameter<>("field3", 2));
    SearchResults results = LuceneIndexQueries.query(index, query);
    Assert.assertEquals(1, results.getTotalNbOfResults());
    Document result = results.documents().iterator().next();
    Assert.assertNotNull(result.getField("field3"));
    Assert.assertEquals("doc2", result.get("id"));
    Assert.assertEquals("2", result.get("field3"));
    results.terminate();
  }

  @Test
  public void testWildcard1() throws IndexException {
    BasicQuery<WildcardParameter> query = BasicQuery.newBasicQuery(new WildcardParameter("wildcard", "ele*"));
    SearchResults results = LuceneIndexQueries.query(index, query);
    Assert.assertEquals(2, results.getTotalNbOfResults());
    Iterator<Document> docs = results.documents().iterator();
    while (docs.hasNext()) {
      String id = docs.next().get("id");
      Assert.assertTrue("doc4".equals(id) || "doc2".equals(id));
    }
    results.terminate();
  }

  @Test
  public void testWildcard2() throws IndexException {
    BasicQuery<WildcardParameter> query = BasicQuery.newBasicQuery(new WildcardParameter("wildcard", "elep*"));
    SearchResults results = LuceneIndexQueries.query(index, query);
    Assert.assertEquals(1, results.getTotalNbOfResults());
    Iterator<Document> docs = results.documents().iterator();
    Assert.assertEquals("doc4", docs.next().get("id"));
    results.terminate();
  }

  @Test
  public void testWildcard3() throws IndexException {
    Query query = new Question.Builder().field("wildcard").question("wat*").supportWildcards(true).build().toQuery();
    TopFieldCollector results;
    IndexReader reader = null;
    try {
      results = TopFieldCollector.create(Sort.INDEXORDER, 10, 200);
      LuceneIndexQueries.query(index, query, results);
      Assert.assertEquals(2, results.getTotalHits());
      reader = LuceneIndexQueries.grabReader(index);
      Assert.assertNotNull(reader);
      for (ScoreDoc scoreDoc : results.topDocs().scoreDocs) {
        Document doc = reader.document(scoreDoc.doc);
        Assert.assertNotNull(doc);
        String id = Fields.toString(doc.getField("id"));
        Assert.assertTrue("doc1".equals(id) || "doc2".equals(id));
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } finally {
      LuceneIndexQueries.releaseQuietly(index, reader);
    }
  }

  @Test
  public void testWildcard4() throws IndexException {
    BasicQuery<WildcardParameter> query = BasicQuery.newBasicQuery(new WildcardParameter("wildcard", "water?elon"));
    SearchResults results = LuceneIndexQueries.query(index, query);
    Assert.assertEquals(1, results.getTotalNbOfResults());
    Iterator<Document> docs = results.documents().iterator();
    Assert.assertEquals("doc1", docs.next().get("id"));
    results.terminate();
  }

  @Test
  public void testSorting1() throws IndexException {
    BasicQuery<TermParameter> query = BasicQuery.newBasicQuery(new TermParameter("field2", "value2"));
    // sort by sorting1, order is 4, 2, 1, 3
    query.setSort(new Sort(new SortField("sorting1", SortField.Type.STRING)));
    SearchResults results = LuceneIndexQueries.query(index, query);
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
    SearchResults results = LuceneIndexQueries.query(index, query);
    Assert.assertEquals(4, results.getTotalNbOfResults());
    Iterator<Document> docs = results.documents().iterator();
    Assert.assertEquals("doc2", docs.next().get("id"));
    Assert.assertEquals("doc1", docs.next().get("id"));
    Assert.assertEquals("doc4", docs.next().get("id"));
    Assert.assertEquals("doc3", docs.next().get("id"));
    // sort by sorting2, reverse order is 3, 4, 1, 2
    query.setSort(new Sort(new SortedSetSortField("sorting2", true)));
    results = LuceneIndexQueries.query(index, query);
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
    SearchResults results = LuceneIndexQueries.query(index, query);
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
    SearchResults results = LuceneIndexQueries.query(index, query);
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

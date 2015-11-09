package org.pageseeder.flint.local;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopFieldCollector;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexIO;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.content.SourceForwarder;
import org.pageseeder.flint.query.PredicateSearchQuery;
import org.pageseeder.flint.query.SearchQuery;
import org.pageseeder.flint.query.SearchResults;
import org.pageseeder.flint.util.Queries;
import org.pageseeder.flint.utils.TestUtils;
import org.pageseeder.xmlwriter.XMLWriter;
import org.pageseeder.xmlwriter.XMLWriterImpl;

public class QueryTest {

  private static File template  = new File("src/test/resources/template.xsl");
  private static File documents = new File("src/test/resources/query");
  private static File indexRoot = new File("tmp/index");

  private static LocalIndex index;
  private static IndexManager manager;
  
  @BeforeClass
  public static void init() {
    index = new LocalIndex(indexRoot);
    index.setTemplate("xml", template.toURI());
    manager = new IndexManager(new LocalFileContentFetcher(), new TestListener());
    manager.setDefaultTranslator(new SourceForwarder("xml", "UTF-8"));
    manager.start();
    System.out.println("Starting manager!");
    LocalIndexer indexer = new LocalIndexer(manager, index);
    indexer.indexDocuments(documents);
    System.out.println("Documents indexed");
    // wait a bit
    TestUtils.wait(2);
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
    SearchResults results = manager.query(index.getIndex(), query);
    Assert.assertEquals(0, results.getTotalNbOfResults());
  }
  @Test
  public void testQuery2() throws IndexException, IOException {
    SearchQuery query = new PredicateSearchQuery("field1:value1");
    SearchResults results = manager.query(index.getIndex(), query);
    Assert.assertEquals(4, results.getTotalNbOfResults());
  }

  @Test
  public void testQuery3() throws IndexException, IOException {
    SearchQuery query = new PredicateSearchQuery("field1:value2");
    SearchResults results = manager.query(index.getIndex(), query);
    Assert.assertEquals(3, results.getTotalNbOfResults());
  }

  @Test
  public void testQuery4() throws IndexException {
    SearchQuery query = new PredicateSearchQuery("field1:value3");
    SearchResults results = manager.query(index.getIndex(), query);
    Assert.assertEquals(1, results.getTotalNbOfResults());
  }

  @Test
  public void testQuery5() throws IndexException {
    SearchQuery query = new PredicateSearchQuery("field1:value4");
    SearchResults results = manager.query(index.getIndex(), query);
    Assert.assertEquals(1, results.getTotalNbOfResults());
  }

  private void outputResults(SearchResults results) throws IOException {
    XMLWriter xml = new XMLWriterImpl(new PrintWriter(System.out));
    results.toXML(xml);
    xml.close();
  }
}

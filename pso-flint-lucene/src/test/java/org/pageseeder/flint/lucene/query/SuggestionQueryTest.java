package org.pageseeder.flint.lucene.query;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.content.SourceForwarder;
import org.pageseeder.flint.local.LocalFileContentFetcher;
import org.pageseeder.flint.local.LocalIndexer;
import org.pageseeder.flint.lucene.LuceneIndexQueries;
import org.pageseeder.flint.lucene.LuceneLocalIndex;
import org.pageseeder.flint.lucene.utils.TestListener;
import org.pageseeder.flint.lucene.utils.TestUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SuggestionQueryTest {

  private static File template  = new File("src/test/resources/template.xsl");
  private static File documents = new File("src/test/resources/suggestion");
  private static File indexRoot = new File("tmp/index");

  private static LuceneLocalIndex index;
  private static IndexManager manager;

  @BeforeClass
  public static void init() {
    // clean up previous test's data
    if (indexRoot.listFiles() != null) for (File f : indexRoot.listFiles()) f.delete();
    indexRoot.delete();
    try {
      index = new LuceneLocalIndex(indexRoot, "suggestion", new StandardAnalyzer(), documents);
      index.setTemplate("xml", template.toURI());
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    manager = new IndexManager(new LocalFileContentFetcher(), new TestListener());
    manager.setDefaultTranslator(new SourceForwarder("xml", "UTF-8"));
    System.out.println("Starting manager!");
    LocalIndexer indexer = new LocalIndexer(manager, index);
    indexer.indexFolder(documents, null);
    System.out.println("Documents indexed!");
    // wait a bit
    TestUtils.wait(2);
  }

  @AfterClass
  public static void after() {
    // stop index
    System.out.println("Stopping manager!");
    manager.stop();
    System.out.println("-----------------------------------");
  }

  @Test
  public void testSingleTerm() throws IndexException, IOException {
    SuggestionQuery query = new SuggestionQuery(Collections.singletonList(new Term("name", "elec")));
    IndexReader reader = null;
    try {
      reader = LuceneIndexQueries.grabReader(index);
      query.compute(reader);
    } finally {
      LuceneIndexQueries.releaseQuietly(index, reader);
    }
    SearchResults results = LuceneIndexQueries.query(index, query);
    Assert.assertEquals(4, results.getTotalNbOfResults());
    for (Document doc : results.documents()) {
      String id = doc.get("id");
      Assert.assertTrue(id.equals("doc1") || id.equals("doc2") || id.equals("doc3") || id.equals("doc5"));
    }
    results.terminate();
  }

  @Test
  public void testSingleTermWithCondition() throws IndexException, IOException {
    SuggestionQuery query1 = new SuggestionQuery(Collections.singletonList(new Term("name", "elec")), new TermQuery(new Term("color", "blue")));
    SuggestionQuery query2 = new SuggestionQuery(Collections.singletonList(new Term("name", "elec")), new TermQuery(new Term("color", "yellow")));
    SuggestionQuery query3 = new SuggestionQuery(Collections.singletonList(new Term("name", "elec")), new TermQuery(new Term("color", "red")));
    SuggestionQuery query4 = new SuggestionQuery(Collections.singletonList(new Term("name", "gui")),  new TermQuery(new Term("color", "blue")));
    IndexReader reader = null;
    try {
      reader = LuceneIndexQueries.grabReader(index);
      query1.compute(reader);
      query2.compute(reader);
      query3.compute(reader);
      query4.compute(reader);
    } finally {
      LuceneIndexQueries.releaseQuietly(index, reader);
    }

    SearchResults results1 = LuceneIndexQueries.query(index, query1);
    Assert.assertEquals(2, results1.getTotalNbOfResults());
    for (Document doc : results1.documents()) {
      Assert.assertTrue(doc.get("id").equals("doc2") || doc.get("id").equals("doc5"));
    }

    SearchResults results2 = LuceneIndexQueries.query(index, query2);
    Assert.assertEquals(2, results2.getTotalNbOfResults());
    for (Document doc : results2.documents()) {
      Assert.assertTrue(doc.get("id").equals("doc1") || doc.get("id").equals("doc3"));
    }

    SearchResults results3 = LuceneIndexQueries.query(index, query3);
    Assert.assertEquals(1, results3.getTotalNbOfResults());
    Assert.assertEquals("doc5", results3.documents().iterator().next().get("id"));

    SearchResults results4 = LuceneIndexQueries.query(index, query4);
    Assert.assertEquals(1, results4.getTotalNbOfResults());
    Assert.assertEquals("doc5", results4.documents().iterator().next().get("id"));

  }

  @Test
  public void testMultiTermsOR() throws IndexException, IOException {
    List<Term> terms1 = new ArrayList<>();
    terms1.add(new Term("name", "elec"));
    terms1.add(new Term("name", "gui"));
    SuggestionQuery query1 = new SuggestionQuery(terms1);

    List<Term> terms2 = new ArrayList<>();
    terms2.add(new Term("name", "ac"));
    terms2.add(new Term("name", "gu"));
    SuggestionQuery query2 = new SuggestionQuery(terms2);

    IndexReader reader = null;
    try {
      reader = LuceneIndexQueries.grabReader(index);
      query1.compute(reader);
      query2.compute(reader);
    } finally {
      LuceneIndexQueries.releaseQuietly(index, reader);
    }

    SearchResults results1 = LuceneIndexQueries.query(index, query1);
    Assert.assertEquals(5, results1.getTotalNbOfResults());
    for (Document doc : results1.documents()) {
      String id = doc.get("id");
      Assert.assertTrue(id.equals("doc1") || id.equals("doc2") || id.equals("doc3") || id.equals("doc4") || id.equals("doc5"));
    }
    results1.terminate();

    SearchResults results2 = LuceneIndexQueries.query(index, query2);
    Assert.assertEquals(3, results2.getTotalNbOfResults());
    for (Document doc : results2.documents()) {
      String id = doc.get("id");
      Assert.assertTrue(id.equals("doc4") || id.equals("doc5") || id.equals("doc6"));
    }
    results2.terminate();
  }

  @Test
  public void testMultiTermsAND() throws IndexException, IOException {
    List<Term> terms1 = new ArrayList<>();
    terms1.add(new Term("name", "elec"));
    terms1.add(new Term("name", "gui"));
    SuggestionQuery query1 = new SuggestionQuery(terms1, false);

    List<Term> terms2 = new ArrayList<>();
    terms2.add(new Term("name", "ac"));
    terms2.add(new Term("name", "gu"));
    SuggestionQuery query2 = new SuggestionQuery(terms2, false);

    List<Term> terms3 = new ArrayList<>();
    terms3.add(new Term("name", "ac"));
    terms3.add(new Term("name", "invalid"));
    SuggestionQuery query3 = new SuggestionQuery(terms3, false);

    IndexReader reader = null;
    try {
      reader = LuceneIndexQueries.grabReader(index);
      query1.compute(reader);
      query2.compute(reader);
      query3.compute(reader);
    } finally {
      LuceneIndexQueries.releaseQuietly(index, reader);
    }

    SearchResults results1 = LuceneIndexQueries.query(index, query1);
    Assert.assertEquals(1, results1.getTotalNbOfResults());
    Assert.assertEquals("doc5", results1.documents().iterator().next().get("id"));
    results1.terminate();

    SearchResults results2 = LuceneIndexQueries.query(index, query2);
    Assert.assertEquals(2, results2.getTotalNbOfResults());
    for (Document doc : results2.documents()) {
      String id = doc.get("id");
      Assert.assertTrue(id.equals("doc4") || id.equals("doc6"));
    }
    results2.terminate();

    SearchResults results3 = LuceneIndexQueries.query(index, query3);
    Assert.assertEquals(2, results3.getTotalNbOfResults());
    for (Document doc : results3.documents()) {
      String id = doc.get("id");
      Assert.assertTrue(id.equals("doc4") || id.equals("doc6"));
    }
    results3.terminate();
  }

  @Test
  public void testMultiTermsORWithCondition() throws IndexException, IOException {
    List<Term> terms1 = new ArrayList<>();
    terms1.add(new Term("name", "elec"));
    terms1.add(new Term("name", "gui"));
    SuggestionQuery query1 = new SuggestionQuery(terms1, new TermQuery(new Term("color", "blue")));

    List<Term> terms2 = new ArrayList<>();
    terms2.add(new Term("name", "ac"));
    terms2.add(new Term("name", "gu"));
    SuggestionQuery query2 = new SuggestionQuery(terms2, new TermQuery(new Term("color", "green")));

    IndexReader reader = null;
    try {
      reader = LuceneIndexQueries.grabReader(index);
      query1.compute(reader);
      query2.compute(reader);
    } finally {
      LuceneIndexQueries.releaseQuietly(index, reader);
    }

    SearchResults results1 = LuceneIndexQueries.query(index, query1);
    Assert.assertEquals(2, results1.getTotalNbOfResults());
    for (Document doc : results1.documents()) {
      String id = doc.get("id");
      Assert.assertTrue(id.equals("doc2") || id.equals("doc5"));
    }
    results1.terminate();

    SearchResults results2 = LuceneIndexQueries.query(index, query2);
    Assert.assertEquals(2, results2.getTotalNbOfResults());
    for (Document doc : results2.documents()) {
      String id = doc.get("id");
      Assert.assertTrue(id.equals("doc4") || id.equals("doc6"));
    }
    results2.terminate();
  }

  @Test
  public void testMultiTermsANDWithCondition() throws IndexException, IOException {
    List<Term> terms1 = new ArrayList<>();
    terms1.add(new Term("name", "elec"));
    terms1.add(new Term("name", "gui"));
    SuggestionQuery query1 = new SuggestionQuery(terms1, new TermQuery(new Term("color", "gold")), false);

    List<Term> terms2 = new ArrayList<>();
    terms2.add(new Term("name", "ac"));
    terms2.add(new Term("name", "gu"));
    SuggestionQuery query2 = new SuggestionQuery(terms2, new TermQuery(new Term("color", "gold")), false);

    IndexReader reader = null;
    try {
      reader = LuceneIndexQueries.grabReader(index);
      query1.compute(reader);
      query2.compute(reader);
    } finally {
      LuceneIndexQueries.releaseQuietly(index, reader);
    }

    SearchResults results1 = LuceneIndexQueries.query(index, query1);
    Assert.assertEquals(0, results1.getTotalNbOfResults());
    results1.terminate();

    SearchResults results2 = LuceneIndexQueries.query(index, query2);
    Assert.assertEquals(1, results2.getTotalNbOfResults());
    Assert.assertEquals("doc6", results2.documents().iterator().next().get("id"));
    results2.terminate();
  }

  @Test
  public void testMultiTermsMultiFieldsAND() throws IndexException, IOException {

    List<Term> terms2 = new ArrayList<>();
    terms2.add(new Term("name", "elec"));
    terms2.add(new Term("name", "tra"));
    terms2.add(new Term("description", "elec"));
    terms2.add(new Term("description", "tra"));
    SuggestionQuery query2 = new SuggestionQuery(terms2, false);

    List<Term> terms3 = new ArrayList<>();
    terms3.add(new Term("name", "elec"));
    terms3.add(new Term("name", "tra"));
    terms3.add(new Term("description", "elec"));
    terms3.add(new Term("description", "tra"));
    terms3.add(new Term("color", "elec"));
    terms3.add(new Term("color", "tra"));
    SuggestionQuery query3 = new SuggestionQuery(terms3, false);

    IndexReader reader = null;
    try {
      reader = LuceneIndexQueries.grabReader(index);
      query2.compute(reader);
      query3.compute(reader);
    } finally {
      LuceneIndexQueries.releaseQuietly(index, reader);
    }

    SearchResults results2 = LuceneIndexQueries.query(index, query2);
    Assert.assertEquals(3, results2.getTotalNbOfResults());
    for (Document doc : results2.documents()) {
      String id = doc.get("id");
      Assert.assertTrue(id.equals("doc1") || id.equals("doc2") || id.equals("doc3"));
    }
    results2.terminate();

    SearchResults results3 = LuceneIndexQueries.query(index, query3);
    Assert.assertEquals(3, results3.getTotalNbOfResults());
    for (Document doc : results3.documents()) {
      String id = doc.get("id");
      Assert.assertTrue(id.equals("doc1") || id.equals("doc2") || id.equals("doc3"));
    }
    results3.terminate();
  }

}

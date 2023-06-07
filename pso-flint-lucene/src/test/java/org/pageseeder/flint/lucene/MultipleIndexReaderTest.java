package org.pageseeder.flint.lucene;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.pageseeder.flint.Index;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.Requester;
import org.pageseeder.flint.content.SourceForwarder;
import org.pageseeder.flint.indexing.IndexJob;
import org.pageseeder.flint.lucene.facet.StringFieldFacet;
import org.pageseeder.flint.lucene.query.PredicateSearchQuery;
import org.pageseeder.flint.lucene.query.SearchResults;
import org.pageseeder.flint.lucene.search.Terms;
import org.pageseeder.flint.lucene.utils.TestListener;
import org.pageseeder.flint.lucene.utils.TestUtils;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MultipleIndexReaderTest {

  private static final File template  = new File("src/test/resources/template.xsl");
  private static final File indexRoot = new File("tmp/indexes");

  private static final int NB_INDEXES = 20;
  private static final int NB_DOCUMENTS = 50;
  private static final int NB_FIELDS = 20;
  private static final int NB_VALUES = 4;
  private static final String QUERY = "+field1:value1 +field5:value2";
  private static final Map<String, Index> indexes = new HashMap<>();
  private static IndexManager manager;

  private final static ExecutorService threads = Executors.newCachedThreadPool();

  @Before
  public void init() {
    indexRoot.mkdir();

    manager = new IndexManager(job -> {
      StringBuilder xml = new StringBuilder("<documents version='5.0'><document>");
      xml.append("<field tokenize='false' name='").append(TestUtils.ID_FIELD).append("'>").append(job.getContentID()).append("</field>");
      for (String field : fields()) {
        for (int k = 1; k <= NB_VALUES; k++) {
          xml.append("<field tokenize='false' name='").append(field).append("'>").append("value").append(k).append("</field>");
        }
      }
      xml.append("</document></documents>");
      return new TestUtils.TestContent(job.getContentID(), xml.toString());
    }, new TestListener());
    manager.setDefaultTranslator(new SourceForwarder("xml", "UTF-8"));
    System.out.println("Initialising indexes");


    for (int i = 1; i <= NB_INDEXES; i++) init(i);
    System.out.println("Starting indexing");
    long before = System.currentTimeMillis();
    Requester req = new Requester("MultipleIndexReaderTest");
    for (Index index : indexes.values()) {
      for (int i = 1; i <= NB_DOCUMENTS; i++) {
        manager.index(index.getIndexID()+"-doc"+i, TestUtils.TYPE, index, req, IndexJob.Priority.HIGH, null);
      }
    }
    // wait a bit
    while (!manager.getStatus().isEmpty()) {
      TestUtils.wait(1);
    }
    long taken = System.currentTimeMillis() - before;
    TestUtils.wait(2);
    System.out.println("Indexing done in "+taken+"ms");
  }

  public void init(int nb) {
    File myRoot = new File(indexRoot, "index"+nb);
    if (!myRoot.exists()) myRoot.mkdir();
    // clean up previous test's data
    for (File f : myRoot.listFiles()) f.delete();
    myRoot.delete();
    try {
      Index index = new LuceneIndex("index"+nb, myRoot, new StandardAnalyzer());
      index.setTemplates(TestUtils.TYPE, TestUtils.MEDIA_TYPE, template.toURI());
      indexes.put("index"+nb, index);
    } catch (Exception ex) {
      LoggerFactory.getLogger(TestUtils.class).error("Something went wrong", ex);
    }
  }

  @AfterClass
  public static void after() {
    // stop index
    System.out.println("Stopping manager!");
    manager.stop();
    System.out.println("-----------------------------------");
  }


  @Test
  public void test1() throws IndexException, IOException {
    MultipleIndexReader multi1 = LuceneIndexQueries.getMultipleIndexReader(new ArrayList<>(indexes.values()));
    IndexReader reader1 = multi1.grab();
    Assert.assertEquals(NB_DOCUMENTS * NB_INDEXES, reader1.numDocs());
    for (String field : fields()) {
      List<Term> terms = Terms.terms(reader1, field);
      Assert.assertEquals(NB_VALUES, terms.size());
    }
    System.out.println("Searched multi indexes 1");

    // some searchers
    for (int i = 0; i < 10; i++) {
      List<Index> searchers = randoms();
      for (Index index : searchers) {
        threads.execute(new ParallelSearcher(index));
      }
      threads.execute(new MultiParallelSearcher(searchers));
    }
    // close a few indexes
    List<Index> toclose = randoms();
    for (Index i : toclose) { i.close(); }
    System.out.println("Closed "+toclose.size()+" indexes");
    SearchResults results = LuceneIndexQueries.query(new ArrayList<>(indexes.values()), new PredicateSearchQuery(QUERY));
    Assert.assertEquals(NB_INDEXES * NB_DOCUMENTS, results.getTotalNbOfResults());
    results.terminate();
    // more searchers
    for (int i = 0; i < 10; i++) {
      List<Index> searchers = randoms();
      for (Index index : searchers) {
        threads.execute(new ParallelSearcher(index));
      }
      threads.execute(new MultiParallelSearcher(searchers));
    }

    for (String field : fields()) {
      List<Term> terms = Terms.terms(reader1, field);
      Assert.assertEquals(NB_VALUES, terms.size());
    }
    System.out.println("Searched multi indexes 1 again");
    while (ParallelSearcher.searching.get() > 0 || MultiParallelSearcher.searching.get() > 0) {
      TestUtils.wait(1);
    }
    // release multi reader
    multi1.releaseSilently();
  }

  private List<Index> randoms() {
    int nb = Math.round((float) NB_INDEXES / 3);
    Random r = new Random(System.nanoTime());
    List<Index> rands = new ArrayList<>(nb);
    while (rands.size() < nb) {
      int rand = r.nextInt(NB_INDEXES) + 1; // avoid 0
      if (indexes.containsKey("index"+rand))
        rands.add(indexes.get("index"+rand));
    }
    return rands;
  }

  private static List<String> fields() {
    List<String> fields = new ArrayList<>();
    for (int i = 1; i <= NB_FIELDS; i++) {
      fields.add("field"+i);
    }
    return fields;
  }
  private static class MultiParallelSearcher implements Runnable {
    private final static AtomicInteger searching = new AtomicInteger(0);
    private final List<Index> indexes;
    MultiParallelSearcher(List<Index> idxs) {
      this.indexes = idxs;
    }
    @Override
    public void run() {
      synchronized (searching) {
        searching.incrementAndGet();
      }
      try {
        MultipleIndexReader multi = LuceneIndexQueries.getMultipleIndexReader(indexes);
        IndexReader reader = multi.grab();
//        Assert.assertEquals(NB_DOCUMENTS * indexes.size(), reader.numDocs());
        for (String field : fields()) {
          List<Term> terms = Terms.terms(reader, field);
          Assert.assertEquals(NB_VALUES, terms.size());
        }
        IndexSearcher searcher = new IndexSearcher(reader);
        for (String field : fields()) {
          StringFieldFacet facet = StringFieldFacet.newFacet(field);
          facet.compute(searcher, 20);
          Assert.assertEquals(NB_VALUES, facet.getTotalTerms());
        }
        SearchResults results = LuceneIndexQueries.query(indexes, new PredicateSearchQuery(QUERY));
        Assert.assertEquals(NB_DOCUMENTS * indexes.size(), results.getTotalNbOfResults());
        results.terminate();
        multi.releaseSilently();
        System.out.println("Searched multi index");
      } catch (IndexException | IOException ex) {
        LoggerFactory.getLogger(TestUtils.class).error("Something went wrong", ex);
      }
      synchronized (searching) {
        searching.decrementAndGet();
      }
    }
  }

  private static class ParallelSearcher implements Runnable {
    private final static AtomicInteger searching = new AtomicInteger(0);
    private final Index index;
    ParallelSearcher(Index idx) {
      this.index = idx;
    }
    @Override
    public void run() {
      synchronized (searching) {
        searching.incrementAndGet();
      }
      // read a single index at the same time
      if (index == null) System.out.println("null index");
      else try {
        IndexReader reader = LuceneIndexQueries.grabReader(index);
        if (reader == null) System.out.println("null reader for "+index.getIndexID());
        else {
          Assert.assertEquals(NB_DOCUMENTS, reader.numDocs());
          for (String field : fields()) {
            List<Term> terms = Terms.terms(reader, field);
            Assert.assertEquals(NB_VALUES, terms.size());
          }
          SearchResults results = LuceneIndexQueries.query(index, new PredicateSearchQuery(QUERY));
          Assert.assertEquals(NB_DOCUMENTS, results.getTotalNbOfResults());
          results.terminate();
          LuceneIndexQueries.releaseQuietly(index, reader);
        }
      } catch (IndexException | IOException ex) {
        System.out.println(ex);
      }
      synchronized (searching) {
        searching.decrementAndGet();
      }
    }
  }
}

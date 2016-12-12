package org.pageseeder.flint.solr.query;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.xml.transform.TransformerException;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.common.SolrDocumentList;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.Requester;
import org.pageseeder.flint.content.Content;
import org.pageseeder.flint.content.ContentFetcher;
import org.pageseeder.flint.content.SourceForwarder;
import org.pageseeder.flint.indexing.IndexJob;
import org.pageseeder.flint.indexing.IndexJob.Priority;
import org.pageseeder.flint.solr.index.SolrIndex;
import org.pageseeder.flint.solr.index.TestListener;

public class QueryTest {

  private final static File template  = new File("src/test/resources/template.xsl");

  private static SolrIndex index;
  private static IndexManager manager;
  private static SolrQueryManager queries;
  private static final long RANDOM = Math.abs(new Random(System.nanoTime()).nextLong());
  
  private final static String MAIN_XML_DATA = "<documents version='5.0'>\n"+
      "<document>\n"+
      "<field name='"+TestUtils.ID_FIELD+"'>doc1</field>\n"+
      "<field name='_path'>doc1</field>\n"+
      "<field name='_lastmodified'>0</field>\n"+
      "<field name='test_term'>value1_"+RANDOM+"</field>\n"+
      "<field name='test_facet'>value1</field>\n"+
      "<field name='test_text_highlight'>This text is used to test the highlight feature.</field>\n"+
      "<field name='test_join_to'>join1</field>\n"+
      "<field name='test_group'>value1</field>\n"+
    "</document>\n"+
    "<document>\n"+
      "<field name='"+TestUtils.ID_FIELD+"'>doc2</field>\n"+
      "<field name='_path'>doc2</field>\n"+
      "<field name='_lastmodified'>0</field>\n"+
      "<field name='test_term'>value2_"+RANDOM+"</field>\n"+
      "<field name='test_facet'>value2</field>\n"+
      "<field name='test_text_highlight'>This text is used to test the highlight feature as well and it has the word highlight twice.</field>\n"+
      "<field name='test_group'>value2</field>\n"+
      "<field name='test_join_to'>join2</field>\n"+
    "</document>\n"+
    "<document>\n"+
      "<field name='"+TestUtils.ID_FIELD+"'>doc3</field>\n"+
      "<field name='_path'>doc3</field>\n"+
      "<field name='_lastmodified'>0</field>\n"+
      "<field name='test_term'>value3_"+RANDOM+"</field>\n"+
      "<field name='test_facet'>value1</field>\n"+
      "<field name='test_text_highlight'>This text does not have the word high light and its value is very long so the idea is that it should be at the end of the results.</field>\n"+
      "<field name='test_text_highlight2'>But this one does: highlight.</field>\n"+
      "<field name='test_group'>value1</field>\n"+
      "<field name='test_join_to'>join3</field>\n"+
    "</document>\n"+
    "<document>\n"+
      "<field name='"+TestUtils.ID_FIELD+"'>doc4</field>\n"+
      "<field name='_path'>doc4</field>\n"+
      "<field name='_lastmodified'>0</field>\n"+
      "<field name='test_term'>value2_"+RANDOM+"</field>\n"+
      "<field name='test_facet'>value1</field>\n"+
      "<field name='test_text_highlight'>Just highlight.</field>\n"+
      "<field name='test_group'>value3</field>\n"+
      "<field name='test_join_to'>join5</field>\n"+
    "</document>\n"+
  "</documents>";

  private final static String JOIN_XML_DATA = "<documents version='5.0'>\n"+
      "<document>\n"+
      "<field name='"+TestUtils.ID_FIELD+"'>join-doc1</field>\n"+
      "<field name='_path'>doc1</field>\n"+
      "<field name='_lastmodified'>0</field>\n"+
      "<field name='test_filter'>include</field>\n"+
      "<field name='test_join_from'>join1</field>\n"+
    "</document>\n"+
    "<document>\n"+
      "<field name='"+TestUtils.ID_FIELD+"'>join-doc2</field>\n"+
      "<field name='_path'>doc2</field>\n"+
      "<field name='_lastmodified'>0</field>\n"+
      "<field name='test_filter'>exclude</field>\n"+
      "<field name='test_join_from'>join2</field>\n"+
    "</document>\n"+
    "<document>\n"+
      "<field name='"+TestUtils.ID_FIELD+"'>join-doc3</field>\n"+
      "<field name='_path'>doc3</field>\n"+
      "<field name='_lastmodified'>0</field>\n"+
      "<field name='test_filter'>include</field>\n"+
      "<field name='test_join_from'>join3</field>\n"+
    "</document>\n"+
    "<document>\n"+
      "<field name='"+TestUtils.ID_FIELD+"'>join-doc4</field>\n"+
      "<field name='_path'>doc4</field>\n"+
      "<field name='_lastmodified'>0</field>\n"+
      "<field name='test_filter'>include</field>\n"+
      "<field name='test_join_from'>join4</field>\n"+
    "</document>\n"+
  "</documents>";

  @BeforeClass
  public static void init() throws IndexException {
    System.out.println("indexing with "+RANDOM);
    index = new SolrIndex("test-solr-query", TestUtils.CATALOG);
    try {
      index.setTemplates(TestUtils.TYPE, TestUtils.MEDIA_TYPE, template.toURI());
    } catch (TransformerException ex) {
      ex.printStackTrace();
    }
    // just in case
    try {
      index.getIndexIO().clearIndex();
    } catch (IndexException ex) {
      ex.printStackTrace();
    }
    manager = new IndexManager(new ContentFetcher() {
      @Override
      public Content getContent(IndexJob job) {
        // delete?
        if (job.getContentID().startsWith("delete-")) {
          return new TestUtils.TestContent(job.getContentID().substring(7), null);
        }
        // test for the join index
        String xml = "test-solr-join".equals(job.getIndex().getIndexID()) ? JOIN_XML_DATA : MAIN_XML_DATA;
        return new TestUtils.TestContent(job.getContentID(), xml);
      }
    }, new TestListener());
    manager.setDefaultTranslator(new SourceForwarder("xml", "UTF-8"));
    System.out.println("Starting manager!");
    manager.index("content", TestUtils.TYPE, index, new Requester(QueryTest.class.getName()), Priority.HIGH, null);
    System.out.println("Documents indexed");
    // wait a bit
    TestUtils.wait(1);
    queries = new SolrQueryManager(index);
  }

  @AfterClass
  public static void after() {
    // stop index
    System.out.println("Stopping manager!");
    index.close();
    manager.stop();
    System.out.println("-----------------------------------");
  }

  @Test
  public void testSelect1() {
    try {
      // query
      SolrQuery query1 = new SolrQuery(TestUtils.ID_FIELD+":doc1");
      // results
      List<String> results = new ArrayList<>();
      // run
      queries.select(query1, TestUtils.results(results, TestUtils.ID_FIELD));
      // test
      Assert.assertEquals(1, results.size());
      Assert.assertEquals("doc1", results.iterator().next());
    } catch (Exception ex) {
      ex.printStackTrace();
      Assert.fail(ex.getMessage());
    }
  }

  @Test
  public void testSelect2() {
    try {
      // query
      SolrQuery query1 = new SolrQuery("test_term:value2_"+RANDOM);
      // results
      List<String> results = new ArrayList<>();
      // run
      queries.select(query1, TestUtils.results(results, TestUtils.ID_FIELD));
      // test
      Assert.assertEquals(2, results.size());
      Iterator<String> all = results.iterator();
      Assert.assertEquals("doc2", all.next());
      Assert.assertEquals("doc4", all.next());
    } catch (Exception ex) {
      ex.printStackTrace();
      Assert.fail(ex.getMessage());
    }
  }

  @Test
  public void testQueryFacets() {
    try {
      // query
      SolrQuery query1 = new SolrQuery("test_term:value1_"+RANDOM+" test_term:value2_"+RANDOM);
      Facets facets = new Facets(Collections.singletonList("test_facet"));
      // results
      List<String> results = new ArrayList<>();
      // run
      queries.select(query1, TestUtils.results(results, TestUtils.ID_FIELD), facets);
      // test
      Assert.assertEquals(3, results.size());
      List<FacetField> ff = facets.getFacetFields();
      Assert.assertEquals(1, ff.size());
      FacetField ffield = ff.get(0);
      Assert.assertEquals("test_facet", ffield.getName());
      Assert.assertEquals(2,            ffield.getValueCount());
      Assert.assertEquals("value1",     ffield.getValues().get(0).getName());
      Assert.assertEquals(2,            ffield.getValues().get(0).getCount());
      Assert.assertEquals("value2",     ffield.getValues().get(1).getName());
      Assert.assertEquals(1,            ffield.getValues().get(1).getCount());
    } catch (Exception ex) {
      ex.printStackTrace();
      Assert.fail(ex.getMessage());
    }
  }

  @Test
  public void testFacets() {
    try {
      // query
      Facets facets = new Facets(Collections.singletonList("test_facet"));
      // run
      queries.facets(facets);
      // test
      List<FacetField> ff = facets.getFacetFields();
      Assert.assertEquals(1, ff.size());
      FacetField ffield = ff.get(0);
      Assert.assertEquals("test_facet", ffield.getName());
      Assert.assertEquals(2,            ffield.getValueCount());
      Assert.assertEquals("value1",     ffield.getValues().get(0).getName());
      Assert.assertEquals(3,            ffield.getValues().get(0).getCount());
      Assert.assertEquals("value2",     ffield.getValues().get(1).getName());
      Assert.assertEquals(1,            ffield.getValues().get(1).getCount());
    } catch (Exception ex) {
      ex.printStackTrace();
      Assert.fail(ex.getMessage());
    }
  }

  @Test
  public void testHighlight() {
    try {
      // query
      SolrQuery query1 = new SolrQuery("test_text_highlight:highlight test_text_highlight2:highlight");
      // results
      List<String> results = new ArrayList<>();
      List<DocumentHighlight> highlights = new ArrayList<DocumentHighlight>();
      // run
      queries.select(query1, TestUtils.results(results, TestUtils.ID_FIELD), null, new Highlights("test_text_highlight").results(highlights));
      // test
      Assert.assertEquals(4, highlights.size());
      DocumentHighlight first = highlights.get(0);
      Assert.assertEquals("doc4", first.id());
      Assert.assertEquals(1, first.highlights("test_text_highlight").size());
      Assert.assertEquals("Just <em>highlight</em>.", first.highlights("test_text_highlight").get(0));
      DocumentHighlight second = highlights.get(1);
      Assert.assertEquals("doc3", second.id());
      Assert.assertNull(second.highlights("test_text_highlight"));
      Assert.assertNull(second.highlights("test_text_highlight2"));
      DocumentHighlight third = highlights.get(2);
      Assert.assertEquals("doc2", third.id());
      Assert.assertEquals(1, third.highlights("test_text_highlight").size());
      Assert.assertEquals("This text is used to test the <em>highlight</em> feature as well and it has the word <em>highlight</em> twice.", third.highlights("test_text_highlight").get(0));
      DocumentHighlight fourth = highlights.get(3);
      Assert.assertEquals("doc1", fourth.id());
      Assert.assertEquals(1, fourth.highlights("test_text_highlight").size());
      Assert.assertEquals("This text is used to test the <em>highlight</em> feature.", fourth.highlights("test_text_highlight").get(0));
    } catch (Exception ex) {
      ex.printStackTrace();
      Assert.fail(ex.getMessage());
    }
  }

  @Test
  public void testGroup() {
    try {
      // query
      SolrQuery query1 = new SolrQuery("_lastmodified:0");
      // results
      Map<String, SolrDocumentList> groups = queries.group(query1, "test_group", 10);
      // test
      Assert.assertEquals(3, groups.size());
      SolrDocumentList list1 = groups.get("value1");
      Assert.assertEquals(2, list1.size());
      Assert.assertEquals("doc1", list1.get(0).getFieldValue(TestUtils.ID_FIELD));
      Assert.assertEquals("doc3", list1.get(1).getFieldValue(TestUtils.ID_FIELD));
      SolrDocumentList list2 = groups.get("value2");
      Assert.assertEquals(1, list2.size());
      Assert.assertEquals("doc2", list2.get(0).getFieldValue(TestUtils.ID_FIELD));
      SolrDocumentList list3 = groups.get("value3");
      Assert.assertEquals(1, list3.size());
      Assert.assertEquals("doc4", list3.get(0).getFieldValue(TestUtils.ID_FIELD));
    } catch (Exception ex) {
      ex.printStackTrace();
      Assert.fail(ex.getMessage());
    }
  }

  @Test
  public void testJoin1() throws IndexException {
    // second index
    SolrIndex index2 = new SolrIndex("test-solr-join", TestUtils.CATALOG);
    try {
      index2.setTemplates(TestUtils.TYPE, TestUtils.MEDIA_TYPE, template.toURI());
    } catch (TransformerException ex) {
      ex.printStackTrace();
    }
    try {
      index2.getIndexIO().clearIndex();
    } catch (IndexException ex) {
      ex.printStackTrace();
    }
    manager.index("content", TestUtils.TYPE, index2, new Requester(QueryTest.class.getName()), Priority.HIGH, null);
    // wait a bit
    TestUtils.wait(1);

    // query
    SolrQuery query = new SolrQuery("_lastmodified:0");
    // join query
    SolrQuery joinQuery = new SolrQuery("test_filter:include");
    // results
    List<String> results = new ArrayList<>();
    // results
    queries.join(query, "test_join_from", "test_join_to", "test-solr-join", joinQuery, TestUtils.results(results, TestUtils.ID_FIELD));
    // test
    Assert.assertEquals(2, results.size());
    Assert.assertEquals("doc1", results.get(0));
    Assert.assertEquals("doc3", results.get(1));
    
  }

  @Test
  public void testJoin2() throws IndexException {
    // second index
    SolrIndex index2 = new SolrIndex("test-solr-join", TestUtils.CATALOG);
    try {
      index2.setTemplates(TestUtils.TYPE, TestUtils.MEDIA_TYPE, template.toURI());
    } catch (TransformerException ex) {
      ex.printStackTrace();
    }
    try {
      index2.getIndexIO().clearIndex();
    } catch (IndexException ex) {
      ex.printStackTrace();
    }
    manager.index("content", TestUtils.TYPE, index2, new Requester(QueryTest.class.getName()), Priority.HIGH, null);
    // wait a bit
    TestUtils.wait(1);
    // query
    SolrQuery query = new SolrQuery("_lastmodified:0");
    // join query
    SolrQuery joinQuery = new SolrQuery("test_filter:exclude");
    // results
    List<String> results = new ArrayList<>();
    // results
    queries.join(query, "test_join_from", "test_join_to", "test-solr-join", joinQuery, TestUtils.results(results, TestUtils.ID_FIELD));
    // test
    Assert.assertEquals(1, results.size());
    Assert.assertEquals("doc2", results.get(0));
    
  }

  @Test
  public void testJoin3() throws IndexException {
    // second index
    SolrIndex index2 = new SolrIndex("test-solr-join", TestUtils.CATALOG);
    try {
      index2.setTemplates(TestUtils.TYPE, TestUtils.MEDIA_TYPE, template.toURI());
    } catch (TransformerException ex) {
      ex.printStackTrace();
    }
    try {
      index2.getIndexIO().clearIndex();
    } catch (IndexException ex) {
      ex.printStackTrace();
    }
    manager.index("content", TestUtils.TYPE, index2, new Requester(QueryTest.class.getName()), Priority.HIGH, null);
    // wait a bit
    TestUtils.wait(1);
    // query
    SolrQuery query = new SolrQuery("_lastmodified:0");
    // join query
    SolrQuery joinQuery = new SolrQuery("_lastmodified:0");
    // results
    List<String> results = new ArrayList<>();
    // results
    queries.join(query, "test_join_from", "test_join_to", "test-solr-join", joinQuery, TestUtils.results(results, TestUtils.ID_FIELD));
    // test
    Assert.assertEquals(3, results.size());
    Assert.assertEquals("doc1", results.get(0));
    Assert.assertEquals("doc2", results.get(1));
    Assert.assertEquals("doc3", results.get(2));
    
  }

}

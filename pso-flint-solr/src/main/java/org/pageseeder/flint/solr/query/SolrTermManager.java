/*
 * Copyright (c) 1999-2015 Allette systems pty. ltd.
 */
package org.pageseeder.flint.solr.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.TermsResponse;
import org.apache.solr.client.solrj.response.TermsResponse.Term;
import org.pageseeder.flint.Index;
import org.pageseeder.flint.indexing.FlintField;
import org.pageseeder.flint.solr.index.SolrIndexIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ciber Cai
 * @since 21 September ,2016
 */
public class SolrTermManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(SolrTermManager.class);

  private final String _requestHanlder;

  private final SolrIndexIO _solr;

  private final int _limit;

  public SolrTermManager(Index index) {
    this(index, 20, null);
  }

  public SolrTermManager(Index index, int limit) {
    this(index, limit, null);
  }

  public SolrTermManager(Index index, int limit, String requestHanlder) {
    this._solr = (SolrIndexIO) index.getIndexIO();
    this._limit = limit;
    this._requestHanlder = requestHanlder != null ? requestHanlder : "/terms";
  }

  public List<SolrTerm> listTerms(FlintField field) {
    return listTerms(Arrays.asList(field));
  }

  public List<SolrTerm> listTerms(List<FlintField> fields) {
    LOGGER.info("List term for fields {}", fields);
    List<SolrTerm> terms = new ArrayList<>();

    SolrQuery query = new SolrQuery("*:*");
    query.setRequestHandler(this._requestHanlder);
    query.setTerms(true);
    query.setTermsLimit(this._limit);
    for (FlintField field : fields) {
      query.addTermsField(field.name());
    }
    query.setTermsMinCount(1);
    query.setTermsSortString("count");

    QueryResponse response = this._solr.request(new QueryRequest(query));
    if (response != null) {
      TermsResponse tresponse = response.getTermsResponse();
      for (FlintField field : fields) {
        List<Term> ts = tresponse.getTerms(field.name());
        if (ts != null) {
          for (Term t : ts) {
            SolrTerm.Builder builder = new SolrTerm.Builder();
            builder.field(field).term(t.getTerm()).frequency(t.getFrequency());
            terms.add(builder.build());
          }
        }
      }
    }
    return terms;
  }

}

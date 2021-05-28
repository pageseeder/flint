/*
 * Copyright (c) 1999-2016 Allette systems pty. ltd.
 */
package org.pageseeder.flint.solr.query;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.Group;
import org.apache.solr.client.solrj.response.GroupCommand;
import org.apache.solr.client.solrj.response.GroupResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.pageseeder.flint.Index;
import org.pageseeder.flint.solr.index.SolrIndexIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The manager to execute the Solr Query.
 *
 * @author Ciber Cai
 * @since 22 August 2016
 */
public class SolrQueryManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(SolrQueryManager.class);

  private final SolrIndexIO _solr;

  private final int _start;

  private final int _row;

  public SolrQueryManager(Index index) {
    this(index, 0, 10);
  }

  public SolrQueryManager(Index index, int start, int row) {
    this._solr = (SolrIndexIO) index.getIndexIO();
    this._start = start;
    this._row = row;
  }

  /**
   * @param facets the facets to list
   */
  public void facets(Facets facets) {
    select(new SolrQuery("*:*"), null, facets, null);
  }

  /**
   * @param query the {@link SolrQuery}
   * @param consumer the {@link Consumer}
   * @return the {@link SearchResultsMetadata}
   */
  public SearchResultsMetadata select(SolrQuery query, Consumer<SolrDocument> consumer) {
    return select(query, consumer, null, null);
  }

  /**
   * @param query the {@link SolrQuery}
   * @param consumer the {@link Consumer}
   * @param facets   the {@link Facets}
   * @return the {@link SearchResultsMetadata}
   */
  public SearchResultsMetadata select(SolrQuery query, Consumer<SolrDocument> consumer, Facets facets) {
    return select(query, consumer, facets, null);
  }

  /**
   * @param query the {@link SolrQuery}
   * @param consumer the {@link Consumer}
   * @param highlights the {@link Highlights}
   * @return the {@link SearchResultsMetadata}
   */
  public SearchResultsMetadata select(SolrQuery query, Consumer<SolrDocument> consumer, Highlights highlights) {
    return select(query, consumer, null, highlights);
  }


  /**
   * @param query              the {@link SolrQuery}
   * @param documentConsumer   how to handle the results
   * @param facets             the {@link Facets}
   * @param highlights         the {@link Highlights}
   *
   * @return the {@link SearchResultsMetadata}
   */
  public SearchResultsMetadata select(SolrQuery query, Consumer<SolrDocument> documentConsumer, Facets facets, Highlights highlights) {
    if (query == null) throw new NullPointerException("Search query is null");

    query.setRows(this._row);
    query.setStart(this._start);

    if (highlights != null) {
      //solrQuery.setHighlightSimplePost("role");
      query.setHighlight(true);
      query.setHighlightSnippets(highlights.snippet);
      query.setHighlightFragsize(highlights.fragsize);
      query.addHighlightField(highlights._field);
    }

    if (facets != null) {
      query.setFacet(true);
      for (String facet : facets._facets) {
        if (facet != null && !facet.isEmpty()) {
          query.addFacetField(facet);
        }
      }
      query.setFacetLimit(facets.limit);
      query.setFacetMinCount(facets.mincount);
      query.setFacetMissing(facets.missing);
      query.setFacetPrefix(facets.prefix);
      query.setFacetSort(facets.sort);
      if (facets.prefixes != null) {
        for (String facet : facets.prefixes.keySet())
          query.setFacetPrefix(facet, facets.prefixes.get(facet));
      }
    }

    LOGGER.info("query {}", query.toQueryString());
    QueryResponse response = this._solr.query(query);

    if (response != null) {
      LOGGER.info("response size {}", response.getResponse().size());

      // highlights
      if (highlights != null) {
        if (highlights.consumer != null) {
          response.getHighlighting().forEach(highlights.consumer);
        } else if (highlights.results != null) {
          response.getHighlighting().forEach(new BiConsumer<String, Map<String, List<String>>>() {
            @Override
            public void accept(String id, Map<String, List<String>> highlightMap) {
              DocumentHighlight.Builder builder = new DocumentHighlight.Builder();
              builder.id(id);
              for (Entry<String, List<String>> entry : highlightMap.entrySet()) {
                builder.highlights(entry.getKey(), entry.getValue());
              }
              highlights.results.add(builder.build());
            }
          });
        }
      }

      // facets
      if (facets != null) {
        facets.facetFields = response.getFacetFields();
        facets.facetDates  = response.getFacetDates();
      }

      // results
      SolrDocumentList list = response.getResults();
      if (list != null) {
        if (documentConsumer != null)
          list.forEach(documentConsumer);
        return new SearchResultsMetadata(query, list.getNumFound(), this._start, this._row);
      }

    }
    return null;
  }

  /**
   * @param query the {@link SolrQuery}
   *
   * @return the {@link QueryResponse}
   */
  public QueryResponse query(SolrQuery query) {
    return query(query, null, null);
  }

  /**
   * @param field      the field searched
   * @param phrase     the phrase to search for
   * @param facets     the facets details
   * @param highlights the highlights details
   *
   * @return the {@link QueryResponse}
   */
  public QueryResponse query(String field, String phrase, Facets facets, Highlights highlights) {
    return query(new SolrQuery(field+":\""+phrase+"\""), facets, highlights);
  }

  /**
   * @param query      the {@link SolrQuery}
   * @param facets     the facets details
   * @param highlights the highlights details
   *
   * @return the {@link QueryResponse}
   */
  public QueryResponse query(SolrQuery query, Facets facets, Highlights highlights) {
    if (query == null) throw new NullPointerException("Search query is null");

    query.setRows(this._row);
    query.setStart(this._start);

    if (highlights != null) {
      query.setHighlight(true);
      query.addHighlightField(highlights._field);
      query.setHighlightRequireFieldMatch(highlights.fieldMustMatch);
      query.setHighlightSnippets(highlights.snippet);
      query.setHighlightFragsize(highlights.fragsize);
      query.setHighlightSimplePre(highlights.pre);
      query.setHighlightSimplePost(highlights.post);
    }

    if (facets != null) {
      query.setFacet(true);
      for (String facet : facets._facets)
        query.addFacetField(facet);
      query.setFacetLimit(facets.limit);
      query.setFacetMinCount(facets.mincount);
      query.setFacetMissing(facets.missing);
      query.setFacetPrefix(facets.prefix);
      query.setFacetSort(facets.sort);
      if (facets.prefixes != null) {
        for (String facet : facets.prefixes.keySet())
          query.setFacetPrefix(facet, facets.prefixes.get(facet));
      }
    }

    LOGGER.info("query {}", query.toQueryString());
    return this._solr.query(query);
  }

  /**
   * @param query          the {@link SolrQuery}
   * @param groupingField  the name of the field to group with
   * @param groupLimit     the number of results per group
   *
   * @return the list of groups loaded
   */
  public Map<String, SolrDocumentList> group(SolrQuery query, String groupingField, int groupLimit) {
    // build consumer
    final Map<String, SolrDocumentList> results = new LinkedHashMap<String, SolrDocumentList>();
    Consumer<GroupCommand> consumer = new Consumer<GroupCommand>() {
      @Override
      public void accept(GroupCommand comm) {
        if (groupingField != null && !groupingField.equals(comm.getName())) return;
        List<Group> groups = comm.getValues();
        if (groups != null) {
          for (Group group : groups) {
            results.put(group.getGroupValue(), group.getResult());
          }
        }
      }
    };
    // run query
    group(query, consumer, groupingField, groupLimit);
    // return results
    return results;
  }

  /**
   * @param query          the {@link SolrQuery}
   * @param consumers      how to handle the results (key is the group value)
   * @param groupingField  the name of the field to group with
   * @param groupLimit     the number of results per group
   */
  public void group(SolrQuery query, Map<String, Consumer<SolrDocument>> consumers, String groupingField, int groupLimit) {
    // build consumer
    Consumer<GroupCommand> consumer = new Consumer<GroupCommand>() {
      @Override
      public void accept(GroupCommand comm) {
        if (groupingField != null && !groupingField.equals(comm.getName())) return;
        List<Group> groups = comm.getValues();
        if (groups != null) {
          for (Group group : groups) {
            Consumer<SolrDocument> consumer = consumers.get(group.getGroupValue());
            if (consumer != null)
              group.getResult().forEach(consumer);
          }
        }
      }
    };
    // run query
    group(query, consumer, groupingField, groupLimit);
  }

  /**
   * @param query          the {@link SolrQuery}
   * @param consumer       how to handle the results
   * @param groupingField  the name of the field to group with
   * @param groupLimit     the number of results per group
   */
  public void group(SolrQuery query, Consumer<GroupCommand> consumer, String groupingField, int groupLimit) {
    if (query == null) throw new NullPointerException("Search query is null");
    if (groupingField == null) throw new NullPointerException("Grouping field is null");

    LOGGER.info("query {}", query.toQueryString());

    query.setRows(this._row);
    query.setStart(this._start);

    // grouping
    query.setParam("group", true);
    //the number of groups that matches the the query
    query.setParam("group.ngroups", true);
    // the grouping field
    query.setParam("group.field", groupingField);
    // number of group limit
    query.setParam("group.limit", String.valueOf(groupLimit));

    QueryResponse response = this._solr.query(query);
    if (response != null) {
      LOGGER.info("response size {}", response.getResponse().size());
      GroupResponse groupResponse = response.getGroupResponse();
      groupResponse.getValues().forEach(consumer);
    }
  }

  /**
   * @param query      the query in the main index
   * @param from       the field to join from (in the joined index)
   * @param to         the field to join to (in the main index)
   * @param fromIndex  the index to join (if null, join is in main index
   * @param joinQuery
   * @param consumer
   */
  public void join(SolrQuery query, String from, String to, String fromIndex, SolrQuery joinQuery, Consumer<SolrDocument> consumer) {
    if (query == null) throw new NullPointerException("Search query is null");
    if (from == null) throw new NullPointerException("from is null");
    if (to == null) throw new NullPointerException("to is null");
    if (fromIndex == null) throw new NullPointerException("fromIndex is null");

    query.setRows(this._row);
    query.setStart(this._start);

    // add the filter query
    String otherIndex = fromIndex == null ? "" : " fromIndex=" + fromIndex;
    query.addFilterQuery("{!join from=" + from + " to=" + to + otherIndex + "}" + joinQuery.get("q"));

    LOGGER.info("query {}", query);

    QueryResponse response = this._solr.query(query);

    if (response != null) {
      LOGGER.info("response size {}", response.getResponse().size());
      // standard query
      SolrDocumentList list = response.getResults();
      if (list != null) {
        list.forEach(consumer);
      }
    }
  }


}


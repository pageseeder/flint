package org.pageseeder.flint.solr.index;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient.RemoteSolrException;
import org.apache.solr.client.solrj.request.LukeRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.LukeResponse;
import org.apache.solr.client.solrj.response.LukeResponse.FieldInfo;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.params.SolrParams;
import org.pageseeder.flint.Index;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexIO;
import org.pageseeder.flint.content.DeleteRule;
import org.pageseeder.flint.indexing.FlintDocument;
import org.pageseeder.flint.solr.SolrCoreManager;
import org.pageseeder.flint.solr.SolrFlintConfig;
import org.pageseeder.flint.solr.SolrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrIndexIO implements IndexIO {

  private final static Logger LOGGER = LoggerFactory.getLogger(SolrIndexIO.class);

  private final HttpSolrClient _client;

  private SolrIndexStatus currentStatus;

  public SolrIndexIO(Index index) {
    // build client to connect to solr
    String url = SolrFlintConfig.getInstance().getSolrServerURL();
    if (url.charAt(url.length() - 1) != '/') url += '/';
    this._client = new HttpSolrClient(url + index.getIndexID());
    this._client.setAllowCompression(true);
    // make sure it exists on solr server
    new SolrCoreManager().createCore(index.getIndexID(), index.getCatalog());
  }

  @Override
  public long getLastTimeUsed() {
    SolrIndexStatus info = getCurrentStatus();
    return info == null || info.getLastModified() == null ? -1 : info.getLastModified().getTime();
  }

  @Override
  public void stop() throws IndexException {
    // nothing to do here?
    try {
      this._client.close();
    } catch (IOException ex) {
      LOGGER.error("Failed to close index!", ex);
    }
  }

  @Override
  public void maybeRefresh() {
    // reset
    this.currentStatus = null;
  }

  @Override
  public void maybeCommit() {
    try {
      // commit
      this._client.commit();
      // reset
      this.currentStatus = null;
    } catch (SolrServerException | IOException ex) {
      LOGGER.error("Failed to commit index!", ex);
    }
  }

  @Override
  public boolean clearIndex() throws IndexException {
    UpdateResponse resp;
    try {
      resp = this._client.deleteByQuery("*:*");
      resp = this._client.commit();
      // reset
      this.currentStatus = null;
    } catch (SolrServerException | IOException ex) {
      throw new IndexException("Failed to clear index", ex);
    }
    return resp.getStatus() == 0;
  }

  @Override
  public boolean deleteDocuments(DeleteRule rule) throws IndexException {
    if (rule instanceof SolrDeleteRule) {
      SolrDeleteRule drule = (SolrDeleteRule) rule;
      try {
        UpdateResponse resp;
        if (drule.deleteByID()) {
          resp = this._client.deleteById(drule.getDeleteID());
        } else {
          resp = this._client.deleteByQuery(drule.getDeleteQuery());
        }
        return resp.getStatus() == 0;
      } catch (SolrServerException | IOException ex) {
        throw new IndexException("Failed to delete document(s)", ex);
      }
    }
    return false;
  }

  @Override
  public boolean updateDocuments(DeleteRule rule, List<FlintDocument> documents) throws IndexException {
    // delete first
    if (rule != null) deleteDocuments(rule);
    // add then
    try {
      UpdateResponse resp = this._client.add(SolrUtils.toDocuments(documents));
      return resp.getStatus() == 0;
    } catch (SolrServerException | IOException ex) {
      throw new IndexException("Failed to add document(s)", ex);
    }
  }

  public SolrIndexStatus getCurrentStatus() {
    if (this.currentStatus == null) {
      LukeRequest request = new LukeRequest();
      request.setShowSchema(true);
      try {
        LukeResponse response = request.process(this._client);
        if (response != null) {
          this.currentStatus = new SolrIndexStatus();
          this.currentStatus
              .numDocs(response.getNumDocs() != null ? response.getNumDocs() : 0)
              .elapsedTime(response.getElapsedTime())
              .numTerms(response.getNumTerms() != null ? response.getNumTerms() : 0)
              .maxDoc(response.getMaxDoc() != null ? response.getMaxDoc() : 0)
              .version(response.getIndexInfo().get("version").toString())
              .lastModified((Date) response.getIndexInfo().get("lastModified"))
              .current((Boolean) response.getIndexInfo().get("current"))
              .numSegments((int) response.getIndexInfo().get("segmentCount"))
              .size((Long) response.getIndexInfo().get("segmentsFileSizeInBytes"));
          for (Entry<String, FieldInfo> field : response.getFieldInfo().entrySet()) {
            this.currentStatus.field(field.getKey(), field.getValue().getDocs());
          }
        }

      } catch (RemoteSolrException | SolrServerException | IOException ex) {
        LOGGER.error("Cannot get solr info ", ex);
      }
    }
    return this.currentStatus;
  }

  public QueryResponse query(SolrQuery query) {
    try {
      return this._client.query(query);
    } catch (SolrServerException | IOException ex) {
      LOGGER.error("Failed to run solr query {}", query, ex);
    }
    return null;
  }

  public QueryResponse query(SolrParams params) {
    try {
      return this._client.query(params);
    } catch (SolrServerException | IOException ex) {
      LOGGER.error("Failed to run query with params {}", params, ex);
    }
    return null;
  }

  public QueryResponse request(QueryRequest request) {
    try {
      return request.process(this._client);
    } catch (SolrServerException | IOException ex) {
      LOGGER.error("Failed to run query request {}", request, ex);
    }
    return null;
  }

  public SolrResponse process(SolrRequest<? extends SolrResponse> request) {
    try {
      return request.process(this._client);
    } catch (SolrServerException | IOException ex) {
      LOGGER.error("Failed to process request {}", request, ex);
    }
    return null;
  }

  public SolrClient getClient() {
    return this._client;
  }

}

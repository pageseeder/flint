/*
 * Copyright (c) 1999-2016 Allette systems pty. ltd.
 */
package org.pageseeder.flint.solr;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient.RemoteSolrException;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.params.CoreAdminParams.CoreAdminAction;
import org.apache.solr.common.util.NamedList;
import org.pageseeder.flint.solr.index.SolrIndexStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A server level of manager to deal with the Core's management.
 *
 *
 * @author Ciber Cai
 * @since 16 August 2016
 */
public class SolrCoreManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(SolrCoreManager.class);

  private static final int SUCCESS_STATUS = 0;

  private final SolrClient _solr;

  public SolrCoreManager() {
    this._solr = new HttpSolrClient.Builder(SolrFlintConfig.getInstance().getServerURL()).build();
  }

  @SuppressWarnings("unchecked")
  public Map<String, SolrIndexStatus> listCores() throws SolrFlintException {
    CoreAdminResponse response = null;
    try {
      CoreAdminRequest req = new CoreAdminRequest();
      req.setAction(CoreAdminAction.STATUS);
      response = req.process(this._solr);
    } catch (RemoteSolrException | SolrServerException | IOException ex) {
      if (ex.getCause() != null && ex.getCause() instanceof ConnectException) throw new SolrFlintException(true);
      throw new SolrFlintException("Failed to list Solr cores", ex);
    }
    if (response == null) return Collections.emptyMap();
    Map<String, SolrIndexStatus> indexes = new HashMap<String, SolrIndexStatus>();
    response.getCoreStatus().forEach(new Consumer<Entry<String, NamedList<Object>>>() {
      @Override
      public void accept(Entry<String, NamedList<Object>> entry) {
        indexes.put(entry.getKey(), SolrIndexStatus.fromNamedList((NamedList<Object>) entry.getValue().get("index")));
      }
    });
    return indexes;
  }


  /**
   * According to the document https://cwiki.apache.org/confluence/display/solr/CoreAdmin+API,
   * The SOLR core creation is not a proper API.
   * To create a core, you can either use the command 'solr create -c [name]'
   * or use the configSet (current implementation).
   *
   * @param name the name of core.
   * @return the status of creation
   */
  public boolean createCore(String name, String config) throws SolrFlintException {
    CoreAdminResponse response = null;

    // check core exist
    if (!exists(name)) {
      // make sure config set exists
      LOGGER.info("Solr core {} - creating with collection/config {}", name, config);
      try {
        CoreAdminRequest.Create create = new CoreAdminRequest.Create();
        create.setCollection(config);
        create.setCollectionConfigName(config);
        create.setCoreName(name);
        response = create.process(this._solr);
      } catch (RemoteSolrException | SolrServerException | IOException ex) {
        LOGGER.error("Cannot create core {}", name, ex);
        if (ex.getCause() != null && ex.getCause() instanceof ConnectException) throw new SolrFlintException(true);
        throw new SolrFlintException("Failed to create index "+name+": "+ex.getMessage(), ex);
      }
    } else {
//      LOGGER.info("Solr core {} already exists", name);
      LOGGER.info("Solr core {} already exists - reloading it", name);
      try {
        response = CoreAdminRequest.reloadCore(name, this._solr);
      } catch (RemoteSolrException | SolrServerException | IOException ex) {
        if (ex.getCause() != null && ex.getCause() instanceof ConnectException) throw new SolrFlintException(true);
        throw new SolrFlintException("Failed to reload core " + name+": "+ex.getMessage(), ex);
      }
    }
    return response != null && response.getStatus() == SUCCESS_STATUS;
  }

  /**
   * @param name the name of the core
   * @return  the status of deletion
   */
  public boolean deleteCore(String name) throws SolrFlintException {
    LOGGER.info("Solr core {} - deleting", name);

    CoreAdminResponse response = null;
    if (exists(name)) {
      try {
        response = CoreAdminRequest.Create.unloadCore(name, this._solr);
      } catch (RemoteSolrException | SolrServerException | IOException ex) {
        if (ex.getCause() != null && ex.getCause() instanceof ConnectException) throw new SolrFlintException(true);
        throw new SolrFlintException("Failed to unload core " + name+": "+ex.getMessage(), ex);
      }
    } else {
      return true;
    }
    return response != null && response.getStatus() == 0;
  }

  /**
   * @return the status whether the server is alive.
   */
  public boolean isAlive() {
    try {
      SolrPingResponse resp = this._solr.ping();
      return 0 == resp.getStatus();
    } catch (RemoteSolrException | SolrServerException | IOException ex) {
      if (ex.getCause() != null && ex.getCause() instanceof ConnectException) return false;
      LOGGER.error("Failed to ping Solr server: "+ex.getMessage(), ex);
    }
    return false;
  }

  /**
   * @param name the name of core
   * @return the status whether it exists
   * @throws SolrFlintException 
   */
  public boolean exists(String name) throws SolrFlintException {
    try {
      CoreAdminResponse response = CoreAdminRequest.getStatus(name, this._solr);
      return response.getCoreStatus(name).get("instanceDir") != null;
    } catch (RemoteSolrException ex) {
      if (ex.code() == 404) return false;
      throw new SolrFlintException("Failed to check core " + name+": "+ex.getMessage(), ex);
    } catch (SolrServerException ex) {
      if (ex.getCause() != null && ex.getCause() instanceof ConnectException) throw new SolrFlintException(true);
      throw new SolrFlintException("Failed to check core " + name+": "+ex.getMessage(), ex);
    } catch (IOException ex) {
      throw new SolrFlintException("Failed to check core " + name+": "+ex.getMessage(), ex);
    }
  }

//  public static void main(String[] args) {
//    try {
////      CollectionAdminRequest.Create c = CollectionAdminRequest.createCollection("local-case", "local-case", 1, 1);
////      System.out.println(c.process(new HttpSolrClient.Builder("http://localhost:8983/solr").build()).getResponse());
//      
//      CoreAdminRequest.Create create = new CoreAdminRequest.Create();
//      create.setCollection("dev-case");
//      create.setCollectionConfigName("dev-case");
//      create.setCoreName("case");
//      System.out.println(create.process(new HttpSolrClient.Builder("http://localhost:8983/solr").build()).getResponse());
//    } catch (RemoteSolrException | SolrServerException | IOException ex) {
//      ex.printStackTrace();
//    }
//  }
}

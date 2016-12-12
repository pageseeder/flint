/*
 * Copyright (c) 1999-2016 Allette systems pty. ltd.
 */
package org.pageseeder.flint.solr;

import java.io.IOException;
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
import org.pageseeder.flint.IndexException;
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

  private static final String DEFAULT_CONFIG_SET = "basic_configs";

  private final SolrClient _solr;

  public SolrCoreManager() {
    this._solr = new HttpSolrClient(SolrFlintConfig.getInstance().getSolrServerURL());
  }

  public Map<String, SolrIndexStatus> listCores() {
    CoreAdminResponse response = null;
    try {
      CoreAdminRequest req = new CoreAdminRequest();
      req.setAction(CoreAdminAction.STATUS);
      response = req.process(this._solr);
    } catch (RemoteSolrException | SolrServerException | IOException ex) {
      LOGGER.error("Cannot list cores {}", ex);
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
  public boolean createCore(String name, String catalog) throws IndexException {
    CoreAdminResponse response = null;

    // check core exist
    if (!exists(name)) {
      // make sure config set exists
      boolean useConfig = true;//useConfigSet(catalog);
      if (!useConfig) {
        LOGGER.warn("Failed to setup config {}, using default config for index {}!", catalog, name);
      }
      LOGGER.info("Solr core {} - creating", name);
      try {
        CoreAdminRequest.Create create = new CoreAdminRequest.Create();
        create.setConfigSet(useConfig ? catalog : DEFAULT_CONFIG_SET);
        create.setCoreName(name);
        response = create.process(this._solr);
      } catch (RemoteSolrException | SolrServerException | IOException ex) {
        LOGGER.error("Cannot create core {}", name, ex);
        throw new IndexException("Failed to create core "+name+": "+ex.getMessage(), ex);
      }
    } else {
      LOGGER.info("Solr core {} already exists - just reload it", name);
      try {
        response = CoreAdminRequest.reloadCore(name, this._solr);
      } catch (RemoteSolrException | SolrServerException e) {
        LOGGER.error("SolrServerException reloading " + name + " core", e);
      } catch (IOException e) {
        LOGGER.error("IOException reloading " + name + " core", e);
      }
    }
    return response != null && response.getStatus() == SUCCESS_STATUS;
  }

  /**
   * @param name the name of the core
   * @return  the status of deletion
   */
  public boolean deleteCore(String name) {
    LOGGER.info("Solr core {} - deleting", name);

    CoreAdminResponse response = null;
    if (exists(name)) {
      try {
        response = CoreAdminRequest.Create.unloadCore(name, this._solr);
      } catch (RemoteSolrException | SolrServerException | IOException ex) {
        LOGGER.error("Cannot unload core {}", name, ex);
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
      LOGGER.warn("Cannot pint solr server {} ", this._solr.toString(), ex);
    }
    return false;
  }

  /**
   * @param name the name of core
   * @return the status whether it exists
   */
  public boolean exists(String name) {
    try {
      CoreAdminResponse response = CoreAdminRequest.getStatus(name, this._solr);
      return response.getCoreStatus(name).get("instanceDir") != null;
    } catch (RemoteSolrException ex) {
      if (ex.code() == 404) return false;
      LOGGER.info("SolrServerException getting " + name + " core status", ex);
      return false;
    } catch (SolrServerException ex) {
      LOGGER.info("SolrServerException getting " + name + " core status", ex);
      return false;
    } catch (IOException ex) {
      LOGGER.info("IOException getting " + name + " core status", ex);
      return false;
    }
  }
//
//  // ---------------------------------------------------------
//  // config set managment
//  // ---------------------------------------------------------
//
//  private final static List<String> configSets = new ArrayList<>();
//  private boolean useConfigSet(String name) {
//    // list them
//    if (configSets.isEmpty()) {
//      try {
//        ConfigSetAdminResponse.List list = new ConfigSetAdminRequest.List().process(this._solr);
//        if (list != null && list.getStatus() == SUCCESS_STATUS) {
//          configSets.addAll(list.getConfigSets());
//        } else {
//          LOGGER.error("Failed to list config sets: {}", list.getErrorMessages());
//        }
//      } catch (RemoteSolrException | SolrServerException | IOException ex) {
//        LOGGER.error("Failed to list config sets", ex);
//      }
//    }
//    // create it?
//    if (!configSets.contains(name)) {
//      // check config sets
//      ConfigSetAdminRequest.Create create = new ConfigSetAdminRequest.Create();
//      create.setConfigSetName(name);
//      create.setBaseConfigSetName(DEFAULT_CONFIG_SET);
//      try {
//        ConfigSetAdminResponse resp = create.process(this._solr);
//        if (resp != null && resp.getStatus() == SUCCESS_STATUS) configSets.add(name);
//      } catch (RemoteSolrException | SolrServerException | IOException ex) {
//        LOGGER.error("Failed to create config set {}", name, ex);
//      }
//    }
//    return configSets.contains(name);
//  }
}

package org.pageseeder.flint.solr;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrFlintConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(SolrFlintConfig.class);

  private static SolrFlintConfig INSTANCE;

  private String serverURL = "http://localhost:8983/solr/";

  private File templatesFolder = null;

//  private final IndexManager _manager;

  public static void setup(File templates, String url) {
    if (INSTANCE == null) {
      INSTANCE = new SolrFlintConfig();
      INSTANCE.templatesFolder = templates;
      INSTANCE.serverURL = url == null ? "http://localhost:8983/solr/" : url;
      LOGGER.info("Solr Flint has been setup with Solr server at {} and templates under {}", INSTANCE.serverURL, INSTANCE.templatesFolder);
    } else {
      LOGGER.warn("Trying to setup Solr Flint twice, ignoring!");
    }
  }

  public static SolrFlintConfig getInstance() {
    if (INSTANCE == null) {
      LOGGER.warn("Solr Flint has not been setup, using default properties!");
      INSTANCE = new SolrFlintConfig();
    }
    return INSTANCE;
  }

  private SolrFlintConfig() {
  }
  
  public String getServerURL() {
    return this.serverURL;
  }

  public File getTemplatesFolder() {
    return this.templatesFolder;
  }

}

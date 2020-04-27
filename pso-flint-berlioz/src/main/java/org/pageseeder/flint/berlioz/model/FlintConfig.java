/*
 * Decompiled with CFR 0_110.
 * 
 * Could not load the following classes:
 *  org.apache.lucene.analysis.Analyzer
 *  org.apache.lucene.analysis.standard.StandardAnalyzer
 *  org.pageseeder.berlioz.GlobalSettings
 *  org.pageseeder.flint.IndexManager
 *  org.pageseeder.flint.api.ContentFetcher
 *  org.pageseeder.flint.api.ContentTranslator
 *  org.pageseeder.flint.api.ContentTranslatorFactory
 *  org.pageseeder.flint.api.IndexListener
 *  org.pageseeder.flint.content.SourceForwarder
 *  org.pageseeder.flint.local.LocalFileContentFetcher
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package org.pageseeder.flint.berlioz.model;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.berlioz.helper.FolderWatcher;
import org.pageseeder.flint.berlioz.helper.QuietListener;
import org.pageseeder.flint.berlioz.model.IndexDefinition.InvalidIndexDefinitionException;
import org.pageseeder.flint.catalog.Catalogs;
import org.pageseeder.flint.content.ContentTranslatorFactory;
import org.pageseeder.flint.indexing.IndexBatch;
import org.pageseeder.flint.local.LocalFileContentFetcher;
import org.pageseeder.flint.solr.SolrCollectionManager;
import org.pageseeder.flint.solr.SolrFlintConfig;
import org.pageseeder.flint.solr.SolrFlintException;
import org.pageseeder.flint.templates.TemplatesCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.TransformerException;
import java.io.File;
import java.util.*;

/**
 * Flint config in berlioz config: <flint> <watcher [watch="true|false"]
 * [root="/psml/content"] [max-folders="1000"] /> <threads [number="10"]
 * [priority="5"] /> <index types="default,books,schools,products">
 * <default name="default" path="/psml/content/" />
 * <books name="book-{name}" path="/psml/content/book/{name}" template=
 * "book.xsl"/>
 * <schools name="school" path="/psml/content/schools" template="school.xsl"/>
 * <products name="product" path="/psml/content/products"
 * [template="products.xsl"]/> </index> </flint>
 * 
 * @author jbreure
 *
 */
public class FlintConfig {
  private static final Logger LOGGER = LoggerFactory.getLogger(FlintConfig.class);
  private static final String DEFAULT_INDEX_NAME = "default";
  private static final String DEFAULT_INDEX_LOCATION = "index";
  private static final String DEFAULT_CATALOG_LOCATION = "catalogs";
  private static final String DEFAULT_CONTENT_LOCATION = "/psml/content";
  private static final String DEFAULT_ITEMPLATES_LOCATION = "ixml";
  private static final int DEFAULT_MAX_WATCH_FOLDERS = 100000;
  private static final int DEFAULT_WATCHER_DELAY_IN_SECONDS = 5;
  private static volatile AnalyzerFactory analyzerFactory = null;
  private final File _directory;
  private final File _ixml;
  private final boolean _useSolr;
  private static FlintConfig SINGLETON = null;
  private final QuietListener listener;
  private final IndexManager manager;
  private final Map<String, IndexDefinition> indexConfigs = new HashMap<>();
  private final Map<String, IndexMaster> indexes = new HashMap<>();
  private final Map<String, SolrIndexMaster> solrIndexes = new HashMap<>();
  private final FolderWatcher watcher;
  private final Collection<String> _extensions = new ArrayList<>();

  public static void setupFlintConfig(File index, File ixml) {
    SINGLETON = new FlintConfig(index, ixml);
  }

  public static synchronized FlintConfig get() {
    if (SINGLETON == null)
      SINGLETON = FlintConfig.buildDefaultConfig();
    return SINGLETON;
  }

  public IndexManager getManager() {
    return this.manager;
  }

  public boolean useSolr() {
    return this._useSolr;
  }

  /**
   * Build an object used to query multiple indexes at the same time.
   * 
   * @param names
   *          list of index names
   * 
   * @return the multi indexes master object
   */
  public MultipleIndexesMaster getMultiMaster(Collection<String> names) {
    List<IndexMaster> masters = new ArrayList<>(names.size());
    for (String name : names) {
      IndexMaster master = getMaster(name);
      if (master != null)
        masters.add(master);
    }
    return new MultipleIndexesMaster(masters);
  }

  /**
   * @return the index master with the name "default"
   */
  public IndexMaster getMaster() {
    return getMaster(DEFAULT_INDEX_NAME);
  }

  /**
   * The index master is not created if not in memory.
   * 
   * @param name the master name
   * 
   * @return the index master with the name provided
   */
  public IndexMaster getMaster(String name) {
    return getMaster(name, true);
  }

  /**
   * @param name              the master name
   * @param createIfNotFound  whether or not to create the index if not existing
   * 
   * @return the index master with the name provided
   */
  public IndexMaster getMaster(String name, boolean createIfNotFound) {
    String key = name == null ? DEFAULT_INDEX_NAME : name;
    // make sure only one gets created
    synchronized (this.indexes) {
      if (!this.indexes.containsKey(key) && createIfNotFound) {
        IndexDefinition def = getIndexDefinitionFromIndexName(key);
        if (def == null) {
          // no config found
          LOGGER.error("Failed to create index {}, no matching index definition found in configuration", key);
        } else {
          IndexMaster master = createLuceneMaster(key, def);
          if (master != null)
            this.indexes.put(key, master);
        }
      }
      return this.indexes.get(key);
    }
  }

  /**
   * @return the index master with the default name
   */
  public SolrIndexMaster getSolrMaster() {
    return getSolrMaster(DEFAULT_INDEX_NAME);
  }

  /**
   * @param name the master name
   * 
   * @return the index master with the name provided
   */
  public SolrIndexMaster getSolrMaster(String name) {
    try {
      return getSolrMaster(name, false);
    } catch (SolrFlintException ex) {
      // should not happen if we don't create it
      LOGGER.error("Impossible!", ex);
      return null;
    }
  }

  /**
   * @param name              the master name
   * @param createIfNotFound  whether or not to create the index if not existing
   * 
   * @return the index master with the name provided
   */
  public SolrIndexMaster getSolrMaster(String name, boolean createIfNotFound) throws SolrFlintException {
    // make sure only one gets created
    synchronized (this.solrIndexes) {
      if (!this.solrIndexes.containsKey(name) && createIfNotFound) {
        IndexDefinition def = getIndexDefinitionFromIndexName(name);
        if (def == null) {
          // any indexes that have been defined on the server but not in the configuration
          // will not have an IndexDefinition.
          LOGGER.info("No index configuration found for the Solr collection {}", name);
        } else {
          SolrIndexMaster master = createSolrMaster(name, def);
          if (master != null)
            this.solrIndexes.put(name, master);
        }
      }
      return this.solrIndexes.get(name);
    }
  }

  /**
   * Close and removes index from list. Also deletes index files from index root
   * folder.
   * 
   * @param name
   *          the index name
   * 
   * @return true if completely removed
   */
  public boolean deleteMaster(String name) {
    String key = name == null ? DEFAULT_INDEX_NAME : name;
    if (this.indexes.containsKey(key)) {
      // close index
      IndexMaster master = this.indexes.remove(key);
      master.getIndex().close();
      // remove files
      File root = new File(this._directory, key);
      if (root.exists() && root.isDirectory()) {
        File[] ff = root.listFiles();
        if (ff != null) for (File f : ff) f.delete();
        return root.delete();
      }
      return true; // ???
    } else if (this.solrIndexes.containsKey(key)) {
      // close index
      SolrIndexMaster master = this.solrIndexes.get(key);
      master.getIndex().close();
      // delete collection from solr
      try {
        new SolrCollectionManager().deleteCollection(key);
      } catch (SolrFlintException ex) {
        LOGGER.error("Failed to delete collection {} from Solr server", key, ex);
        return false;
      }
      // remove from local list
      this.solrIndexes.remove(key);
      return true;
    }
    return false;
  }

  private static FlintConfig buildDefaultConfig() {
    // set catalog location
    File catalogs = new File(GlobalSettings.getAppData(), DEFAULT_CATALOG_LOCATION);
    Catalogs.setRoot(catalogs);
    // create config
    File index = new File(GlobalSettings.getAppData(), DEFAULT_INDEX_LOCATION);
    if (!index.exists()) index.mkdirs();
    File ixml = new File(GlobalSettings.getAppData(), DEFAULT_ITEMPLATES_LOCATION);
    if (!ixml.exists()) {
      File wixml = new File(GlobalSettings.getWebInf(), DEFAULT_ITEMPLATES_LOCATION);
      if (wixml.exists()) ixml = wixml;
    }
    return new FlintConfig(index, ixml);
  }

  private FlintConfig(File directory, File ixml) {
    this._directory = directory;
    this._ixml = ixml;
    // solr?
    String url = GlobalSettings.get("flint.solr.url", GlobalSettings.get("flint.solr")); // legacy
    String zkh = GlobalSettings.get("flint.solr.zookeeper");
    this._useSolr = url != null;
    if (this._useSolr) {
      Collection<String> zkhosts = new ArrayList<>();
      if (zkh != null) {
        for (String z : zkh.split(",")) {
          if (z != null && !z.trim().isEmpty())
            zkhosts.add(z.trim());
        }
      }
      SolrFlintConfig.setup(ixml, url, zkhosts);
    }
    // manager
    this._extensions.addAll(Arrays.asList(GlobalSettings.get("flint.index.extensions", "psml").split(",")));
    if (!this._extensions.contains("psml")) LOGGER.warn("PSML should be in the list of supported extensions");
    int nbThreads = GlobalSettings.get("flint.threads.number", 10);
    int threadPriority = GlobalSettings.get("flint.threads.priority", 5);
    this.listener = new QuietListener(LOGGER);
    this.manager = new IndexManager(new LocalFileContentFetcher(), this.listener, nbThreads, false);
    this.manager.setThreadPriority(threadPriority);
    createTranslatorFactories();
    // watch is on?
    boolean watch = GlobalSettings.get("flint.watcher.watch", true);
    if (watch) {
      File root = new File(GlobalSettings.getAppData(),
          GlobalSettings.get("flint.watcher.root", DEFAULT_CONTENT_LOCATION));
      int maxFolders = GlobalSettings.get("flint.watcher.max-folders", DEFAULT_MAX_WATCH_FOLDERS);
      int indexingDelay = GlobalSettings.get("flint.watcher.delay", DEFAULT_WATCHER_DELAY_IN_SECONDS);
      String excludes = GlobalSettings.get("flint.watcher.excludes");
      this.watcher = new FolderWatcher(root, maxFolders, indexingDelay);
      if (excludes != null) this.watcher.setIgnore(Arrays.asList(excludes.split(",")));
      this.watcher.start();
    } else {
      this.watcher = null;
    }
    // load index definitions
    String types = GlobalSettings.get("flint.index.types", "default");
    types_loop: for (String type : types.split(",")) {
      String indexName = GlobalSettings.get("flint.index." + type + ".name", DEFAULT_INDEX_NAME);
      String path = GlobalSettings.get("flint.index." + type + ".path", DEFAULT_CONTENT_LOCATION);
      String excludes = GlobalSettings.get("flint.index." + type + ".excludes");
      File template = new File(ixml, GlobalSettings.get("flint.index." + type + ".template", indexName + ".xsl"));
      IndexDefinition def;
      try {
        def = new IndexDefinition(type, indexName, path,
            excludes == null ? null : Arrays.asList(excludes.split(",")),
            template, this._extensions);
        LOGGER.debug("New index config for {} with index name {}, path {} and template {}", type, indexName, path,
            template.getAbsolutePath());
      } catch (InvalidIndexDefinitionException ex) {
        LOGGER.warn("Ignoring invalid index definition {}: {}", type, ex.getMessage());
        continue;
      }
      // check for clashes
      for (IndexDefinition existing : this.indexConfigs.values()) {
        if (def.indexNameClash(existing)) {
          LOGGER.warn("Ignoring invalid index definition {} as it clashes with definition {}", type,
              existing.getName());
          continue types_loop;
        }
      }
      String regexInclude = GlobalSettings.get("flint.index." + type + ".files.includes");
      String regexExclude = GlobalSettings.get("flint.index." + type + ".files.excludes");
      // set filters
      def.setIndexingFilesRegex(regexInclude, regexExclude);
      // solr attributes
      def.setSolrAttribute("num-shards",   GlobalSettings.get("flint.index." + type + ".solr.num-shards"));
      def.setSolrAttribute("num-replicas", GlobalSettings.get("flint.index." + type + ".solr.num-replicas"));
      def.setSolrAttribute("shards",       GlobalSettings.get("flint.index." + type + ".solr.shards"));
      def.setSolrAttribute("max-shards",   GlobalSettings.get("flint.index." + type + ".solr.max-shards"));
      def.setSolrAttribute("router",       GlobalSettings.get("flint.index." + type + ".solr.router"));
      def.setSolrAttribute("router-field", GlobalSettings.get("flint.index." + type + ".solr.router-field"));
      // autosuggests
      loadAutoSuggests(def);
      this.indexConfigs.put(type, def);
    }
    // load solr indexes at startup
    if (this._useSolr) {
      try {
        listSolrIndexes();
      } catch (SolrFlintException ex) {
        LOGGER.warn("Failed to load solr indexes at startup!", ex);
      }
    }
  }

  /**
   * Create factories that supports the extensions in this config.
   */
  private void createTranslatorFactories() {
    if (this._extensions.contains("psml") || this._extensions.contains("xml"))
      this.manager.registerTranslatorFactory(new PSMLContentTranslatorFactory());
    String tikaRequired = this._extensions.stream().filter(s -> !s.equals("psml") && !s.equals("xml")).findFirst().orElse(null);
    if (tikaRequired != null) {
      try {
        Class tikaFactory = FlintConfig.class.getClassLoader().loadClass("org.pageseeder.flint.berlioz.tika.TikaTranslatorFactory");
        if (tikaFactory != null)
          this.manager.registerTranslatorFactory((ContentTranslatorFactory) tikaFactory.newInstance());
      } catch (ClassNotFoundException ex) {
        LOGGER.warn("Flint TIKA Translator Factory class not available, make sure library pso-flint-berlioz-tika is on the classpath to support extension {}", tikaRequired);
      } catch (IllegalAccessException | InstantiationException ex) {
        LOGGER.error("Failed to create TIKA Translator Factory", ex);
      }
    }
  }

  /**
   * Stop the watcher if there is one and the manager.
   */
  public final void stop() {
    // save catalogs
    Catalogs.saveAll();
    // stop watcher if there is one
    if (this.watcher != null)
      this.watcher.stop();
    // shutdown all indexes
    for (IndexMaster index : this.indexes.values()) {
      index.close();
    }
    // stop everything
    this.manager.stop();
  }

  public final File getRootDirectory() {
    return this._directory;
  }

  public final File getTemplatesDirectory() {
    return this._ixml;
  }

  @Deprecated
  public Collection<IndexMaster> listIndexes() {
    return listLuceneIndexes();
  }

  public Collection<IndexMaster> listLuceneIndexes() {
    if (this._useSolr) return Collections.emptyList();
    if (this.indexes.isEmpty()) {
      // load indexes
      for (File folder : this._directory.listFiles()) {
        if (folder.isDirectory()) {
          if (!folder.getName().endsWith("_autosuggest"))
            getMaster(folder.getName());
        } else {
          // delete all files from old index
          folder.delete();
        }
      }
    }
    return this.indexes.values();
  }

  public Collection<SolrIndexMaster> listSolrIndexes() throws SolrFlintException {
    return listSolrIndexes(this.solrIndexes.isEmpty());
  }

  public Collection<SolrIndexMaster> listSolrIndexes(boolean refreshFromServer) throws SolrFlintException {
    if (!this._useSolr) return Collections.emptyList();
    if (refreshFromServer) {
      // clear existing
      this.solrIndexes.clear();
      SolrCollectionManager cores = new SolrCollectionManager();
      Collection<String> all = cores.listCollections();
      // load indexes
      for (String name : all) {
        try {
          getSolrMaster(name, true);
        } catch (SolrFlintException ex) {
          LOGGER.error("Failed to create index "+name, ex);
        }
      }
    }
    return this.solrIndexes.values();
  }

  public Collection<IndexDefinition> listDefinitions() {
    return new ArrayList<>(this.indexConfigs.values());
  }

  public static synchronized void setAnalyzerFactory(AnalyzerFactory factory) {
    analyzerFactory = factory;
  }

  public static synchronized Analyzer newAnalyzer() {
    if (analyzerFactory == null)
      return new StandardAnalyzer();
    return analyzerFactory.getAnalyzer();
  }

  public IndexDefinition getIndexDefinition(String defname) {
    return this.indexConfigs.get(defname);
  }

  public IndexDefinition getIndexDefinitionFromIndexName(String indexname) {
    if (indexname == null)
      return null;
    // find config
    for (IndexDefinition config : this.indexConfigs.values()) {
      // name matches?
      if (config.indexNameMatches(indexname))
        return config;
    }
    return null;
  }

  public void reloadTemplate(String defname) {
    // clear templates cache
    TemplatesCache.clear();
    // remove template from this definition
    IndexDefinition def = getIndexDefinition(defname);
    if (def != null) {
      // loop through index folders
      for (File folder : this._directory.listFiles()) {
        if (folder.isDirectory() && def.indexNameMatches(folder.getName())) {
          IndexMaster master = getMaster(folder.getName());
          try {
            if (master != null) master.reloadTemplate();
            def.setTemplateError(null); // reset error
          } catch (TransformerException ex) {
            def.setTemplateError(ex.getMessageAndLocation());
            return;
          }
        }
      }
    }
  }

  public Collection<IndexBatch> getPastBatches() {
    return this.listener.getBatches();
  }

  private IndexMaster createLuceneMaster(String name, IndexDefinition def) {
    // build content path
    File content = def.buildContentRoot(GlobalSettings.getAppData(), name);
    File index = new File(this._directory, name);
    try {
      IndexMaster master = IndexMaster.create(getManager(), name, content, index, this._extensions, def);
      def.setTemplateError(null); // reset error
      return master;
    } catch (TransformerException ex) {
      def.setTemplateError(ex.getMessageAndLocation());
      return null;
    }
  }

  private SolrIndexMaster createSolrMaster(String name, IndexDefinition def) throws SolrFlintException {
    // build content path
    File content = def.buildContentRoot(GlobalSettings.getAppData(), name);
    try {
      SolrIndexMaster master = SolrIndexMaster.create(getManager(), name, content, this._extensions, def);
      def.setTemplateError(null); // reset error
      return master;
    } catch (TransformerException ex) {
      def.setTemplateError(ex.getMessageAndLocation());
      return null;
    }
  }

  private void loadAutoSuggests(IndexDefinition def) {
    String propPrefix = "flint.index." + def.getName() + '.';
    // autosuggests
    String autosuggests = GlobalSettings.get(propPrefix + "autosuggests");
    if (autosuggests != null) {
      for (String autosuggest : autosuggests.split(",")) {
        String fields = GlobalSettings.get(propPrefix + autosuggest + ".fields");
        String terms = GlobalSettings.get(propPrefix + autosuggest + ".terms", "false");
        String rfields = GlobalSettings.get(propPrefix + autosuggest + ".result-fields");
        String criteriafields = GlobalSettings.get(propPrefix + autosuggest + ".criteria-fields");
        String weight = GlobalSettings.get(propPrefix + autosuggest + ".weight", "");
        String suggesters = GlobalSettings.get(propPrefix + autosuggest + ".suggesters");
        if (fields != null) {
          if (this._useSolr) {
            LOGGER.warn("Autosuggest definition for {} will be ignored in solr mode. "
                + "In order to use it, make sure it is defined in the solr config.", autosuggest);
            continue;
          }
          Map<String, Float> weights = new HashMap<>();
          for (String w : weight.split(",")) {
            String[] parts = w.split(":");
            if (parts.length == 2) {
              try {
                weights.put(parts[0], Float.valueOf(parts[1]));
              } catch (NumberFormatException ex) {
                LOGGER.error("Autosuggeset {}: ignoring invalid weight for field {}: not a number! ()", autosuggest,
                    parts[0], parts[1]);
              }
            }
          }
          def.addAutoSuggest(autosuggest, fields, terms, rfields, criteriafields, weights);
        } else if (this._useSolr && suggesters != null) {
          def.addAutoSuggest(autosuggest, Arrays.asList(suggesters.split(",")));
        } else {
          LOGGER.warn("Ignoring invalid autosuggest definition for {}: fields {}, terms {}, result fields {}",
              autosuggest, null, terms, rfields);
        }
      }
    }
  }
}

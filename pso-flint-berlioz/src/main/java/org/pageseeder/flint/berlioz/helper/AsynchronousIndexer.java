package org.pageseeder.flint.berlioz.helper;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.pageseeder.berlioz.util.ISO8601;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.berlioz.model.FlintConfig;
import org.pageseeder.flint.berlioz.model.IndexMaster;
import org.pageseeder.flint.berlioz.model.SolrIndexMaster;
import org.pageseeder.flint.berlioz.util.Files;
import org.pageseeder.flint.local.LocalIndexer;
import org.pageseeder.flint.local.LocalIndexer.Action;
import org.pageseeder.flint.lucene.LuceneIndexQueries;
import org.pageseeder.flint.solr.query.SolrQueryManager;
import org.pageseeder.xmlwriter.XMLWritable;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsynchronousIndexer implements Runnable, XMLWritable, FileFilter {

  /**
   * private logger
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(AsynchronousIndexer.class);

  private final IndexMaster _luceneIndex;

  private final SolrIndexMaster _solrIndex;

  private String folder = null;

  private Date modifiedAfter = null;

  private String pathRegex = null;

  private long started = -1;

  private boolean done = false;

  private LocalIndexer indexer = null;

  private boolean useIndexDate = true;

  private final static ExecutorService threads = Executors.newCachedThreadPool();

  private final static Map<String, AsynchronousIndexer> indexers = new ConcurrentHashMap<>();

  /**
   * Create a new indexer for the index provided.
   * @param index
   */
  public AsynchronousIndexer(IndexMaster index) {
    this._luceneIndex = index;
    this._solrIndex = null;
  }

  /**
   * Create a new indexer for the index provided.
   * @param index
   */
  public AsynchronousIndexer(SolrIndexMaster index) {
    this._luceneIndex = null;
    this._solrIndex = index;
  }

  /**
   * Set a date that is the lower limit for the last modified date of the files to index.
   * 
   * @param modifiedAfter the date
   */
  public void setModifiedAfter(Date modifiedAfter) {
    this.modifiedAfter = modifiedAfter;
  }

  /**
   * Set a Regex that the path of the files to index must match.
   * It is relative to the folder specified.
   * 
   * @param regex the regular expression
   */
  public void setPathRegex(String regex) {
    if (regex == null || regex.isEmpty()) this.pathRegex = null;
    else this.pathRegex = regex.replaceFirst("^/", ""); // remove first '/'
  }

  /**
   * If the index last modified date is used to select which files to index
   * @param useIndxDate whether or not to use index last modif date
   */
  public void setUseIndexDate(boolean useIndxDate) {
    this.useIndexDate = useIndxDate;
  }
  
  /**
   * the root folder of the files to index.
   * 
   * @param afolder the folder, relative to the index's content root
   */
  public void setFolder(String afolder) {
    this.folder = afolder;
  }

  public boolean start() {
    String name = getName();
    // only one active thread per index
    AsynchronousIndexer indexer = getIndexer(name);
    if (indexer != null) {
      if (!indexer.done) return false;
    }
    // put new one
    indexers.put(name, this);
    threads.execute(this);
    return true;
  }

  @Override
  public void run() {
    // set started time
    this.started = System.currentTimeMillis();

    String afolder = this.folder == null ? "/" : this.folder;

     // load existing documents
    IndexManager manager = FlintConfig.get().getManager();
    Map<String, Long> existing = this._luceneIndex != null ? getLuceneExistingContent(afolder) : getSolrExistingContent(afolder);
    if (existing == null) return;

    // find root folder
    File location = getContentLocation();
    File root = new File(location, afolder);

    // use local indexer
    this.indexer = new LocalIndexer(manager, this._luceneIndex == null ? this._solrIndex.getIndex() : this._luceneIndex.getIndex());
    this.indexer.setFileFilter(this);
    this.indexer.setUseIndexDate(this.useIndexDate);
    this.indexer.indexFolder(root, existing);

    // mark as finished
    this.done = true;
  }

  private Map<String, Long> getLuceneExistingContent(String afolder) {
    Map<String, Long> existing = new HashMap<String, Long>();
    IndexReader reader;
    try {
      reader = LuceneIndexQueries.grabReader(this._luceneIndex.getIndex());
    } catch (IndexException ex) {
      LOGGER.error("Failed to retrieve reader for index {}", this._luceneIndex.getName(), ex);
      return null;
    }
    try {
      for (int i = 0; i < reader.numDocs(); i++) {
        Document doc = reader.document(i);
        String src = doc.get("_src");
        String path = doc.get("_path");
        String lm   = doc.get("_lastmodified");
        // folder and regex
        if (lm != null && src != null && path != null &&
            path.startsWith(afolder) && 
            (this.pathRegex == null || path.substring(afolder.length()).matches(this.pathRegex))) {
          try {
            existing.put(src, Long.valueOf(lm));
          } catch (NumberFormatException ex) {
            // ignore, should never happen anyway
          }
        }
      }
    } catch (IOException ex) {
      LOGGER.error("Failed to load existing documents from index {}", this._luceneIndex.getName(), ex);
    } finally {
      LuceneIndexQueries.releaseQuietly(this._luceneIndex.getIndex(), reader);
    }
    return existing;
  }

  private Map<String, Long> getSolrExistingContent(String afolder) {
    // load all docs
    Map<String, Long> existing = new HashMap<String, Long>();
    new SolrQueryManager(this._solrIndex.getIndex()).select(new SolrQuery("*:*"), new Consumer<SolrDocument>() {
      @Override
      public void accept(SolrDocument doc) {
        String src  = (String) doc.get("_src");
        String path = (String) doc.get("_path");
        Long lm     = (Long) doc.get("_lastmodified");
        // folder and regex
        if (lm != null && src != null && path != null &&
            path.startsWith(afolder) && 
            (AsynchronousIndexer.this.pathRegex == null || path.substring(afolder.length()).matches(AsynchronousIndexer.this.pathRegex))) {
          try {
            existing.put(src, lm);
          } catch (NumberFormatException ex) {
            // ignore, should never happen anyway
          }
        }
      }
    });
    return existing;
  }

  /**
   * File filter method
   */
  @Override
  public boolean accept(File file) {
    // check with index's file filter
    if (!getIndexingFileFilter().accept(file))
      return false;
    // now check with regex
    if (this.pathRegex != null) {
      File root = new File(getContentLocation(), this.folder == null ? "/" : this.folder);
      String path = Files.path(root, file);
      if (path != null && !path.matches(this.pathRegex))
        return false;
    }
    // last check is date
    return this.modifiedAfter == null || file.lastModified() > this.modifiedAfter.getTime();
  }

  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("indexer");
    if (this.started > 0) xml.attribute("started", ISO8601.DATETIME.format(this.started));
    xml.attribute("index", getName());
    xml.attribute("completed", String.valueOf(this.done));
    if (this.folder != null) xml.attribute("folder", this.folder);
    if (this.indexer != null) {
      // batch
      BatchXMLWriter.batchToXML(this.indexer.getBatch(), xml);
      // files
      Map<String, Action> files = this.indexer.getIndexedFiles();
      xml.openElement("files");
      xml.attribute("count", files.size());
      String root = getContentLocation().getAbsolutePath();
      int max = 100;
      int current = 0;
      for (String path : files.keySet()) {
        xml.openElement("file");
        if (path.startsWith(root)) {
          xml.attribute("path", path.substring(root.length()));
        } else {
          xml.attribute("path", path);
        }
        xml.attribute("action", files.get(path).name().toLowerCase());
        xml.closeElement();
        if (current++ > max) break;
      }
      xml.closeElement();
    }
    xml.closeElement();
  }

  // static methods
  public static AsynchronousIndexer getIndexer(String name) {
    return indexers.get(name);
  }

  private String getName() {
    return this._luceneIndex == null ? this._solrIndex.getName() : this._luceneIndex.getName();
  }

  private File getContentLocation() {
    return this._luceneIndex != null ? this._luceneIndex.getIndex().getContentLocation() : this._solrIndex.getIndex().getContentLocation();
  }

  private FileFilter getIndexingFileFilter() {
    return this._luceneIndex != null ? this._luceneIndex.getIndexingFileFilter() : this._solrIndex.getIndexingFileFilter();
  }
}

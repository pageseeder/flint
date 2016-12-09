package org.pageseeder.flint.berlioz.model;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.pageseeder.berlioz.util.FileUtils;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.Requester;
import org.pageseeder.flint.berlioz.model.IndexDefinition.AutoSuggestDefinition;
import org.pageseeder.flint.indexing.FlintDocument;
import org.pageseeder.flint.indexing.IndexJob;
import org.pageseeder.flint.local.LocalFileContent;
import org.pageseeder.flint.solr.index.SolrLocalIndex;
import org.pageseeder.flint.solr.query.AutoSuggest;

/**
 * 
 */
public final class SolrIndexMaster {

  /**
   * private logger
   */
//  private static final Logger LOGGER = LoggerFactory.getLogger(SolrIndexMaster.class);
  private final IndexManager _manager;
  private final FileFilter _indexingFileFilter;
  private final File _contentRoot;
  private final SolrLocalIndex _index;
  private final IndexDefinition _def;

  private final Map<String, AutoSuggest> _autosuggests = new HashMap<>();
  
  public static SolrIndexMaster create(IndexManager mgr, String name,
         File content, IndexDefinition def) throws TransformerException {
    return create(mgr, name, content, "psml", def);
  }

  public static SolrIndexMaster create(IndexManager mgr, String name,
         File content, String extension, IndexDefinition def) throws TransformerException {
    return new SolrIndexMaster(mgr, name, content, extension, def);
  }

  private SolrIndexMaster(IndexManager mgr, String name, File content,
      String extension, IndexDefinition def) throws TransformerException {
    this._manager = mgr;
    this._contentRoot = content;
    this._index = new SolrLocalIndex(name, def.getName(), content);
    this._index.setTemplate(extension, def.getTemplate().toURI());
    this._def = def;
    this._indexingFileFilter = def.buildFileFilter(this._contentRoot);
    // create autosuggests
    for (String an : this._def.listAutoSuggestNames()) {
      getAutoSuggest(an);
    }
  }

  public String getName() {
    return this.getIndex().getIndexID();
  }

  public void reloadTemplate() throws TransformerException {
    reloadTemplate("psml");
  }

  public void reloadTemplate(String extension) throws TransformerException {
    this._index.setTemplate(extension, this._def.getTemplate().toURI());
  }

  public boolean isInIndex(File file) {
    return FileUtils.contains(_contentRoot, file);
  }

  public SolrLocalIndex getIndex() {
    return this._index;
  }

  /**
   * Use definition name as catalog (one catalog per definition).
   * @return the catalog name
   */
  public String getCatalog() {
    return this._def.getName();
  }

  public void clear() {
    Requester requester = new Requester("clear berlioz index");
    this._manager.clear(this._index, requester, IndexJob.Priority.HIGH);
  }

//  public SearchResults query(SearchQuery query) throws IndexException {
//    return LuceneIndexQueries.query(this._index, query);
//  }
//
//  public SearchResults query(SearchQuery query, SearchPaging paging) throws IndexException {
//    return LuceneIndexQueries.query(this._index, query, paging);
//  }

  public long lastModified() {
    return this._index.getIndexIO().getLastTimeUsed();
  }

  public IndexDefinition getIndexDefinition() {
    return this._def;
  }

  public FileFilter getIndexingFileFilter() {
    return this._indexingFileFilter;
  }

  public AutoSuggest getAutoSuggest(String name) {
    // block the list of suggesters so another thread doesn't try to create the same one
    synchronized (this._autosuggests) {
      AutoSuggest autosuggest = this._autosuggests.get(name);
      // create?
      if (autosuggest == null) {
        AutoSuggestDefinition ad = this._def.getAutoSuggest(name);
        autosuggest = new AutoSuggest(this._index, name, ad == null ? null : ad.getSolrSuggesters());
        this._autosuggests.put(name, autosuggest);
      }
      return autosuggest;
    }
  }

  public void close() {
    this._index.close();
  }

  public void generateIXML(File f, Writer out) throws IndexException, IOException {
    // create content
    LocalFileContent content = new LocalFileContent(f, null);
    this._manager.contentToIXML(this._index, content, this._index.getParameters(f), out);
  }

  public List<FlintDocument> generateLuceneDocuments(File f) throws IndexException, IOException {
    // create content
    LocalFileContent content = new LocalFileContent(f, null);
    return this._manager.contentToDocuments(this._index, content, this._index.getParameters(f));
  }


}
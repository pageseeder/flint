package org.pageseeder.flint.solr.index;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.pageseeder.flint.content.DeleteRule;
import org.pageseeder.flint.indexing.FlintField;
import org.pageseeder.flint.local.LocalIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrLocalIndex extends LocalIndex {

  /**
   * Logger will receive debugging and low-level data, use the listener to capture specific indexing operations.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(SolrLocalIndex.class);

  private final File _root;

  private final SolrIndexIO _io;

  public SolrLocalIndex(String name, String config, File root) {
    super(name, config);
    this._root = root;
    this._io = new SolrIndexIO(this);
  }

  public SolrIndexStatus getIndexStatus() {
    return this._io.getCurrentStatus();
  }

  @Override
  public SolrIndexIO getIndexIO() {
    return this._io;
  }

  @Override
  public File getContentLocation() {
    return this._root;
  }

  @Override
  public DeleteRule getDeleteRule(File file) {
    return SolrDeleteRule.deleteByQuery("_src:\"" + fileToSrc(file) + "\"");
  }

  @Override
  public Collection<FlintField> getFields(File file) {
    Collection<FlintField> fields = new ArrayList<>();
    if (file.exists()) {
      fields.add(buildField("_src", fileToSrc(file)));
      fields.add(buildField("_path", fileToPath(file)));
      fields.add(buildField("_lastmodified", String.valueOf(file.lastModified())));
    }
    return fields;
  }

  @Override
  public Map<String, String> getParameters(File file) {
    HashMap<String, String> params = new HashMap<>();
    if (file.exists()) {
      params.put("_src", fileToSrc(file));
      params.put("_path", fileToPath(file));
      params.put("_lastmodified", String.valueOf(file.lastModified()));
      params.put("_filename", file.getName());
    }
    return params;
  }

  public String fileToSrc(File f) {
    return f.getAbsolutePath().replace(File.separatorChar, '/');
  }

  public String fileToPath(File f) {
    try {
      String rootPath = this._root.getCanonicalPath();
      String thisPath = f.getCanonicalPath();
      if (thisPath.startsWith(rootPath))
        return thisPath.substring(rootPath.length()).replace(File.separatorChar, '/');
    } catch (IOException ex) {
      LOGGER.error("Failed to compute file relative path", ex);
    }
    return f.getAbsolutePath();
  }
  
  public File pathToFile(String path) {
    return new File(this._root, path);
  }

  private FlintField buildField(String name, String value) {
    // use filed builder as it will add the fields to the catalog
    return new FlintField(getCatalog()).name(name).value(value);
  }
}

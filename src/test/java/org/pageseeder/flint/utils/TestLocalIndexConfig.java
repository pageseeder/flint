package org.pageseeder.flint.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexableField;
import org.pageseeder.flint.content.DeleteRule;
import org.pageseeder.flint.local.LocalIndexConfig;

public class TestLocalIndexConfig extends LocalIndexConfig {

  private final File index;
  private final File content;

  public TestLocalIndexConfig(File idx, File ctnt) {
    this.index = idx;
    this.content = ctnt;
  }

  @Override
  public File getContent() {
    return this.content;
  }

  @Override
  public DeleteRule getDeleteRule(File file) {
    return new DeleteRule("_path", file.getAbsolutePath());
  }

  @Override
  public File getIndexLocation() {
    return this.index;
  }

  @Override
  public Map<String, String> getParameters(File file) {
    return Collections.singletonMap("_path", file.getAbsolutePath());
  }

  @Override
  public Collection<IndexableField> getFields(File file) {
    Collection<IndexableField> fields = new ArrayList<>();
    fields.add(new StringField("_path", file.getAbsolutePath(), Field.Store.YES));
    return fields;
  }
}

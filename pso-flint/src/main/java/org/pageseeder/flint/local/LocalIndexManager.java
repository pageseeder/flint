package org.pageseeder.flint.local;

import java.io.File;
import java.io.FileFilter;
import java.util.List;

import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.Requester;
import org.pageseeder.flint.content.SourceForwarder;
import org.pageseeder.flint.indexing.IndexJob.Priority;
import org.pageseeder.flint.indexing.IndexListener;

public class LocalIndexManager {

  private final IndexManager manager;

  protected LocalIndexManager(List<String> extensions) {
    this.manager = new IndexManager(new LocalFileContentFetcher());
    this.manager.setDefaultTranslator(new SourceForwarder(extensions, "UTF-8"));
  }

  protected LocalIndexManager(IndexListener listener, List<String> extensions) {
    this.manager = new IndexManager(new LocalFileContentFetcher(), listener);
    this.manager.setDefaultTranslator(new SourceForwarder(extensions, "UTF-8"));
  }

  protected LocalIndexManager(IndexListener listener, int threads, boolean single, List<String> extensions) {
    this.manager = new IndexManager(new LocalFileContentFetcher(), listener, threads, single);
    this.manager.setDefaultTranslator(new SourceForwarder(extensions, "UTF-8"));
  }

  public void clear(LocalIndex index) {
    this.manager.clear(index, new Requester("Clearing index"), Priority.LOW);
  }

  public void shutdown() {
    this.manager.stop();
  }

  public boolean isIndexing() {
    return !this.manager.getStatus().isEmpty();
  }

  public void indexNewContent(LocalIndex index) {
    indexNewContent(index, index.getContentLocation());
  }

  public void indexNewContent(LocalIndex index, File content) {
    LocalIndexer indexer = new LocalIndexer(this.manager, index);
    indexer.indexFolder(content, null);
  }

  public void indexNewContent(LocalIndex index, FileFilter filter, File content) {
    LocalIndexer indexer = new LocalIndexer(this.manager, index);
    indexer.setFileFilter(filter);
    indexer.indexFolder(content, null);
  }

  public void indexFile(LocalIndex index, File file, String requester) {
    this.manager.index(file.getAbsolutePath(), LocalFileContentType.SINGLETON, index, new Requester(requester), Priority.HIGH, null);
  }

  public void indexFile(LocalIndex index, File file) {
    this.manager.index(file.getAbsolutePath(), LocalFileContentType.SINGLETON, index, new Requester("Indexing single file"), Priority.HIGH, null);
  }
}

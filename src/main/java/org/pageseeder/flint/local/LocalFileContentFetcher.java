package org.pageseeder.flint.local;

import java.io.File;

import org.pageseeder.flint.content.Content;
import org.pageseeder.flint.content.ContentFetcher;
import org.pageseeder.flint.indexing.IndexJob;

public class LocalFileContentFetcher implements ContentFetcher {

  @Override
  public Content getContent(IndexJob job) {
    if (job.getIndex() instanceof LocalIndex) {
      LocalIndex index = (LocalIndex) job.getIndex();
      File file = new File(job.getContentID());
      return new LocalFileContent(file, index.getDeleteRule(file));
    }
    throw new IllegalArgumentException("Index must be LocalIndex");
  }

}

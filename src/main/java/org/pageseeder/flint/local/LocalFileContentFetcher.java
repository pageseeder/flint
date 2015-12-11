package org.pageseeder.flint.local;

import java.io.File;

import org.pageseeder.flint.IndexJob;
import org.pageseeder.flint.api.Content;
import org.pageseeder.flint.api.ContentFetcher;

public class LocalFileContentFetcher implements ContentFetcher {

  @Override
  public Content getContent(IndexJob job) {
    if (job.getIndex() instanceof LocalIndex) {
      LocalIndex index = (LocalIndex) job.getIndex();
      return new LocalFileContent(new File(job.getContentID()), index.getConfig());
    }
    throw new IllegalArgumentException("Index must be LocalIndex");
  }

}

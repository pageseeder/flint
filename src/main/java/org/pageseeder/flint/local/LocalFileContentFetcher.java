package org.pageseeder.flint.local;

import java.io.File;

import org.pageseeder.flint.IndexJob;
import org.pageseeder.flint.api.Content;
import org.pageseeder.flint.api.ContentFetcher;

public class LocalFileContentFetcher implements ContentFetcher {

  @Override
  public Content getContent(IndexJob job) {
    if (job.getRequester() instanceof LocalIndexer) {
      LocalIndexer indexer = (LocalIndexer) job.getRequester();
      return new LocalFileContent(new File(job.getContentID()), indexer.getContentRoot());
    }
    return new LocalFileContent(new File(job.getContentID()));
  }

}

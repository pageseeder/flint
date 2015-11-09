package org.pageseeder.flint.local;

import java.io.File;

import org.pageseeder.flint.api.Content;
import org.pageseeder.flint.api.ContentFetcher;
import org.pageseeder.flint.api.ContentType;

public class LocalFileContentFetcher implements ContentFetcher {

  @Override
  public Content getContent(String id, ContentType ctype) {
    File f = new File(id);
    return new LocalFileContent(f);
  }

}

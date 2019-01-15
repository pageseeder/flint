package org.pageseeder.flint.berlioz.model;

import org.pageseeder.flint.content.ContentTranslator;
import org.pageseeder.flint.content.ContentTranslatorFactory;
import org.pageseeder.flint.content.SourceForwarder;

import java.util.ArrayList;
import java.util.Collection;

public class PSMLContentTranslatorFactory implements ContentTranslatorFactory {

  private final static Collection<String> mimeTypes = new ArrayList<>();
  static {
    mimeTypes.add("psml");
    mimeTypes.add("xml");
  }

  @Override
  public Collection<String> getMimeTypesSupported() {
    return mimeTypes;
  }

  @Override
  public ContentTranslator createTranslator(String mimeType) {
    if (mimeType == null) return null;
    if (mimeTypes.contains(mimeType.toLowerCase()))
      return new SourceForwarder(mimeType, "UTF-8");
    return null;
  }
}

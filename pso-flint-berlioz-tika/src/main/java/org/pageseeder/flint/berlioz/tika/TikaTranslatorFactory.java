package org.pageseeder.flint.berlioz.tika;

import org.pageseeder.flint.content.ContentTranslator;
import org.pageseeder.flint.content.ContentTranslatorFactory;

import java.util.*;

public class TikaTranslatorFactory implements ContentTranslatorFactory {

  /**
   * Default to 30MB.
   */
  public static final int MAX_INDEXING_SIZE = 30000000;

  /**
   * XML MIME types supported.
   */
  protected static final List<String> MIME_TYPES = new ArrayList<String>();

  static {
    MIME_TYPES.add("pdf");
    MIME_TYPES.add("docx");
    MIME_TYPES.add("doc");
    MIME_TYPES.add("pptx");
    MIME_TYPES.add("ppt");
    MIME_TYPES.add("rtf");
    MIME_TYPES.add("jpeg");
    MIME_TYPES.add("jpg");
    MIME_TYPES.add("png");
    MIME_TYPES.add("bmp");
    MIME_TYPES.add("gif");
    MIME_TYPES.add("txt");
    MIME_TYPES.add("html");
  }

  /**
   * The list of translators
   */
  private final Map<String, ContentTranslator> translators;

  public TikaTranslatorFactory() {
    this.translators = new HashMap<>();
    for (String mtype : MIME_TYPES) {
      this.translators.put(mtype, new TikaTranslator());
    }
  }

  @Override
  public Collection<String> getMimeTypesSupported() {
    return this.translators.keySet();
  }

  @Override
  public ContentTranslator createTranslator(String mimeType) {
    return mimeType == null ? null : this.translators.get(mimeType.toLowerCase());
  }
}

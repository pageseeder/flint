package org.weborganic.flint;

import java.io.InputStreamReader;
import java.io.Reader;

import org.weborganic.flint.content.Content;
import org.weborganic.flint.content.ContentTranslator;
/**
 * Simple xml translator that handles XML MIME type by simply forwarding the content without translation.
 * 
 * @author Jean-Baptiste Reure
 * @version 10 March 2010
 */
public class SourceForwarder implements ContentTranslator {
  
  private final String mimeType;
  
  /**
   * Forwards data for one mime type only, will return in any other case
   */
  public SourceForwarder(String mType) {
    if (mType == null) throw new IllegalArgumentException("MIME Type cannot be null");
    this.mimeType = mType;
  }
  
  public Reader translate(Content content) {
    if (content.isDeleted()) return null;
    if (!this.mimeType.equals(content.getMimeType())) return null;
    return new InputStreamReader(content.getSource());
  }

}

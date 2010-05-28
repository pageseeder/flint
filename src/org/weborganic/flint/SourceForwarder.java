package org.weborganic.flint;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.weborganic.flint.content.Content;
import org.weborganic.flint.content.ContentTranslator;
/**
 * Simple xml translator that handles XML MIME type by simply forwarding the content without translation.
 * 
 * @author Jean-Baptiste Reure
 * @version 10 March 2010
 */
public class SourceForwarder implements ContentTranslator {

  private final List<String> mimeTypes = new ArrayList<String>();

  /**
   * Forwards data for one MIME type only, will return in any other case.
   * 
   * @param type 
   */
  public SourceForwarder(String type) {
    if (type == null) throw new IllegalArgumentException("MIME Type cannot be null");
    this.mimeTypes.add(type);
  }
  /**
   * Forwards data for one MIME type only, will return in any other case.
   * 
   * @param type 
   */
  public SourceForwarder(List<String> types) {
    if (types == null) throw new IllegalArgumentException("MIME Types cannot be null");
    this.mimeTypes.addAll(types);
  }

  public Reader translate(Content content) throws IndexException {
    if (content.isDeleted()) return null;
    if (!this.mimeTypes.contains(content.getMimeType())) return null;
    return new InputStreamReader(content.getSource());
  }

}

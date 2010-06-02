package org.weborganic.flint;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
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
  
  private final String charset;

  /**
   * Forwards data for one MIME type only, will return in any other case.
   * 
   * @param type 
   */
  public SourceForwarder(String type, String encoding) {
    if (type == null) throw new IllegalArgumentException("MIME Type cannot be null");
    this.mimeTypes.add(type);
    this.charset = encoding;
  }
  /**
   * Forwards data for one MIME type only, will return in any other case.
   * 
   * @param type 
   */
  public SourceForwarder(List<String> types, String encoding) {
    if (types == null) throw new IllegalArgumentException("MIME Types cannot be null");
    this.mimeTypes.addAll(types);
    this.charset = encoding;
  }

  public Reader translate(Content content) throws IndexException {
    if (content.isDeleted()) return null;
    if (!this.mimeTypes.contains(content.getMimeType())) return null;
    try {
      return new InputStreamReader(content.getSource(), this.charset);
    } catch (UnsupportedEncodingException e) {
      throw new IndexException("Unsupported Encoding "+this.charset, e);
    }
  }

}

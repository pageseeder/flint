package org.pageseeder.flint.templates;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

/**
 * A utility class to load/cache XSLT templates used when indexing.
 * 
 * @author Jean-Baptiste Reure
 */
public class TemplatesCache {

  /**
   * The single template factory.
   */
  private static TransformerFactory FACTORY = null;

  /**
   * The cache.
   */
  private static final Map<URI, Templates> CACHE = new HashMap<URI, Templates>();

  /**
   * Retrieve a template, will compile it if not found in cache.
   * 
   * @param path the path of the template
   * 
   * @return the compiled template
   * 
   * @throws TransformerException if the template is invalid
   */
  public static Templates get(URI path) throws TransformerException {
    // look in cache
    Templates cached = CACHE.get(path);
    if (cached != null) return cached;
    // build a new one
    if (FACTORY == null) FACTORY = TransformerFactory.newInstance();
    Templates built = FACTORY.newTemplates(new StreamSource(new File(path)));
    // store it
    if (built != null) CACHE.put(path, built);
    return built;
  }

  /**
   * Clear the cache of all the compiled templates.
   */
  public static void clear() {
    CACHE.clear();
  }

}

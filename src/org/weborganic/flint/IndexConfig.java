/*
 * This file is part of the Flint library.
 * 
 * For licensing information please see the file license.txt included in the release. A copy of this licence can also be
 * found at http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint;

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.weborganic.flint.content.ContentType;

/**
 * An IndexConfig provides the details needed to build the data to index from the original content.
 * 
 * <p>The path to a valid XSLT script is needed and parameters can be provided as well.
 * 
 * <p>The XSLT script should produce valid IndexXML format (see DTD).
 * 
 * @author Jean-Baptiste Reure
 * @version 26 February 2010
 */
public class IndexConfig {

  /**
   * Logger to use for this config object.
   */
  private static final Logger LOGGER = Logger.getLogger(IndexConfig.class);

  /**
   * The XSLT script.
   */
  private final Map<ContentDefinition, Templates> scripts = new ConcurrentHashMap<ContentDefinition, Templates>();

  /**
   * A list of parameters.
   */
  private final Map<ContentDefinition, Map<String, String>> parameters = new ConcurrentHashMap<ContentDefinition, Map<String, String>>();

  /**
   * Add a list of parameters for the given Content definition.
   * 
   * @param type     the ContentType
   * @param mimeType the Content's MIME Type
   * @param params   the list of parameters
   */
  public void addParameters(ContentType type, String mimeType, Map<String, String> params) {
    addParameters(type, mimeType, null, params);
  }

  /**
   * Add a list of parameters for the given Content definition.
   * 
   * @param type     the ContentType
   * @param mimeType the Content's MIME Type
   * @param configId the config ID, can be <code>null</code>
   * @param params   the list of parameters
   */
  public void addParameters(ContentType type, String mimeType, String configId, Map<String, String> params) {
    ContentDefinition def = new ContentDefinition(type, mimeType, configId);
    LOGGER.debug("Adding " + params.size() + " parameters for " + def.toString());
    this.parameters.put(def, params);
  }

  /**
   * Add a single parameter for the given Content definition.
   * 
   * @param type       the ContentType
   * @param mimeType   the Content's MIME Type
   * @param paramname  the parameter's name
   * @param paramvalue the parameter's value
   */
  public void addParameter(ContentType type, String mimeType, String paramname, String paramvalue) {
    addParameter(type, mimeType, null, paramname, paramvalue);
  }

  /**
   * Add a single parameter for the given Content definition.
   * 
   * @param type       the ContentType
   * @param mimeType   the Content's MIME Type
   * @param configId   the config ID, can be <code>null</code>
   * @param paramname  the parameter's name
   * @param paramvalue the parameter's value
   */
  public void addParameter(ContentType type, String mimeType, String configId, String paramname, String paramvalue) {
    ContentDefinition def = new ContentDefinition(type, mimeType, configId);
    Map<String, String> params = this.parameters.get(def);
    if (params == null) params = new HashMap<String, String>();
    params.put(paramname, paramvalue);
    this.parameters.put(def, params);
    LOGGER.debug("Adding parameter " + paramname + " for " + def.toString());
  }

  /**
   * Return a list of parameters for the given content definition.
   * 
   * @param type     the ContentType
   * @param mimeType the Content's MIME Type
   * @return the list of parameters for the given Content definition (never null).
   */
  public Map<String, String> getParameters(ContentType type, String mimeType) {
    return getParameters(type, mimeType, null);
  }

  /**
   * Return a list of parameters for the given content definition.
   * 
   * @param type     the ContentType
   * @param mimeType the Content's MIME Type
   * @param configId the config ID, can be <code>null</code>
   * @return the list of parameters for the given Content definition (never <code>null</code>).
   */
  public Map<String, String> getParameters(ContentType type, String mimeType, String configId) {
    Map<String, String> params = this.parameters.get(new ContentDefinition(type, mimeType, configId));
    if (params == null) return Collections.emptyMap();
    return Collections.unmodifiableMap(params);
  }

  /**
   * Return the compiled XSLT script, <code>null</code> if not found.
   * 
   * @param type     the ContentType
   * @param mimeType the Content's MIME Type
   * @return the compiled XSLT script, <code>null</code> if not found.
   */
  public Templates getTemplates(ContentType type, String mimeType) {
    return getTemplates(type, mimeType, null);
  }

  /**
   * Return the compiled XSLT script, <code>null</code> if not found.
   * 
   * @param type     the ContentType
   * @param mimeType the Content's Mime Type
   * @param configId the config ID, can be <code>null</code>
   * @return the compiled XSLT script, null if not found.
   */
  public Templates getTemplates(ContentType type, String mimeType, String configId) {
    ContentDefinition def = new ContentDefinition(type, mimeType, configId);
    LOGGER.debug("Retrieving templates for " + def.toString());
    return this.scripts.get(def);
  }

  /**
   * Add an XSLT script for the given Content definition.
   * 
   * @param type     the ContentType
   * @param mimeType the Content's MIME Type
   * @param xsltScript the full path to the XSLT script
   */
  public void addTemplates(ContentType type, String mimeType, URI xsltScript) {
    addTemplates(type, mimeType, null, xsltScript);
  }

  /**
   * Add an XSLT script for the given Content definition.
   * 
   * @param type     the ContentType
   * @param mimeType the Content's MIME Type
   * @param configId the config ID, can be <code>null</code>
   * @param xsltScript the full path to the XSLT script
   */
  public void addTemplates(ContentType type, String mimeType, String configId, URI xsltScript) {
    try {
      ContentDefinition def = new ContentDefinition(type, mimeType, configId);
      LOGGER.debug("Adding templates for " + def.toString());
      this.scripts.put(def, loadTemplates(xsltScript));
    } catch (Exception e) {
      LOGGER.debug("Failed to load XSLT script " + xsltScript + ": " + e.getMessage(), e);
      throw new IllegalArgumentException("Invalid XSLT script " + xsltScript + ": " + e.getMessage());
    }
  }

  /**
   * Gets the stylesheet at path from the cache or if not in the cache loads it and stores it in 
   * the cache for later use.
   * 
   * @param path Path of stylesheet
   * @return the compiled templates
   * 
   * @throws TransformerException if problem parsing stylesheet
   */
  private static Templates loadTemplates(URI path) throws TransformerException {
    TransformerFactory factory = TransformerFactory.newInstance();
    return factory.newTemplates(new StreamSource(new File(path)));
  }

  /**
   * 
   * @author Jean-Baptiste Reure
   */
  private static final class ContentDefinition {

    private final ContentType ctype;

    private final String mtype;

    private final String configid;

    public ContentDefinition(ContentType ct, String mt, String cid) {
      this.configid = cid;
      this.ctype = ct;
      this.mtype = mt;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof ContentDefinition) || obj == null) return false;
      // FIXME Poor performance creates a string for each comparison!!!
      return obj.toString().equals(this.toString());
    }

    @Override
    public String toString() {
      return this.ctype.toString() + "|" + this.mtype + "|" + (this.configid == null? "" : this.configid);
    }

    @Override
    public int hashCode() {
      return this.ctype.hashCode() * 13 + this.mtype.hashCode() * 11
          + (this.configid == null? 7 : this.configid.hashCode() * 7);
    }
  }

}

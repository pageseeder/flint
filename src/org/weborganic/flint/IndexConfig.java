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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.flint.content.ContentType;
import org.weborganic.flint.util.Beta;

/**
 * Provides the details needed to build the data to index from the original content.
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
  private static final Logger LOGGER = LoggerFactory.getLogger(IndexConfig.class);

  /**
   * The XSLT script.
   */
  private final Map<ContentDefinition, Templates> _templates = new ConcurrentHashMap<ContentDefinition, Templates>();

  /**
   * A list of parameters.
   */
  private final Map<ContentDefinition, Map<String, String>> _parameters = new ConcurrentHashMap<ContentDefinition, Map<String, String>>();

  // Parameters management =========================================================================

  /**
   * Sets the parameters to supply to the templates when indexing content with the specified content type and media 
   * type.
   * 
   * <p>Any existing parameters for this content and media type will be discarded.
   * 
   * @param type       the type of content to index.
   * @param media      the media type of the content (eg. "application/xml")
   * @param parameters the name-value map of parameters to set.
   */
  public void setParameters(ContentType type, String media, Map<String, String> parameters) {
    setParameters(type, media, null, parameters);
  }

  /**
   * Sets the list of parameters to supply to the templates when indexing content with the specified content type,
   * media type and using configuration ID.
   * 
   * <p>Any existing parameters for this content and media type will be discarded.
   * 
   * @param type       the type of content to index.
   * @param media      the media type of the content (eg. "application/xml")
   * @param config     the configuration ID; may be <code>null</code>
   * @param parameters the name-value map of parameters to set.
   */
  public void setParameters(ContentType type, String media, String config, Map<String, String> parameters) {
    ContentDefinition def = new ContentDefinition(type, media, config);
    LOGGER.debug("Adding {} parameters for {}", parameters.size(), def);
    this._parameters.put(def, parameters);
  }

  /**
   * Adds parameters to the parameters to supply to the templates when indexing content with the specified content 
   * type and media type.
   * 
   * <p>Any existing parameters with the same name for this content and media type will be discarded.
   * 
   * <p>Note: This method will add to existing parameters, to set parameters, use 
   * {@link #setParameters(ContentType, String, Map)} instead.
   * 
   * @param type       the type of content to index.
   * @param media      the media type of the content (eg. "application/xml")
   * @param parameters the name-value map of parameters to add.
   */
  @Beta public void addParameters(ContentType type, String media, Map<String, String> parameters) {
    addParameters(type, media, null, parameters);
  }

  /**
   * Adds parameters to the parameters to supply to the templates when indexing content with the specified content 
   * type and media type.
   * 
   * <p>Any existing parameters with the same name for this content and media type will be discarded.
   * 
   * <p>Note: This method will add to existing parameters, to set parameters, use 
   * {@link #setParameters(ContentType, String, Map)} instead.
   * 
   * @param type       the type of content to index.
   * @param media      the media type of the content (eg. "application/xml")
   * @param config     the configuration ID; may be <code>null</code>
   * @param parameters the name-value map of parameters to add.
   */
  @Beta public void addParameters(ContentType type, String media, String config, Map<String, String> parameters) {
    ContentDefinition def = new ContentDefinition(type, media, config);
    Map<String, String> p = this._parameters.get(def);
    if (p == null) {
      p = new HashMap<String, String>();
      this._parameters.put(def, p);
    }
    p.putAll(parameters);
    LOGGER.debug("Adding {} parameters for {}", parameters.size(), def);
  }

  /**
   * Adds a single parameter to the parameters to supply to the templates when indexing content with the specified
   * content type and media type.
   * 
   * <p>Any existing parameter with the same name for this content and media type will be discarded.
   *
   * @param type  the type of content to index.
   * @param media the media type of the content (eg. "application/xml")
   * @param name  the name of the parameter to add
   * @param value the value of the parameter to add
   */
  public void addParameter(ContentType type, String media, String name, String value) {
    addParameter(type, media, null, name, value);
  }

  /**
   * Adds a single parameter to the parameters to supply to the templates when indexing content with the specified
   * content type and media type for a specific configuration ID.
   * 
   * <p>Any existing parameter with the same name for this content and media type will be discarded.
   * 
   * @param type    the type of content to index.
   * @param media   the media type of the content (eg. "application/xml")
   * @param config  the configuration ID; may be <code>null</code>
   * @param name    the name of the parameter to add
   * @param value   the value of the parameter to add
   */
  public void addParameter(ContentType type, String media, String config, String name, String value) {
    ContentDefinition def = new ContentDefinition(type, media, config);
    Map<String, String> p = this._parameters.get(def);
    if (p == null) {
      p = new HashMap<String, String>();
      this._parameters.put(def, p);
    }
    p.put(name, value);
    LOGGER.debug("Adding parameter {} for {}", name, def);
  }

  /**
   * Returns a list of parameters for the given content type and media type (and not matching a specific configuration
   * ID).
   * 
   * @param type    the type of content to index.
   * @param media   the media type of the content (eg. "application/xml")
   * 
   * @return the list of parameters for the given Content definition (never <code>null</code>).
   */
  public Map<String, String> getParameters(ContentType type, String media) {
    return getParameters(type, media, null);
  }

  /**
   * Returns an unmodifiable list of parameters for the given content type and media type and matching a specific 
   * configuration ID.
   * 
   * @param type     the type of content to index.
   * @param media    the media type of the content (eg. "application/xml")
   * @param configId the configuration ID; may be <code>null</code>
   * 
   * @return the list of parameters for the given Content definition (never <code>null</code>).
   */
  public Map<String, String> getParameters(ContentType type, String media, String configId) {
    Map<String, String> params = this._parameters.get(new ContentDefinition(type, media, configId));
    if (params == null) return Collections.emptyMap();
    return Collections.unmodifiableMap(params);
  }

  // Templates management =========================================================================

  /**
   * Returns the compiled XSLT templates or <code>null</code> if there are no templates associated with
   * this content type and media type.
   * 
   * @param type  the type of content to index.
   * @param media the media type of the content (eg. "application/xml")
   * 
   * @return the compiled XSLT template; <code>null</code> if not found.
   */
  public Templates getTemplates(ContentType type, String media) {
    return getTemplates(type, media, null);
  }

  /**
   * Returns the compiled XSLT templates, <code>null</code>  if there are no templates associated with
   * this content type and media type for the specified configuration ID.
   * 
   * @param type   the type of content to index.
   * @param media  the media type of the content (eg. "application/xml")
   * @param config the configuration ID, can be <code>null</code>
   * 
   * @return the compiled XSLT templates; <code>null</code> if not found.
   */
  public Templates getTemplates(ContentType type, String media, String config) {
    ContentDefinition def = new ContentDefinition(type, media, config);
    LOGGER.debug("Retrieving templates for {}", def);
    return this._templates.get(def);
  }

  /**
   * Adds an XSLT script for the given Content definition this content type and media type for the 
   * specified configuration ID.
   * 
   * @deprecated Use {@link #setTemplates(ContentType, String, URI)} instead.
   * 
   * @param type     the type of content to index.
   * @param media    the media type of the content (eg. "application/xml")
   * @param template the full path to the XSLT template file.
   */
  @Deprecated public void addTemplates(ContentType type, String media, URI template) {
    setTemplates(type, media, null, template);
  }

  /**
   * Adds an XSLT script for the given Content definition.
   * 
   * @deprecated Use {@link #setTemplates(ContentType, String, String, URI)} instead.
   * 
   * @param type     the type of content to index.
   * @param media    the media type of the content (eg. "application/xml").
   * @param config   the config ID, can be <code>null</code>.
   * @param template the full path to the XSLT template file.
   */
  @Deprecated public void addTemplates(ContentType type, String media, String config, URI template) {
    setTemplates(type, media, config, template);
  }


  /**
   * Sets the XSLT templates to use for the specified content type and media type.
   * 
   * @param type     the type of content to index.
   * @param media    the media type of the content (eg. "application/xml").
   * @param template the full path to the XSLT template file.
   */
  public void setTemplates(ContentType type, String media, URI template) {
    setTemplates(type, media, null, template);
  }

  /**
   * Sets the XSLT templates to use for the specified content type, media type and configuration ID.
   * 
   * @param type     the type of content to index.
   * @param media    the media type of the content (eg. "application/xml").
   * @param config   the config ID, can be <code>null</code>.
   * @param template the full path to the XSLT template file.
   */
  public void setTemplates(ContentType type, String media, String config, URI template) {
    try {
      ContentDefinition def = new ContentDefinition(type, media, config);
      LOGGER.debug("Adding templates for {}", def);
      this._templates.put(def, loadTemplates(template));
    } catch (TransformerException ex) {
      LOGGER.warn("Failed to load XSLT script " + template + ": " + ex.getMessageAndLocation(), ex);
      throw new IllegalArgumentException("Invalid XSLT script " + template + ": " + ex.getMessageAndLocation());
    }
  }

  // Private helpers ==============================================================================

  /**
   * Gets the stylesheet at path from the cache or if not in the cache loads it and stores it in 
   * the cache for later use.
   * 
   * @param path Path to the XSLT templates.
   * 
   * @return the compiled templates.
   * 
   * @throws TransformerException if thrown by the {@link TransformerFactory} while parsing the stylesheet
   */
  private static Templates loadTemplates(URI path) throws TransformerException {
    TransformerFactory factory = TransformerFactory.newInstance();
    return factory.newTemplates(new StreamSource(new File(path)));
  }

  /**
   * A simple immutable object to use as a key and optimised for fast retrieval.
   * 
   * @author Jean-Baptiste Reure
   * @author Christophe Lauret
   * @version 29 July 2010
   */
  private static final class ContentDefinition {

    /** Content type */
    private final ContentType _type;

    /** Media Type */
    private final String _media;

    /** Configuration ID */
    private final String _configId;

    /** Pre-computed hash code for this object */
    private final int hashCode;

    /**
     * Creates a new content definition.
     * 
     * @param type     The content type
     * @param media    The media type
     * @param configId The configuration ID (may be <code>null</code>)
     */
    public ContentDefinition(ContentType type, String media, String configId) {
      this._type = type;
      this._media = media;
      this._configId = configId;
      this.hashCode = hashCode(type, media, configId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
      if (!(o instanceof ContentDefinition)) return false;
      return this.equals((ContentDefinition)o);
    }

    /**
     * Compares two content definition for equality.
     * 
     * @param def the content definition to compare for equality.
     * @return <code>true</code> if the type, media and config ID are equal;
     *         <code>false</code> otherwise.
     */
    public boolean equals(ContentDefinition def) {
      if (def == null) return false;
      if (this == def) return true;
      if (!this._type.equals(def._type)) return false;
      if (!this._media.equals(def._media)) return false;
      if (this._configId == null) return def._configId == null;
      return this._configId.equals(def._configId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      return this.hashCode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
      return this._type.toString() + "|" + this._media + "|" + (this._configId == null? "" : this._configId);
    }

    /**
     * Computes the hash code to make a more efficient key.
     * 
     * @param type  The content type
     * @param media The media type
     * @param id    The configuration (may be <code>null</code>)
     * 
     * @return the hashcode for this object.
     */
    private static int hashCode(ContentType type, String media, String id) {
     return type.hashCode() * 13 + media.hashCode() * 19 + (id == null? 7 : id.hashCode() * 7);
    }
  }

}

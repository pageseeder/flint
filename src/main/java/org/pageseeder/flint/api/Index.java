/*
 * Copyright 2015 Allette Systems (Australia)
 * http://www.allette.com.au
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pageseeder.flint.api;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class Index {

  /**
   * Logger to use for this config object.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(Index.class);


  private final String _id;

  private final Directory _directory;
  /**
   * The XSLT script.
   */
  private final Map<ContentDefinition, Templates> _templates = new ConcurrentHashMap<ContentDefinition, Templates>();

  private final Analyzer _analyzer;

  public Index(String id, File dir, Analyzer analyzer) throws IOException {
    this(id, FSDirectory.open(dir.toPath()), analyzer);
  }

  public Index(String id, Directory dir, Analyzer analyzer) {
    this._directory = dir;
    this._id = id;
    this._analyzer = analyzer;
  }

  public Analyzer getAnalyzer() {
    return this._analyzer;
  }
  
  /**
   * Return the unique identifier for this index.
   *
   * @return The Index ID.
   */
  public final String getIndexID() {
    return this._id;
  }

  /**
   * Return the Index Directory object.
   *
   * @return The Index Directory object
   */
  public final Directory getIndexDirectory() {
    return this._directory;
  }

  // Parameters management =========================================================================

  /**
   * Used to define parameters specific to a content (should be overwritten by sub-classes).
   * 
   * @param content the actual content
   * 
   * @return the list of parameters, <code>null</code> if none
   */
  public Map<String, String> getParameters(Content content) {
    return null;
  }

  // Templates management =========================================================================

  /**
   * Returns the compiled XSLT templates, <code>null</code>  if there are no templates associated with
   * this content type and media type for the specified configuration ID.
   *
   * @param type   the type of content to index.
   * @param media  the media type of the content (eg. "application/xml")
   *
   * @return the compiled XSLT templates; <code>null</code> if not found.
   */
  public Templates getTemplates(ContentType type, String media) {
    ContentDefinition def = new ContentDefinition(type, media);
//    LOGGER.debug("Retrieving templates for {}", def);
    return this._templates.get(def);
  }

  /**
   * Sets the XSLT templates to use for the specified content type, media type and configuration ID.
   *
   * @param type     the type of content to index.
   * @param media    the media type of the content (eg. "application/xml").
   * @param template the full path to the XSLT template file.
   *
   * @throws InvalidTemplatesException If the templates are invalid.
   */
  public void setTemplates(ContentType type, String media, URI template) throws TransformerException {
    ContentDefinition def = new ContentDefinition(type, media);
    LOGGER.debug("Adding templates for {}", def);
    this._templates.put(def, loadTemplates(template));
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

    /** Pre-computed hash code for this object */
    private final int hashCode;

    /**
     * Creates a new content definition.
     *
     * @param type     The content type
     * @param media    The media type
     * @param configId The configuration ID (may be <code>null</code>)
     */
    public ContentDefinition(ContentType type, String media) {
      this._type = type;
      this._media = media;
      this.hashCode = hashCode(type, media);
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
      return this._media == null && def._media == null || this._media.equals(def._media);
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
      return this._type.toString() + "|" + this._media;
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
    private static int hashCode(ContentType type, String media) {
     return type.hashCode() * 13 + (media == null ? 0 : (media.hashCode() * 19));
    }
  }

}

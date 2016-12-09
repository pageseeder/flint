/*
 * Copyright (c) 1999-2015 Allette systems pty. ltd.
 */
package org.pageseeder.flint.solr.index;

import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.common.util.NamedList;
import org.pageseeder.xmlwriter.XMLWritable;
import org.pageseeder.xmlwriter.XMLWriter;

/**
 * The meta data of Solr.
 *
 * @author Ciber Cai
 * @since 22 September ,2016
 */
public class SolrIndexStatus implements XMLWritable, Serializable {

  private static final long serialVersionUID = -5743282468784638428L;

  private int numDocs = 0;

  private int numTerms = 0;

  private long elapsedTime = -1;

  private int maxDoc = 0;

  private boolean current = false;

  private Date lastModified = null;

  private String version;

  private int numSegments = 0;

  private long size = 0;

  private Map<String, Integer> _fields = new HashMap<>();

  /**
   * @return the numDocs
   */
  public int numDocs() {
    return this.numDocs;
  }

  /**
   * @return the numTerms
   */
  public int numTerms() {
    return this.numTerms;
  }

  /**
   * @return the elapsedTime
   */
  public long elapsedTime() {
    return this.elapsedTime;
  }

  /**
   * @return the maxDoc
   */
  public int maxDoc() {
    return this.maxDoc;
  }

  /**
   * @return the current
   */
  public boolean isCurrent() {
    return this.current;
  }

  /**
   * @return the version
   */
  public String version() {
    return this.version;
  }

  /**
   * @return the numSegments
   */
  public int numSegments() {
    return this.numSegments;
  }

  /**
   * @return the size
   */
  public long size() {
    return this.size;
  }

  public SolrIndexStatus numDocs(int numDocs) {
    this.numDocs = numDocs;
    return this;
  }

  /**
   * @param elapsedTime the elaspsed time
   * @return {@link SolrIndexStatus}
   */
  public SolrIndexStatus elapsedTime(long elapsedTime) {
    this.elapsedTime = elapsedTime;
    return this;
  }

  /**
   * @param numTerms the total number of terms
   * @return {@link SolrIndexStatus}
   */
  public SolrIndexStatus numTerms(int numTerms) {
    this.numTerms = numTerms;
    return this;

  }

  /**
   * @param maxDoc the max doc
   * @return {@link SolrIndexStatus}
   */
  public SolrIndexStatus maxDoc(int maxDoc) {
    this.maxDoc = maxDoc;
    return this;
  }

  /**
   * @param version the version
   * @return {@link SolrIndexStatus}
   */
  public SolrIndexStatus version(String version) {
    this.version = version;
    return this;
  }

  /**
   * @param lastModified the last modified date
   * @return {@link SolrIndexStatus}
   */
  public SolrIndexStatus lastModified(Date lastModified) {
    this.lastModified = lastModified;
    return this;
  }

  /**
   * @param current whether is correct
   * @return {@link SolrIndexStatus}
   */
  public SolrIndexStatus current(boolean current) {
    this.current = current;
    return this;
  }

  /**
   * @return the index last modified date
   */
  public Date getLastModified() {
    return this.lastModified;
  }

  /**
   * @param numSegments the number of segment
   * @return {@link SolrIndexStatus}
   */
  public SolrIndexStatus numSegments(int numSegments) {
    this.numSegments = numSegments;
    return this;
  }

  public void fields(Map<String, Integer> fields) {
    if (fields != null) {
      this._fields.clear();
      this._fields.putAll(fields);
    }
  }

  public void field(String field, int docs) {
    if (field != null && docs >= 0)
      this._fields.put(field, Integer.valueOf(docs));
  }

  /**
   * @param size the size of index
   * @return {@link SolrIndexStatus}
   */
  public SolrIndexStatus size(long size) {
    this.size = size;
    return this;
  }

  public static SolrIndexStatus fromNamedList(NamedList<Object> list) {
    return new SolrIndexStatus().current(list.getBooleanArg("current"))
                                .numDocs((int) list.get("numDocs"))
                                .maxDoc((int) list.get("maxDoc"))
                                .lastModified((Date) list.get("lastModified"))
                                .version(list.get("version").toString())
                                .lastModified((Date) list.get("lastModified"))
                                .numSegments((int) list.get("segmentCount"))
                                .size((Long) list.get("sizeInBytes"));
  }

  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("index-status");
    xml.attribute("numDocs", String.valueOf(this.numDocs));
    xml.attribute("numTerms", String.valueOf(this.numTerms));
    xml.attribute("maxDoc", String.valueOf(this.maxDoc));
    xml.attribute("elapsedTime", String.valueOf(this.elapsedTime));
    xml.attribute("version", this.version);
    if (this.lastModified != null) {
      xml.attribute("last-modified-date", iso8601(this.lastModified));
    }
    xml.attribute("current", this.current ? "yes" : "no");
    xml.attribute("size", String.valueOf(this.size));
    xml.attribute("numSegments", String.valueOf(this.numSegments));
    if (this._fields != null && !this._fields.isEmpty()) {
      xml.openElement("fields");
      for (String fn : this._fields.keySet()) {
        xml.openElement("field");
        xml.attribute("name", fn);
        xml.attribute("docs", this._fields.get(fn));
        xml.closeElement();
      }
      xml.closeElement();
    }
    xml.closeElement();
  }

  private static String iso8601(Date date) {
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
    return df.format(date);
  }

}

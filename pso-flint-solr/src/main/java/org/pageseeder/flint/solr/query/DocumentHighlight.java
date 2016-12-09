/*
 * Copyright (c) 1999-2015 Allette systems pty. ltd.
 */
package org.pageseeder.flint.solr.query;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pageseeder.xmlwriter.XMLWritable;
import org.pageseeder.xmlwriter.XMLWriter;

/**
 *  A simple object for representing a High lighting.
 *
 * @author Ciber Cai
 * @since 8 September 2016
 */
public class DocumentHighlight implements XMLWritable, Serializable {

  /* DocumentHighlight.java */
  private static final long serialVersionUID = -3628054959898754494L;

  private final String _id;

  private final Map<String, List<String>> _highlights = new HashMap<String, List<String>>();

  private DocumentHighlight(Builder builder) {
    this._id = builder.id;
    this._highlights.putAll(builder.highlights);
  }

  public String id() {
    return this._id;
  }

  public List<String> highlights(String field) {
    return this._highlights.get(field);
  }

  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("highlights");
    xml.attribute("for", this._id);
    for (String field : this._highlights.keySet()) {
      for (String text : this._highlights.get(field)) {
        xml.openElement("highlight");
        xml.attribute("field", field);
        xml.writeText(text);
        xml.closeElement();
      }
    }
    xml.closeElement();
  }

  public static class Builder {

    private String id;

    private final Map<String, List<String>> highlights = new HashMap<String, List<String>>();

    public Builder id(String theid) {
      this.id = theid;
      return this;
    }

    public Builder highlight(String field, String ahighlight) {
      List<String> h = this.highlights.get(field);
      if (h == null) h = new ArrayList<>();
      h.add(ahighlight);
      this.highlights.put(field, h);
      return this;
    }

    public Builder highlights(String field, List<String> highlights) {
      this.highlights.put(field, highlights);
      return this;
    }

    public DocumentHighlight build() {
      return new DocumentHighlight(this);
    }

  }

}

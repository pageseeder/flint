/*
 * Copyright (c) 1999-2015 Allette systems pty. ltd.
 */
package org.pageseeder.flint.solr.query;

import java.io.IOException;
import java.io.Serializable;

import org.pageseeder.flint.indexing.FlintField;
import org.pageseeder.xmlwriter.XMLWritable;
import org.pageseeder.xmlwriter.XMLWriter;

/**
 * @author Ciber Cai
 * @since 21 September ,2016
 */
public class SolrTerm implements XMLWritable, Serializable {

  private static final long serialVersionUID = 4743889862182502609L;

  private final FlintField _field;
  private final String _term;
  private final long _frequency;

  private SolrTerm(Builder builder) {
    this._field = builder.field;
    this._term = builder.term;
    this._frequency = builder.frequency;
  }

  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("term");
    xml.attribute("field", this._field.name());
    xml.attribute("term", this._term);
    xml.attribute("frequency", String.valueOf(this._frequency));
    xml.closeElement();
  }

  public static class Builder {
    private FlintField field;
    private String term;
    private long frequency;

    public Builder field(FlintField field) {
      this.field = field;
      return this;
    }

    public Builder term(String term) {
      this.term = term;
      return this;
    }

    public Builder frequency(long frequency) {
      this.frequency = frequency;
      return this;
    }

    public SolrTerm build() {
      return new SolrTerm(this);
    }

  }

}

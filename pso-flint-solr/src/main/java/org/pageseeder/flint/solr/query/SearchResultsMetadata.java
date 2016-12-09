/*
 * Copyright (c) 1999-2015 Allette systems pty. ltd.
 */
package org.pageseeder.flint.solr.query;

import java.io.IOException;
import java.io.Serializable;

import org.apache.solr.client.solrj.SolrQuery;
import org.pageseeder.xmlwriter.XMLWritable;
import org.pageseeder.xmlwriter.XMLWriter;

/**
 * @author Ciber Cai
 * @since 7 September ,2016
 */
public class SearchResultsMetadata implements Serializable, XMLWritable {

  /* SolrResult.java */
  private static final long serialVersionUID = 2098716091239675835L;

  private final SolrQuery _query;

  private final long _numFound;

  private final long _start;

  private final long _row;

  private final long _totalPages;

  private final long _currentPage;

  public SearchResultsMetadata(SolrQuery query, long numFound, long start, long row) {
    this._query = query;
    this._numFound = numFound;
    this._start = start;
    this._row = row;
    this._totalPages = this._row > 0 ? ((this._numFound + this._row - 1) / this._row) : 0;
    long cur = (start + this._row) / row;
    this._currentPage = cur >= this._totalPages ? this._totalPages : cur > 0 ? cur : 1;
  }

  public long getNumberFound() {
    return this._numFound;
  }

  public long getStart() {
    return this._start;
  }

  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("results");
    xml.attribute("total", String.valueOf(this._numFound));
    xml.attribute("start", String.valueOf(this._start));
    xml.attribute("row", String.valueOf(this._row));
    xml.attribute("last-page", String.valueOf(this._totalPages));
    xml.attribute("current-page", String.valueOf(this._currentPage));

    if (this._totalPages > 1) {
      xml.openElement("pages");
      printPagination(xml);
      xml.closeElement(); // pages
    }

    // query
    xml.openElement("query");
    for (String name : this._query.getParameterNames()) {
      xml.openElement("param");
      xml.attribute("name", name);
      String[] values = this._query.getParams(name);
      if (values != null) {
        if (values.length == 1) xml.attribute("value", values[0]);
        else {
          StringBuilder sb = new StringBuilder();
          for (String value : values) {
            sb.append(',').append(value);
          }
          xml.attribute("value", sb.substring(1));
        }
      }
      xml.closeElement(); // param
    }
    xml.closeElement(); // query
    
    xml.closeElement(); // result

  }

  private void printPagination(XMLWriter xml) throws IOException {
    // print all
    if (this._totalPages < 5) {
      for (int i = 1; i <= this._totalPages; i++) {
        xml.element("page", String.valueOf(i));
      }
    } else {

      long middle = 2;
      long leftStart = this._currentPage - middle;
      long rightEnd = this._currentPage + middle;

      if (leftStart <= 0) {
        rightEnd += Math.abs(leftStart) + 1;
        leftStart = 1;
      }

      if (rightEnd > this._totalPages) {
        leftStart -= Math.abs(rightEnd - this._totalPages);
        rightEnd = this._totalPages;
      }

      for (long i = leftStart; i < rightEnd + 1; i++) {
        xml.element("page", String.valueOf(i));
      }

    }

  }

}

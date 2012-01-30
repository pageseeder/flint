/*
 * This file is part of the Flint library.
 * 
 * For licensing information please see the file license.txt included in the release. A copy of this licence can also be
 * found at http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.query;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import com.topologi.diffx.xml.XMLWriter;

/**
 * A basic search parameter wrapping a simple {@link Term}.
 * 
 * @author Christophe Lauret
 * @version 13 August 2010
 */
public final class TermParameter implements SearchParameter {

  /**
   * The wrapped Lucene Term.
   */
  private final Term _term;

  /**
   * Creates a new term parameter from the specified field name and text.
   *
   * @param field The name of the field to search.
   * @param text  The text to match in the field value.
   * 
   * @throws NullPointerException If either parameter is <code>null</code>.
   */
  public TermParameter(String field, String text) throws NullPointerException {
    if (field == null) throw new NullPointerException("field");
    if (text == null) throw new NullPointerException("text");
    this._term = new Term(field, text);
  }

  /**
   * Creates a new term parameter from the specified term.
   *
   * @param term The term to match.
   * 
   * @throws NullPointerException If either is <code>null</code>.
   */
  public TermParameter(Term term) throws NullPointerException {
    if (term == null) throw new NullPointerException("term");
    this._term = term;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isEmpty() {
    return this._term.field().isEmpty() || this._term.text().isEmpty();
  }

  /**
   * Returns the wrapped term.
   * @return the wrapped term.
   */
  public Term term() {
    return this._term;
  }

  /**
   * {@inheritDoc}
   */
  public Query toQuery() {
    if (this.isEmpty()) { return null; }
    return new TermQuery(this._term);
  }

  /**
   * {@inheritDoc}
   */
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("term-parameter");
    // indicate whether this search term is empty
    xml.attribute("is-empty", Boolean.toString(this.isEmpty()));
    // not empty, give details
    if (!this.isEmpty()) {
      xml.element("field", this._term.field());
      xml.element("text", this._term.text());
    }
    xml.closeElement();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return this._term.toString();
  }

}

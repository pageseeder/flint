package org.weborganic.flint.query;

import java.io.IOException;

import org.apache.lucene.search.Query;
import org.weborganic.flint.search.Facet;

import com.topologi.diffx.xml.XMLWriter;

/**
 * The selected value for a facet.
 * 
 * @author Christophe Lauret
 * @version 13 August 2010
 */
public final class FacetParameter implements SearchParameter {

  /**
   * The facet that is selected.
   */
  private final Facet _facet;

  /**
   * The value of this selected facet.
   */
  private final String _value; 

  /**
   * The Query for this selected facet.
   */
  private final Query _query; 

  /**
   * Creates a new facet value.
   * 
   * @param facet the facet.
   * @param value the selected value. 
   */
  public FacetParameter(Facet facet, String value) {
    this._facet = facet;
    this._value = value;
    this._query = this._facet.forValue(value);
  }

  /**
   * Returns the wrapped facet.
   * 
   * @return the facet.
   */
  public Facet facet() {
    return this._facet;
  }

  /**
   * Returns the selected value.
   * 
   * @return the selected value.
   */
  public String value() {
    return this._value;
  }

  /**
   * 
   */
  public boolean isEmpty() {
    return this._query == null;
  }

  /**
   * Returns the Query for this selected facet.
   * @return the Query for this selected facet.
   */
  public Query toQuery() {
    return this._query;
  }

  /**
   * 
   */
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("selected-facet");
    xml.attribute("name", this._facet.name());
    xml.attribute("value", this._value);
    this._facet.toXML(xml);
    xml.closeElement();
  }

}

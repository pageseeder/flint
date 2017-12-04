package org.pageseeder.flint.lucene.facet;

import org.pageseeder.xmlwriter.XMLWritable;

/**
 * Base class for all flexible facets implementations.
 *
 * @author Christophe Lauret
 *
 * @version 5.1.3
 */
public abstract class FlexibleFacet implements XMLWritable {

  /**
   * The name of this facet
   */
  protected final String _name;

  /**
   * If the facet was computed in a "flexible" way
   */
  protected transient boolean flexible = false;

  protected FlexibleFacet(String name) {
    this._name = name;
  }

  /**
   * Returns the name of the field.
   * @return the name of the field.
   */
  public String name() {
    return this._name;
  }

  /**
   * The type of facet.
   *
   * <p>The type is usually fixed for the implementating class.</p>
   *
   * @return the type of facet.
   */
  public abstract String getType();

  /**
   * Indicates if the facet was computed in a "flexible" way
   */
  public boolean isFlexible() {
    return this.flexible;
  }

}

package org.pageseeder.flint.lucene.facet;

import org.apache.lucene.document.DateTools;
import org.pageseeder.flint.lucene.util.Bucket;
import org.pageseeder.flint.lucene.util.Dates;
import org.pageseeder.xmlwriter.XMLWritable;

import java.text.ParseException;

/**
 * Base class for all flexible facets implementations.
 *
 * @author Christophe Lauret
 *
 * @version 5.1.3
 */
public abstract class FlexibleFacet<T> implements XMLWritable {

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
   * @return the name of the field.
   */
  public String name() {
    return this._name;
  }

  /**
   * Indicates if the facet was computed in a "flexible" way.
   */
  public boolean isFlexible() {
    return this.flexible;
  }

  /**
   * The type of facet.
   *
   * <p>The type is usually fixed for the implementing class.
   *
   * @return the type of facet.
   */
  public abstract String getType();

  /**
   * The type of facet.
   *
   * <p>The type is usually fixed for the implementing class.
   *
   * @return the type of facet.
   */
  public abstract Bucket<T> getValues();

  final static String toDateString(String date, DateTools.Resolution resolution) {
    try {
      return Dates.format(DateTools.stringToDate(date), resolution);
    } catch (ParseException ex) {
      // should not happen as the string is coming from the date formatter in the first place
      throw new IllegalArgumentException(ex);
    }
  }
}

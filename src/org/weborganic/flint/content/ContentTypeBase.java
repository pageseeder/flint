package org.weborganic.flint.content;

import org.weborganic.flint.util.Beta;

/**
 * A base implementation for content types.
 * 
 * @author Christophe Lauret
 * @version 28 July 2010
 */
@Beta public class ContentTypeBase {

  /**
   * The name of this content type (immutable);
   */
  private final String _name;

  /**
   * A hashcode value for faster retrieval.
   */
  private final int _hashCode;

  /**
   * Creates a new content type with the given name. 
   * 
   * @param name 
   */
  public ContentTypeBase(String name) {
    this._name = name;
    this._hashCode = name.hashCode(); 
  }

  /**
   * Returns a pre-computed hashcode value based on the name.
   * 
   * {@inheritDoc}
   */
  @Override
  public final int hashCode() {
    return this._hashCode;
  }

  @Override
  public final boolean equals(Object o) {
    if (o == this) return true;
    if (o instanceof ContentTypeBase)
      return this._name.equals(((ContentTypeBase)o)._name);
    return false;
  }

  /**
   * The name of this content type.
   * 
   * @return the name of this content type.
   */
  public final String name() {
    return this._name;
  }

  @Override
  public final String toString() {
    return this._name;
  }
}

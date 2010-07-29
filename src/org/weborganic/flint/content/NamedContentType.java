package org.weborganic.flint.content;

import org.weborganic.flint.util.Beta;

/**
 * A basic implementation of a content type with a name.
 * 
 * <p>Two content types with the same name are considered equal.
 * 
 * <p>The string are interned to enabled strict equality comparison.
 * 
 * @author Christophe Lauret
 * @version 29 July 2010
 */
@Beta public final class NamedContentType implements ContentType {

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
  public NamedContentType(String name) {
    this._name = name.intern();
    this._hashCode = this._name.hashCode(); 
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

  /**
   * {@inheritDoc}
   */
  @Override
  public final boolean equals(Object o) {
    if (o instanceof NamedContentType)
      return this.equals((NamedContentType)o);
    return false;
  }

  /**
   * Compares two named content types for equality.
   * 
   * @param type The type of equality.
   * @return <code>true</code> if the two content types are equal;
   *         <code>false</code> otherwise.
   */
  public final boolean equals(NamedContentType type) {
    if (type == this) return true;
    if (this._name == type._name) return true;
    return this._name.equals(type._name);
  }

  /**
   * The name of this content type.
   * 
   * @return the name of this content type.
   */
  public String name() {
    return this._name;
  }

  @Override
  public String toString() {
    return this._name;
  }
}

/*
 * Copyright 2015 Allette Systems (Australia)
 * http://www.allette.com.au
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pageseeder.flint.content;

import org.pageseeder.flint.api.ContentType;
import org.pageseeder.flint.util.Beta;

/**
 * A basic immutable implementation of a content type with a name.
 *
 * <p>Two content types with the same name are considered equal.
 *
 * <p>The name string can be interned to enable strict equality comparison.
 *
 * @author Christophe Lauret
 * @version 27 February 2013
 */
@Beta public final class NamedContentType implements ContentType {

  /**
   * A default content type instance for when no more than one type is needed by an application.
   */
  public static final NamedContentType DEFAULT = new NamedContentType("default");

  /**
   * The name of this content type (immutable);
   */
  private final String _name;

  /**
   * A hashcode value for faster retrieval.
   */
  private final int _hashCode;

  /**
   * Creates a new content type with the given name interning the string automatically.
   *
   * <p>Warning: this is useful only
   *
   * @param name The name of this content type.
   */
  public NamedContentType(String name) {
    this._name = name.intern();
    this._hashCode = this._name.hashCode();
  }

  /**
   * Creates a new content type with the given name.
   *
   * <p>If the application uses many content types, it is best NOT to intern the string.
   *
   * @param name   The name of the content type.
   * @param intern <code>true</code> to intern the string; <code>false</code> otherwise.
   */
  public NamedContentType(String name, boolean intern) {
    this._name = intern? name.intern() : name;
    this._hashCode = this._name.hashCode();
  }

  /**
   * Returns a pre-computed hashcode value based on the name.
   *
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return this._hashCode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    // filter out objects of the wrong type
    if (!(o instanceof NamedContentType)) return false;
    return this.equals((NamedContentType)o);
  }

  /**
   * Compares two named content types for equality.
   *
   * @param type The type of equality.
   * @return <code>true</code> if the two content types are equal;
   *         <code>false</code> otherwise.
   */
  public boolean equals(NamedContentType type) {
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

  /**
   * Returns the name of this content type.
   *
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return this._name;
  }
}

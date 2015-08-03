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
package org.pageseeder.flint.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * The bucket is a list of items with a fixed size ordered by count.
 *
 * <p>The bucket has a limited capacity.
 *
 * @param <T> The type of objects in this bucket.
 *
 * @author Christophe Lauret
 * @version 16 February 2012
 */
@Beta public final class Bucket<T> implements Iterable<T> {

  /**
   * The size of the bucket.
   */
  private final int _capacity;

  /**
   * The sorted set of entries.
   */
  private final SortedSet<Entry<T>> _entries;

  /**
   * Whether to include entries which count is zero.
   */
  private final boolean _acceptZero;

  /**
   * The minimum count to be included in the bucket.
   */
  private transient int minCount = 1;

  /**
   * Indicates the number of entries that have been considered.
   */
  private int _considered = 0;

  /**
   * Creates a new bucket.
   *
   * @param capacity The capacity of this bucket.
   *
   * @throws IllegalArgumentException If the capacity is < 0.
   */
  public Bucket(int capacity) throws IllegalArgumentException {
   this._capacity = capacity;
   this._entries = new TreeSet<Entry<T>>();
   this._acceptZero = false;
  }

  /**
   * Creates a new bucket.
   *
   * @param capacity   The capacity of this bucket.
   * @param acceptZero <code>true</code> to accept item counts equal to zero;
   *                   <code>false</code> to only accept item counts greater than zero.
   *
   * @throws IllegalArgumentException If the capacity is < 0.
   */
  public Bucket(int capacity, boolean acceptZero) throws IllegalArgumentException {
   this._capacity = capacity;
   this._entries = new TreeSet<Entry<T>>();
   if (acceptZero) { this.minCount = 0; }
   this._acceptZero = acceptZero;
  }

  /**
   * Indicates whether this bucket contains any item.
   *
   * @return <code>true</code> if the size of this bucket is 0; <code>false</code> otherwise.
   */
  public boolean isEmpty() {
    return this._entries.isEmpty();
  }

  /**
   * Indicates whether this bucket has reached its capacity.
   *
   * @return <code>true</code> if the size of this bucket is capacity; <code>false</code> otherwise.
   */
  public boolean isFull() {
    return this._entries.size() == this._capacity;
  }

  /**
   * Adds an object to this bucket.
   *
   * @param item  A new Item
   * @param count Its cardinality
   */
  public void add(T item, int count) {
    if (count > 0 || this._acceptZero) {
      this._considered++;
    }
    if (count >= this.minCount) {
      this._entries.add(new Entry<T>(item, count));
      if (this._entries.size() > this._capacity) {
        this._entries.remove(this._entries.last());
        this.minCount = this._entries.last().count();
      }
    }
  }

  /**
   * Return the count for the specified item.
   *
   * @param item the item
   * @return the count for the item; 0 if not found.
   */
  public int count(T item) {
    for (Entry<T> e : this._entries) {
      if (e._item.equals(item)) return e.count();
    }
    return 0;
  }

  /**
   * Returns an iterator over the items in the bucket.
   *
   * @return an iterator over the items in the bucket.
   */
  @Override
  public Iterator<T> iterator() {
    return this.items().iterator();
  }

  /**
   * Returns a unmodifiable set of entries in the bucket.
   *
   * @return a unmodifiable set of entries in the bucket.
   */
  public Set<Entry<T>> entrySet() {
    return Collections.unmodifiableSet(this._entries);
  }

  /**
   * Returns the list of items.
   *
   * @return the list of items.
   */
  public List<T> items() {
    List<T> items = new ArrayList<T>(this._entries.size());
    for (Entry<T> e : this._entries) { items.add(e.item()); }
    return items;
  }

  /**
   * Returns the capacity of this bucket.
   *
   * @return the capacity of this bucket.
   */
  public int capacity() {
    return this._capacity;
  }

  /**
   * Returns the number of items that have been considered for inclusion in this bucket.
   *
   * <p>The value returned by this method is always a positive integer. This method effectively
   * counts the number of times the {@link #add(Object, int)} method has been invoked.
   *
   * <p>This method is useful to know whether many items were missing from the facet or not.
   *
   * @return The number of items that have been considered for inclusion in this bucket.
   */
  public int getConsidered() {
    return this._considered;
  }

  /**
   * An Item-Count pair used as an entry for the bucket.
   *
   * @author Christophe Lauret
   *
   * @param <T> the type of object being counted.
   */
  public static final class Entry<T> implements Comparable<Entry<T>> {

    /**
     * The item itself.
     */
    private final T _item;

    /**
     * The item count.
     */
    private final int _count;

    /**
     * Creates a new item count the fields are final.
     *
     * @param item  The term.
     * @param count Its occurrence.
     */
    public Entry(T item, int count) {
      this._item = item;
      this._count = count;
    }

    /**
     * @return the wrapped item.
     */
    public T item() {
      return this._item;
    }

    /**
     * @return the wrapped item count.
     */
    public int count() {
      return this._count;
    }

    /**
     * {@inheritDoc}
     */
    @Override public int hashCode() {
      return this._count * 19 + this._item.hashCode() + 11;
    }

    /**
     * {@inheritDoc}
     */
    @Override public boolean equals(Object o) {
      if (o instanceof Entry<?>) return this.equals((Entry<?>)o);
      return false;
    }

    /**
     * Two {@link Entry} instances are equals only if they have the same count.
     *
     * <p>If the both items are comparable, they also need to be equal.
     *
     * @param o the item count to compare with.
     * @return <code>true</code> if equal; <code>false</code> otherwise.
     */
    public boolean equals(Entry<?> o) {
      //
      if (this._count != o._count) return false;
      return comparable(this._item, o._item)? this._item.equals(o._item) : true;
    }

    /**
     * Compare the using the item count.
     *
     * <p>If both entries have the same count, and the items are comparable, this method returns
     * the result of their comparison.
     *
     * @param e the entry to compare.
     * @return the result of comparison.
     */
    @Override
    @SuppressWarnings("unchecked")
    public int compareTo(Entry<T> e) {
      int c = e._count - this._count;
      if (c != 0) return c;
      return comparable(this._item, e._item)? ((Comparable<T>)this._item).compareTo(e._item) : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override public String toString() {
      return this._item.toString() + ':' + this._count;
    }

    /**
     * Indicates whether both objects passed as arguments are comparable.
     * @param o1 First object to compare.
     * @param o2 First object to compare.
     * @return <code>true</code> if both implement the {@link Comparable} interface; <code>false</code> otherwise.
     */
    private static boolean comparable(Object o1, Object o2) {
      return o1 instanceof Comparable<?> && o2 instanceof Comparable<?>;
    }

  }

}

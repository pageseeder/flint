package org.weborganic.flint.util;

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
 * @version 28 July 2010
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
   * The minimum count to be included in the bucket.
   */
  private transient int minCount = 0;

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
      if (o instanceof Entry<?>) {
        return this.equals((Entry<?>)o);
      }
      return false;
    }

    /**
     * Two {@link Entry} are equals only if the item and the count are equal.
     * 
     * @param o the item count to compare with.
     * @return <code>true</code> if equal; <code>false</code> otherwise.
     */
    public boolean equals(Entry<?> o) {
      return this._count == o._count && this._item.equals(o._item);
    }

    /**
     * Compare the using the frequency.
     * 
     * @param e the entry to compare.
     * @return the result of comparison.
     */
    public int compareTo(Entry<T> e) {
      return e._count - this._count;
    }

    /**
     * {@inheritDoc}
     */
    @Override public String toString() {
      return this._item.toString() + ':' + this._count;
    }

  }

}

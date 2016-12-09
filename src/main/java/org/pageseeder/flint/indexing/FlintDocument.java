package org.pageseeder.flint.indexing;

import java.util.ArrayList;
import java.util.List;
/**
 * Represent a lucene document.
 * 
 * @author Jean-Baptiste Reure
 */
public class FlintDocument {

  /**
   * The list of fields.
   */
  private final List<FlintField> _fields = new ArrayList<>();

  /**
   * Add a new field.
   * 
   * @param field the new field.
   */
  public void add(FlintField field) {
    this._fields.add(field);
  }

  /**
   * @return <code>true</code> if there are no fields in this document.
   */
  public boolean isEmpty() {
    return this._fields.isEmpty();
  }

  /**
   * @return the list of fields.
   */
  public List<FlintField> fields() {
    return this._fields;
  }

  /**
   * Remove all fields with the given name.
   * 
   * @param name the name of the fields to remove.
   */
  public void removeFields(String name) {
    if (name == null) return;
    List<FlintField> toremove = new ArrayList<>();
    for (FlintField field : this._fields) {
      if (name.equals(field.name())) {
        toremove.add(field);
      }
    }
    this._fields.removeAll(toremove);
  }

}

package org.pageseeder.flint.util;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.BytesRef;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexIO;

public class DocValuesUpdater {

  private final IndexIO _io;
  private final Term _term;

  private String error = null;
  public DocValuesUpdater(IndexIO io, Term term) {
    this._io = io;
    this._term = term;
  }

  public boolean updateSorted(String field, long value) {
    return update(new SortedNumericDocValuesField(field, value));
  }

  public boolean updateSorted(String field, String value) {
    return update(new SortedDocValuesField(field, new BytesRef(value)));
  }

  public String getError() {
    return this.error;
  }

  public boolean update(Field field) {
    this.error = null;
    try {
      this._io.updateDocValues(this._term, field);
      return true;
    } catch (IndexException ex) {
      this.error = ex.getMessage();
      return false;
    }
  }
}

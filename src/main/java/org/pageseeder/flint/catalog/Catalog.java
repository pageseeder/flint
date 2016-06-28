package org.pageseeder.flint.catalog;

import java.io.IOException;
import java.util.HashMap;

import org.apache.lucene.document.FieldType.NumericType;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;
import org.pageseeder.flint.index.FieldBuilder;
import org.pageseeder.xmlwriter.XMLWritable;
import org.pageseeder.xmlwriter.XMLWriter;

public class Catalog implements XMLWritable {

  private final HashMap<String, CatalogEntry> _fields = new HashMap<>();

  private final String _name;

  public Catalog(String name) {
    this._name = name;
  }

  public String name() {
    return this._name;
  }

  public void addFieldType(FieldBuilder builder) {
    // ignore non-indexed fields
    if (builder.index() == IndexOptions.NONE) return;
    synchronized (this._fields) {
      CatalogEntry existing = this._fields.get(builder.name());
      if (existing == null || !existing.equals(new CatalogEntry(builder, false)))
        this._fields.put(builder.name(), new CatalogEntry(builder, existing != null));
    }
  }

  public void addFieldType(boolean stored, String name, boolean tokenized, DocValuesType dt, NumericType num, float boost) {
    synchronized (this._fields) {
      CatalogEntry newone = new CatalogEntry(stored, dt, tokenized, boost, num, false);
      CatalogEntry existing = this._fields.get(name);
      if (existing == null || !existing.equals(newone))
        this._fields.put(name, new CatalogEntry(stored, dt, tokenized, boost, num, existing != null));
    }
  }

  public NumericType getNumericType(String fieldname) {
    CatalogEntry entry = this._fields.get(fieldname);
    return entry == null ? null : entry.num;
  }

  public boolean isTokenized(String fieldname) {
    CatalogEntry entry = this._fields.get(fieldname);
    return entry != null && entry.tokenized;
  }

  public DocValuesType getDocValuesType(String fieldname) {
    CatalogEntry entry = this._fields.get(fieldname);
    return entry == null ? null : entry.docValues;
  }

  public void clear() {
    synchronized (this._fields) {
      this._fields.clear();
    }
  }
  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("catalog");
    xml.attribute("name", this._name);
    for (String fname : this._fields.keySet()) {
      xml.openElement("field");
      xml.attribute("name", fname);
      CatalogEntry entry = this._fields.get(fname);
      xml.attribute("stored",     String.valueOf(entry.stored));
      xml.attribute("tokenized",  String.valueOf(entry.tokenized));
      if (entry.num != null)
        xml.attribute("numeric-type", entry.num.name().toLowerCase());
      if (entry.docValues != null)
        xml.attribute("doc-values", entry.docValues == DocValuesType.SORTED_SET ? "sorted-set" :
                                    entry.docValues == DocValuesType.SORTED ||
                                    entry.docValues == DocValuesType.SORTED_NUMERIC ? "sorted" : "none");
      if (entry.boost != 1.0)
        xml.attribute("boost", String.valueOf(entry.boost));
      if (entry.error) xml.attribute("error", "true");
      xml.closeElement();
    }
    xml.closeElement();
  }

  public static class CatalogEntry {
    private final boolean tokenized;
    private final boolean error;
    private final boolean stored;
    private final DocValuesType docValues;
    private final NumericType num;
    private final float boost;
    public CatalogEntry(boolean s, DocValuesType dv, boolean t, float b, NumericType n, boolean e) {
      this.stored = s;
      this.docValues = dv;
      this.tokenized = t;
      this.boost = b;
      this.num = n;
      this.error = e;
    }
    public CatalogEntry(FieldBuilder builder, boolean err) {
      this.stored = builder.store();
      this.tokenized = builder.tokenize();
      this.boost = builder.boost();
      this.num = builder.numericType();
      this.error = err;
      this.docValues = builder.docValues();
    }
    @Override
    public boolean equals(Object obj) {
      if (obj instanceof CatalogEntry) {
        CatalogEntry entry = (CatalogEntry) obj;
        return this.tokenized == entry.tokenized &&
               this.stored    == entry.stored &&
               this.boost     == entry.boost &&
               this.num       == entry.num &&
               this.docValues == entry.docValues;
      }
      return false;
    }
    @Override
    public int hashCode() {
      return (int) (this.boost * 10000) * 31 +
             (this.num == null ? 13 : this.num.hashCode() * 17) +
             (this.stored    ? 19 : 2) +
             (this.docValues == null ? 23 : this.docValues.hashCode() * 7) +
             (this.tokenized ? 5 : 11);
    }
  }
}

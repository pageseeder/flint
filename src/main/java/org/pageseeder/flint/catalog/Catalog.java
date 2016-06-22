package org.pageseeder.flint.catalog;

import java.io.IOException;
import java.util.HashMap;

import org.apache.lucene.document.FieldType.NumericType;
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
    synchronized (this._fields) {
      CatalogEntry existing = this._fields.get(builder.name());
      if (existing == null || !existing.equals(new CatalogEntry(builder, false)))
        this._fields.put(builder.name(), new CatalogEntry(builder, existing != null));
    }
  }

  public void addFieldType(boolean stored, boolean indexed, String name, boolean tokenized, NumericType num, float boost) {
    synchronized (this._fields) {
      CatalogEntry newone = new CatalogEntry(stored, indexed, tokenized, boost, num, false);
      CatalogEntry existing = this._fields.get(name);
      if (existing == null || !existing.equals(newone))
        this._fields.put(name, new CatalogEntry(stored, indexed, tokenized, boost, num, existing != null));
    }
  }

  public NumericType getSearchNumericType(String fieldname) {
    CatalogEntry entry = this._fields.get(fieldname);
    return entry == null || !entry.indexed ? null : entry.num;
  }

  public boolean isTokenizedForSearch(String fieldname) {
    CatalogEntry entry = this._fields.get(fieldname);
    return entry == null || !entry.indexed ? false : entry.tokenized;
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
      xml.attribute("stored",    String.valueOf(entry.stored));
      xml.attribute("indexed",   String.valueOf(entry.indexed));
      xml.attribute("tokenized", String.valueOf(entry.tokenized));
      if (entry.num != null)
        xml.attribute("numeric-type", String.valueOf(entry.num.name().toLowerCase()));
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
    private final boolean indexed;
    private final NumericType num;
    private final float boost;
    public CatalogEntry(boolean s, boolean i, boolean t, float b, NumericType n, boolean e) {
      this.stored = s;
      this.indexed = i;
      this.tokenized = t;
      this.boost = b;
      this.num = n;
      this.error = e;
    }
    public CatalogEntry(FieldBuilder builder, boolean err) {
      this.stored = builder.store();
      this.indexed = builder.index() != IndexOptions.NONE;
      this.tokenized = builder.tokenize();
      this.boost = builder.boost();
      this.num = builder.numericType();
      this.error = err;
    }
    @Override
    public boolean equals(Object obj) {
      if (obj instanceof CatalogEntry) {
        CatalogEntry entry = (CatalogEntry) obj;
        return this.tokenized == entry.tokenized &&
               this.indexed   == entry.indexed &&
               this.stored    == entry.stored &&
               this.boost     == entry.boost &&
               this.num       == entry.num;
      }
      return false;
    }
    @Override
    public int hashCode() {
      return (int) (this.boost * 10000) * 31 +
             this.num.hashCode() * 7 +
             (this.stored    ? 13 : 2) +
             (this.indexed   ? 17 : 23) +
             (this.tokenized ? 5 : 11);
    }
  }
}

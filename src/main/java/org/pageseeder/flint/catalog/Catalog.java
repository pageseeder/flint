package org.pageseeder.flint.catalog;

import java.io.IOException;
import java.util.HashMap;

import org.apache.lucene.document.FieldType.NumericType;
import org.apache.lucene.index.IndexOptions;
import org.pageseeder.flint.index.FieldBuilder;
import org.pageseeder.xmlwriter.XMLWritable;
import org.pageseeder.xmlwriter.XMLWriter;

public class Catalog implements XMLWritable {

  private final HashMap<String, CatalogEntry> _stored = new HashMap<>();

  private final HashMap<String, CatalogEntry> _indexed = new HashMap<>();

  private final String _name;

  public Catalog(String name) {
    this._name = name;
  }

  public String name() {
    return this._name;
  }

  public void addFieldType(FieldBuilder builder) {
    if (builder.store()) {
      this._stored.put(builder.name(), new CatalogEntry(builder, this._stored.containsKey(builder.name())));
    }
    if (builder.index() != IndexOptions.NONE) {
      this._indexed.put(builder.name(), new CatalogEntry(builder, this._indexed.containsKey(builder.name())));
    }
  }

  public void addFieldType(boolean stored, String name, boolean tokenized, NumericType num, float boost) {
    if (stored) {
      this._stored.put(name, new CatalogEntry(tokenized, boost, num, false));
    } else {
      this._indexed.put(name, new CatalogEntry(tokenized, boost, num, false));
    }
  }

  public NumericType getSearchNumericType(String fieldname) {
    CatalogEntry entry = this._indexed.get(fieldname);
    return entry == null ? null : entry.num;
  }

  public boolean isTokenizedForSearch(String fieldname) {
    CatalogEntry entry = this._indexed.get(fieldname);
    return entry == null ? false : entry.tokenized;
  }

  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("catalog");
    // indexed fields first
    xml.openElement("fields");
    xml.attribute("type", "indexed");
    toXML(this._indexed, xml);
    xml.closeElement();
    // stored fields first
    xml.openElement("fields");
    xml.attribute("type", "stored");
    toXML(this._stored, xml);
    xml.closeElement();
    xml.closeElement();
  }

  private void toXML(HashMap<String, CatalogEntry> fields, XMLWriter xml) throws IOException {
    for (String fname : fields.keySet()) {
      xml.openElement("field");
      xml.attribute("name", fname);
      CatalogEntry entry = fields.get(fname);
      xml.attribute("tokenized", String.valueOf(entry.tokenized));
      if (entry.num != null)
        xml.attribute("numeric-type", String.valueOf(entry.num.name().toLowerCase()));
      if (entry.boost != 1.0)
        xml.attribute("boost", String.valueOf(entry.boost));
      if (entry.error) xml.attribute("error", "true");
      xml.closeElement();
    }
  }
  public static class CatalogEntry {
    private final boolean tokenized;
    private final boolean error;
    private final NumericType num;
    private final float boost;
    public CatalogEntry(boolean t, float b, NumericType n, boolean e) {
      this.tokenized = t;
      this.boost = b;
      this.num = n;
      this.error = e;
    }
    public CatalogEntry(FieldBuilder builder, boolean err) {
      this.tokenized = builder.tokenize();
      this.boost = builder.boost();
      this.num = builder.numericType();
      this.error = err;
    }
  }
}

package org.pageseeder.flint.lucene;

import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.util.BytesRef;
import org.pageseeder.flint.catalog.Catalogs;
import org.pageseeder.flint.indexing.FlintDocument;
import org.pageseeder.flint.indexing.FlintField;
import org.pageseeder.flint.lucene.util.Dates;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class FlintDocumentConverter {

  private final Map<String, String> warnings = new HashMap<>();

  public boolean hasWarnings() {
    return !this.warnings.isEmpty();
  }

  public Collection<String> fieldsWithWarnings() {
    return this.warnings.keySet();
  }

  public String getWarning(String field) {
    return this.warnings.get(field);
  }

  public List<Document> convert(List<FlintDocument> fdocs) {
    Map<String, FlintField> forCatalog = new HashMap<>();
    List<Document> docs = new ArrayList<>();
    for (FlintDocument fdoc : fdocs) {
      Document doc = new Document();
      for (FlintField field : fdoc.fields()) {
        // check catalog first
        if (Catalogs.updateField(field)) {
          this.warnings.put(field.name(), "field has been updated because of a different definition in the catalog");
        }
        List<Field> thefields = toFields(field, forCatalog);

        if (thefields != null) {
          for (Field thefield : thefields)
            doc.add(thefield);
        } else {
          this.warnings.put(field.name(), "field is ignored because it is invalid");
        }
      }
      // add fields to catalog
      for (FlintField ff : forCatalog.values()) {
        if (ff.catalog() != null) Catalogs.newField(ff.catalog(), ff);
      }
      docs.add(doc);
    }
    return docs;
  }

  private List<Field> toFields(FlintField ffield, Map<String, FlintField> forCatalog) {
    if (ffield.name() == null)
      throw new IllegalStateException("Unable to build field, field name not set");
    if (ffield.index() == null)
      throw new IllegalStateException("Unable to build field, field index not set");
    if (ffield.value() == null)
      throw new IllegalStateException("Unable to build field, field value not set");

    List<Field> fields;
    // check if docvalues
    if (ffield.isDocValues()) {
      fields = toDocValuesFields(ffield);
      if (fields != null)
        forCatalog.put(ffield.name(), ffield); // priority over normal fields
    } else {
      // normal field then
      fields = toNormalFields(ffield);
      if (!fields.isEmpty()) {
        Field main = fields.get(0);
        if (main.fieldType() != null &&
            main.fieldType().indexOptions() != IndexOptions.NONE &&
            !forCatalog.containsKey(ffield.name())) // lesser priority
          forCatalog.put(ffield.name(), ffield);
      }
    }
    return fields;
  }

  // ----------------------------------------------------------------------------------------------
  //                                      private helpers
  // ----------------------------------------------------------------------------------------------

  private List<Field> toNormalFields(FlintField ffield) {
    // get value
    String value = ffield.value().toString();
    // compute value, using numeric type
    List<Field> fields = new ArrayList<>();
    if (ffield.numeric() != null) {
      Field field = toDateOrNumericField(ffield);
      if (field != null) fields.add(field);
    } else if (ffield.dateformat() != null) {
      Date date = value.isEmpty() ? null : toDate(ffield.name(), value, ffield.dateformat());
      fields.add(new Field(ffield.name(), date != null ? Dates.toString(date, LuceneUtils.toResolution(ffield.resolution())) : "", toType(ffield)));
    } else {
      fields.add(new Field(ffield.name(), value, toType(ffield)));
    }
    return fields;
  }

  private List<Field> toDocValuesFields(FlintField ffield) {
    // check doc values
    List<Field> fields = new ArrayList<>();
    switch (ffield.docValues()) {
      case FORCED_NONE:
        return null;
      case SORTED_NUMERIC:
        Field field = toDateOrNumericField(ffield);
        if (field != null) fields.add(field);
        break;
      case SORTED:
      case SORTED_SET:
        String name = ffield.name();
        String value;
        BytesRef bytes;
        if (ffield.dateformat() != null) {
          String date = Dates.toString(toDate(name, ffield.value().toString(), ffield.dateformat()), LuceneUtils.toResolution(ffield.resolution()));
          value = date == null ? "" : date;
          bytes = new BytesRef(value);
        } else {
          value = ffield.value().toString();
          bytes = new BytesRef(ffield.value());
        }
        // add field and the doc values equivalent
        fields.add(new Field(name, value, toType(ffield)));
        fields.add(ffield.docValues() == FlintField.DocValuesType.SORTED_SET ?
            new SortedSetDocValuesField(name, bytes) :
            new SortedDocValuesField(name, bytes));
        break;
    }
    return fields;
  }

  private Field toDateOrNumericField(FlintField ffield) {
    // shortcut
    if (ffield.numeric() == null) return null;
    String name = ffield.name();
    String value = ffield.value().toString();
    Field.Store stored = ffield.store() ? Field.Store.YES : Field.Store.NO;
    if (ffield.dateformat() != null) {
      Number date = Dates.toNumber(toDate(name, value, ffield.dateformat()), LuceneUtils.toResolution(ffield.resolution()));
      // only int or long possible for dates
      if (date instanceof Long) return new LongField(name, date.longValue(), stored);
      if (date instanceof Integer) return new IntField(name, date.intValue(), stored);
      this.warnings.put(ffield.name(),"ignoring field as it has a date format but no date");
    } else {
      try {
        switch (ffield.numeric()) {
          case DOUBLE: return new DoubleField(name, Double.parseDouble(value), stored);
          case FLOAT: return new FloatField(name, Float.parseFloat(value), stored);
          case LONG: return new LongField(name, Long.parseLong(value), stored);
          case INT: return new IntField(name, Integer.parseInt(value), stored);
        }
      } catch (NumberFormatException ex) {
        this.warnings.put(ffield.name(),"ignoring number field with invalid value '"+value+"'");
      }
    }
    return null;
  }
  private static FieldType toType(FlintField ffield) {
    FieldType type = new FieldType();
    type.setStored(ffield.store());
    type.setTokenized(ffield.tokenize());
    type.setIndexOptions(toIndexOptions(ffield.index()));
    if (ffield.index() != org.pageseeder.flint.indexing.FlintField.IndexOptions.NONE) {
      type.setOmitNorms(ffield.omitNorms());
      type.setStoreTermVectors(ffield.termVector());
      type.setStoreTermVectorOffsets(ffield.termVectorOffsets());
      type.setStoreTermVectorPositions(ffield.termVectorPositions());
      type.setStoreTermVectorPayloads(ffield.termVectorPayloads());
    }
    return type;
  }
  private static IndexOptions toIndexOptions(org.pageseeder.flint.indexing.FlintField.IndexOptions options) {
    if (options == null) return null;
    switch (options) {
      case NONE                                     : return IndexOptions.NONE;
      case DOCS                                     : return IndexOptions.DOCS;
      case DOCS_AND_FREQS                           : return IndexOptions.DOCS_AND_FREQS;
      case DOCS_AND_FREQS_AND_POSITIONS             : return IndexOptions.DOCS_AND_FREQS_AND_POSITIONS;
      case DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS : return IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS;
    }
    return null;
  }

  /**
   * Return the string value used by Lucene 3 for dates.
   *
   * @param value  The value to turn into a date
   * @param format The date format to parse
   *
   * @return The string value for use by Lucene.
   */
  private Date toDate(String name, String value, SimpleDateFormat format) {
    if (value == null || value.isEmpty()) return null;
    try {
      return format.parse(value);
    } catch (ParseException ex) {
      this.warnings.put(name,"ignoring unparseable date '"+value+"' with format '"+format.toPattern()+"'");
      return null;
    }
  }
}

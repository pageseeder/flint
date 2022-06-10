package org.pageseeder.flint.lucene;

import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;
import org.pageseeder.flint.catalog.Catalogs;
import org.pageseeder.flint.indexing.FlintDocument;
import org.pageseeder.flint.indexing.FlintField;
import org.pageseeder.flint.lucene.util.Dates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;

public class LuceneUtils {

  /**
   * The logger for this class.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(LuceneUtils.class);

  public static List<Document> toDocuments(List<FlintDocument> fdocs, Map<String, String> warnings) {
    Map<String, FlintField> forCatalog = new HashMap<>();
    List<Document> docs = new ArrayList<>();
    for (FlintDocument fdoc : fdocs) {
      Document doc = new Document();
      for (FlintField field : fdoc.fields()) {
        // check catalog first
        if (Catalogs.updateField(field)) {
          warnings.put(field.name(), "field has been updated because of a different definition in the catalog");
        }
        List<Field> thefields = toFields(field, forCatalog);

        if (thefields != null) {
          for (Field thefield : thefields)
            doc.add(thefield);
        } else {
          warnings.put(field.name(), "field is ignored because it is invalid");
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

  private static List<Field> toFields(FlintField ffield, Map<String, FlintField> forCatalog) {
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
      if (fields != null && !fields.isEmpty()) {
        Field main = fields.get(0);
        if (main.fieldType() != null &&
            main.fieldType().indexOptions() != IndexOptions.NONE &&
            !forCatalog.containsKey(ffield.name())) // lesser priority
          forCatalog.put(ffield.name(), ffield);
      }
    }
    return fields;
  }

  public static Resolution toResolution(org.pageseeder.flint.indexing.FlintField.Resolution resolution) {
    if (resolution == null) return null;
    switch (resolution) {
      case DAY         : return Resolution.DAY;
      case HOUR        : return Resolution.HOUR;
      case MILLISECOND : return Resolution.MILLISECOND;
      case MINUTE      : return Resolution.MINUTE;
      case MONTH       : return Resolution.MONTH;
      case SECOND      : return Resolution.SECOND;
      case YEAR        : return Resolution.YEAR;
    }
    return null;
  }

  // ----------------------------------------------------------------------------------------------
  //                                      private helpers
  // ----------------------------------------------------------------------------------------------

  private static List<Field> toNormalFields(FlintField ffield) {
   // construct the field type
    // get value
    String name = ffield.name();
    String value = ffield.value().toString();
    // compute value, using numeric type
    List<Field> fields = new ArrayList<>();
    if (ffield.numeric() != null) {
      // get date number if this is a numeric date
      if (ffield.dateformat() != null) {
        Number date = Dates.toNumber(toDate(value, ffield.dateformat()), toResolution(ffield.resolution()));
        // only int or long possible for dates
        if (date != null && date instanceof Long) {
          fields.add(new LongPoint(ffield.name(), date.longValue()));
        } else if (date != null && date instanceof Integer) {
          fields.add(new IntPoint(ffield.name(), date.intValue()));
        } else {
          LOGGER.warn("Ignoring field {} as it has a date format but no date", ffield.name());
          return null;
        }
        fields.add(new StoredField(name, value));
      } else {
        try {
          Field field = null;
          switch (ffield.numeric()) {
            case FLOAT:
              field = new FloatPoint(ffield.name(), Float.parseFloat(value));
              break;
            case DOUBLE:
              field = new DoublePoint(ffield.name(), Double.parseDouble(value));
              break;
            case INT:
              field = new IntPoint(ffield.name(), Integer.parseInt(value));
              break;
            case LONG:
              field = new LongPoint(ffield.name(), Long.parseLong(value));
              break;
          }
          if (field != null) {
            fields.add(field);
            fields.add(new StoredField(name, value));
          }
        } catch (NumberFormatException ex) {
          LOGGER.error("Number field {} with invalid value {} will be stored as a String", ffield.name(), value);
        }
      }
    } else if (ffield.dateformat() != null) {
      Date date = value.isEmpty() ? null : toDate(value, ffield.dateformat());
      fields.add(new Field(ffield.name(), date != null ? Dates.toString(date, toResolution(ffield.resolution())) : "", toType(ffield)));
    } else {
      fields.add(new Field(ffield.name(), value, toType(ffield)));
    }
    return fields;
  }

  private static List<Field> toDocValuesFields(FlintField ffield) {
    String name = ffield.name();
    String value = ffield.value().toString();
    // check doc values
    List<Field> fields = new ArrayList<>();
    switch (ffield.docValues()) {
      case FORCED_NONE:
        return null;
      case SORTED_NUMERIC:
        if (ffield.numeric() != null && ffield.dateformat() != null) {
          Number date = Dates.toNumber(toDate(value, ffield.dateformat()), toResolution(ffield.resolution()));
          // only int or long possible for dates
          if (date != null && date instanceof Long) {
            long l = Long.parseLong(value);
            fields.add(new LongPoint(name, l));
            fields.add(new SortedNumericDocValuesField(name, l));
          } else if (date != null && date instanceof Integer) {
            int i = Integer.parseInt(value);
            fields.add(new IntPoint(name, i));
            fields.add(new SortedNumericDocValuesField(name, i));
          } else {
            LOGGER.warn("Ignoring field {} as it has a date format but no date", name);
            return null;
          }
        } else if (ffield.numeric() != null) {
          fields.addAll(toSortedNumericFields(ffield));
        }
        fields.add(new StoredField(name, value));
        break;
      case SORTED:
      case SORTED_SET:
        boolean isSortedSet = ffield.docValues() == FlintField.DocValuesType.SORTED_SET;
        if (ffield.dateformat() != null) {
          String date = Dates.toString(toDate(value, ffield.dateformat()), toResolution(ffield.resolution()));
          if (date == null) date = "";
          fields.add(new Field(name, date, toType(ffield)));
          fields.add(isSortedSet ? new SortedSetDocValuesField(name, new BytesRef(date)) : new SortedDocValuesField(name, new BytesRef(date)));
        } else {
          fields.add(new Field(name, value, toType(ffield)));
          fields.add(isSortedSet ? new SortedSetDocValuesField(name, new BytesRef(ffield.value())) : new SortedDocValuesField(name, new BytesRef(ffield.value())));
        }
        break;
    }
    return fields;
  }

  private static List<Field> toSortedNumericFields(FlintField ffield) {
    String name = ffield.name();
    List<Field> fields = new ArrayList<>();
    switch (ffield.numeric()) {
      case DOUBLE:
        double d = Double.parseDouble(ffield.value().toString());
        fields.add(new SortedNumericDocValuesField(name, NumericUtils.doubleToSortableLong(d)));
        fields.add(new DoublePoint(name, d));
        break;
      case FLOAT:
        float f = Float.parseFloat(ffield.value().toString());
        fields.add(new SortedNumericDocValuesField(name, NumericUtils.floatToSortableInt(f)));
        fields.add(new FloatPoint(name, f));
        break;
      case LONG:
        long l = Long.parseLong(ffield.value().toString());
        fields.add(new SortedNumericDocValuesField(name, l));
        fields.add(new LongPoint(name, l));
        break;
      case INT:
        int i = Integer.parseInt(ffield.value().toString());
        fields.add(new SortedNumericDocValuesField(name, i));
        fields.add(new IntPoint(name, i));
        break;
    }
    return fields;
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
  private static Date toDate(String value, DateFormat format) {
    try {
      return format.parse(value);
    } catch (ParseException ex) {
      LOGGER.error("Ignoring unparsable date '{}' with format={}", value, format);
      return null;
    }
  }
}

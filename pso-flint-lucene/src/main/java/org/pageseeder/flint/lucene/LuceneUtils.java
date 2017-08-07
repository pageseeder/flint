package org.pageseeder.flint.lucene;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.CompressionTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.FieldType.NumericType;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;
import org.pageseeder.flint.catalog.Catalogs;
import org.pageseeder.flint.indexing.FlintDocument;
import org.pageseeder.flint.indexing.FlintField;
import org.pageseeder.flint.lucene.util.Dates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LuceneUtils {

  /**
   * The logger for this class.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(LuceneUtils.class);

  public static List<Document> toDocuments(List<FlintDocument> fdocs) {
    Map<String, FlintField> forCatalog = new HashMap<String, FlintField>();
    List<Document> docs = new ArrayList<Document>();
    for (FlintDocument fdoc : fdocs) {
      Document doc = new Document();
      for (FlintField field : fdoc.fields()) {
        Field thefield = toField(field, forCatalog);
        if (thefield != null) {
          doc.add(thefield);
        } else {
          LOGGER.warn("Ignoring invalid field {}", field.name());
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

  private static Field toField(FlintField ffield, Map<String, FlintField> forCatalog) {
    if (ffield.name() == null)
      throw new IllegalStateException("Unable to build field, field name not set");
    if (ffield.index() == null)
      throw new IllegalStateException("Unable to build field, field index not set");
    if (ffield.value() == null)
      throw new IllegalStateException("Unable to build field, field value not set");

    Field field;
    // check if compressed first
    if (ffield.compressed()) {
      field = toCompressedField(ffield);
      if (field != null) forCatalog.put(ffield.name(), ffield); // priority over normal fields
    }
    // check if docvalues next
    else if (ffield.isDocValues()) {
      field = toDocValuesField(ffield);
      if (field != null) forCatalog.put(ffield.name(), ffield); // priority over normal fields
    }
    // normal field then
    else {
      field = toNormalField(ffield);
      if (field != null && field.fieldType() != null &&
          field.fieldType().indexOptions() != IndexOptions.NONE &&
          !forCatalog.containsKey(ffield.name())) // lesser priority
        forCatalog.put(ffield.name(), ffield);
    }
    if (field != null) {
      // Sets the boost if necessary
      if (ffield.boost() != FlintField.DEFAULT_BOOST_VALUE) {
        field.setBoost(ffield.boost());
      }
    }
    return field;
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

  private static Field toNormalField(FlintField ffield) {
 // construct the field type
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
    // get value
    String value = ffield.value().toString();
    // compute value, using numeric type
    Field field = null;
    if (ffield.numeric() != null) {
      type.setNumericType(toNumericType(ffield.numeric()));
      // get date number if this is a numeric date
      if (ffield.dateformat() != null) {
        Number date = Dates.toNumber(toDate(value, ffield.dateformat()), toResolution(ffield.resolution()));
        // only int or long possible for dates
        if (date != null && date instanceof Long) {
          field = new LongField(ffield.name(), (Long) date, type);
          type.setNumericPrecisionStep(ffield.precisionStep() != null ? ffield.precisionStep() : NumericUtils.PRECISION_STEP_DEFAULT);
        } else if (date != null && date instanceof Integer) {
          field = new IntField(ffield.name(), (Integer) date, type);
          type.setNumericPrecisionStep(ffield.precisionStep() != null ? ffield.precisionStep() : NumericUtils.PRECISION_STEP_DEFAULT_32);
        } else {
          LOGGER.warn("Ignoring field {} as it has a date format but no date", ffield.name());
          return null;
        }
      } else {
        try {
          switch (ffield.numeric()) {
            case FLOAT:
              type.setNumericPrecisionStep(ffield.precisionStep() != null ? ffield.precisionStep() : NumericUtils.PRECISION_STEP_DEFAULT_32);
              field = new FloatField(ffield.name(), Float.parseFloat(value), type);
              break;
            case DOUBLE:
              type.setNumericPrecisionStep(ffield.precisionStep() != null ? ffield.precisionStep() : NumericUtils.PRECISION_STEP_DEFAULT);
              field = new DoubleField(ffield.name(), Double.parseDouble(value), type);
              break;
            case INT:
              type.setNumericPrecisionStep(ffield.precisionStep() != null ? ffield.precisionStep() : NumericUtils.PRECISION_STEP_DEFAULT_32);
              field = new IntField(ffield.name(), Integer.parseInt(value), type);
              break;
            case LONG:
              type.setNumericPrecisionStep(ffield.precisionStep() != null ? ffield.precisionStep() : NumericUtils.PRECISION_STEP_DEFAULT);
              field = new LongField(ffield.name(), Long.parseLong(value), type);
              break;
          }
        } catch (NumberFormatException ex) {
          LOGGER.error("Number field {} with invalid value {} will be stored as a String", ffield.name(), value);
        }
      }
    } else if (ffield.dateformat() != null) {
      Date date = toDate(value, ffield.dateformat());
      field = new Field(ffield.name(), date != null ? Dates.toString(date, toResolution(ffield.resolution())) : "", type);
    } else {
      field = new Field(ffield.name(), value, type);
    }
    return field;
  }
  private static Field toDocValuesField(FlintField ffield) {
    Field field = null;
    // check doc values
    switch (ffield.docValues()) {
      case FORCED_NONE:
        return null;
      case SORTED_SET:
        field = new SortedSetDocValuesField(ffield.name(), new BytesRef(ffield.value()));
        break;
      case SORTED_NUMERIC:
        if (ffield.numeric() != null && ffield.dateformat() != null) {
          Number date = Dates.toNumber(toDate(ffield.value().toString(), ffield.dateformat()), toResolution(ffield.resolution()));
          // only int or long possible for dates
          if (date != null && date instanceof Long) {
            field = new SortedNumericDocValuesField(ffield.name(), (Long) date);
          } else if (date != null && date instanceof Integer) {
            field = new SortedNumericDocValuesField(ffield.name(), (Integer) date);
          } else {
            LOGGER.warn("Ignoring field {} as it has a date format but no date", ffield.name());
          }
          break;
        } else if (ffield.numeric() != null) {
          switch (ffield.numeric()) {
            case DOUBLE:
              field = new SortedNumericDocValuesField(ffield.name(), NumericUtils.doubleToSortableLong(Double.parseDouble(ffield.value().toString())));
            case FLOAT:
              field = new SortedNumericDocValuesField(ffield.name(), NumericUtils.floatToSortableInt(Float.parseFloat(ffield.value().toString())));
            case LONG:
              field = new SortedNumericDocValuesField(ffield.name(), Long.parseLong(ffield.value().toString()));
            case INT:
              field = new SortedNumericDocValuesField(ffield.name(), Integer.parseInt(ffield.value().toString()));
          }
          break;
        }
      case SORTED:
        field = new SortedDocValuesField(ffield.name(), new BytesRef(ffield.value()));
        break;
      default:
        if (ffield.numeric() != null && ffield.dateformat() == null) {
          switch (ffield.numeric()) {
            case DOUBLE:
              field = new NumericDocValuesField(ffield.name(), NumericUtils.doubleToSortableLong(Double.parseDouble(ffield.value().toString())));
            case FLOAT:
              field = new NumericDocValuesField(ffield.name(), NumericUtils.floatToSortableInt(Float.parseFloat(ffield.value().toString())));
            case LONG:
              field = new NumericDocValuesField(ffield.name(), Long.parseLong(ffield.value().toString()));
            case INT:
              field = new NumericDocValuesField(ffield.name(), Integer.parseInt(ffield.value().toString()));
          }
        }
    }
    return field;
  }

  private static Field toCompressedField(FlintField ffield) {
    // Generate a compressed field
    byte[] value = CompressionTools.compressString(ffield.value().toString());
    FieldType type = new FieldType();
    type.setStored(true);
    return new Field(ffield.name(), value, type);
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

  private static NumericType toNumericType(org.pageseeder.flint.indexing.FlintField.NumericType numeric) {
    if (numeric == null) return null;
    switch (numeric) {
      case INT    : return NumericType.INT;
      case FLOAT  : return NumericType.FLOAT;
      case DOUBLE : return NumericType.DOUBLE;
      case LONG   : return NumericType.LONG;
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
      return format.parse(value.toString());
    } catch (ParseException ex) {
      LOGGER.error("Ignoring unparsable date '{}' with format={}", value, format);
      return null;
    }
  }
}

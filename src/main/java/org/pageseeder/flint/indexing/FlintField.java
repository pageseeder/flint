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
package org.pageseeder.flint.indexing;

import org.pageseeder.flint.catalog.Catalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * A builder for fields.
 *
 * <p>This class can be used to temporarily hold values required to creates new fields and
 * ensure that the field can be build without errors.
 *
 * @author Christophe Lauret
 * @version 10 February 2012
 */
public final class FlintField {

  /**
   * Use the GMT time zone.
   */
  private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

  /**
   * The default boost value for the term.
   */
  public static enum NumericType {
    INT, FLOAT, DOUBLE, LONG;
  };

  public static enum DocValuesType {
    NONE, FORCED_NONE, SORTED, SORTED_NUMERIC, SORTED_SET;
  }

  public static enum IndexOptions {
    NONE, DOCS, DOCS_AND_FREQS, DOCS_AND_FREQS_AND_POSITIONS, DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS,
  }

  public static enum Resolution {
    YEAR, MONTH, DAY, HOUR, MINUTE, SECOND, MILLISECOND;
  }

  /**
   * The logger for this class.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(FlintField.class);

  /**
   * Name of the catalog this field will be added to.
   */
  private final String _catalog;

  /**
   * The name of the field currently processed.
   */
  private String _name;

  /**
   * The 'store' flag of the field to build.
   */
  private boolean _store = true;

  /**
   * The 'tokenize' flag of the field to build.
   */
  private boolean _tokenize = true;

  /**
   * The 'sorted' flag of the field to build.
   */
  private DocValuesType _docValues = DocValuesType.NONE;

  /**
   * The 'index' attribute of the field to build.
   */
  private IndexOptions _index = IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS;

  /**
   * If norms are omitted.
   */
  private boolean _omitNorms = false;

  /**
   * The vector flag of the field to build.
   */
  private boolean _vector = true;

  /**
   * The vector positions of the field to build.
   */
  private boolean _vectorPositions = true;

  /**
   * The vector payloads of the field to build.
   */
  private boolean _vectorPayloads = true;

  /**
   * The vector offsets of the field to build.
   */
  private boolean _vectorOffsets = true;

  /**
   * Date format to use (only if the value is a date)
   */
  private SimpleDateFormat _dateformat;

  /**
   * The 'resolution' attribute of the field currently processed - determines the granularity of date stored.
   */
  private Resolution _resolution;

  /**
   * The value of the field currently processed.
   */
  private CharSequence _value;

  /**
   * The class of a the number type for a numeric field.
   */
  private NumericType _numeric;

  /**
   * The precision step to use for numeric field.
   */
  private Integer _precisionStep = null;

  /**
   * If the field's value is compressed.
   */
  private boolean _compressed = false;

  // Constuctor
  // ----------------------------------------------------------------------------------------------

  /**
   * @param catalog the name of the catalog this field will be added to.
   */
  public FlintField(String catalog) {
    this._catalog = catalog;
  }

  /**
   * @deprecated
   */
  public FlintField cloneCompressed() {
    FlintField compressed = new FlintField(this._catalog);
    compressed.name(this._name);
    if (this._value != null)
      compressed.value(this._value.toString());
    compressed.compressed(true);
    return compressed;
  }

  public FlintField cloneNoDocValues() {
    FlintField cloned = new FlintField(this._catalog);
    cloned._name = this._name;
    cloned._value = this._value;
    cloned._compressed = this._compressed;
    cloned._dateformat = this._dateformat;
    cloned._index = this._index;
    cloned._numeric = this._numeric;
    cloned._omitNorms = this._omitNorms;
    cloned._precisionStep = this._precisionStep;
    cloned._resolution = this._resolution;
    cloned._store = this._store;
    cloned._tokenize = this._tokenize;
    cloned._vector = this._vector;
    cloned._vectorOffsets = this._vectorOffsets;
    cloned._vectorPayloads = this._vectorPayloads;
    cloned._vectorPositions = this._vectorPositions;
    cloned._docValues = DocValuesType.FORCED_NONE;
    return cloned;
  }

  // Setters
  // ----------------------------------------------------------------------------------------------

  /**
   * The name of the field to build.
   *
   * @param name The name of the field to build.
   * @return this builder.
   */
  public FlintField name(String name) {
    this._name = name;
    return this;
  }

  /**
   * The value of the field to build.
   *
   * @param value The value of the field to build.
   * @return this builder.
   */
  public FlintField value(String value) {
    this._value = value;
    return this;
  }

  /**
   * Set whether this field's value is compressed.
   * @deprecated
   *
   * @param compressed If this field's value is compressed.
   * @return this builder.
   */
  public FlintField compressed(boolean compressed) {
    this._compressed = compressed;
    return this;
  }

  /**
   * Set the field store for the field to build.
   *
   * @param store The field store for the field to build.
   * @return this builder.
   */
  public FlintField store(boolean store) {
    this._store = store;
    return this;
  }

  /**
   * Set the field store for the field to build.
   *
   * @param store The field store for the field to build as a string.
   * @return this builder.
   */
  public FlintField store(String store) {
    if (store != null) this._store = Boolean.parseBoolean(store);
    return this;
  }

  /**
   * Set the field tokenize for the field to build.
   *
   * @param tokenize The field tokenize for the field to build.
   * @return this builder.
   */
  public FlintField tokenize(boolean tokenize) {
    this._tokenize = tokenize;
    return this;
  }

  /**
   * Set the field tokenize for the field to build.
   *
   * @param tokenize The field tokenize for the field to build as a string.
   * @return this builder.
   */
  public FlintField tokenize(String tokenize) {
    if (tokenize != null) this._tokenize = Boolean.parseBoolean(tokenize);
    return this;
  }

  /**
   * Set the field index for the field to build.
   *
   * @param index The field index for the field to build.
   * @return this builder.
   */
  public FlintField index(IndexOptions index) {
    this._index = index;
    return this;
  }

  /**
   * Set the omit norms flag.
   *
   * @param omit the new value
   *
   * @return this builder.
   */
  public FlintField omitNorms(boolean omit) {
    this._omitNorms = omit;
    return this;
  }

  /**
   * Set the field index for the field to build.
   *
   * @param index The field index for the field to build as a string.
   * @return this builder.
   */
  public FlintField index(String index) {
    if (index != null) this._index = toIndexOptions(index);
    return this;
  }

  /**
   * @param docValues if the field should be indexed as doc values.
   * @return this builder.
   */
  public FlintField docValues(DocValuesType docValues) {
    this._docValues = docValues == null ? DocValuesType.NONE : docValues;
    return this;
  }

  /**
   * Supported values are "none", "sorted" and "sorted-set".
   * If the parameter is null, the same behaviour as "none" is applied.
   *
   * @param docValues the doc values type.
   * @param numeric   if this field is numeric
   *
   * @return this builder.
   */
  public FlintField docValues(String docValues, boolean numeric) {
    this._docValues = toDocValues(docValues, numeric);
    return this;
  }

  /**
   * Sets the term vector.
   *
   * @param vector The term vector for the field to build as a string.
   * @return this builder.
   */
  public FlintField termVector(boolean vector) {
    this._vector = vector;
    return this;
  }

  /**
   * Sets the term vector offsets flag.
   *
   * @param vectorOffsets The term vector offsets flag for the field to build as a string.
   * @return this builder.
   */
  public FlintField termVectorOffsets(boolean vectorOffsets) {
    this._vectorOffsets = vectorOffsets;
    return this;
  }

  /**
   * Sets the term vector positions flag.
   *
   * @param vectorPositions The term vector positions flag for the field to build as a string.
   * @return this builder.
   */
  public FlintField termVectorPositions(boolean vectorPositions) {
    this._vectorPositions = vectorPositions;
    return this;
  }

  /**
   * Sets the term vector payloads flag.
   *
   * @param vectorPayloads The term vector payloads flag for the field to build as a string.
   * @return this builder.
   */
  public FlintField termVectorPayloads(boolean vectorPayloads) {
    this._vectorPayloads = vectorPayloads;
    return this;
  }

  /**
   * Sets the term vector.
   *
   * @param vector The term vector for the field to build as a string.
   * @return this builder.
   */
  public FlintField termVector(String vector) {
    if (vector != null) this._vector = Boolean.parseBoolean(vector);
    return this;
  }

  /**
   * Sets the term vector offsets flag.
   *
   * @param vectorOffsets The term vector offsets flag for the field to build as a string.
   * @return this builder.
   */
  public FlintField termVectorOffsets(String vectorOffsets) {
    if (vectorOffsets != null) this._vectorOffsets = Boolean.parseBoolean(vectorOffsets);
    return this;
  }

  /**
   * Sets the term vector positions flag.
   *
   * @param vectorPositions The term vector positions flag for the field to build as a string.
   * @return this builder.
   */
  public FlintField termVectorPositions(String vectorPositions) {
    if (vectorPositions != null) this._vectorPositions = Boolean.parseBoolean(vectorPositions);
    return this;
  }

  /**
   * Sets the term vector payloads flag.
   *
   * @param vectorPayloads The term vector payloads flag for the field to build as a string.
   * @return this builder.
   */
  public FlintField termVectorPayloads(String vectorPayloads) {
    if (vectorPayloads != null) this._vectorPayloads = Boolean.parseBoolean(vectorPayloads);
    return this;
  }

  /**
   * @deprecated no more boost at index time
   *
   * @param boost The boost value for this field as a string.
   * @return this builder.
   */
  public FlintField boost(float boost) {
    return this;
  }

  /**
   * @deprecated no more boost at index time
   *
   * @see #toBoost(String)
   *
   * @param boost The boost value for this field as a string.
   * @return this builder.
   */
  public FlintField boost(String boost) {
    return this;
  }

  /**
   * Returns the date format for this field.
   *
   * @param dateformat A date format to parse dates.
   * @return this builder.
   */
  public FlintField dateFormat(SimpleDateFormat dateformat) {
    this._dateformat = dateformat;
    return this;
  }

  /**
   * Returns the date format for this field.
   *
   * @param dateformat A date format to parse dates.
   * @return this builder.
   */
  public FlintField dateFormat(String dateformat) {
    this._dateformat = toDateFormat(dateformat);
    return this;
  }

  /**
   * Returns the date resolution for this field.
   *
   * @param resolution A date resolution to parse dates.
   * @return this builder.
   */
  public FlintField resolution(Resolution resolution) {
    this._resolution = resolution;
    return this;
  }

  /**
   * Returns the date resolution for this field.
   *
   * @see FlintField#toResolution(String)
   *
   * @param resolution A date resolution to parse dates.
   * @return this builder.
   */
  public FlintField resolution(String resolution) {
    if (resolution != null) this._resolution = toResolution(resolution);
    return this;
  }

  /**
   * Sets the number type for a numeric type, set to <code>null</code> for a string.
   *
   * @param type The number type for the numeric field to build.
   * @return this builder.
   */
  public FlintField numeric(NumericType type) {
    this._numeric = type;
    return this;
  }

  /**
   * Sets the number type for a numeric type, set to <code>null</code> for a string.
   *
   * @param type The number type for the numeric field to build.
   * @return this builder.
   */
  public FlintField numeric(String type) {
    if (type != null) this._numeric = toNumeric(type);
    return this;
  }

  /**
   * Sets the precision step (only applies to numeric fields).
   *
   * <p>Good values depend on usage and data type.
   * <p>Suitable values are generally between 1 and 8, see below (copied from Lucene doc)
   * <ul>
   *   <li>The default for all data types is 4.</li>
   *   <li>Ideal value in most cases for 64 bit data types (long, double) is 6 or 8.</li>
   *   <li>Ideal value in most cases for 32 bit data types (int, float) is 4.</li>
   *   <li>For low cardinality fields larger precision steps are good.</li>
   * </ul>
   *
   * @param precision The precision step.
   * @return this builder.
   */
  public FlintField precisionStep(int precision) {
    this._precisionStep = precision;
    return this;
  }

  /**
   * Sets the precision step (only applies to numeric fields).
   *
   * @see #precisionStep(int)
   *
   * @param precision The precision step to be parsed as an integer.
   * @return this builder.
   */
  public FlintField precisionStep(String precision) {
    if (precision == null) return this;
    try {
      this._precisionStep = Integer.parseInt(precision);
    } catch (NumberFormatException ex) {
      LOGGER.error("Unable to parse precision step {} as an integer - ignored and used default", precision);
    }
    return this;
  }

  // Getters
  // ----------------------------------------------------------------------------------------------

  /**
   * Returns the name of the field to build.
   *
   * @return The name of the field to build.
   */
  public String name() {
    return this._name;
  }

  public String catalog() {
    return this._catalog;
  }

  /**
   * Returns the value of the field to build.
   *
   * @return The value of the field to build.
   */
  public CharSequence value() {
    return this._value;
  }

  /**
   * Returns the field store for the field to build.
   *
   * @return The field store for the field to build.
   */
  public boolean store() {
    return this._store;
  }

  /**
   * Returns the field tokenize for the field to build.
   *
   * @return The field tokenize for the field to build.
   */
  public boolean tokenize() {
    return this._tokenize;
  }

  /**
   * Returns the field index for the field to build.
   *
   * @return The field index for the field to build.
   */
  public IndexOptions index() {
    return this._index;
  }

  /**
   * Returns the boost value for this field.
   * @deprecated no more boost at index time
   *
   * @return The boost value for this field.
   */
  public float boost() {
    return 1f;
  }

  /**
   * Returns the numeric type for this field.
   *
   * @return the numeric type for this field.
   */
  public NumericType numericType() {
    return this._numeric;
  }

  public boolean isDocValues() {
    return (this._docValues != DocValuesType.NONE && this._docValues != DocValuesType.FORCED_NONE);
  }

  public boolean isNumeric() {
    return this._numeric != null;
  }

  /**
   * Returns the doc values type for this field.
   *
   * @return the doc values type for this field.
   */
  public DocValuesType docValues() {
    return this._docValues;
  }

  /**
   * @deprecated
   */
  public boolean compressed() {
    return this._compressed;
  }

  public NumericType numeric() {
    return this._numeric;
  }

  public SimpleDateFormat dateformat() {
    return this._dateformat;
  }

  public Integer precisionStep() {
    return this._precisionStep;
  }

  public Resolution resolution() {
    return this._resolution;
  }

  public boolean omitNorms() {
    return this._omitNorms;
  }

  public boolean termVector() {
    return this._vector;
  }

  public boolean termVectorOffsets() {
    return this._vectorOffsets;
  }

  public boolean termVectorPayloads() {
    return this._vectorPayloads;
  }

  public boolean termVectorPositions() {
    return this._vectorPositions;
  }

  // Static utility methods
  // ----------------------------------------------------------------------------------------------

  /**
   * Returns the Index Options.
   *
   * @param index the index options as a string
   *
   * @return The corresponding Lucene 5 constant.
   */
  public static IndexOptions toIndexOptions(String index) {
    if (index == null) return null;
    try {
      return IndexOptions.valueOf(index.toUpperCase().replace('-', '_'));
    } catch (IllegalArgumentException ex) {
      LOGGER.warn("Invalid index option: {}", index);
      return null;
    }
  }

  /**
   * Returns the resolution for date.
   *
   * @param resolution The date tools resolution.
   *
   * @return The corresponding Lucene 5 constant.
   */
  public static Resolution toResolution(String resolution) {
    if (resolution == null) return null;
    else if ("year".equals(resolution))        return Resolution.YEAR;
    else if ("month".equals(resolution))       return Resolution.MONTH;
    else if ("day".equals(resolution))         return Resolution.DAY;
    else if ("hour".equals(resolution))        return Resolution.HOUR;
    else if ("minute".equals(resolution))      return Resolution.MINUTE;
    else if ("second".equals(resolution))      return Resolution.SECOND;
    else if ("milli".equals(resolution))       return Resolution.MILLISECOND;
    else if ("millisecond".equals(resolution)) return Resolution.MILLISECOND;
    LOGGER.warn("Invalid date resolution: {}, defaulting to Resolution.DAY", resolution);
    return Resolution.DAY;
  }

  /**
   * Returns the numeric type.
   *
   * <p>If the value does not match one of the value number types
   * this method will return <code>null</code> and the field will be indexed as a normal String field.
   *
   * @param type The number type; one of ("int","float","double" or "long").
   *
   * @return The corresponding <code>NumericType</code> instance or <code>null</code>.
   */
  public static NumericType toNumeric(String type) {
    if (type == null) return null;
    for (NumericType nt : NumericType.values()) {
      if (type.equalsIgnoreCase(nt.name()))
        return nt;
    }
    LOGGER.warn("Invalid number type : {}, defaulting to null (string)", type);
    return null;
  }

  /**
   * Returns the doc values type.
   *
   * <p>If the value does not match one of the doc values types
   * this method will return <code>null</code> and the field will be indexed as a normal field.
   *
   * @param type The doc values type.
   *
   * @return The corresponding <code>DocValuesType</code> instance or <code>null</code>.
   */
  public static DocValuesType toDocValues(String type, boolean numeric) {
    if (type == null)              return DocValuesType.NONE;
    if ("sorted".equals(type))     return numeric ? DocValuesType.SORTED_NUMERIC : DocValuesType.SORTED;
    if ("sorted-set".equals(type)) return DocValuesType.SORTED_SET;
    if ("none".equals(type))       return DocValuesType.NONE;
    LOGGER.warn("Invalid doc values type : {}, defaulting to none", type);
    return DocValuesType.NONE;
  }

  /**
   * @deprecated no more boost at index time
   *
   * @param boost The boost value.
   * @return The corresponding boost value as a float.
   */
  public static float toBoost(String boost) {
    return 1f;
  }

  /**
   * Returns the date format to use, allowing recycling.
   *
   * <p>Set the current date format to <code>null<code> if the format is <code>null</code>.
   *
   * <p>Otherwise retrieve from map or create an instance if it has never been created.
   *
   * <p>Note: we only set the timezone if the date format includes a time component; otherwise we default to GMT to
   * ensure that Lucene will preserve the date.
   *
   * @param format The date format used.
   * @return the corresponding date format or <code>null</code>.
   */
  public static SimpleDateFormat toDateFormat(String format) {
    if (format == null) return null;
    try {
      SimpleDateFormat df = new SimpleDateFormat(format);
      df.setTimeZone(GMT);
      return df;
    } catch (IllegalArgumentException ex) {
      LOGGER.warn("Ignoring unusable date format '"+format+"'", ex);
    }
    return null;
  }

}

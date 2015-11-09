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
package org.pageseeder.flint.index;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import org.apache.lucene.document.CompressionTools;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.FieldType.NumericType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.NumericUtils;
import org.pageseeder.flint.util.Dates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A builder for fields.
 *
 * <p>This class can be used to temporarily hold values required to creates new fields and
 * ensure that the field can be build without errors.
 *
 * @author Christophe Lauret
 * @version 10 February 2012
 */
public final class FieldBuilder {

  /**
   * The default boost value for the term.
   */
  private static final float DEFAULT_BOOST_VALUE = 1.0f;

  /**
   * The logger for this class.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(FieldBuilder.class);

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
  private DateFormat _dateformat;

  /**
   * The 'resolution' attribute of the field currently processed - determines the granularity of date stored.
   */
  private DateTools.Resolution _resolution;

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
  private int _precisionStep = NumericUtils.PRECISION_STEP_DEFAULT;

  /**
   * The value of the field currently processed.
   */
  private float _boost = DEFAULT_BOOST_VALUE;

  // Setters
  // ----------------------------------------------------------------------------------------------

  /**
   * The name of the field to build.
   *
   * @param name The name of the field to build.
   * @return this builder.
   */
  public FieldBuilder name(String name) {
    this._name = name;
    return this;
  }

  /**
   * The value of the field to build.
   *
   * @param value The value of the field to build.
   * @return this builder.
   */
  public FieldBuilder value(String value) {
    this._value = value;
    return this;
  }

  /**
   * Set the field store for the field to build.
   *
   * @param store The field store for the field to build.
   * @return this builder.
   */
  public FieldBuilder store(boolean store) {
    this._store = store;
    return this;
  }

  /**
   * Set the field store for the field to build.
   *
   * @param store The field store for the field to build as a string.
   * @return this builder.
   */
  public FieldBuilder store(String store) {
    if (store != null) this._store = Boolean.parseBoolean(store);
    return this;
  }

  /**
   * Set the field tokenize for the field to build.
   *
   * @param tokenize The field tokenize for the field to build.
   * @return this builder.
   */
  public FieldBuilder tokenize(boolean tokenize) {
    this._tokenize = tokenize;
    return this;
  }

  /**
   * Set the field tokenize for the field to build.
   *
   * @param tokenize The field tokenize for the field to build as a string.
   * @return this builder.
   */
  public FieldBuilder tokenize(String tokenize) {
    if (tokenize != null) this._tokenize = Boolean.parseBoolean(tokenize);
    return this;
  }

  /**
   * Set the field index for the field to build.
   *
   * @param index The field index for the field to build.
   * @return this builder.
   */
  public FieldBuilder index(IndexOptions index) {
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
  public FieldBuilder omitNorms(boolean omit) {
    this._omitNorms = omit;
    return this;
  }

  /**
   * Set the field index for the field to build.
   *
   * @see #toFieldIndex(String)
   *
   * @param index The field index for the field to build as a string.
   * @return this builder.
   */
  public FieldBuilder index(String index) {
    if (index != null) this._index = toIndexOptions(index);
    return this;
  }

  /**
   * Sets the term vector.
   *
   * @param vector The term vector for the field to build as a string.
   * @return this builder.
   */
  public FieldBuilder termVector(boolean vector) {
    this._vector = vector;
    return this;
  }

  /**
   * Sets the term vector offsets flag.
   *
   * @param vectorOffsets The term vector offsets flag for the field to build as a string.
   * @return this builder.
   */
  public FieldBuilder termVectorOffsets(boolean vectorOffsets) {
    this._vectorOffsets = vectorOffsets;
    return this;
  }

  /**
   * Sets the term vector positions flag.
   *
   * @param vectorPositions The term vector positions flag for the field to build as a string.
   * @return this builder.
   */
  public FieldBuilder termVectorPositions(boolean vectorPositions) {
    this._vectorPositions = vectorPositions;
    return this;
  }

  /**
   * Sets the term vector payloads flag.
   *
   * @param vectorPayloads The term vector payloads flag for the field to build as a string.
   * @return this builder.
   */
  public FieldBuilder termVectorPayloads(boolean vectorPayloads) {
    this._vectorPayloads = vectorPayloads;
    return this;
  }

  /**
   * Sets the term vector.
   *
   * @param vector The term vector for the field to build as a string.
   * @return this builder.
   */
  public FieldBuilder termVector(String vector) {
    if (vector != null) this._vector = Boolean.parseBoolean(vector);
    return this;
  }

  /**
   * Sets the term vector offsets flag.
   *
   * @param vectorOffsets The term vector offsets flag for the field to build as a string.
   * @return this builder.
   */
  public FieldBuilder termVectorOffsets(String vectorOffsets) {
    if (vectorOffsets != null) this._vectorOffsets = Boolean.parseBoolean(vectorOffsets);
    return this;
  }

  /**
   * Sets the term vector positions flag.
   *
   * @param vectorPositions The term vector positions flag for the field to build as a string.
   * @return this builder.
   */
  public FieldBuilder termVectorPositions(String vectorPositions) {
    if (vectorPositions != null) this._vectorPositions = Boolean.parseBoolean(vectorPositions);
    return this;
  }

  /**
   * Sets the term vector payloads flag.
   *
   * @param vectorPayloads The term vector payloads flag for the field to build as a string.
   * @return this builder.
   */
  public FieldBuilder termVectorPayloads(String vectorPayloads) {
    if (vectorPayloads != null) this._vectorPayloads = Boolean.parseBoolean(vectorPayloads);
    return this;
  }

  /**
   * Returns the boost value for this field.
   *
   * @param boost The boost value for this field as a string.
   * @return this builder.
   */
  public FieldBuilder boost(float boost) {
    this._boost = boost;
    return this;
  }

  /**
   * Returns the boost value for this field.
   *
   * @see #toBoost(String)
   *
   * @param boost The boost value for this field as a string.
   * @return this builder.
   */
  public FieldBuilder boost(String boost) {
    if (boost != null) this._boost = toBoost(boost);
    return this;
  }

  /**
   * Returns the date format for this field.
   *
   * @param dateformat A date format to parse dates.
   * @return this builder.
   */
  public FieldBuilder dateFormat(DateFormat dateformat) {
    this._dateformat = dateformat;
    return this;
  }

  /**
   * Returns the date resolution for this field.
   *
   * @param resolution A date resolution to parse dates.
   * @return this builder.
   */
  public FieldBuilder resolution(DateTools.Resolution resolution) {
    this._resolution = resolution;
    return this;
  }

  /**
   * Returns the date resolution for this field.
   *
   * @see FieldBuilder#toResolution(String)
   *
   * @param resolution A date resolution to parse dates.
   * @return this builder.
   */
  public FieldBuilder resolution(String resolution) {
    if (resolution != null) this._resolution = toResolution(resolution);
    return this;
  }

  /**
   * Sets the number type for a numeric type, set to <code>null</code> for a string.
   *
   * @param type The number type for the numeric field to build.
   * @return this builder.
   */
  protected FieldBuilder numeric(NumericType type) {
    this._numeric = type;
    return this;
  }

  /**
   * Sets the number type for a numeric type, set to <code>null</code> for a string.
   *
   * @param type The number type for the numeric field to build.
   * @return this builder.
   */
  public FieldBuilder numeric(String type) {
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
   * @see org.apache.lucene.document.NumericField
   *
   * @param precision The precision step.
   * @return this builder.
   */
  public FieldBuilder precisionStep(int precision) {
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
  public FieldBuilder precisionStep(String precision) {
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
   *
   * @return The boost value for this field.
   */
  public float boost() {
    return this._boost;
  }

  // Reset and build
  // ----------------------------------------------------------------------------------------------

  /**
   * Indicates whether all required attributes have been set.
   *
   * <p>The required attributes are:
   * <ul>
   *   <li>Name, method {@link #name(String)} must have been called once.</li>
   *   <li>Field Store, method {@link #store(Store)} or {@link #store(String)} must have been called once.</li>
   *   <li>Field Index, method {@link #index(Index)} or {@link #index(String)} must have been called once.</li>
   *   <li>Value, method {@link #value(String)} must have been called once.</li>
   * </ul>
   *
   * @return <code>true</code> if the {@link #build()} method can be called safely;
   *        <code>false</code> otherwise;
   */
  public boolean isReady() {
    return (this._name != null && this._index != null && this._value != null);
  }

  /**
   * Resets all this class attribute so that a new field can be build.
   *
   * <p>Invoke this function once a field has been build or before building a new field.
   */
  public void reset() {
    this._index = IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS;
    this._omitNorms = false;
    this._store = true;
    this._tokenize = true;
    this._vector = true;
    this._vectorOffsets = true;
    this._vectorPositions = true;
    this._vectorPayloads = true;
    this._name = null;
    this._value = null;
    this._boost = DEFAULT_BOOST_VALUE;
    this._dateformat = null;
    this._resolution = null;
    this._numeric = null;
    this._precisionStep = NumericUtils.PRECISION_STEP_DEFAULT;
  }

  /**
   * Builds the field from the values in this builder.
   *
   * @return the field from the values in this builder.
   *
   * @throws IllegalStateException If the builder is not ready.
   */
  public IndexableField build() throws IllegalStateException {
    checkReady();
    // get value
    String value = this._value.toString();
    if (this._dateformat != null) {
      Date date = toDate(value, this._dateformat);
      value = (date != null)? Dates.toString(date, this._resolution) : "";
    }
    // construct the field type
    FieldType type = new FieldType();
    type.setNumericType(this._numeric);
    type.setNumericPrecisionStep(this. _precisionStep);
    type.setStored(this._store);
    type.setTokenized(this._tokenize);
    type.setIndexOptions(this._index);
    type.setOmitNorms(this._omitNorms);
    type.setStoreTermVectors(this._vector);
    type.setStoreTermVectorOffsets(this._vectorOffsets);
    type.setStoreTermVectorPositions(this._vectorPositions);
    type.setStoreTermVectorPayloads(this._vectorPayloads);
    // build field
    Field field = new Field(this._name, value, type);
    // Sets the boost if necessary
    if (this._boost != 1.0f) field.setBoost(this._boost);
    return field;
  }

  /**
   * Builds an stored unindexed compressed version of the field from the values in this builder.
   *
   * @return the field from the values in this builder.
   *
   * @throws IllegalStateException If the builder is not ready.
   */
  public Field buildCompressed() throws IllegalStateException {
    checkReady();
    // Generate a compressed field
    byte[] value = CompressionTools.compressString(this._value.toString());
    FieldType type = new FieldType();
    type.setStored(true);
    Field field = new Field(this._name, value, type);
    if (this._boost != 1.0f) {
      field.setBoost(this._boost);
    }
    return field;
  }

  /**
   * Checks that required attributes have been set
   *
   * @throws IllegalStateException Should any missing attribute prevent a build.
   */
  private void checkReady() throws IllegalStateException {
    if (this._name  == null)
      throw new IllegalStateException("Unable to build field, field name not set");
    if (this._index == null)
      throw new IllegalStateException("Unable to build field, field index not set");
    if (this._value == null)
      throw new IllegalStateException("Unable to build field, field value not set");
  }

  // Static utility methods
  // ----------------------------------------------------------------------------------------------

  /**
   * Returns the resolution for date.
   *
   * @see DateTools.Resolution
   *
   * @param resolution The date tools resolution.
   *
   * @return The corresponding Lucene 3 constant.
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
   * @see DateTools.Resolution
   *
   * @param resolution The date tools resolution.
   *
   * @return The corresponding Lucene 3 constant.
   */
  public static DateTools.Resolution toResolution(String resolution) {
    if (resolution == null) return null;
    else if ("year".equals(resolution))    return DateTools.Resolution.YEAR;
    else if ("month".equals(resolution))   return DateTools.Resolution.MONTH;
    else if ("day".equals(resolution))     return DateTools.Resolution.DAY;
    else if ("hour".equals(resolution))    return DateTools.Resolution.HOUR;
    else if ("minute".equals(resolution))  return DateTools.Resolution.MINUTE;
    else if ("second".equals(resolution))  return DateTools.Resolution.SECOND;
    else if ("milli".equals(resolution))   return DateTools.Resolution.MILLISECOND;
    LOGGER.warn("Invalid date resolution: {}, defaulting to Resolution.DAY", resolution);
    return DateTools.Resolution.DAY;
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
    if ("int".equals(type))    return NumericType.INT;
    if ("float".equals(type))  return NumericType.FLOAT;
    if ("double".equals(type)) return NumericType.DOUBLE;
    if ("long".equals(type))   return NumericType.LONG;
    LOGGER.warn("Invalid number type : {}, defaulting to null (string)", type);
    return null;
  }

  /**
   * Returns the boost value for a field as a float.
   *
   * <p>If the float parsing fails, this method returns the default boost value.
   *
   * @param boost The boost value.
   * @return The corresponding boost value as a float.
   */
  public static float toBoost(String boost) {
    if (boost == null) return DEFAULT_BOOST_VALUE;
    try {
      return Float.parseFloat(boost);
    } catch (NumberFormatException ex) {
      LOGGER.warn("Could not parse boost value '{}' as float, using {}", boost, DEFAULT_BOOST_VALUE);
      return DEFAULT_BOOST_VALUE;
    }
  }

  // Private helpers
  // ----------------------------------------------------------------------------------------------

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

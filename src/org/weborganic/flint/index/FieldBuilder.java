package org.weborganic.flint.index;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import org.apache.lucene.document.CompressionTools;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.util.NumericUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.flint.util.Dates;

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
   * Possible number type for a numeric field.
   */
  protected static enum NumericType { FLOAT, INT, DOUBLE, LONG };

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
  private Field.Store _store;

  /**
   * The 'index' attribute of the field to build.
   */
  private Field.Index _index;

  /**
   * The 'termVector' attribute of the field to build.
   */
  private Field.TermVector _vector;

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
  public FieldBuilder store(Field.Store store) {
    this._store = store;
    return this;
  }

  /**
   * Set the field store for the field to build.
   *
   * @see #toFieldStore(String)
   *
   * @param store The field store for the field to build as a string.
   * @return this builder.
   */
  public FieldBuilder store(String store) {
    this._store = toFieldStore(store);
    return this;
  }

  /**
   * Set the field index for the field to build.
   *
   * @param index The field index for the field to build.
   * @return this builder.
   */
  public FieldBuilder index(Field.Index index) {
    this._index = index;
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
    this._index = toFieldIndex(index);
    return this;
  }

  /**
   * Sets the term vector.
   *
   * @param vector The term vector for the field to build.
   * @return this builder.
   */
  public FieldBuilder termVector(Field.TermVector vector) {
    this._vector = vector;
    return this;
  }

  /**
   * Sets the term vector.
   *
   * @see FieldBuilder#toTermVector(String)
   *
   * @param vector The term vector for the field to build as a string.
   * @return this builder.
   */
  public FieldBuilder termVector(String vector) {
    this._vector = toTermVector(vector);
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
    this._boost = toBoost(boost);
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
    this._resolution = toResolution(resolution);
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
    this._numeric = toNumeric(type);
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
  public Field.Store store() {
    return this._store;
  }

  /**
   * Returns the field index for the field to build.
   *
   * @return The field index for the field to build.
   */
  public Field.Index index() {
    return this._index;
  }

  /**
   * Returns the term vector.
   *
   * @return The term vector for the field to build.
   */
  public Field.TermVector termVector() {
    return this._vector;
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
    return (this._name != null && this._index != null && this._store != null && this._value != null);
  }

  /**
   * Resets all this class attribute so that a new field can be build.
   *
   * <p>Invoke this function once a field has been build or before building a new field.
   */
  public void reset() {
    this._index = null;
    this._store = null;
    this._vector = null;
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
  public Fieldable build() throws IllegalStateException {
    checkReady();
    // construct the field
    Fieldable field = null;
    String value = this._value.toString();
    // a numeric field
    if (this._numeric != null) {
      NumericField nf = new NumericField(this._name, this._precisionStep, this._store, this._index != Index.NO);
      // handle dates
      if (this._dateformat != null) {
        Date date = toDate(value, this._dateformat);
        if (date != null) {
          field = setValue(nf, this._numeric, Dates.toNumber(date, this._resolution));
        }
      }
      if (field == null)
        field = setValue(nf, this._numeric, value);

    // normal field (string-based)
    } else {
      if (this._dateformat != null) {
        Date date = toDate(value, this._dateformat);
        value = (date != null)? Dates.toString(date, this._resolution) : "";
      }
      if (this._vector != null)
        field = new Field(this._name, value, this._store, this._index, this._vector);
      else
        field = new Field(this._name, value, this._store, this._index);
    }
    // Sets the boost if necessary
    if (this._boost != 1.0f)
      field.setBoost(this._boost);
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
    Field field = new Field(this._name, value, Field.Store.YES);
    if (this._boost != 1.0f)
      field.setBoost(this._boost);
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
    if (this._store == null)
      throw new IllegalStateException("Unable to build field, field store not set");
    if (this._value == null)
      throw new IllegalStateException("Unable to build field, field value not set");
  }

  // Static utility methods
  // ----------------------------------------------------------------------------------------------

  /**
   * Returns the Lucene 3 Field Store matching the specified value.
   *
   * @see Field.Store
   *
   * @param store The store flag as a string.
   *
   * @return The corresponding Lucene 3 constant or <code>null</code> if none matches.
   */
  public static Store toFieldStore(String store) {
    if ("no".equals(store))  return Field.Store.NO;
    if ("yes".equals(store)) return Field.Store.YES;
    LOGGER.warn("Invalid field store value: {}", store);
    return null;
  }

  /**
   * Returns the Lucene 3 Field Index matching the specified value.
   *
   * @see Field.Index
   *
   * @param index The index flag as a string.
   *
   * @return The corresponding Lucene 3 constant or <code>null</code> if none matches.
   */
  public static Index toFieldIndex(String index) {
    if (index == null) return null;
    try {
      return Index.valueOf(index.toUpperCase().replace('-', '_'));
    } catch (IllegalArgumentException ex) {
      LOGGER.warn("Invalid field index value: {}", index);
      return null;
    }
  }

  /**
   * Returns the Lucene 3 Field Index from the attribute value.
   *
   * @see TermVector
   *
   * @param vector The term vector.
   *
   * @return The corresponding Lucene 3 constant or <code>null</code> if none matches.
   */
  public static TermVector toTermVector(String vector) {
    if (vector == null) return null;
    try {
      return TermVector.valueOf(vector.toUpperCase().replace('-', '_'));
    } catch (IllegalArgumentException ex) {
      LOGGER.warn("Invalid term vector value: {}, defaulting to Field.TermVector.NO", vector);
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
   * Sets the value of the numeric field.
   *
   * @param nf      The Lucene numeric field
   * @param numeric The numeric type (float, int, double or long)
   * @param value   The actual value as a number
   *
   * @return the numeric field from the values in this builder; or <code>null</code>
   *
   * @throws IllegalStateException If the builder is not ready.
   */
  private static NumericField setValue(NumericField nf, NumericType numeric, String value) throws IllegalStateException {
    try {
      switch (numeric) {
        case FLOAT:  return nf.setFloatValue(Float.parseFloat(value));
        case INT:    return nf.setIntValue(Integer.parseInt(value));
        case DOUBLE: return nf.setDoubleValue(Double.parseDouble(value));
        case LONG:   return nf.setLongValue(Long.parseLong(value));
        default: throw new IllegalArgumentException("Unknown numeric type:"+numeric);
      }
    } catch (NumberFormatException ex) {
      LOGGER.error("Failed to parse {} as a {}", value.toString(), numeric);
      return null;
    }
  }

  /**
   * Sets the value of the numeric field.
   *
   * @param nf      The Lucene numeric field
   * @param numeric The numeric type (float, int, double or long)
   * @param value   The actual value as a number
   *
   * @return the numeric field from the values in this builder; or <code>null</code>
   *
   * @throws IllegalStateException If the builder is not ready.
   */
  private static NumericField setValue(NumericField nf, NumericType numeric, Number value)
      throws IllegalStateException {
    try {
      switch (numeric) {
        case FLOAT:  return nf.setFloatValue(value.floatValue());
        case INT:    return nf.setIntValue(value.intValue());
        case DOUBLE: return nf.setDoubleValue(value.doubleValue());
        case LONG:   return nf.setLongValue(value.longValue());
        default: throw new IllegalArgumentException("Unknown numeric type:"+numeric);
      }
    } catch (NumberFormatException ex) {
      LOGGER.error("Failed to parse {} as a {}", value.toString(), numeric);
      return null;
    }
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

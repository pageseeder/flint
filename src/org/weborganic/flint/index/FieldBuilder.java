package org.weborganic.flint.index;

import java.text.DateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.apache.lucene.document.CompressionTools;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;

/**
 * A builder for fields.
 * 
 * <p>This class can be used to temporarily hold values required to creates new fields and
 * ensure that the field can be build without errors.
 * 
 * @author Christophe Lauret
 * @version 2 March 2010
 */
public final class FieldBuilder {

  /**
   * The default boost value for the term.
   */
  private static final float DEFAULT_BOOST_VALUE = 1.0f;

  /**
   * The logger for this class.
   */
  private static final Logger LOGGER = Logger.getLogger(IndexDocumentHandler_2_0.class);

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
   * @param index The field index for the field to build.
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
   * @param <code>true</code> if the {@link #build()} method can be called safely;
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
  }

  /**
   * Builds the field from the values in this builder.
   * 
   * @return the field from the values in this builder.
   * 
   * @throws IllegalStateException If the builder is not ready.
   */
  public Field build() throws IllegalStateException {
    checkReady();
    String value = this._value.toString();
    if (this._dateformat != null)
      value = toDateField(value, this._dateformat, this._resolution);
    // construct the field
    Field field = null;
    if (this._vector != null)
      field = new Field(this._name, value, this._store, this._index, this._vector);
    else
      field = new Field(this._name, value, this._store, this._index);
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
    LOGGER.warn("Invalid field store value: "+store);
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
      LOGGER.warn("Invalid field index value: "+index);
      return null;
    }
  }

  /**
   * Returns the Lucene 3 Field Index from the attribute value.
   * 
   * @see TermVector
   * 
   * @param index The index flag as a string.
   * 
   * @return The corresponding Lucene 3 constant or <code>null</code> if none matches.
   */
  public static TermVector toTermVector(String vector) {
    if (vector == null) return null;
    try {
      return TermVector.valueOf(vector.toUpperCase().replace('-', '_'));
    } catch (IllegalArgumentException ex) {
      LOGGER.warn("Invalid term vector value: "+vector+", defaulting to Field.TermVector.NO");
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
    if ("year".equals(resolution))    return DateTools.Resolution.YEAR;
    if ("month".equals(resolution))   return DateTools.Resolution.MONTH;
    if ("day".equals(resolution))     return DateTools.Resolution.DAY;
    if ("hour".equals(resolution))    return DateTools.Resolution.HOUR;
    if ("minute".equals(resolution))  return DateTools.Resolution.MINUTE;
    if ("second".equals(resolution))  return DateTools.Resolution.SECOND;
    if ("milli".equals(resolution))   return DateTools.Resolution.MILLISECOND;
    LOGGER.warn("Invalid date resolution: "+resolution+", defaulting to Resolution.DAY");
    return DateTools.Resolution.DAY;
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
      LOGGER.warn("Could not parse boost value '"+boost+"' as float, using "+DEFAULT_BOOST_VALUE);
      return DEFAULT_BOOST_VALUE;
    }
  }

  // Private helpers
  // ----------------------------------------------------------------------------------------------

  /**
   * Return the string value used by Lucene 3 for dates.
   *
   * @return The string value for use by Lucene.
   * 
   * @throws IllegalArgumentException Should an invalid date be parsed.
   */
  private static String toDateField(String value, DateFormat format, Resolution resolution) {
    try {
      Date date = format.parse(value.toString());
      return DateTools.timeToString(date.getTime(), resolution);
    } catch (Exception ex) {
      LOGGER.warn("Ignoring unparsable date '"+value+"', format="+format+", resolution="+resolution, ex);
      //throw new IllegalArgumentException("Unparseable date field!", ex);
      return "";
    }
  }

}

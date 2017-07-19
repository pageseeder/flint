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
package org.pageseeder.flint.lucene.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;

/**
 * A collection of utility methods to deal with dates.
 *
 * <p>By default, Flint uses ISO8601.
 *
 * @author Christophe Lauret
 * @version 10 September 2010
 */
public final class Dates {

  /**
   * The ISO 8601 Date and time format (required for resolution > day only)
   *
   * @see <a href="http://www.iso.org/iso/date_and_time_format">ISO: Numeric representation of Dates and Time</a>
   */
  private static final String ISO8601_DATETIME = "yyyy-MM-dd'T'HH:mm:ssZ";

  /**
   * The maximum length for a field to expand.
   */
  private static final int ONE_SECOND_IN_MS = 1000;

  /**
   * One minute in milliseconds.
   */
  private static final int ONE_MINUTE_IN_MS = 60000;

  /**
   * One hour in milliseconds.
   */
  private static final int ONE_HOUR_IN_MS = 3600000;

  /**
   * The ISO 8601 date and time formatter to use (when resolution is greater than day).
   */
  private static final DateFormat ISO_DATETIME = new SimpleDateFormat(ISO8601_DATETIME);

  /** Utility class. */
  private Dates() {
  }

  /**
   * Format the value as an ISO8601 date time.
   *
   * @param value       the value from the index
   * @param offset      the timezone offset (adjust for the specified offset)
   *
   * @return the corresponding value.
   *
   * @throws ParseException if the value is not a parseable date.
   */
  public static synchronized String toISODateTime(String value, int offset) throws ParseException {
    int x = findNonDigitCharacter(value);
    if (x != -1) throw new ParseException("Value is not a valid Lucene date", x);
    Date date = DateTools.stringToDate(value);
    // Only adjust for day light savings...
    TimeZone tz = TimeZone.getDefault();
    int rawOffset = tz.inDaylightTime(date)? offset - ONE_HOUR_IN_MS : offset;
    tz.setRawOffset(rawOffset);
    ISO_DATETIME.setTimeZone(tz);
    String formatted = ISO_DATETIME.format(date);

    // the Java timezone does not include the required ':'
    return formatted.substring(0, formatted.length() - 2) + ":" + formatted.substring(formatted.length() - 2);
  }

  /**
   * Returns an ISO 8601 calendar date from a Lucene Index value.
   *
   * @param value the value from the index
   *
   * @return the corresponding value.
   *
   * @throws ParseException if the value is not a parseable date.
   */
  public static String toISODate(String value) throws ParseException {
    int x = findNonDigitCharacter(value);
    if (x != -1) throw new ParseException("Value is not a valid Lucene date", x);
    // Odd case when it is zero??
    if ("0".equals(value)) return "";
    final int length = value.length();
    switch (length) {
      case 4:
        // Resolution.YEAR (stored in Lucene as 'yyyy')
        return value;
      case 6:
        // Resolution.MONTH (stored in Lucene as 'yyyymm')
        return value.substring(0, 4)+'-'+value.substring(4);
      case 8:
        // Resolution.DAY (stored in Lucene as 'yyyymmdd')
        return value.substring(0, 4)+'-'+value.substring(4, 6)+'-'+value.substring(6);
      default:
        throw new ParseException("Value is not a valid Lucene date", 0);
    }
  }

  /**
   * Indicates whether the specified value may be a date value in Lucene.
   *
   * @param value The value to check
   *
   * @return <code>true</code> if the specified value may be parseable as a date;
   *         <code>false</code> otherwise.
   */
  public static boolean isLuceneDate(String value) {
    if (value == null || value.isEmpty()) return false;
    final int length = value.length();
    switch (length) {
      case 4:  // yyyy (Resolution.YEAR)
      case 6:  // yyyymm (Resolution.MONTH)
      case 8:  // yyyymmdd (Resolution.DAY)
      case 10: // yyyymmddhh (Resolution.HOUR)
      case 12: // yyyymmddhhmm (Resolution.MINUTE)
      case 14: // yyyymmddhhmmss (Resolution.SECOND)
      case 17: // yyyymmddhhmmssxxx (Resolution.MILLISECOND)
        return findNonDigitCharacter(value) == -1;
      default:
    }
    return false;
  }

  /**
   * Formats the specified date as a String with the given resolution using ISO8601.
   *
   * <p>Formats based on the resolution using the ISO8601 extended format:
   * <ul>
   *   <li>Year <code>[YYYY]</code>
   *   <li>Month <code>[YYYY]-[MM]</code>
   *   <li>Day <code>[YYYY]-[MM]-[DD]</code>
   *   <li>Hour <code>[YYYY]-[MM]-[DD]T[hh]</code>
   *   <li>Minute <code>[YYYY]-[MM]-[DD]T[hh]:[mm]</code>
   *   <li>Second <code>[YYYY]-[MM]-[DD]T[hh]:[mm]:[ss]</code>
   *   <li>MilliSecond <code> [YYYY]-[MM]-[DD]T[hh]:[mm]:[ss].[sss]</code>
   * </ul>
   *
   * <p>Note: dates returned in local time.
   *
   * @param date       The date to format
   * @param resolution The resolution for the formatting
   *
   * @return the formatted date as ISO8601 or <code>null</code>.
   */
  public static String format(Date date, Resolution resolution) {
    if (date == null) return null;
    Calendar calendar = GregorianCalendar.getInstance();
    calendar.setTime(date);
    StringBuilder iso = new StringBuilder();
    // Year [YYYY]
    iso.append(leftZeroPad4(calendar.get(GregorianCalendar.YEAR)));
    if (resolution == Resolution.YEAR) return iso.toString();
    // Month [YYYY]-[MM]
    iso.append('-').append(leftZeroPad2(calendar.get(GregorianCalendar.MONTH) + 1));
    if (resolution == Resolution.MONTH) return iso.toString();
    // Day [YYYY]-[MM]-[DD]
    iso.append('-').append(leftZeroPad2(calendar.get(GregorianCalendar.DAY_OF_MONTH)));
    if (resolution == Resolution.DAY) return iso.toString();
    // TODO: Times require the time zone
    String z = toTimeZone(calendar.getTimeZone().getOffset(date.getTime()));
    // Hour [YYYY]-[MM]-[DD]T[hh]
    iso.append('T').append(leftZeroPad2(calendar.get(GregorianCalendar.HOUR_OF_DAY)));
    if (resolution == Resolution.HOUR) return iso.append(z).toString();
    // Minute [YYYY]-[MM]-[DD]T[hh]:[mm]
    iso.append(':').append(leftZeroPad2(calendar.get(GregorianCalendar.MINUTE)));
    if (resolution == Resolution.MINUTE) return iso.append(z).toString();
    // Second [YYYY]-[MM]-[DD]T[hh]:[mm]:[ss]
    iso.append(':').append(leftZeroPad2(calendar.get(GregorianCalendar.SECOND)));
    if (resolution == Resolution.SECOND) return iso.append(z).toString();
    // MilliSecond [YYYY]-[MM]-[DD]T[hh]:[mm]:[ss].[sss]
    iso.append('.').append(leftZeroPad3(calendar.get(GregorianCalendar.MILLISECOND)));
    return iso.append(z).toString();
  }

  /**
   * Return the string value used by Lucene 3 for dates.
   *
   * @param date       The date to turn to a string
   * @param resolution The resolution for the formatting
   *
   * @return The string value for use by Lucene.
   */
  public static String toString(Date date, Resolution resolution) {
    if (date == null) return null;
    return DateTools.timeToString(date.getTime(), resolution);
  }

  /**
   * Return the string value used by Lucene 3 for dates.
   *
   * @param date       The date to turn to a string
   * @param resolution The resolution for the formatting
   *
   * @return The string value for use by Lucene.
   */
  public static String toString(OffsetDateTime date, Resolution resolution) {
    if (date == null) return null;
    return DateTools.timeToString(date.toInstant().toEpochMilli(), resolution);
  }

  /**
   * Return the string value used by Lucene 3 for dates.
   *
   * @param date       The date to turn to a string
   * @param resolution The resolution for the formatting
   *
   * @return The string value for use by Lucene.
   */
  public static String toString(LocalDateTime date, Resolution resolution) {
    if (date == null) return null;
    return DateTools.timeToString(date.toInstant(ZoneOffset.UTC).toEpochMilli(), resolution);
  }

  /**
   * Return the numeric value used by Lucene 3 for dates.
   *
   * @param date       The date to convert
   * @param resolution The resolution for the formatting
   *
   * @return The numeric value for use by Lucene.
   */
  public static Number toNumber(Date date, Resolution resolution) {
    if (date == null) return null;
    long timems = date.getTime();
    // Resolution higher than Day -> Long
    if (resolution == Resolution.MILLISECOND) return Long.valueOf(timems);
    else if (resolution == Resolution.SECOND) return Long.valueOf(timems / ONE_SECOND_IN_MS);
    else if (resolution == Resolution.MINUTE) return Long.valueOf(timems / ONE_MINUTE_IN_MS);
    else if (resolution == Resolution.HOUR)   return Long.valueOf(timems / ONE_HOUR_IN_MS);
    // Resolution lower than Day -> Integer
    Calendar c = GregorianCalendar.getInstance();
    c.setTimeInMillis(timems);
    if (resolution == Resolution.DAY) return Integer.valueOf(c.get(GregorianCalendar.YEAR) * 10000 + (c.get(GregorianCalendar.MONTH) + 1) * 100 + c.get(GregorianCalendar.DAY_OF_MONTH));
    if (resolution == Resolution.MONTH) return Integer.valueOf(c.get(GregorianCalendar.YEAR) * 100 + c.get(GregorianCalendar.MONTH) + 1);
    if (resolution == Resolution.YEAR) return Integer.valueOf(c.get(GregorianCalendar.YEAR));
    return null;
  }

  // private helpers ------------------------------------------------------------------------------

  /**
   * Pads the given numbers with zeros to the left.
   *
   * @param value to pad (eg. 2)
   * @return The padded value (eg. "02")
   */
  private static String leftZeroPad2(int value) {
    return (value < 10)? "0" + Integer.toString(value) : Integer.toString(value);
  }

  /**
   * Pads the given numbers with zeros to the left.
   *
   * @param value to pad (eg. 2)
   * @return The padded value (eg. "002")
   */
  private static String leftZeroPad3(int value) {
    if (value >= 100) return Integer.toString(value);
    if (value >= 10)  return "0" + Integer.toString(value);
    return "00" + Integer.toString(value);
  }

  /**
   * Pads the given numbers with zeros to the left.
   *
   * @param value to pad (eg. 2)
   * @return The padded value (eg. "0002")
   */
  private static String leftZeroPad4(int value) {
    if (value >= 1000) return Integer.toString(value);
    if (value >= 100)  return "0" + Integer.toString(value);
    if (value >= 10)   return "00" + Integer.toString(value);
    else return "000" + Integer.toString(value);
  }

  /**
   * Returns the timezone component of and ISO 8601 date as <code>+[hh]:[ss]</code>,
   * <code>-[hh]:[ss]</code> or <code>Z</code>.
   *
   * @param offset in milliseconds.
   * @return the timezone component as <code>+[hh]:[ss]</code>, <code>-[hh]:[ss]</code> or <code>Z</code>
   */
  private static String toTimeZone(int offset) {
    if (offset == 0) return "Z";
    int _offset = offset;
    StringBuilder z = new StringBuilder(6);
    z.append(_offset >= 0? '+' : '-');
    if (_offset < 0) {
      _offset = _offset*-1;
    }
    z.append(leftZeroPad2(_offset / (1000*60*60))).append(':').append(leftZeroPad2((_offset / (1000*60)) % 60));
    return z.toString();
  }

  /**
   * Indicates whether the specified value is made of digits only.
   *
   * @param value The value to test.
   *
   * @return The index of any character is not a digit [0-9];
   *         <code>-1</code> otherwise.
   */
  private static int findNonDigitCharacter(String value) {
    final int length = value.length();
    for (int i = 0; i < length; i++) {
      char c = value.charAt(i);
      if (c < '0' || c > '9') return i;
    }
    return -1;
  }

}

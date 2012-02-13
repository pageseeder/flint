/*
 * This file is part of the Flint import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
tic-license-2.0.php
 */
package org.weborganic.flint.util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

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

  /** Utility class. */
  private Dates() {
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
    if (resolution == Resolution.DAY) {
      return Integer.valueOf(c.get(GregorianCalendar.YEAR) * 10000 + (c.get(GregorianCalendar.MONTH) + 1) * 100 + c.get(GregorianCalendar.DAY_OF_MONTH));
    }
    if (resolution == Resolution.MONTH) {
      return Integer.valueOf(c.get(GregorianCalendar.YEAR) * 100 + c.get(GregorianCalendar.MONTH) + 1);
    }
    if (resolution == Resolution.YEAR) {
      return Integer.valueOf(c.get(GregorianCalendar.YEAR));
    }
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
    StringBuilder z = new StringBuilder(6);
    z.append(offset >= 0? '+' : '-');
    if (offset < 0) offset = offset*-1;
    z.append(leftZeroPad2(offset / (1000*60*60))).append(':').append(leftZeroPad2((offset / (1000*60)) % 60));
    return z.toString();
  }

}

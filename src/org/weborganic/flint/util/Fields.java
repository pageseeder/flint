/**
 * 
 */
package org.weborganic.flint.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A set of utility methods for dealing with search fields.
 * 
 * @author Christophe Lauret
 * @version 12 August 2010
 */
public final class Fields {

  /** Utility class */
  private Fields() {
  }

  /**
   * Returns a mapping of fields with a default boost value of 1.0.
   * 
   * @param fields the list of fields to create the map.
   * @return the corresponding map with each field value mapped to a boost value of 1.0 
   */
  @Beta
  public static Map<String, Float> asBoostMap(List<String> fields) {
    Map<String, Float> map = new HashMap<String, Float>();
    for (String f : fields) {
      map.put(f, 1.0f);
    }
    return map;
  }

  /**
   * Indicates whether the given field name is valid.
   * 
   * <p>This method does not check for the existence of the field.
   * 
   * @param field the name of the field to check.
   * @return <code>true</code> if the field name is a valid name for the index;
   *         <code>false</code> otherwise.
   */
  @Beta
  public static boolean isValidName(String field) {
    return field != null && field.length() > 0;
  }

  /**
   * Returns a list of valid field names.
   * 
   * @param fields the list of fields to create the map.
   * @return a list of valid field names. 
   */
  @Beta
  public static List<String> filterNames(List<String> fields) {
    List<String> names = new ArrayList<String>();
    for (String f : fields) {
      if (isValidName(f)) names.add(f);
    }
    return names;
  }

  /**
   * Returns a list of possible field values from the specified text.
   * 
   * <p>You can use this method to extract the list of terms or phrase values to create a query.
   * 
   * <p>Spaces are ignored unless they are within double quotation marks.
   * 
   * <p>See examples below: 
   * <pre>
   * |Big|             => [Big]
   * |Big bang|        => [Big, bang]
   * |   Big   bang |  => [Big, bang]
   * |The "Big bang"|  => [The, "Big bang"]
   * |The "Big bang|   => [The, "Big, bang]
   * </pre>
   * 
   * <p>Note: this class does not excludes terms which could be considered stop words by the index.
   * 
   * @param text The text for which values are needed.
   * @return the corresponding list of values.
   */
  @Beta
  public static List<String> toValues(String text) {
    List<String> values = new ArrayList<String>();
    Pattern p = Pattern.compile("(\\\"[^\\\"]+\\\")|(\\S+)");
    Matcher m = p.matcher(text);
    while (m.find()) {
      values.add(m.group());
    }
    return values;
  }

}

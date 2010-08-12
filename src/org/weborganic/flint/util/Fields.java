/**
 * 
 */
package org.weborganic.flint.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
   * Returns a mapping of fields to their boost value.
   */
  @Beta public static Map<String, Float> asBoostMap(List<String> fields) {
    Map<String, Float> map = new HashMap<String, Float>();
    for (String f : fields) {
      map.put(f, 1.0f);
    }
    return map;
  }

}

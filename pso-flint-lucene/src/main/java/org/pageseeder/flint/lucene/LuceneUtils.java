package org.pageseeder.flint.lucene;

import org.apache.lucene.document.DateTools;
import org.pageseeder.flint.indexing.FlintField;

public class LuceneUtils {

  public static DateTools.Resolution toResolution(FlintField.Resolution resolution) {
    if (resolution == null) return null;
    switch (resolution) {
      case DAY         : return DateTools.Resolution.DAY;
      case HOUR        : return DateTools.Resolution.HOUR;
      case MILLISECOND : return DateTools.Resolution.MILLISECOND;
      case MINUTE      : return DateTools.Resolution.MINUTE;
      case MONTH       : return DateTools.Resolution.MONTH;
      case SECOND      : return DateTools.Resolution.SECOND;
      case YEAR        : return DateTools.Resolution.YEAR;
    }
    return null;
  }
}

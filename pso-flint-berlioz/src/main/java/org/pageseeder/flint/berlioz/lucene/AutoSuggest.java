/*
 * Decompiled with CFR 0_110.
 * 
 * Could not load the following classes:
 *  org.pageseeder.berlioz.BerliozException
 *  org.pageseeder.berlioz.Beta
 *  org.pageseeder.berlioz.content.ContentRequest
 *  org.pageseeder.xmlwriter.XMLWriter
 */
package org.pageseeder.flint.berlioz.lucene;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.flint.berlioz.model.IndexMaster;
import org.pageseeder.flint.berlioz.util.GeneratorErrors;
import org.pageseeder.flint.lucene.search.AutoSuggest.Suggestion;
import org.pageseeder.xmlwriter.XMLWriter;

/*
 * Used to clear an index.
 */
@Beta
public final class AutoSuggest extends LuceneIndexGenerator {
//  private static final Logger LOGGER = LoggerFactory.getLogger(AutoSuggest.class);

  @Override
  public void processSingle(IndexMaster index, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    String name    = req.getParameter("name");
    String fields  = req.getParameter("fields", req.getParameter("field", "fulltext"));
    String term    = req.getParameter("term");
    String results = req.getParameter("results", "10");
    boolean terms  = "true".equals(req.getParameter("terms", "false"));
    String rfields = req.getParameter("return-fields", req.getParameter("return-field"));
    String criteriaFields = req.getParameter("criteria-fields", "");
    String criteriaValues = req.getParameter("criteria-values", "");
    String weight  = req.getParameter("weight", "");
    // validate fields
    if (term == null) {
      GeneratorErrors.noParameter(req, xml, "term");
      return;
    }
    int nbresults;
    try {
      nbresults = Integer.parseInt(results);
    } catch (NumberFormatException ex) {
      GeneratorErrors.invalidParameter(req, xml, "results");
      return;
    }
    org.pageseeder.flint.lucene.search.AutoSuggest suggestor;
    if (name == null) {
      // compute weights
      Map<String, Float> weights = new HashMap<>();
      for (String w : weight.split(",")) {
        String[] parts = w.split(":");
        if (parts.length == 2) {
          try {
            weights.put(parts[0], Float.valueOf(parts[1]));
          } catch (NumberFormatException ex) {
            GeneratorErrors.invalidParameter(req, xml, "weight");
            return;
          }
        }
      }
      suggestor = index.getAutoSuggest(Arrays.asList(fields.split(",")), terms, 2,
          rfields == null ? null : Arrays.asList(rfields.split(",")),
          criteriaFields == null ? null : Arrays.asList(criteriaFields.split(",")),
          weights);
      if (suggestor == null) {
        GeneratorErrors.error(req, xml, "server", "Failed to create autosuggest", ContentStatus.INTERNAL_SERVER_ERROR);
        return;
      }
    } else {
      suggestor = index.getAutoSuggest(name);
      if (suggestor == null) {
        GeneratorErrors.invalidParameter(req, xml, "name");
        return;
      }
    }


    List<String> criteria =  (criteriaValues == null || criteriaValues.trim().length() == 0)? null : Arrays.asList(criteriaValues.split(","));

    List<Suggestion> suggestions = suggestor.suggest(term, criteria, nbresults);

    // output
    xml.openElement("suggestions");
    for (Suggestion sug : suggestions) {
      xml.openElement("suggestion");
      xml.attribute("text",      sug.text);
      xml.attribute("highlight", sug.highlight);
      if (sug.document != null) {
        for (String field : sug.document.keySet()) {
          for (String value : sug.document.get(field)) {
            xml.openElement("field");
            xml.attribute("name", field);
            xml.writeText(value);
            xml.closeElement();
          }
        }
      }
      xml.closeElement();
    }
    xml.closeElement();
  }

  @Override
  public void processMultiple(Collection<IndexMaster> indexes, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    GeneratorErrors.error(req, xml, "forbidden", "Cannnot autosuggest on multiple indexes yet", ContentStatus.BAD_REQUEST);
  }
}

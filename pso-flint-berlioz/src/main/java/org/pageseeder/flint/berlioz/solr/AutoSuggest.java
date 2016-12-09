/*
 * Decompiled with CFR 0_110.
 * 
 * Could not load the following classes:
 *  org.pageseeder.berlioz.BerliozException
 *  org.pageseeder.berlioz.Beta
 *  org.pageseeder.berlioz.content.ContentRequest
 *  org.pageseeder.xmlwriter.XMLWriter
 */
package org.pageseeder.flint.berlioz.solr;

import java.io.IOException;
import java.util.List;

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.flint.berlioz.model.SolrIndexMaster;
import org.pageseeder.flint.berlioz.util.GeneratorErrors;
import org.pageseeder.flint.solr.query.AutoSuggest.Suggestion;
import org.pageseeder.xmlwriter.XMLWriter;

/*
 * Used to clear an index.
 */
@Beta
public final class AutoSuggest extends SolrIndexGenerator {
//  private static final Logger LOGGER = LoggerFactory.getLogger(AutoSuggest.class);

  @Override
  public void process(SolrIndexMaster index, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    String name    = req.getParameter("name");
    String term    = req.getParameter("term");
    String results = req.getParameter("results", "10");
    // validate fields
    if (term == null) {
      GeneratorErrors.noParameter(req, xml, "term");
      return;
    }
    if (name == null) {
      GeneratorErrors.noParameter(req, xml, "name");
      return;
    }
    int nbresults;
    try {
      nbresults = Integer.parseInt(results);
    } catch (NumberFormatException ex) {
      GeneratorErrors.invalidParameter(req, xml, "results");
      return;
    }
    org.pageseeder.flint.solr.query.AutoSuggest suggestor;
    suggestor = index.getAutoSuggest(name);
    if (suggestor == null) {
      GeneratorErrors.invalidParameter(req, xml, "name");
      return;
    }
    List<Suggestion> suggestions = suggestor.suggest(term, nbresults);

    // output
    xml.openElement("suggestions");
    for (Suggestion sug : suggestions) {
      xml.openElement("suggestion");
      xml.attribute("text", sug.text);
      if (sug.payload != null) {
        xml.attribute("payload", sug.payload);
      }
      xml.closeElement();
    }
    xml.closeElement();
  }

}

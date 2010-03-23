/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.FuzzyTermEnum;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.WildcardTermEnum;

import com.topologi.diffx.xml.XMLWritable;
import com.topologi.diffx.xml.XMLWriter;

/**
 * A search assistant provides a decomposition of the query as well as possible terms 
 * replacements in order to assist the user modify his search.
 * 
 * @author Christophe Lauret (Weborganic)
 * 
 * @version 11 January 2007
 */
public final class SearchAssistant implements XMLWritable {

  /**
   * The logger for this class.
   */
  private static final Logger LOGGER = Logger.getLogger(SearchAssistant.class);

  /**
   * The index reader.
   */
  private final IndexReader ireader;

  /**
   * The Lucene Query.
   */
  private final Query query;

  /**
   * Creates a new SearchResults.
   *
   * @param ireader The index reader.
   * @param query   The Lucene search query.
   */
  public SearchAssistant(IndexReader ireader, Query query) {
    this.ireader = ireader;
    this.query = query;
  }

  /**
   * Serialises the search assistant as XML.
   * 
   * @param xml The XML writer.
   * 
   * @throws IOException Should there be any I/O exception while writing the XML.
   */
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("search-assistant", true);

    Collection terms = QueryUtils.getTerms(this.query);

    // iterate over all the terms in the query
    for (Iterator i = terms.iterator(); i.hasNext();) {
      Term term = (Term)i.next();

      // find the fuzzy terms
      ArrayList flist = fuzzyTerms(term);
      ArrayList wlist = wildcardTerms(term);

      if (!(flist.isEmpty() && wlist.isEmpty())) {
        xml.openElement("term-assistant", true);
        xml.openElement("term", false);
        xml.attribute("field", term.field());
        xml.writeText(term.text());
        xml.closeElement();

        // find the wildcard terms
        if (!flist.isEmpty()) {
          xml.openElement("fuzzy-terms", true);
          for (int j = 0; j < flist.size(); j++) {
            xml.element("term", (String)flist.get(j));
          }
          xml.closeElement();
        }

        // find the wildcard terms
        if (!wlist.isEmpty()) {
          xml.openElement("wildcard-terms", true);
          for (int j = 0; j < wlist.size(); j++) {
            xml.element("term", (String)wlist.get(j));
          }
          xml.closeElement();
        }

        // close 'term-assistant'
        xml.closeElement();
      }
    }

    // close 'search-assistant'
    xml.closeElement();
  }
  
  /**
   * Returns the list of fuzzy terms for the given term.
   *  
   * @param term The search term.
   * 
   * @return The list of fuzzy term for the given term.
   * 
   * @throws IOException Should and error be reported by the index reader
   */
  private ArrayList fuzzyTerms(Term term) throws IOException {
    // find the fuzzy terms
    FuzzyTermEnum fuzzy = new FuzzyTermEnum(this.ireader, term);
    LOGGER.debug("Extracting fuzzy terms for "+term);
    ArrayList terms = new ArrayList();
    do {
      Term t = fuzzy.term();
      if (t != null) {
        LOGGER.debug("Found "+t.text());
        terms.add(t.text());
      }
    } while (fuzzy.next());
    fuzzy.close();
    return terms;
  }
  
  /**
   * Returns the list of wildcard terms for the given term.
   *  
   * @param term The search term.
   * 
   * @return The list of wildcard term for the given term.
   * 
   * @throws IOException Should and error be reported by the index reader
   */
  private ArrayList wildcardTerms(Term term) throws IOException {
    // find the wildcard terms
    Term wildcardTerm = term.createTerm(term.text()+WildcardTermEnum.WILDCARD_STRING);
    WildcardTermEnum wildcard = new WildcardTermEnum(this.ireader, wildcardTerm);
    LOGGER.debug("Extracting wildcard terms for "+term);
    ArrayList terms = new ArrayList();
    do {
      Term t = wildcard.term();
      if (t != null) {
        LOGGER.debug("Found "+t.text());
        terms.add(t.text());
      }
    } while (wildcard.next());
    wildcard.close();
    return terms;
  }
}


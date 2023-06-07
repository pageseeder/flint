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
package org.pageseeder.flint.berlioz.lucene;

import java.io.IOException;
import java.util.Collection;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.util.MD5;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.berlioz.model.IndexMaster;
import org.pageseeder.flint.lucene.MultipleIndexReader;
import org.pageseeder.flint.lucene.search.Terms;
import org.pageseeder.flint.lucene.util.Bucket;
import org.pageseeder.flint.lucene.util.Bucket.Entry;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lookup the fuzzy term for the specified term.
 *
 * <p>Generate an ETag based on the parameters and the last modified date of the index.
 *
 * @author Christophe Lauret
 * @author Jean-Baptiste Reure
 *
 * @version 0.6.20 - 27 September 2011
 * @since 0.6.0
 */
public final class LookupPrefixTerms extends LuceneIndexGenerator implements Cacheable {

  /**
   * Logger for debugging
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(LookupPrefixTerms.class);

  @Override
  public String getETag(ContentRequest req) {
    // Get relevant parameters
    String etag = req.getParameter("term", "keyword") + '%' +
        req.getParameter("field", "") + '%' +
        buildIndexEtag(req);
    // MD5 of computed etag value
    return MD5.hash(etag);
  }

  @Override
  public void processMultiple(Collection<IndexMaster> masters, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    // Create a new query object
    String field = req.getParameter("field", "keyword");
    String value = req.getParameter("term", "");
    if (value.isEmpty()) {
      req.setStatus(ContentStatus.BAD_REQUEST);
      return;
    }
    Term term = new Term(field, value);

    LOGGER.debug("Looking up prefix terms for {}", term);
    xml.openElement("prefix-terms");

    MultipleIndexReader multiReader = buildMultiReader(masters);
    try {
      IndexReader reader = multiReader.grab();
      Bucket<Term> bucket = new Bucket<>(20);
      Terms.prefix(reader, bucket, term);
      for (Entry<Term> e : bucket.entrySet()) {
        Terms.toXML(xml, e.item(), e.count());
      }
    } catch (IOException | IndexException ex) {
      throw new BerliozException("Exception thrown while fetching fuzzy terms", ex);
    } finally {
      multiReader.releaseSilently();
      xml.closeElement();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void processSingle(IndexMaster index, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    // Create a new query object
    String field = req.getParameter("field", "keyword");
    String value = req.getParameter("term", "");
    if (value.isEmpty()) {
      req.setStatus(ContentStatus.BAD_REQUEST);
      return;
    }
    Term term = new Term(field, value);

    LOGGER.debug("Looking up prefix terms for {}", term);
    xml.openElement("prefix-terms");
    IndexReader reader = null;
    try {
      Bucket<Term> bucket = new Bucket<>(20);
      reader = index.grabReader();
      Terms.prefix(reader, bucket, term);
      for (Entry<Term> e : bucket.entrySet()) {
        Terms.toXML(xml, e.item(), e.count());
      }
    } catch (IOException | IndexException ex) {
      throw new BerliozException("Exception thrown while fetching fuzzy terms", ex);
    } finally {
      index.releaseSilently(reader);
      xml.closeElement();
    }
  }

}

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
package org.pageseeder.flint.lucene.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.pageseeder.flint.Index;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.lucene.LuceneIndexQueries;

/**
 * A collection of utility methods to manipulate and extract terms.
 *
 * @author Christophe Lauret
 * @version 18 March 2011
 */
public final class Facets {

//  /**
//   * private logger
//   */
//  private final static Logger LOGGER = LoggerFactory.getLogger(Facets.class);

  /** Utility class. */
  private Facets() {
  }

  /**
   * Returns the list of term and how frequently they are used by performing a fuzzy match on the
   * specified term.
   *
   * @param field  the field to use as a facet
   * @param upTo   the max number of values to return
   * @param query  a predicate to apply on the facet (can be null or empty)
   *
   * @return the facte instance.
   *
   * @throws IOException    if there was an error reading the index or creating the condition query
   * @throws IndexException if there was an error getting the reader or searcher.
   */
  public static FieldFacet getFacet(String field, int upTo, Query query, Index index) throws IndexException, IOException {
    FieldFacet facet = null;
    IndexReader reader = null;
    IndexSearcher searcher = null;
    try {
      // Retrieve all terms for the field
      reader = LuceneIndexQueries.grabReader(index);
      facet = FieldFacet.newFacet(field, reader);

      // search
      searcher = LuceneIndexQueries.grabSearcher(index);
      facet.compute(searcher, query, upTo);

    } finally {
      LuceneIndexQueries.releaseQuietly(index, reader);
      LuceneIndexQueries.releaseQuietly(index, searcher);
    }
    return facet;
  }

  /**
   * Returns the list of term and how frequently they are used by performing a fuzzy match on the
   * specified term.
   *
   * @param fields the fields to use as facets
   * @param upTo   the max number of values to return
   * @param query  a predicate to apply on the facet (can be null or empty)
   *
   * @throws IndexException if there was an error reading the indexes or creating the condition query
   * @throws IllegalStateException If one of the indexes is not initialised
   */
  public static List<FieldFacet> getFacets(List<String> fields, int upTo, Query query, Index index) throws IOException, IndexException {
    if (query == null)
      return getFacets(fields, upTo, index);
    // parameter checks
    if (fields == null || fields.isEmpty() || index == null)
      return Collections.emptyList();
    List<FieldFacet> facets = new ArrayList<FieldFacet>();
    for (String field : fields) {
      if (field.length() > 0) {
        facets.add(getFacet(field, upTo, query, index));
      }
    }
    return facets;
  }

  /**
   * Returns the list of term and how frequently they are used by performing a fuzzy match on the
   * specified term.
   *
   * @param fields the fields to use as facets
   * @param upTo   the max number of values to return
   * @param query  a predicate to apply on the facet (can be null or empty)
   *
   * @throws IndexException if there was an error reading the indexes or creating the condition query
   * @throws IllegalStateException If one of the indexes is not initialised
   */
  public static List<FieldFacet> getFacets(int maxValues, Index index) throws IOException, IndexException {
    return getFacets(null, maxValues, index);
  }

  /**
   * Returns the list of term and how frequently they are used by performing a fuzzy match on the
   * specified term.
   *
   * @param fields the fields to use as facets
   * @param upTo   the max number of values to return
   * @param query  a predicate to apply on the facet (can be null or empty)
   *
   * @throws IndexException if there was an error reading the indexes or creating the condition query
   * @throws IllegalStateException If one of the indexes is not initialised
   */
  public static List<FieldFacet> getFacets(List<String> fields, int maxValues, Index index) throws IOException, IndexException {
    List<FieldFacet> facets = new ArrayList<FieldFacet>();
    // use reader
    IndexReader reader     = LuceneIndexQueries.grabReader(index);
    IndexSearcher searcher = LuceneIndexQueries.grabSearcher(index);
    try {
      // loop through fields
      List<String> loopfields = fields == null ? Terms.fields(reader) : fields;
      for (String field : loopfields) {
        if (field.length() > 0 && field.charAt(0) != '_') {
          FieldFacet facet = FieldFacet.newFacet(field, reader, maxValues);
          if (facet != null) {
            facet.compute(searcher, null, maxValues);
            facets.add(facet);
          }
        }
      }
    } finally {
      LuceneIndexQueries.releaseQuietly(index, reader);
      LuceneIndexQueries.releaseQuietly(index, searcher);
    }
    return facets;
  }

  /**
   * Returns the list of term and how frequently they are used by performing a fuzzy match on the
   * specified term.
   *
   * @param fields the fields to use as facets
   * @param upTo   the max number of values to return
   * @param query  a predicate to apply on the facet (can be null or empty)
   *
   * @throws IndexException if there was an error reading the indexes or creating the condition query
   * @throws IllegalStateException If one of the indexes is not initialised
   */
  public static List<FieldFacet> getFacets(List<String> fields, int upTo, Query query, List<Index> indexes) throws IOException, IndexException {
    // parameter checks
    if (fields == null || fields.isEmpty() || indexes.isEmpty())
      return Collections.emptyList();
    // check for one index only
    if (indexes.size() == 1)
      return getFacets(fields, upTo, query, indexes.get(0));
    // retrieve all searchers and readers
    Map<Index, IndexReader> readers = new HashMap<>();
    // grab a reader for each indexes
    for (int i = 0; i < indexes.size(); i++) {
      Index index = indexes.get(i);
      readers.put(index, LuceneIndexQueries.grabReader(index));
    }
    List<FieldFacet> facets = new ArrayList<FieldFacet>();
    try {
      // Retrieve all terms for the field
      IndexReader multiReader = new MultiReader(readers.values().toArray(new IndexReader[] {}));
      IndexSearcher multiSearcher = new IndexSearcher(multiReader);
      for (String field : fields) {
        if (field.length() > 0) {
          FieldFacet facet = FieldFacet.newFacet(field, multiReader);
          // search
          facet.compute(multiSearcher, query, upTo);
          // store it
          facets.add(facet);
        }
      }
    } finally {
      // now release everything we used
      for (Entry<Index, IndexReader> entry : readers.entrySet())  {
        LuceneIndexQueries.release(entry.getKey(), entry.getValue());
      }
    }
    return facets;
  }

  /**
   * Returns the list of term and how frequently they are used by performing a fuzzy match on the
   * specified term.
   *
   * @param fields the fields to use as facets
   * @param upTo   the max number of values to return
   * @param query  a predicate to apply on the facet (can be null or empty)
   *
   * @throws IndexException if there was an error reading the indexes or creating the condition query
   * @throws IllegalStateException If one of the indexes is not initialised
   */
  public static List<FieldFacet> getFacets(int maxValues, List<Index> indexes) throws IOException, IndexException {
    return getFacets(null, maxValues, indexes);
  }

  /**
   * Returns the list of term and how frequently they are used by performing a fuzzy match on the
   * specified term.
   *
   * @param fields the fields to use as facets
   * @param upTo   the max number of values to return
   * @param query  a predicate to apply on the facet (can be null or empty)
   *
   * @throws IndexException if there was an error reading the indexes or creating the condition query
   * @throws IllegalStateException If one of the indexes is not initialised
   */
  public static List<FieldFacet> getFacets(List<String> fields, int maxValues, List<Index> indexes) throws IOException, IndexException {
    // retrieve all searchers and readers
    Map<Index, IndexReader> readers = new HashMap<>();
    // grab a reader for each indexes
    for (int i = 0; i < indexes.size(); i++) {
      Index index = indexes.get(i);
      readers.put(index, LuceneIndexQueries.grabReader(index));
    }
    List<FieldFacet> facets = new ArrayList<FieldFacet>();
    try {
      // Retrieve all terms for the field
      IndexReader multiReader = new MultiReader(readers.values().toArray(new IndexReader[] {}));
      IndexSearcher multiSearcher = new IndexSearcher(multiReader);
      // loop through fields
      List<String> loopfields = fields == null ? Terms.fields(multiReader) : fields;
      for (String field : loopfields) {
        if (field.length() > 0) {
          FieldFacet facet = FieldFacet.newFacet(field, multiReader, maxValues);
          if (facet != null) {
            facet.compute(multiSearcher, null, maxValues);
            facets.add(facet);
          }
        }
      }
    } finally {
      // now release everything we used
      for (Entry<Index, IndexReader> entry : readers.entrySet())  {
        LuceneIndexQueries.release(entry.getKey(), entry.getValue());
      }
    }
    return facets;
  }

}

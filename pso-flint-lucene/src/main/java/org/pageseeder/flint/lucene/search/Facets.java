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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.pageseeder.flint.Index;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.catalog.Catalog;
import org.pageseeder.flint.catalog.Catalogs;
import org.pageseeder.flint.lucene.LuceneIndexQueries;
import org.pageseeder.flint.lucene.LuceneUtils;
import org.pageseeder.flint.lucene.facet.*;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

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
   * @deprecated use FlexibleFieldFacet instead
   *
   * @param field  the field to use as a facet
   * @param upTo   the max number of values to return
   * @param query  a predicate to apply on the facet (can be null or empty)
   *
   * @return the facet instance.
   *
   * @throws IOException    if there was an error reading the index or creating the condition query
   * @throws IndexException if there was an error getting the reader or searcher.
   */
  public static FieldFacet getFacet(String field, int upTo, Query query, Index index) throws IndexException, IOException {
    FieldFacet facet;
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
   * @deprecated use FlexibleFieldFacet instead
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
    List<FieldFacet> facets = new ArrayList<>();
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
   * @deprecated use FlexibleFieldFacet instead
   *
   * @param maxValues  the max number of values to return
   * @param index      the index to search
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
   * @deprecated use FlexibleFieldFacet instead
   *
   * @param fields     the fields to use as facets
   * @param maxValues  the max number of values to return
   * @param index      the index to search
   *
   * @throws IndexException if there was an error reading the indexes or creating the condition query
   * @throws IllegalStateException If one of the indexes is not initialised
   */
  public static List<FieldFacet> getFacets(List<String> fields, int maxValues, Index index) throws IOException, IndexException {
    List<FieldFacet> facets = new ArrayList<>();
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
            facet.compute(searcher, maxValues);
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
   * @deprecated use FlexibleFieldFacet instead
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
    for (Index index : indexes) {
      readers.put(index, LuceneIndexQueries.grabReader(index));
    }
    List<FieldFacet> facets = new ArrayList<>();
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
   * @deprecated use FlexibleFieldFacet instead
   *
   * @param maxValues  the max number of values to return
   * @param indexes    the indexes to search
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
   * @deprecated use FlexibleFieldFacet instead
   *
   * @param fields     the fields to use as facets
   * @param maxValues  the max number of values to return
   * @param indexes    the indexes to search
   *
   * @throws IndexException if there was an error reading the indexes or creating the condition query
   * @throws IllegalStateException If one of the indexes is not initialised
   */
  public static List<FieldFacet> getFacets(List<String> fields, int maxValues, List<Index> indexes) throws IOException, IndexException {
    // retrieve all searchers and readers
    Map<Index, IndexReader> readers = new HashMap<>();
    // grab a reader for each indexes
    for (Index index : indexes) {
      readers.put(index, LuceneIndexQueries.grabReader(index));
    }
    List<FieldFacet> facets = new ArrayList<>();
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
            facet.compute(multiSearcher, maxValues);
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

  /**
   * Create a new facet using the name and catalog provided.
   * If the field is numeric, no facet is created (null is returned).
   *
   * @param name    the field name
   * @param catalog the catalog containing a possible field definition
   *
   * @return the facet if created, can be null
   */
  public static FlexibleFieldFacet createFacet(String name, Catalog catalog) {
    if (catalog != null && catalog.getNumericType(name) != null)
      return null;
    if (catalog != null && catalog.getResolution(name) != null)
      return DateFieldFacet.newFacet(name, LuceneUtils.toResolution(catalog.getResolution(name)));
    return StringFieldFacet.newFacet(name);
  }

  /**
   * Create a new range facet using the name and catalog provided.
   *
   * @param name    the field name
   * @param ranges  the list of ranges
   * @param catalog the catalog containing a possible field definition
   *
   * @return the facet (never null)
   */
  public static FlexibleRangeFacet createRangeFacet(String name, Collection<FlexibleRangeFacet.Range> ranges, Catalog catalog) {
    if (catalog != null && catalog.getNumericType(name) != null) {
      NumericRangeFacet.Builder builder = new NumericRangeFacet.Builder().name(name).numeric(catalog.getNumericType(name));
      for (FlexibleRangeFacet.Range range : ranges) builder.addRange(range);
      return builder.build();
    }
    if (catalog != null && catalog.getResolution(name) != null) {
      DateRangeFacet.Builder builder = new DateRangeFacet.Builder().name(name).resolution(LuceneUtils.toResolution(catalog.getResolution(name)));
      for (FlexibleRangeFacet.Range range : ranges) builder.addRange(range);
      return builder.build();
    }
    StringRangeFacet.Builder builder = new StringRangeFacet.Builder().name(name);
    for (FlexibleRangeFacet.Range range : ranges) builder.addRange(range);
    return builder.build();
  }


  /**
   * Returns the list of term and how frequently they are used by performing a fuzzy match on the
   * specified term.
   *
   * @param fields     the fields to use as facets
   * @param maxValues  the max number of values to return
   * @param indexes    the indexes to search
   *
   * @throws IllegalStateException If one of the indexes is not initialised
   */
  public static List<FlexibleFieldFacet> getFlexibleFacets(List<String> fields, int maxValues, List<Index> indexes) throws IOException {
    // retrieve all searchers and readers
    Map<Index, IndexReader> readers = new HashMap<>();
    // grab a reader for each index
    // assume they all have the same catalog
    Catalog catalog = null;
    for (Index index : indexes) {
      if (catalog == null) catalog = Catalogs.getCatalog(index.getCatalog());
      readers.put(index, LuceneIndexQueries.grabReader(index));
    }
    List<FlexibleFieldFacet> facets = new ArrayList<>();
    try {
      // Retrieve all terms for the field
      IndexReader multiReader = new MultiReader(readers.values().toArray(new IndexReader[] {}));
      IndexSearcher multiSearcher = new IndexSearcher(multiReader);
      // loop through fields
      List<String> loopfields = fields == null ? Terms.fields(multiReader) : fields;
      for (String field : loopfields) {
        if (field.length() > 0) {
          FlexibleFieldFacet facet = createFacet(field, catalog);
          if (facet != null) {
            facet.compute(multiSearcher, maxValues);
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


  /**
   * Returns the list of term and how frequently they are used by performing a fuzzy match on the
   * specified term.
   *
   * @param fields the fields to use as facets
   * @param upTo   the max number of values to return
   * @param query  a predicate to apply on the facet (can be null or empty)
   *
   * @throws IllegalStateException If one of the indexes is not initialised
   */
  public static List<FlexibleFieldFacet> getFlexibleFacets(List<String> fields, int upTo, Query query, List<Index> indexes) throws IOException {
    // parameter checks
    if (fields == null || fields.isEmpty() || indexes.isEmpty())
      return Collections.emptyList();
    // check for one index only
    if (indexes.size() == 1)
      return getFlexibleFacets(fields, upTo, query, indexes.get(0));
    // retrieve all searchers and readers
    Map<Index, IndexReader> readers = new HashMap<>();
    // grab a reader for each index
    // assume they all have the same catalog
    Catalog catalog = null;
    for (Index index : indexes) {
      if (catalog == null) catalog = Catalogs.getCatalog(index.getCatalog());
      readers.put(index, LuceneIndexQueries.grabReader(index));
    }
    List<FlexibleFieldFacet> facets = new ArrayList<>();
    try {
      // Retrieve all terms for the field
      IndexReader multiReader = new MultiReader(readers.values().toArray(new IndexReader[] {}));
      IndexSearcher multiSearcher = new IndexSearcher(multiReader);
      for (String field : fields) {
        if (field.length() > 0) {
          FlexibleFieldFacet facet = createFacet(field, catalog);
          if (facet != null) {
            // search
            facet.compute(multiSearcher, query, upTo);
            // store it
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

  /**
   * Returns the list of term and how frequently they are used by performing a fuzzy match on the
   * specified term.
   *
   * @param fields     the fields to use as facets
   * @param maxValues  the max number of values to return
   * @param index      the index to search
   *
   * @throws IllegalStateException If one of the indexes is not initialised
   */
  public static List<FlexibleFieldFacet> getFlexibleFacets(List<String> fields, int maxValues, Index index) throws IOException {
    return getFlexibleFacets(fields, maxValues, null, index);
  }

  /**
   * Returns the list of term and how frequently they are used by performing a fuzzy match on the
   * specified term.
   *
   * @param fields     the fields to use as facets
   * @param maxValues  the max number of values to return
   * @param query      the base query, can be null
   * @param index      the index to search
   *
   * @throws IllegalStateException If one of the indexes is not initialised
   */
  public static List<FlexibleFieldFacet> getFlexibleFacets(List<String> fields, int maxValues, Query query, Index index) throws IOException {
    List<FlexibleFieldFacet> facets = new ArrayList<>();
    Catalog catalog = Catalogs.getCatalog(index.getCatalog());
    // use reader
    IndexReader reader     = LuceneIndexQueries.grabReader(index);
    IndexSearcher searcher = LuceneIndexQueries.grabSearcher(index);
    try {
      // loop through fields
      List<String> loopfields = fields == null ? Terms.fields(reader) : fields;
      for (String field : loopfields) {
        if (field.length() > 0 && field.charAt(0) != '_') {
          FlexibleFieldFacet facet = createFacet(field, catalog);
          if (facet != null) {
            if (query == null) facet.compute(searcher, maxValues);
            else facet.compute(searcher, query, maxValues);
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
}

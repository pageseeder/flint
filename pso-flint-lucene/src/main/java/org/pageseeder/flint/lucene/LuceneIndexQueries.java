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
package org.pageseeder.flint.lucene;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.search.*;
import org.pageseeder.flint.Index;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexIO;
import org.pageseeder.flint.lucene.query.SearchPaging;
import org.pageseeder.flint.lucene.query.SearchQuery;
import org.pageseeder.flint.lucene.query.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to handle lucene queries.
 *
 * @author Jean-Baptiste Reure
 *
 * @version 27 May 2019
 */
public final class LuceneIndexQueries {

  /**
   * Logger will receive debugging and low-level data, use the listener to capture specific indexing operations.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(LuceneIndexQueries.class);

  // Public external methods
  // ----------------------------------------------------------------------------------------------

  /**
   * Run a search on the given Index.
   *
   * @param index the Index to run the search on
   * @param query the query to run
   * @return the search results
   * @throws IndexException if any error occurred while performing the search
   */
  public static SearchResults query(Index index, SearchQuery query) throws IndexException {
    return query(index, query, new SearchPaging());
  }

  /**
   * Run a search on the given Index.
   *
   * @param index  the Index to run the search on
   * @param query  the query to run
   * @param paging paging details (can be <code>null</code>)
   *
   * @return the search results
   *
   * @throws IndexException if any error occurred while performing the search
   */
  public static SearchResults query(Index index, SearchQuery query, SearchPaging paging) throws IndexException {
    LuceneIndexIO io = getIndexIO(index);
    IndexSearcher searcher = io == null ? null : io.bookSearcher();
    if (searcher != null) {
      try {
        Query lquery = query.toQuery();
        if (lquery == null) {
          io.releaseSearcher(searcher);
          throw new IndexException("Failed performing a query on the Index because the query is null",
              new NullPointerException("Null query"));
        }
        LOGGER.debug("Performing search [{}] on index {}", query, index);
        Sort sort = query.getSort();
        if (sort == null) {
          sort = Sort.INDEXORDER;
        }
        // load the scores
        TopFieldCollector tfc = TopFieldCollector.create(sort, paging.getHitsPerPage() * paging.getPage(), Integer.MAX_VALUE);
        searcher.search(lquery, tfc);
        return new SearchResults(query, tfc.topDocs().scoreDocs, tfc.getTotalHits(), paging, io, searcher);
      } catch (IOException ex) {
        io.releaseSearcher(searcher);
        throw new IndexException("Failed performing a query on the Index because of an I/O problem", ex);
      } catch (IllegalArgumentException ex) {
        if (ex.getMessage() != null
            && ex.getMessage().contains("was indexed with bytesPerDim")
            && ex.getMessage().contains("but this query has bytesPerDim")) {
          // no matches
          return new SearchResults(query, new ScoreDoc[] {}, 0, paging, io, searcher);
        } else {
          io.releaseSearcher(searcher);
          throw new IndexException("Failed performing invalid query", ex);
        }
      }
    }
    return null;
  }

  /**
   * Run a search on the given Index.
   *
   * @param index    the Index to run the search on
   * @param query    the query to run
   * @param results  the results' collector
   *
   * @throws IndexException if any error occurred while performing the search
   */
  public static void query(Index index, Query query, Collector results) throws IndexException {
    LuceneIndexIO io = getIndexIO(index);
    IndexSearcher searcher = io == null ? null : io.bookSearcher();
    if (searcher != null) {
      try {
        LOGGER.debug("Performing search [{}] on index {}", query, index);
        // load the scores
        searcher.search(query, results);
      } catch (IOException e) {
        throw new IndexException("Failed performing a query on the Index because of an I/O problem", e);
      } finally {
        io.releaseSearcher(searcher);
      }
    }
  }

  /**
   * Run a search on the given Indexes.
   *
   * @param indexes the Indexes to run the search on
   * @param query   the query to run
   *
   * @return the search results
   *
   * @throws IndexException if any error occurred while performing the search
   */
  public static SearchResults query(List<Index> indexes, SearchQuery query) throws IndexException {
    return query(indexes, query, new SearchPaging());
  }

  /**
   * Run a search on the given Indexes.
   *
   * @param indexes  the Indexes to run the search on
   * @param query    the query to run
   * @param paging   paging details (can be <code>null</code>)
   *
   * @return the search results
   *
   * @throws IndexException if any error occurred while performing the search
   */
  public static SearchResults query(List<Index> indexes, SearchQuery query, SearchPaging paging) throws IndexException {
    Query lquery = query.toQuery();
    if (lquery == null)
      throw new IndexException("Failed performing a query because the query is null", new NullPointerException("Null query"));
    // find all readers
    Map<LuceneIndexIO, IndexReader> readersMap = new HashMap<>();
    IndexReader[] readers = new IndexReader[indexes.size()];
    // grab a reader for each indexes
    for (int i = 0; i < indexes.size(); i++) {
      LuceneIndexIO io = getIndexIO(indexes.get(i));
      // make sure index has been setup
      if (io != null) {
        // grab what we need
        IndexReader reader = io.bookReader();
        readers[i] = reader;
        readersMap.put(io, reader);
      }
    }
    try {
      MultiReader reader = new MultiReader(readers);
      IndexSearcher searcher = new IndexSearcher(reader);
      LOGGER.debug("Performing search [{}] on {} indexes", query, readers.length);
      Sort sort = query.getSort();
      if (sort == null)
        sort = Sort.INDEXORDER;
      // load the scores
      TopFieldDocs results = searcher.search(lquery, paging.getHitsPerPage() * paging.getPage(), sort);
      return new SearchResults(query, results, paging, readersMap, searcher);
    } catch (IOException e) {
      for (LuceneIndexIO io : readersMap.keySet())
        io.releaseReader(readersMap.get(io));
      throw new IndexException("Failed performing a query on the Index because of an I/O problem", e);
    }
  }

  public static MultipleIndexReader getMultipleIndexReader(List<Index> indexes) {
    return new MultipleIndexReader(indexes);
  }

  // Lower level API providing access to Lucene objects
  // ----------------------------------------------------------------------------------------------

  /**
   * Returns a near real-time Reader on the index provided.
   *
   * <p>IMPORTANT: the reader should not be closed, it should be used in the following way to ensure
   *  it is made available to other threads:</p>
   * <pre>
   *    IndexReader reader = manager.grabReader(index);
   *    try {
   *      ...
   *    } finally {
   *      manager.release(index, reader);
   *    }
   * </pre>
   *
   * @param index the index that the Index Reader will point to.
   * @return the Index Reader to read from the index
   *
   * @throws IndexException If an IO error occurred when getting the reader.
   */
  public static IndexReader grabReader(Index index) throws IndexException {
    LuceneIndexIO io = getIndexIO(index);
    return io == null ? null : io.bookReader();
  }

  /**
   * Release an {@link IndexReader} after it has been used.
   *
   * <p>It is necessary to release a reader so that it can be reused for other threads.
   *
   * @param index  The index the reader works on.
   * @param reader The actual Lucene index reader.
   *
   * @throws IndexException Wrapping any IO exception
   */
  public static void release(Index index, IndexReader reader) throws IndexException {
    if (reader == null) return;
    LuceneIndexIO io = getIndexIO(index);
    if (io != null) io.releaseReader(reader);
  }

  /**
   * Releases an {@link IndexReader} quietly after it has been used so that it can be used in a <code>finally</code>
   * block.
   *
   * <p>It is necessary to release a reader so that it can be reused for other threads.
   *
   * @param index  The index the reader works on.
   * @param reader The actual Lucene index reader.
   */
  public static void releaseQuietly(Index index, IndexReader reader) {
    if (reader == null)
      return;
    getIndexIO(index).releaseReader(reader);
  }

  /**
   * Returns a near real-time Searcher on the index provided.
   *
   * <p>IMPORTANT: the searcher should not be closed, it should be used in the following way to
   * ensure it is made available to other threads:</p>
   * <pre>
   *    IndexSearcher searcher = manager.grabSearcher(index);
   *    try {
   *      ...
   *    } finally {
   *      manager.release(index, searcher);
   *    }
   * </pre>
   *
   * @param index the index that the searcher will work on.
   * @return the index searcher to use on the index
   *
   * @throws IndexException If an IO error occurred when getting the reader.
   */
  public static IndexSearcher grabSearcher(Index index) throws IndexException {
    LuceneIndexIO io = getIndexIO(index);
    return io.bookSearcher();
  }

  /**
   * Release an {@link IndexSearcher} after it has been used.
   *
   * <p>It is necessary to release a searcher so that it can be reused by other threads.
   *
   * @param index    The index the searcher works on.
   * @param searcher The actual Lucene index searcher.
   *
   * @throws IndexException Wrapping any IO exception
   */
  public static void release(Index index, IndexSearcher searcher) throws IndexException {
    if (searcher == null)
      return;
    LuceneIndexIO io = getIndexIO(index);
    io.releaseSearcher(searcher);
  }

  /**
   * Releases an {@link IndexSearcher} quietly after it has been used so that it can be used in a <code>finally</code>
   * block.
   *
   * <p>It is necessary to release a searcher so that it can be reused for other threads.
   *
   * @param index    The index the searcher works on.
   * @param searcher The actual Lucene index searcher.
   */
  public static void releaseQuietly(Index index, IndexSearcher searcher) {
    if (searcher == null)
      return;
    getIndexIO(index).releaseSearcher(searcher);
  }

  // Private helpers
  // ==============================================================================

  /**
   * Retrieves an IndexIO, creates it if non existent.
   *
   * @param index the index requiring the IO utility.
   *
   * @return the Index IO operations manager
   */
  private static LuceneIndexIO getIndexIO(Index index) {
    if (index == null) return null;
    IndexIO io = index.getIndexIO();
    if (io instanceof LuceneIndexIO)
      return (LuceneIndexIO) io;
    return null;
  }

}

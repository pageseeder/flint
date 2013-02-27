/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint;

import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.lucene.store.AlreadyClosedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager for open indexes.
 *
 * <p>Handles closing of indexes that have been opened for a long time if there are too many open ones.
 *
 * @author Jean-Baptiste Reure
 * @version 26 February 2010
 */
public final class OpenIndexManager {

  /**
   * Logger
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(OpenIndexManager.class);

  /**
   * How to compare opened readers: the "highest" is the one that was used the least recently
   */
  private static final Comparator<IndexIOReadWrite> OPEN_INDEX_COMPARATOR = new Comparator<IndexIOReadWrite>() {

    @Override
    public int compare(IndexIOReadWrite o1, IndexIOReadWrite o2) {
      return o1.getLastTimeUsed() < o2.getLastTimeUsed() ? 1 : o1.getLastTimeUsed() == o2.getLastTimeUsed() ? 0 : -1;
    }

  };

  /**
   * The list of all opened readers
   */
  private static final ConcurrentHashMap<Integer, IndexIOReadWrite> OPEN_INDEXES =
    new ConcurrentHashMap<Integer, IndexIOReadWrite>();

  /**
   * The max number of opened reader allowed at all times
   */
  private static int maxOpenedIndexes = 100;

  /**
   * Utility class.
   */
  private OpenIndexManager() {
  }

  /**
   * @param val the new max number of opened reaer allowed at all times
   */
  public static void setMaxOpenedIndexes(int val) {
    maxOpenedIndexes = val;
  }

  /**
   * Go through all the opened readers and close the oldest ones until the number of
   * opened readers is less than the maximum allowed.
   */
  public static void closeOldReaders() {
//    LOGGER.debug("Currently {} opened reader(s): {}", openedIndexes.size(), openedIndexes);
    while (OPEN_INDEXES.size() > maxOpenedIndexes) {
      // get the oldest one
      IndexIOReadWrite or = Collections.max(OPEN_INDEXES.values(), OPEN_INDEX_COMPARATOR);
      // ok try to close it
      try {
        LOGGER.debug("Closing IO for index {}", or.hashCode());
        or.stop();
      } catch (AlreadyClosedException ex) {
        // good then
      } catch (IndexException ex) {
        LOGGER.error("Failed closing an opened index {}", or.hashCode());
      }
    }
  }

  /**
   * @param index a new opened index to store
   */
  public static void add(IndexIOReadWrite index) {
    LOGGER.debug("Adding new index {}", index.hashCode());
    OPEN_INDEXES.put(index.hashCode(), index);
  }

  /**
   * @param index the index to remove from the list
   */
  public static void remove(IndexIOReadWrite index) {
    if (OPEN_INDEXES.remove(index.hashCode()) != null) {
      LOGGER.debug("Removing index {}", index.hashCode());
    }
  }
}

/*
 * This file is part of the Flint library.
 * 
 * For licensing information please see the file license.txt included in the release. A copy of this licence can also be
 * found at http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint;

import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.lucene.store.AlreadyClosedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager for index readers.
 * Handles closing of readers that have been opened for a long time if there are too many opened.
 * 
 * @author Jean-Baptiste Reure
 * @version 26 February 2010
 */
public class OpenedIndexManager {
  /**
   * Logger
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(OpenedIndexManager.class);
  /**
   * How to compare opened readers: the "highest" is the one that was used the least recently
   */
  private final static Comparator<IndexIOReadWrite> openedIndexComparator = new Comparator<IndexIOReadWrite>() {
    /**
     * {@inheritdoc}
     */
    public int compare(IndexIOReadWrite o1, IndexIOReadWrite o2) {
      return o1.getLastTimeUsed() < o2.getLastTimeUsed() ? 1 : o1.getLastTimeUsed() == o2.getLastTimeUsed() ? 0 : -1;
    }
  };
  /**
   * The list of all opened readers
   */
  private static final ConcurrentHashMap<Integer, IndexIOReadWrite> openedIndexes = new ConcurrentHashMap<Integer, IndexIOReadWrite>();
  /**
   * The max number of opened reader allowed at all times
   */
  private static int MAX_OPENED_INDEXES = 50;
  /**
   * @param val the new max number of opened reaer allowed at all times
   */
  public final static void setMaxOpenedIndexes(int val) {
    MAX_OPENED_INDEXES = val;
  }
  /**
   * Go through all the opened readers and close the oldest ones until the number of
   * opened readers is less than the maximum allowed.
   */
  public final static void closeOldReaders() {
    LOGGER.debug("Currently {} opened reader(s): {}", openedIndexes.size(), openedIndexes);
    while (openedIndexes.size() > MAX_OPENED_INDEXES) {
      // get the oldest one
      IndexIOReadWrite or = Collections.max(openedIndexes.values(), openedIndexComparator);
      // ok try to close it
      try {
        LOGGER.debug("Closing index {}", or.hashCode());
        or.stop();
      } catch (AlreadyClosedException e) {
        // good then
      } catch (IndexException e) {
        LOGGER.error("Failed closing an opened index {}", or.hashCode());
      }
    }
  }
  /**
   * @param index a new opened index to store
   */
  public static void add(IndexIOReadWrite index) {
    LOGGER.debug("Adding new index {}", index.hashCode());
    openedIndexes.put(index.hashCode(), index);
  }
  /**
   * @param index the index to remove from the list
   */
  public static void remove(IndexIOReadWrite index) {
    if (openedIndexes.remove(index.hashCode()) != null) {
      LOGGER.debug("Removing index {}", index.hashCode());
    }
  }
}

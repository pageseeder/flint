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
package org.pageseeder.flint;

import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

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
  private static final Comparator<IndexIO> OPEN_INDEX_COMPARATOR = (o1, o2) -> Long.compare(o2.getLastTimeUsed(), o1.getLastTimeUsed());

  /**
   * The list of all opened readers
   */
  private static final ConcurrentHashMap<Integer, IndexIO> OPEN_INDEXES = new ConcurrentHashMap<>();

  /**
   * The max number of opened reader allowed at all times
   */
  private static int maxOpenedIndexes = 100;

  /**
   * Last time we checked
   */
  private static long LAST_CHECK = 0;

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
    // Don't check for 30mn
    long DELAY_BETWEEN_CHECKS = 30 * 60 * 1000;
    if (System.currentTimeMillis() - LAST_CHECK > DELAY_BETWEEN_CHECKS) {
      LAST_CHECK = System.currentTimeMillis();
      while (OPEN_INDEXES.size() > maxOpenedIndexes) {
        // get the oldest one
        IndexIO or = Collections.max(OPEN_INDEXES.values(), OPEN_INDEX_COMPARATOR);
        // ok try to close it
        try {
          LOGGER.debug("Closing index {}", or.hashCode());
          or.stop();
          LOGGER.debug("Closed index {} - {} opened indexes now", or.hashCode(), OPEN_INDEXES.size());
        } catch (IndexException ex) {
          LOGGER.error("Failed closing an opened index {}", or.hashCode());
        }
      }
    }
  }

  /**
   * @param index a new opened index to store
   */
  public static void add(IndexIO index) {
    OPEN_INDEXES.put(index.hashCode(), index);
    LOGGER.debug("Added new open index {} - {} opened indexes now", index.hashCode(), OPEN_INDEXES.size());
  }

  /**
   * @param index the index to remove from the list
   */
  public static void remove(IndexIO index) {
    if (OPEN_INDEXES.remove(index.hashCode()) != null) {
      LOGGER.debug("Removed index {} - {} opened indexes now", index.hashCode(), OPEN_INDEXES.size());
    }
  }

  /**
   * @param index the index to check
   * @return true if the index is currently in the list of open indexes
   */
  public static boolean isOpen(IndexIO index) {
    return OPEN_INDEXES.containsKey(index.hashCode());
  }

  public static int size() {
    return OPEN_INDEXES.size();
  }
}

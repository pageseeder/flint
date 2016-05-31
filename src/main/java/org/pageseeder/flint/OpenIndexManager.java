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
  private static final Comparator<IndexIO> OPEN_INDEX_COMPARATOR = new Comparator<IndexIO>() {

    @Override
    public int compare(IndexIO o1, IndexIO o2) {
      return o1.getLastTimeUsed() < o2.getLastTimeUsed() ? 1 : o1.getLastTimeUsed() == o2.getLastTimeUsed() ? 0 : -1;
    }

  };

  /**
   * The list of all opened readers
   */
  private static final ConcurrentHashMap<Integer, IndexIO> OPEN_INDEXES =
    new ConcurrentHashMap<Integer, IndexIO>();

  /**
   * The max number of opened reader allowed at all times
   */
  private static int maxOpenedIndexes = 100;

  /**
   * Don't check for 30mn
   */
  private static long DELAY_BETWEEN_CHECKS = 30 * 60 * 1000;

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
    if (System.currentTimeMillis() - LAST_CHECK > DELAY_BETWEEN_CHECKS) {
      while (OPEN_INDEXES.size() > maxOpenedIndexes) {
        // get the oldest one
        IndexIO or = Collections.max(OPEN_INDEXES.values(), OPEN_INDEX_COMPARATOR);
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
      LAST_CHECK = System.currentTimeMillis();
    }
  }

  /**
   * @param index a new opened index to store
   */
  public static void add(IndexIO index) {
    LOGGER.debug("Adding new index {}", index.hashCode());
    OPEN_INDEXES.put(index.hashCode(), index);
  }

  /**
   * @param index the index to remove from the list
   */
  public static void remove(IndexIO index) {
    if (OPEN_INDEXES.remove(index.hashCode()) != null) {
      LOGGER.debug("Removing index {}", index.hashCode());
    }
  }
}

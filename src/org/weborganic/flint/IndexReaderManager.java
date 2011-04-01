/*
 * This file is part of the Flint library.
 * 
 * For licensing information please see the file license.txt included in the release. A copy of this licence can also be
 * found at http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.lucene.index.IndexReader;
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
public class IndexReaderManager {
  /**
   * Logger
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(IndexReaderManager.class);
  /**
   * How to compare opened readers: the "highest" is the one that was used the least recently
   */
  private final static Comparator<OpenedReader> openedReaderComparator = new Comparator<OpenedReader>() {
    /**
     * {@inheritdoc}
     */
    public int compare(OpenedReader o1, OpenedReader o2) {
      return o1.lastTimeUsed < o2.lastTimeUsed ? 1 : o1.lastTimeUsed == o2.lastTimeUsed ? 0 : -1;
    }
  };
  /**
   * The list of all opened readers
   */
  private static final ConcurrentHashMap<Integer, OpenedReader> closableReaders = new ConcurrentHashMap<Integer, OpenedReader>();
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
    LOGGER.debug("Currently {} opened reader(s): {}", closableReaders.size(), closableReaders);
    while (closableReaders.size() > MAX_OPENED_INDEXES) {
      // get the oldest one
      OpenedReader or = Collections.max(closableReaders.values(), openedReaderComparator);
      // ok try to close it
      try {
        LOGGER.debug("Closing reader {}", or.reader.hashCode());
        or.reader.close();
      } catch (AlreadyClosedException e) {
        // good then
      } catch (IOException e) {
        LOGGER.error("Failed closing an opened reader {}", or.reader);
      } finally{
        // even if it failed, we set it as closed so that a new one is opened next time
        or.opened = false;
        // and remove it from the list
        closableReaders.remove(or.reader.hashCode());
      }
    }
  }
  /**
   * @param reader a new opened reader to store
   */
  public static void add(IndexReader reader) {
    LOGGER.debug("Adding new reader {}", reader.hashCode());
    closableReaders.put(reader.hashCode(), new OpenedReader(reader));
  }
  /**
   * @param reader the reader to check
   * @return false if the reader is not in this manager or if it is but is closed, true otherwise
   */
  public static boolean isOpened(IndexReader reader) {
    OpenedReader det = closableReaders.get(reader.hashCode());
    return det != null && det.opened;
  }
  /**
   * @param reader the reader to update
   */
  public static void update(IndexReader reader) {
    OpenedReader det = closableReaders.get(reader.hashCode());
    if (det != null) {
      det.lastTimeUsed = System.currentTimeMillis();
    }
  }
  /**
   * @param reader the reader to remove from the list
   */
  public static void remove(IndexReader reader) {
    if (closableReaders.remove(reader.hashCode()) != null) {
      LOGGER.debug("Removing reader {}", reader.hashCode());
    }
  }
  /**
   * Represent an Index Reader details
   * 
   * @author Jean-Baptiste Reure
   * @version 01 April 2011
   */
  private final static class OpenedReader {
    /**
     * The reader object
     */
    private final IndexReader reader;
    /**
     * The last time this reader was used
     */
    private long lastTimeUsed;
    /**
     * Whether this reader is still opened (within the manager, it could have been closed somewhere else)
     */
    private boolean opened = true;
    /**
     * Build a new reader
     */
    public OpenedReader(IndexReader r) {
      this.reader = r;
      this.lastTimeUsed = System.currentTimeMillis();
    }
    /**
     * {@inheritdoc}
     */
    public String toString() {
      return String.valueOf(this.reader.hashCode());
    }
  }
}

package org.pageseeder.flint.local;

import java.util.Collections;
import java.util.List;

import org.pageseeder.flint.indexing.IndexListener;
import org.pageseeder.flint.log.NoOpListener;

public class LocalIndexManagerFactory {

  private final static List<String> XML_ONLY = Collections.singletonList("xml");

  private LocalIndexManagerFactory() {
  }

  /*
   * Multithreaded
   */
  public static LocalIndexManager createMultiThreads() {
    return new LocalIndexManager(XML_ONLY);
  }

  public static LocalIndexManager createMultiThreads(IndexListener listener) {
    return new LocalIndexManager(listener, XML_ONLY);
  }

  public static LocalIndexManager createMultiThreads(int threads) {
    return createMultiThreads(threads, NoOpListener.getInstance());
  }

  public static LocalIndexManager createMultiThreads(int threads, IndexListener listener) {
    return createMultiThreads(threads, listener, XML_ONLY);
  }

  public static LocalIndexManager createMultiThreads(int threads, IndexListener listener, List<String> extensions) {
    return create(listener, threads, false, extensions);
  }

  public static LocalIndexManager createMultiThreads(int threads, IndexListener listener, List<String> extensions, int debounceDelayInMs) {
    return create(listener, threads, false, extensions, debounceDelayInMs);
  }

  /*
   * Single thread
   */

  public static LocalIndexManager createSingleThread() {
    return createSingleThread(NoOpListener.getInstance());
  }

  public static LocalIndexManager createSingleThread(IndexListener listener) {
    return createSingleThread(listener, XML_ONLY);
  }

  public static LocalIndexManager createSingleThread(IndexListener listener, List<String> extensions) {
    return create(listener, 1, false, extensions);
  }

  public static LocalIndexManager createSingleThread(IndexListener listener, List<String> extensions, int debounceDelayInMs) {
    return create(listener, 1, false, extensions, debounceDelayInMs);
  }

  /*
   * Generic
   */

  public static LocalIndexManager create(IndexListener listener, int threads, boolean single, List<String> extensions) {
    return create(listener, threads, single, extensions, 0);
  }

  /*
   * Generic
   */

  public static LocalIndexManager create(IndexListener listener, int threads, boolean single, List<String> extensions, int debounceDelayInMs) {
    return new LocalIndexManager(listener, threads, single, extensions, debounceDelayInMs);
  }
}

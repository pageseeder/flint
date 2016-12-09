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
   * Multi threaded
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
    return new LocalIndexManager(listener, threads, false, extensions);
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

  /*
   * Generic
   */

  public static LocalIndexManager create(IndexListener listener, int threads, boolean single, List<String> extensions) {
    return new LocalIndexManager(listener, threads, single, extensions);
  }
}

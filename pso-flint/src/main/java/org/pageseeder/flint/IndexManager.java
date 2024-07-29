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

import org.pageseeder.flint.content.*;
import org.pageseeder.flint.indexing.*;
import org.pageseeder.flint.indexing.IndexJob.Priority;
import org.pageseeder.flint.ixml.IndexParser;
import org.pageseeder.flint.ixml.IndexParserFactory;
import org.pageseeder.flint.log.NoOpListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.stream.StreamResult;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Main class from Flint, applications should create one instance of this class.
 *
 * @author Jean-Baptiste Reure
 * @author Christophe Lauret
 *
 * @version 27 February 2013
 */
public final class IndexManager {

  /**
   * Logger will receive debugging and low-level data, use the listener to capture specific indexing operations.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(IndexManager.class);

  /**
   * NB of indexing threads
   */
  private static final int NB_INDEXING_THREADS = 10;

  /**
   * Listens to any problem reported by the indexer.
   */
  private final IndexListener _listener;

  /**
   * The fetcher used to retrieve content to index.
   */
  private final ContentFetcher _fetcher;

  /**
   * The queue were index job are waiting to be run.
   */
  private final IndexJobQueue _indexQueue;

  /**
   * Maps index ID to the indexes IO.
   */
  private static final ConcurrentHashMap<String, Index> ALL_INDEXES = new ConcurrentHashMap<>(100, 0.75f, 11);

  /**
   * Maps MIME types to the ContentTranslator factory to use.
   */
  private final ConcurrentHashMap<String, ContentTranslatorFactory> translatorFactories;

  /**
   * Priority of the thread, default to NORM_PRIORITY (5)
   */
  private int threadPriority = Thread.NORM_PRIORITY;

  /**
   * The thread manager.
   */
  private final ExecutorService multiThreadExecutor;

  /**
   * The thread manager.
   */
  private ExecutorService singleThreadExecutor = null;

  /**
   * A default Translator, used when no Factory matches a certain MIME Type.
   */
  private ContentTranslator _defaultTranslator = null;

  /**
   * Simple constructor which will use a SilentListener.
   *
   * @param cf the Content Fetcher used to retrieve the content to index.
   */
  public IndexManager(ContentFetcher cf) {
    this(cf, NoOpListener.getInstance());
  }

  /**
   * Simple Constructor.
   *
   * @param cf       the Content Fetcher used to retrieve the content to index.
   * @param listener an object used to record events
   */
  public IndexManager(ContentFetcher cf, IndexListener listener) {
    this(cf, listener, NB_INDEXING_THREADS, false);
  }

  /**
   * Simple Constructor.
   *
   * @param cf        the Content Fetcher used to retrieve the content to index.
   * @param listener  an object used to record events
   * @param nbThreads the number of indexing threads
   */
  public IndexManager(ContentFetcher cf, IndexListener listener, int nbThreads, boolean withSingleThread) {
    this._fetcher = cf;
    this._listener = listener;
    this._indexQueue = new IndexJobQueue(withSingleThread);
    // Register default XML factory
    this.translatorFactories = new ConcurrentHashMap<>(16, 0.8f, 2);
    registerTranslatorFactory(new FlintTranslatorFactory());
    // create the worker thread pool
    this.multiThreadExecutor = Executors.newFixedThreadPool(nbThreads, new ThreadFactory() {
      private int threadCount = 1;

      @Override
      public Thread newThread(Runnable r) {
        Thread t = new Thread(r, "indexing-p" + IndexManager.this.threadPriority + "-t" + this.threadCount++);
        t.setPriority(IndexManager.this.threadPriority);
        return t;
      }
    });
    // create separate single thread if needed
    if (withSingleThread) {
      this.singleThreadExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "indexing-p" + IndexManager.this.threadPriority + "-single");
        t.setPriority(IndexManager.this.threadPriority);
        return t;
      });
    }
  }

  // Public external methods
  // ----------------------------------------------------------------------------------------------

  /**
   * Set the default translator to use when no factory matches a MIME type.
   *
   * <p>This to the {@link ContentTranslator} used when the MIME. Default is <code>null</code>.
   *
   * @param translator the translator to use by default.
   */
  public void setDefaultTranslator(ContentTranslator translator) {
    this._defaultTranslator = translator;
  }

  /**
   * Set the priority of the thread.
   *
   * <p>This has no effect if called after the method <code>start()</code> is called.
   *
   * @param priority the priority of the Indexing Thread (from 1 to 10, default is 5)
   *
   * @throws IndexOutOfBoundsException if the priority is less than 1 or greater than 10.
   */
  public void setThreadPriority(int priority) {
    if (priority < 1 || priority > 10)
      throw new IndexOutOfBoundsException("Thread priority is should be between 1 and 10 but was " + priority);
    this.threadPriority = priority;
  }

  /**
   * Register a new factory with the all the MIME types supported by the factory.
   *
   * <p>If there was already a factory registered for a MIME type, it is overwritten.
   *
   * @param factory The factory to register
   */
  public void registerTranslatorFactory(ContentTranslatorFactory factory) {
    Collection<String> types = factory.getMimeTypesSupported();
    for (String type : types) {
      registerTranslatorFactory(type, factory);
    }
  }

  /**
   * Register a new factory with the given MIME type.
   *
   * <p>If there was already a factory registered for this MIME type, it is overwritten.
   * <p>The factory object must support the MIME type provided
   *
   * @param mimeType the MIME type
   * @param factory  the factory to register
   */
  public void registerTranslatorFactory(String mimeType, ContentTranslatorFactory factory) {
    this.translatorFactories.put(mimeType, factory);
  }

  /**
   * Add a new batch update job to the indexing queue.
   *
   * @param batch     the batch job
   * @param contentid the ID of the content
   * @param type      the type of the content
   * @param i         the Index to add the Content to
   * @param r         the Requester calling this method (used for logging)
   * @param p         the Priority of this job
   * @param params    some parameters
   */
  public void indexBatch(IndexBatch batch, String contentid, ContentType type, Index i, Requester r, Priority p,
      Map<String, String> params) {
    indexBatch(batch, contentid, type, i, r, p, false, params);
  }

  /**
   * Add a new batch update job to the indexing queue.
   *
   * @param batch         the batch job
   * @param contentid     the ID of the content
   * @param type          the type of the content
   * @param i             the Index to add the Content to
   * @param r             the Requester calling this method (used for logging)
   * @param p             the Priority of this job
   * @param singleThread  if this job goes in the single thread queue
   * @param params        some parameters
   */
  public void indexBatch(IndexBatch batch, String contentid, ContentType type, Index i, Requester r, Priority p,
      boolean singleThread, Map<String, String> params) {
    indexJob(IndexJob.newBatchJob(batch, contentid, type, i, p, r, params, singleThread), singleThread);
  }

  /**
   * Add a new batch update job to the indexing queue.
   *
   * @param contents the batch contents
   * @param r        the Requester calling this method (used for logging)
   * @param p        the Priority of this job
   */
  public void indexBatch(Map<String, ContentType> contents, Index i, Requester r, Priority p) {
    IndexBatch batch = new IndexBatch(i.getIndexID(), contents.size());
    for (Map.Entry<String, ContentType> entries : contents.entrySet()) {
      indexJob(IndexJob.newBatchJob(batch, entries.getKey(), entries.getValue(), i, p, r, null, false), false);
    }
  }

  /**
   * Add a new update job to the indexing queue.
   *
   * @param contentid the ID of the content
   * @param type      the type of the content
   * @param i         the Index to add the Content to
   * @param r         the Requester calling this method (used for logging)
   * @param p         the Priority of this job
   * @param params    some parameters
   */
  public void index(String contentid, ContentType type, Index i, Requester r, Priority p, boolean singleThread, Map<String, String> params) {
    indexJob(IndexJob.newJob(contentid, type, i, p, r, params), singleThread);
  }

  /**
   * Add a new update job to the indexing queue.
   *
   * @param contentid the ID of the content
   * @param type      the type of the content
   * @param i         the Index to add the Content to
   * @param r         the Requester calling this method (used for logging)
   * @param p         the Priority of this job
   * @param params    some parameters
   */
  public void index(String contentid, ContentType type, Index i, Requester r, Priority p, Map<String, String> params) {
    index(contentid, type, i, r, p, false, params);
  }

  /**
   * Add a job to the single thread queue
   * @param job the job to index
   */
  public void indexSingleThread(IndexJob job) {
    this.indexJob(job, true);
  }

  /**
   * Add a new update job to the indexing queue.
   *
   * @param index      the Index to add the Content to
   * @param requester  the Requester calling this method (used for logging)
   * @param priority   the Priority of this job
   */
  public void clear(Index index, Requester requester, Priority priority) {
    // remove all jobs belonging to this index
    this._indexQueue.clearJobsForIndex(index);
    // add a new job to clear the index
    indexJob(IndexJob.newClearJob(index, priority, requester), false);
  }

  /**
   * Returns the list of waiting jobs for the Requester provided.
   *
   * <p>Note that by the time each job is checked, they might have run already so the method
   * {@link IndexJob#isFinished()} should be called before parsing the job.
   *
   * <p>The list will never be <code>null</code>.
   *
   * @param r the Requester
   * @return the list of jobs waiting (never <code>null</code>)
   */
  public List<IndexJob> getStatus(Requester r) {
    return this._indexQueue.getJobsForRequester(r);
  }

  /**
   * Returns the list of waiting jobs for the index provided.
   *
   * <p>Note that by the time each job is checked, they might have run already so the method
   * {@link IndexJob#isFinished()} should be called before parsing the job.
   *
   * <p>The list will never be <code>null</code>.
   *
   * @param i the index
   * @return the list of jobs waiting (never <code>null</code>)
   */
  public List<IndexJob> getStatus(Index i) {
    return this._indexQueue.getJobsForIndex(i);
  }

  /**
   * Returns the list of waiting job for the all the indexes.
   *
   * <p>Note that by the time each job is checked, they might have run already so the method
   * {@link IndexJob#isFinished()} should be called before parsing the job.
   *
   * <p>The list will never be <code>null</code>.
   *
   * @return the list of jobs waiting (never <code>null</code>)
   */
  public List<IndexJob> getStatus() {
    return this._indexQueue.getAllJobs();
  }

  /**
   * Translate content into iXML data.
   *
   * @param index   the index
   * @param content the actual Content to transform
   * @param params  the parameters to add to the translation
   * @param out     the Writer to write the result to
   * @throws IndexException if anything went wrong
   */
  public void contentToIXML(Index index, Content content, Map<String, String> params, Writer out)
      throws IndexException {
    IndexingThread.translateContent(this, null, index, content, params, new StreamResult(out));
  }

  /**
   * Translate content into Lucene documents.
   *
   * @param index   the index
   * @param content the actual Content to transform
   * @param params  the parameters to add to the translation
   *
   * @return the list of documents produced by the conversion (could be null)
   *
   * @throws IndexException if anything went wrong
   */
  public List<FlintDocument> contentToDocuments(Index index, Content content, Map<String, String> params)
      throws IndexException {
    IndexParser parser = IndexParserFactory.getInstanceForTransformation(null);
    IndexingThread.translateContent(this, null, index, content, params, parser.getResult());
    return parser.getDocuments();
  }

  /**
   * Kills the thread and close all the indexes.
   * Timeout is set to 5 seconds.
   */
  public void stop() {
    stop(5);
  }

  /**
   * Kills the thread and close all the indexes.
   * @param timeout in seconds for each queue
   */
  public void stop(long timeout) {
    // empty queue
    this._indexQueue.clear();
    // Interrupt the threads
    IndexingThread.CLOSING_DOWN = true;
    this.multiThreadExecutor.shutdownNow();
    // wait for finish
    try {
      this.multiThreadExecutor.awaitTermination(timeout, TimeUnit.SECONDS);
    } catch (InterruptedException ex) {
      LOGGER.error("Interrupted while shutting down multiple thread", ex);
      Thread.currentThread().interrupt();
    }
    if (this.singleThreadExecutor != null) {
      this.singleThreadExecutor.shutdownNow();
      try {
        this.singleThreadExecutor.awaitTermination(timeout, TimeUnit.SECONDS);
      } catch (InterruptedException ex) {
        LOGGER.error("Interrupted while shutting down single thread", ex);
        Thread.currentThread().interrupt();
      }
    }
    // Close all indexes
    for (Index index : ALL_INDEXES.values()) {
      index.close();
    }
    // reset flag
    IndexingThread.CLOSING_DOWN = false;
  }

  /**
   * Add the index provided to list
   *
   * @param index the index to add
   */
  static void registerIndex(Index index) {
    ALL_INDEXES.put(index.getIndexID(), index);
  }

  /**
   * Remove the index provided from list
   *
   * @param index the index to remove
   */
  static void deregisterIndex(Index index) {
    ALL_INDEXES.remove(index.getIndexID(), index);
  }

  // protected methods used by the threads

  /**
   * Get a translator to turn content into Flint Index XML
   *
   * @param mediatype the content media type
   *
   * @throws IndexException if anything went wrong
   */
  public ContentTranslator getTranslator(String mediatype) throws IndexException {
    if (mediatype == null)
      throw new NullPointerException("mediatype");
    ContentTranslatorFactory factory = this.translatorFactories.get(mediatype);
    // no factory found
    if (factory == null && this._defaultTranslator == null)
      throw new IndexException("Media Type " + mediatype
          + " is not supported, no Translator Factory was found and no default Translator was specified.", null);
    // load translator
    ContentTranslator translator = factory == null ? this._defaultTranslator : factory.createTranslator(mediatype);
    if (translator == null)
      throw new IndexException("No translator was found for MIME Type " + mediatype + ".", null);
    return translator;
  }

  /**
   *
   * @param job the index job
   *
   * @return the content
   */
  public Content getContent(IndexJob job) {
    // retrieve content
    return this._fetcher.getContent(job);
  }

  // Private helpers
  // ==============================================================================

  /**
   * Start an index job.
   */
  private void indexJob(IndexJob job, boolean singleThread) {
    if (singleThread && this.singleThreadExecutor != null) {
      // add job to queue
      this._indexQueue.addSingleThreadJob(job);
      // start thread to index it
      this.singleThreadExecutor.execute(new IndexingThread(this, this._listener, this._indexQueue, true));
    } else {
      // add job to queue
      this._indexQueue.addMultiThreadJob(job);
      // start thread to index it
      this.multiThreadExecutor.execute(new IndexingThread(this, this._listener, this._indexQueue, false));
    }
  }

}

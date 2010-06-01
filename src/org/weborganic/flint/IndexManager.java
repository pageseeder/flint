/*
 * This file is part of the Flint library.
 * 
 * For licensing information please see the file license.txt included in the release. A copy of this licence can also be
 * found at http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader.FieldOption;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;
import org.weborganic.flint.IndexJob.Priority;
import org.weborganic.flint.content.Content;
import org.weborganic.flint.content.ContentFetcher;
import org.weborganic.flint.content.ContentId;
import org.weborganic.flint.content.ContentTranslator;
import org.weborganic.flint.content.ContentTranslatorFactory;
import org.weborganic.flint.content.ContentType;
import org.weborganic.flint.index.IndexParser;
import org.weborganic.flint.index.IndexParserFactory;
import org.weborganic.flint.log.FlintListener;
import org.weborganic.flint.log.SilentListener;
import org.weborganic.flint.query.SearchPaging;
import org.weborganic.flint.query.SearchQuery;
import org.weborganic.flint.query.SearchResults;
import org.weborganic.flint.util.FlintEntityResolver;
import org.weborganic.flint.util.FlintErrorListener;
import org.xml.sax.InputSource;

/**
 * Main class from Flint, applications should create one instance of this class.
 * 
 * <ul>
 *   <li>To start and stop the indexing thread, use the methods {@link #start()} and {@link #stop()}.</li>
 *   <li>To register IndexConfigs, use the methods registerIndexConfig() and getConfig().</li>
 *   <li>To add/modify/delete content from an Index, use the method {@link #index(ContentId, Index, IndexConfig, Requester, Priority, Map)}</li>
 *   <li>To search an Index, use the methods {@link IndexManager#query()}</li>
 *   <li>to load an Index's statuses, use the method {@link #getStatus()}</li>
 * </ul>
 * 
 * @author Jean-Baptiste Reure
 * @version 26 February 2010
 */
public class IndexManager implements Runnable {

  /**
   * The lucene version with which this manager is compatible.
   */
  public static final Version LUCENE_VERSION = Version.LUCENE_30;

  /**
   * Inactive time before indexes get optimised
   */
  private static final long INACTIVE_OPTIMISE_TIME = 1 * 3600 * 1000; // 1 hour

  /**
   * Delay between each job poll
   */
  private static final long INDEX_JOB_POLL_DELAY = 1 * 1000; // 1 second

  /**
   * This logger.
   */
  private final FlintListener listener;

  /**
   * The fetcher used to retrieve content to index
   */
  private final ContentFetcher fetcher;

  /**
   * The IndexJob queue
   */
  private final IndexJobQueue indexQueue;

  /**
   * List of Indexes
   */
  private final ConcurrentHashMap<String, IndexIO> indexes;

  /**
   * List of ContentTranslatorFactories
   */
  private final ConcurrentHashMap<String, ContentTranslatorFactory> translatorFactories;

  /**
   * Time since the last activity on this manager.
   */
  private long lastActivity;

  /**
   * Priority of the thread, default to NORM_PRIORITY (5)
   */
  private int threadPriority = Thread.NORM_PRIORITY;

  /**
   * The thread manager
   */
  private ExecutorService threadPool = null;

  /**
   * Simple constructor which will use a SilentListener.
   * 
   * @param cf the Content Fetcher used to retrieve the content to index.
   */
  public IndexManager(ContentFetcher cf) {
    this(cf, SilentListener.getInstance());
  }

  /**
   * Simple Constructor.
   * 
   * @param cf       the Content Fetcher used to retrieve the content to index.
   * @param listener an object used to record events
   */
  public IndexManager(ContentFetcher cf, FlintListener listener) {
    this.fetcher = cf;
    this.listener = listener;
    this.indexQueue = new IndexJobQueue(INDEX_JOB_POLL_DELAY);
    this.indexes = new ConcurrentHashMap<String, IndexIO>();
    // register default XML factory
    this.translatorFactories = new ConcurrentHashMap<String, ContentTranslatorFactory>();
    registerTranslatorFactory(new FlintTranslatorFactory());
    this.lastActivity = System.currentTimeMillis();
  }

  // public external methods
  // ----------------------------------------------------------------------------------------------

  /**
   * Set the priority of the thread (this has no effect if called after the method start() is called)
   * 
   * @param priority the priority of the Indexing Thread (from 1 to 10, default is 5)
   */
  public void setThreadPriority(int priority) {
    this.threadPriority = priority;
  }

  /**
   * Register a new factory with the all the MIME types supported by the factory.
   * 
   * <p>If there was already a factory registered for a MIME type, it is overwritten.
   * 
   * @param factory  the factory to register
   */
  public void registerTranslatorFactory(ContentTranslatorFactory factory) {
    List<String> mtypes = factory.getMimeTypesSupported();
    for (String mimeType : mtypes) {
      registerTranslatorFactory(mimeType, factory);
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
   * Add a new update job to the indexing queue.
   * 
   * @param id       the ID of the Content
   * @param i        the Index to add the Content to
   * @param config   the Config to use
   * @param r        the Requester calling this method (used for logging)
   * @param p        the Priority of this job
   * @param params   the dynamic XSLT parameters
   */
  public void index(ContentId id, Index i, IndexConfig config, Requester r, Priority p, Map<String, String> params) {
    IndexJob job = IndexJob.newJob(id, config, i, p, r, params);
    this.indexQueue.addJob(job);
  }

  /**
   * Add a new update job to the indexing queue.
   * 
   * @param ct       the Type of the Content
   * @param id       the ID of the Content
   * @param i        the Index to add the Content to
   * @param config   the Config to use
   * @param r        the Requester calling this method (used for logging)
   * @param p        the Priority of this job
   * @param params   the dynamic XSLT parameters
   */
  public void clear(Index i, Requester r, Priority p) {
    this.indexQueue.addJob(IndexJob.newClearJob(i, p, r));
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
    return this.indexQueue.getJobsForRequester(r);
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
    return this.indexQueue.getJobsForIndex(i);
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
    return this.indexQueue.getAllJobs();
  }

  /**
   * Load the existing field names form the given Index.
   * 
   * @param index the Index to laod the field names from
   * @return the search results
   * @throws IndexException if any error occurred while performing the search
   */
  public Iterator<String> fieldNames(Index index) throws IndexException {
    IndexIO io = null;
    IndexSearcher searcher = null;
    try {
      io = getIndexIO(index);
      searcher = io.bookSearcher();
    } catch (CorruptIndexException e) {
      this.listener.error("Failed getting a Searcher to perform a query because the Index is corrupted", e);
      throw new IndexException("Failed getting a Searcher to perform a query because the Index is corrupted", e);
    } catch (LockObtainFailedException e) {
      this.listener.error("Failed getting a lock on the Index to perform a query", e);
      throw new IndexException("Failed getting a lock on the Index to perform a query", e);
    } catch (IOException e) {
      this.listener.error("Failed getting a searcher to perform a query on the Index because of an I/O problem", e);
      throw new IndexException("Failed getting a searcher to perform a query on the Index because of an I/O problem", e);
    }
    if (searcher != null) {
      try {
        this.listener.debug("Loading field names from index " + index.toString());
        return searcher.getIndexReader().getFieldNames(FieldOption.INDEXED).iterator();
      } finally {
        try {
          io.releaseSearcher(searcher);
        } catch (IOException ioe) {
          this.listener.error("Failed releasing a Searcher after performing a query on the Index because of an I/O problem", ioe);
        }
      }
    }
    return null;
  }

  /**
   * Run a search on the given Index.
   * 
   * @param index the Index to run the search on
   * @param query the query to run
   * @return the search results
   * @throws IndexException if any error occurred while performing the search
   */
  public SearchResults query(Index index, SearchQuery query) throws IndexException {
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
  public SearchResults query(Index index, SearchQuery query, SearchPaging paging) throws IndexException {
    IndexIO io = null;
    IndexSearcher searcher = null;
    try {
      io = getIndexIO(index);
      searcher = io.bookSearcher();
    } catch (CorruptIndexException e) {
      this.listener.error("Failed getting a Searcher to perform a query because the Index is corrupted", e);
      throw new IndexException("Failed getting a Searcher to perform a query because the Index is corrupted", e);
    } catch (LockObtainFailedException e) {
      this.listener.error("Failed getting a lock on the Index to perform a query", e);
      throw new IndexException("Failed getting a lock on the Index to perform a query", e);
    } catch (IOException e) {
      this.listener.error("Failed getting a searcher to perform a query on the Index because of an I/O problem", e);
      throw new IndexException("Failed getting a searcher to perform a query on the Index because of an I/O problem", e);
    }
    if (searcher != null) {
      try {
        Query lquery = query.toQuery();
        if (lquery == null) {
          try {
            io.releaseSearcher(searcher);
          } catch (IOException ioe) {
            this.listener.error("Failed releasing a Searcher after performing a query on the Index because of an I/O problem", ioe);
          }
          this.listener.error("Failed performing a query on the Index because the query is null", null);
          throw new IndexException("Failed performing a query on the Index because the query is null", new NullPointerException("Null query"));
        }
        this.listener.debug("Performing search [" + lquery.rewrite(searcher.getIndexReader()).toString() + "] on index " + index.toString());
        Sort sort = query.getSort();
        if (sort == null) sort = Sort.INDEXORDER;
        // load the scores
        TopFieldCollector tfc = TopFieldCollector.create(sort, paging.getHitsPerPage() * paging.getPage(), true, true, false, true);
        searcher.search(lquery, tfc);
        return new SearchResults(tfc.topDocs().scoreDocs, tfc.getTotalHits(), paging, io, searcher);
      } catch (IOException e) {
        try {
          io.releaseSearcher(searcher);
        } catch (IOException ioe) {
          this.listener.error("Failed releasing a Searcher after performing a query on the Index because of an I/O problem", ioe);
        }
        this.listener.error("Failed performing a query on the Index because of an I/O problem", e);
        throw new IndexException("Failed performing a query on the Index because of an I/O problem", e);
      }
    }
    return null;
  }

  /**
   * Translate content into IDX data.
   * 
   * @param type    the Content Type
   * @param config  the index config, where the XSLT script is registered
   * @param content the actual Content to transform
   * @param params  the parameters to add to the translation
   * @param out     the Writer to write the result to
   * @throws IndexException if anything went wrong
   */
  public void translateContent(ContentType type, IndexConfig config, Content content, Map<String, String> params, Writer out) throws IndexException {
    String mimetype = content.getMimeType();
    // no MIME type found
    if (mimetype == null)
      throw new IndexException("MIME Type not found", null);
    ContentTranslatorFactory factory = this.translatorFactories.get(mimetype);
    // no factory found
    if (factory == null)
      throw new IndexException("MIME Type "+mimetype+" is not supported, no ContentTranslatorFactory found", null);
    // ok translate now
    ContentTranslator translator = factory.createTranslator(mimetype);
    Reader source;
    try {
      source = translator.translate(content);
    } catch (IndexException ex) {
      throw new IndexException("Failed to translate Source content", ex);
    }
    if (source == null)
      throw new IndexException("Failed to translate Content", null);
    // retrieve XSLT script
    Templates templates = config.getTemplates(type, mimetype, content.getConfigID());
    if (templates == null)
      throw new IndexException("Failed to load XSLT script for Content", null);
    // run XSLT script
    try {
      // prepare transformer
      Transformer t = templates.newTransformer();
      t.setErrorListener(new FlintErrorListener(this.listener));
      if (t.getOutputProperty("doctype-public") == null) {
        t.setOutputProperty("doctype-public", FlintEntityResolver.PUBLIC_ID_PREFIX + "Index Documents Compatibility//EN");
        t.setOutputProperty("doctype-system", "http://weborganic.org/schema/flint/index-documents-compatibility.dtd");
      }
      // retrieve parameters
      Map<String, String> parameters = config.getParameters(type, mimetype, content.getConfigID());
      if (parameters != null && params != null) {
        parameters = new HashMap<String, String>(parameters);
        parameters.putAll(params);
      }
      if (parameters != null) for (String paramName : parameters.keySet())
        t.setParameter(paramName, parameters.get(paramName));
      // run transform
      t.transform(new StreamSource(source), new StreamResult(out));
    } catch (Exception ex) {
      throw new IndexException("Failed to create Index XML from Source content", ex);
    }
  }

  // thread related methods
  // ----------------------------------------------------------------------------------------------

  /**
   * Start the Manager by launching the thread.
   */
  public void start() {
    // only start once...
    if (this.threadPool != null) return;
    // create the worker thread pool
    this.threadPool = Executors.newCachedThreadPool(new ThreadFactory() {
      /** {@inheritDoc} */
      public Thread newThread(Runnable r) {
        Thread t = new Thread(r, "Indexing Thread with priority of "+IndexManager.this.threadPriority);
        t.setPriority(IndexManager.this.threadPriority);
        return t;
      }
    });
    this.threadPool.execute(this);
  }
  /**
   * Kills the thread and close all the indexes.
   */
  public void stop() {
    // stop the thread
    this.threadPool.shutdown();
    // close all indexes
    for (Iterator<String> ids = this.indexes.keySet().iterator(); ids.hasNext();) {
      String id = ids.next();
      try {
        this.indexes.get(id).stop();
      } catch (IndexException ex) {
        this.listener.error("Failed to close Index "+id+": " + ex.getMessage(), ex);
      }
    }
  }
  /**
   * The thread's main method
   */
  public void run() {
    IndexJob nextJob = null;
    while (true) {
      try {
        nextJob = this.indexQueue.nextJob();
      } catch (InterruptedException e) {
        // the thread was shutdown, let's die then
        return;
      }
      if (nextJob != null) {
        this.listener.startJob(nextJob);
        try {
          // ok launch the job then
          // load the IO for this job
          IndexIO io;
          try {
            io = getIndexIO(nextJob.getIndex());
          } catch (Exception ex) {
            this.listener.error(nextJob, "Failed to retrieve Index: " + ex.getMessage(), ex);
            continue;
          }
          if (nextJob.isClearJob()) {
            try {
              io.clearIndex();
            } catch (Exception ex) {
              this.listener.error(nextJob, "Failed to clear index", ex);
            }
          } else {
            // retrieve content
            Content content;
            try {
              content = this.fetcher.getContent(nextJob.getContentID());
            } catch (Exception ex) {
              this.listener.error(nextJob, "Failed to retrieve Source content", ex);
              continue;
            }
            if (content == null) {
              this.listener.error(nextJob, "Failed to retrieve Source content", null);
            } else {
              // check if we should delete the document
              if (content.isDeleted()) {
                deleteJob(nextJob, content, io);
              } else {
                updateJob(nextJob, content, io);
              }
            }
          }
        } catch (Throwable ex) {
          this.listener.error(nextJob, "Unkown error: " + ex.getMessage(), ex);
        } finally {
          nextJob.finish();
          this.listener.finishJob(nextJob);
          this.lastActivity = System.currentTimeMillis();
        }
      } else {
        // no jobs available, optimise if not needed
        checkForCommit();
      }
      // clear the job
      nextJob = null;
    }
  };

  // private methods
  // ----------------------------------------------------------------------------------------------

  /**
   * Add or update a document in an index
   * 
   * @param job
   */
  private void updateJob(IndexJob job, Content content, IndexIO io) {
    if (job == null || io == null || content == null) return;
    // translate content
    StringWriter xsltResult = new StringWriter();
    try {
      translateContent(job.getContentID().getContentType(), job.getConfig(), content, job.getParameters(), xsltResult);
    } catch (IndexException e) {
      this.listener.error(job, e.getMessage(), e);
      return;
    }
    // build Lucene documents
    List<Document> documents;
    try {
      IndexParser parser = IndexParserFactory.getInstance();
      documents = parser.process(new InputSource(new StringReader(xsltResult.toString())));
    } catch (Exception ex) {
      this.listener.error(job, "Failed to create Lucene Documents from Index XML", ex);
      return;
      }
    try {
      // add docs to index index
      io.updateDocuments(content.getDeleteRule(), documents);
    } catch (Exception ex) {
      this.listener.error(job, "Failed to add Lucene Documents to Index", ex);
      return;
    }
  }

  /**
   * Delete a doc from an index
   * 
   * @param job
   */
  private void deleteJob(IndexJob job, Content content, IndexIO io) {
    if (job == null || io == null) return;
    try {
      // delete docs from index
      io.deleteDocuments(content.getDeleteRule());
    } catch (Exception ex) {
      this.listener.error(job, "Failed to delete Lucene Documents from Index", ex);
      return;
    }
  };

  /**
   * Retrieve an IndexIO, creates it if non existent
   * 
   * @param index
   * @return
   * @throws IndexException
   */
  private IndexIO getIndexIO(Index index) throws IndexException {
    IndexIO io = this.indexes.get(index.getIndexID());
    if (io == null) {
      this.listener.debug("Creating a new IndexIO for " + index.toString());
      try {
        io = new IndexIO(index);
      } catch (CorruptIndexException e) {
        this.listener.error("Failed creating an Index I/O object for " + index.toString() + " because the Index is corrupted", e);
        throw new IndexException("Failed creating an Index I/O object for " + index.toString() + " because the Index is corrupted", e);
      } catch (LockObtainFailedException e) {
        this.listener.error("Failed getting a lock on the Index to create an Index I/O object for " + index.toString(), e);
        throw new IndexException("Failed getting a lock on the Index to create an Index I/O object for " + index.toString(), e);
      } catch (IOException e) {
        this.listener.error("Failed creating an Index I/O object for " + index.toString() + " because of an I/O problem", e);
        throw new IndexException("Failed creating an Index I/O object for " + index.toString() + " because of an I/O problem", e);
      }
      this.indexes.put(index.getIndexID(), io);
    }
    return io;
  }

  private void checkForCommit() {
    // loop through the indexes and check which one needs committing
    List<IndexIO> ios = new ArrayList<IndexIO>(this.indexes.values());
    for (IndexIO io : ios) {
      try {
        io.maybeCommit();
      } catch (IndexException e) {
        this.listener.error("Failed to perform commit", e);
      }
      // make sure there's no job waiting
      if (!this.indexQueue.isEmpty()) return;
    }
    // ok optimise now?
    if ((System.currentTimeMillis() - lastActivity) > INACTIVE_OPTIMISE_TIME) {
      ios = new ArrayList<IndexIO>(this.indexes.values());
      // loop through the indexes and optimise
      for (IndexIO io : ios) {
        try {
          io.maybeOptimise();
        } catch (IndexException e) {
          this.listener.error("Failed to perform optimise", e);
        }
        // make sure there's no job waiting
        if (!this.indexQueue.isEmpty()) return;
      }
    }
  }

}

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;
import org.weborganic.flint.content.Content;
import org.weborganic.flint.content.ContentFetcher;
import org.weborganic.flint.content.ContentId;
import org.weborganic.flint.content.ContentTranslator;
import org.weborganic.flint.content.ContentTranslatorFactory;
import org.weborganic.flint.content.ContentType;
import org.weborganic.flint.index.IndexParser;
import org.weborganic.flint.index.IndexParserFactory;
import org.weborganic.flint.query.SearchPaging;
import org.weborganic.flint.query.SearchQuery;
import org.weborganic.flint.query.SearchResults;
import org.weborganic.flint.util.FlintEntityResolver;
import org.xml.sax.InputSource;

/**
 * Main class from Flint, applications should create one instance of this class.
 * 
 * - To register IndexConfigs, use the methods registerIndexConfig() and getConfig(). - To add/modify/delete content
 * from an Index, use the methods addToIndex(), updateIndex() and deleteFromIndex() - To search an Index, use the
 * methods query() - to load an Index's statuses, use the method getStatus()
 * 
 * @author Jean-Baptiste Reure
 * @version 26 February 2010
 */
public class IndexManager implements Runnable {
  
  public final static Version LUCENE_VERSION = Version.LUCENE_30;

  /**
   * A list of priorities for IndexJobs
   */
  public enum Priority {HIGH, LOW};

  /**
   * Logger.
   */
  private static final Logger LOGGER = Logger.getLogger(IndexManager.class);

  /**
   * Inactive time before indexes get optimised
   */
  private static final long INACTIVE_OPTIMISE_TIME = 1 * 3600 * 1000; // 1 hour

  /**
   * Delay between each job poll
   */
  private static final long INDEX_JOB_POLL_DELAY = 1 * 1000; // 1 second

  /**
   * The fetcher used to retrieve content to index
   */
  private final ContentFetcher fetcher;

  /**
   * The IndexJob queue
   */
  private final PriorityBlockingQueue<IndexJob> indexQueue;

  /**
   * The list of jobs that had errors
   */
  private final ConcurrentHashMap<String, List<IndexJob>> errorsForIndex;

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
   * Simple Constructor
   * 
   * @param cf the Content Fetcher used to retrieve the content to index.
   */
  public IndexManager(ContentFetcher cf) {
    this.fetcher = cf;
    this.indexQueue = new PriorityBlockingQueue<IndexJob>();
    this.indexes = new ConcurrentHashMap<String, IndexIO>();
    this.errorsForIndex = new ConcurrentHashMap<String, List<IndexJob>>();
    // register default XML factory
    this.translatorFactories = new ConcurrentHashMap<String, ContentTranslatorFactory>();
    registerTranslatorFactory(FlintTranslatorFactory.XML_MIME_TYPE, new FlintTranslatorFactory());
    this.lastActivity = System.currentTimeMillis();
  }

  // public external methods
  // ----------------------------------------------------------------------------------------------
  
  /**
   * Register a new factory with the given MIME type.
   * If there was already a factory registered for this MIME type, it is overwritten.
   * The factory object must support the MIME type provided
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
   * @param ct       the Type of the Content
   * @param id       the ID of the Content
   * @param i        the Index to add the Content to
   * @param config   the Config to use
   * @param r        the Requester calling this method (used for logging)
   * @param p        the Priority of this job
   * @param params   the dynamic XSLt parameters
   */
  public void index(ContentType ct, ContentId id, Index i, IndexConfig config, Requester r, Priority p, Map<String, String> params) {
    LOGGER.debug("Adding Index Job for index " + i.getIndexID());
    this.indexQueue.put(IndexJob.newJob(ct, id, config, i, p, r, params));
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
    if (r == null) return getStatus();
    List<IndexJob> jobs = new ArrayList<IndexJob>();
    for (IndexJob job : this.indexQueue) {
      if (job.isForRequester(r)) jobs.add(job);
    }
    return jobs;
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
    if (i == null) return getStatus();
    List<IndexJob> jobs = new ArrayList<IndexJob>();
    for (IndexJob job : this.indexQueue) {
      if (job.isForIndex(i)) jobs.add(job);
    }
    return jobs;
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
    return new ArrayList<IndexJob>(this.indexQueue);
  }

  /**
   * Returns the list of jobs that had an error for the requester provided.
   * 
   * <p>The list will never be <code>null</code>.
   * 
   * @param r the Requester
   * @return the list of jobs with an error (never <code>null</code>)
   */
  public List<IndexJob> getErrorJobs(Requester r) {
    if (r == null) return getErrorJobs();
    List<IndexJob> jobs = new ArrayList<IndexJob>();
    for (List<IndexJob> errors : this.errorsForIndex.values())
      for (IndexJob job : errors)
        if (job.isForRequester(r)) jobs.add(job);
    return jobs;
  }

  /**
   * Returns the list of jobs that had an error for the index provided.
   * 
   * <p>The list will never be <code>null</code>.
   * 
   * @param i the index
   * @return the list of jobs with an error (never null)
   */
  public List<IndexJob> getErrorJobs(Index i) {
    if (i == null) return getErrorJobs();
    List<IndexJob> jobs = this.errorsForIndex.get(i.getIndexID());
    if (jobs == null) return Collections.emptyList();
    return jobs;
  }

  /**
   * Returns the list of jobs that had an error.
   * 
   * <p>The list will never be <code>null</code>.
   * 
   * @return the list of jobs with an error (never <code>null</code>)
   */
  public List<IndexJob> getErrorJobs() {
    List<IndexJob> jobs = new ArrayList<IndexJob>();
    for (List<IndexJob> errors : this.errorsForIndex.values())
      jobs.addAll(errors);
    return jobs;
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
      LOGGER.error("Failed getting a Searcher to perform a query because the Index is corrupted", e);
      throw new IndexException("Failed getting a Searcher to perform a query because the Index is corrupted", e);
    } catch (LockObtainFailedException e) {
      LOGGER.error("Failed getting a lock on the Index to perform a query", e);
      throw new IndexException("Failed getting a lock on the Index to perform a query", e);
    } catch (IOException e) {
      LOGGER.error("Failed getting a searcher to perform a query on the Index because of an I/O problem", e);
      throw new IndexException("Failed getting a searcher to perform a query on the Index because of an I/O problem", e);
    }
    if (searcher != null) {
      try {
        Query lquery = query.toQuery();
        if (lquery == null) {
          try {
            io.releaseSearcher(searcher);
          } catch (IOException ioe) {
            LOGGER.error("Failed releasing a Searcher after performing a query on the Index because of an I/O problem", ioe);
          }
          LOGGER.error("Failed performing a query on the Index because the query is null");
          throw new IndexException("Failed performing a query on the Index because the query is null", new NullPointerException("Null query"));
        }
        LOGGER.debug("Performing search [" + lquery.rewrite(searcher.getIndexReader()).toString()
            + "] on index " + index.getIndexID());
        TopDocs docs = searcher.search(lquery, null, 10, query.getSort());
        return new SearchResults(docs.scoreDocs, paging, io, searcher);
      } catch (IOException e) {
        try {
          io.releaseSearcher(searcher);
        } catch (IOException ioe) {
          LOGGER.error("Failed releasing a Searcher after performing a query on the Index because of an I/O problem", ioe);
        }
        LOGGER.error("Failed performing a query on the Index because of an I/O problem", e);
        throw new IndexException("Failed performing a query on the Index because of an I/O problem", e);
      }
    }
    return null;
  }

  // thread related methods
  // ----------------------------------------------------------------------------------------------

  /**
   * Start the Manager by launching the thread.
   */
  public void start() {
    // start an index worker thread
    ExecutorService threadPool = Executors.newCachedThreadPool();
    threadPool.execute(this);
  }

  /**
   * The thread's main method
   */
  public void run() {
    while (true) {
      IndexJob nextJob = null;
      try {
        nextJob = this.indexQueue.poll(INDEX_JOB_POLL_DELAY, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        // the thread was shutdown, let's die then
        return;
      }
      if (nextJob != null) {
        LOGGER.debug("Found Job To Run: " + nextJob.toString());
        // ok launch the job then
        // load the IO for this job
        IndexIO io = null;
        try {
          io = getIndexIO(nextJob.getIndex());
        } catch (Exception ex) {
          LOGGER.error("Failed to retrieve Index", ex);
          nextJob.setError("Failed to retrieve Index: " + ex.getMessage());
        }
        // retrieve content
        Content content = this.fetcher.getContent(nextJob.getContentType(), nextJob.getContentID());
        if (content == null) {
          nextJob.setError("Failed to retrieve Source content with ContentType " + nextJob.getContentType() + ", and Content ID "
              + nextJob.getContentID());
        } else {
          // check if we should delete the document
          if (content.isDeleted()) {
            deleteJob(nextJob, content, io);
          } else {
            updateJob(nextJob, content, io);
          }
        }
        // add job to list of errors
        if (nextJob.hasError()) {
          List<IndexJob> errorJobs = this.errorsForIndex.get(nextJob.getIndex().getIndexID());
          if (errorJobs == null) errorJobs = new ArrayList<IndexJob>();
          errorJobs.add(nextJob);
          this.errorsForIndex.put(nextJob.getIndex().getIndexID(), errorJobs);
        }
        this.lastActivity = System.currentTimeMillis();
      } else {
        // no jobs available, optimise if not needed
        checkForCommit();
      }
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
    try {
      // translate content
      ContentTranslatorFactory factory = this.translatorFactories.get(content.getMimeType());
      // TODO decide what to do here
      if (factory == null) {
        LOGGER.error("Mime Type "+content.getMimeType()+" is not supported, no ContentTranslatorFactory found");
        job.setError("Mime Type "+content.getMimeType()+" is not supported, no ContentTranslatorFactory found");
        return;
      }
      // ok translate now
      ContentTranslator translator = factory.createTranslator(content.getMimeType());
      Reader source;
      try {
        source = translator.translate(content);
      } catch (IndexException ex) {
        LOGGER.error("Failed to translate Source content", ex);
        job.setError("Failed to translate Source content: " + ex.getMessage());
        return;
      }
      if (source == null) {
        LOGGER.error("Failed to translate Content");
        job.setError("Failed to translate Content");
        return;
      }
      // retrieve XSLT script
      Templates templates = job.getConfig().getTemplates(job.getContentType(), content.getMimeType(), content.getConfigID());
      if (templates == null) {
        LOGGER.error("Failed to load XSLT script for Content");
        job.setError("Failed to load XSLT script for Content");
        return;
      }
      // run XSLT script
      String result;
      try {
        // prepare transformer
        Transformer t = templates.newTransformer();
        t.setOutputProperty("doctype-public", FlintEntityResolver.PUBLIC_ID_PREFIX + "Index Documents 2.0//EN");
        t.setOutputProperty("doctype-system", "");
        // retrieve parameters
        Map<String, String> params = new HashMap<String, String>(job.getConfig().getParameters(job.getContentType(), content.getMimeType(), content.getConfigID()));
        params.putAll(job.getParameters());
        for (String paramName : params.keySet())
          t.setParameter(paramName, params.get(paramName));
        // run transform
        StringWriter sw = new StringWriter();
        t.transform(new StreamSource(source), new StreamResult(sw));
        result = sw.toString();
      } catch (Exception ex) {
        LOGGER.error("Failed to create Index XML from Source content", ex);
        job.setError("Failed to create Index XML from Source content: " + ex.getMessage());
        return;
      }
      // build Lucene documents
      List<Document> documents;
      try {
        IndexParser parser = IndexParserFactory.getInstance();
        documents = parser.process(new InputSource(new StringReader(result)));
      } catch (Exception ex) {
        LOGGER.error("Failed to create Lucene Documents from Index XML", ex);
        job.setError("Failed to create Lucene Documents from Index XML: " + ex.getMessage());
        return;
        }
      try {
        // add docs to index index
        io.updateDocuments(content.getDeleteRule(), documents);
      } catch (Exception ex) {
        LOGGER.error("Failed to add Lucene Documents to Index", ex);
        job.setError("Failed to add Lucene Documents to Index: " + ex.getMessage());
        return;
      }
    } finally {
      job.finish();
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
      LOGGER.error("Failed to delete Lucene Documents from Index", ex);
      job.setError("Failed to delete Lucene Documents from Index: " + ex.getMessage());
      return;
    } finally {
      job.finish();
    }
  };

  /**
   * Retrieve an IndexIO, creates it if non existent
   * 
   * @param index
   * @return
   * @throws IndexException
   */
  private synchronized IndexIO getIndexIO(Index index) throws IndexException {
    IndexIO io = this.indexes.get(index.getIndexID());
    if (io == null) {
      LOGGER.debug("Creating a new IndexIO for " + index.getIndexID());
      try {
        io = new IndexIO(index);
      } catch (CorruptIndexException e) {
        LOGGER.error("Failed getting a Searcher to perform a query because the Index is corrupted", e);
        throw new IndexException("Failed getting a Searcher to perform a query because the Index is corrupted", e);
      } catch (LockObtainFailedException e) {
        LOGGER.error("Failed getting a lock on the Index to perform a query", e);
        throw new IndexException("Failed getting a lock on the Index to perform a query", e);
      } catch (IOException e) {
        LOGGER.error("Failed getting a searcher to perform a query on the Index because of an I/O problem", e);
        throw new IndexException("Failed getting a searcher to perform a query on the Index because of an I/O problem",
            e);
      }
      this.indexes.put(index.getIndexID(), io);
    }
    return io;
  }

  private void checkForCommit() {
    // loop through the indexes and check which one needs committing
    List<IndexIO> ios;
    synchronized (this.indexes) {
      ios = new ArrayList<IndexIO>(this.indexes.values());
    }
    for (IndexIO io : ios) {
      try {
        io.maybeCommit();
      } catch (IndexException e) {
        LOGGER.error("Failed to perform commit", e);
      }
      // make sure there's no job waiting
      if (this.indexQueue.size() > 0) return;
    }
    // ok optimise now?
    if ((System.currentTimeMillis() - lastActivity) > INACTIVE_OPTIMISE_TIME) {
      synchronized (this.indexes) {
        ios = new ArrayList<IndexIO>(this.indexes.values());
      }
      // loop through the indexes and optimise
      for (IndexIO io : ios) {
        try {
          io.maybeOptimise();
        } catch (IndexException e) {
          LOGGER.error("Failed to perform optimise", e);
        }
        // make sure there's no job waiting
        if (this.indexQueue.size() > 0) return;
      }
    }
  }

}

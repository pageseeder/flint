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

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.transform.Result;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamSource;

import org.apache.lucene.document.Document;
import org.pageseeder.flint.IndexJob.Priority;
import org.pageseeder.flint.api.Content;
import org.pageseeder.flint.api.ContentTranslator;
import org.pageseeder.flint.api.Index;
import org.pageseeder.flint.api.IndexListener;
import org.pageseeder.flint.api.Requester;
import org.pageseeder.flint.index.IndexParser;
import org.pageseeder.flint.index.IndexParserFactory;
import org.pageseeder.flint.util.FlintErrorListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class from Flint, applications should create one instance of this class.
 *
 * <ul>
 *   <li>To start and stop the indexing thread, use the methods {@link #start()} and {@link #stop()}.</li>
 *   <li>To register IndexConfigs, use the methods registerIndexConfig() and getConfig().</li>
 *   <li>To add/modify/delete content from an Index, use the method {@link #index(ContentId, Index, IndexConfig, Requester, Priority, Map)}</li>
 *   <li>To search an Index, use the methods {@link IndexingThread#query()}</li>
 *   <li>to load an Index's statuses, use the method {@link #getStatus()}</li>
 * </ul>
 *
 * @author Jean-Baptiste Reure
 * @authro Christophe Lauret
 *
 * @version 27 February 2013
 */
public final class IndexingThread implements Runnable {

  /**
   * Logger will receive debugging and low-level data, use the listener to capture specific indexing operations.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(IndexingThread.class);

  /**
   * Listens to any problem reported by the indexer.
   */
  private final IndexListener _listener;

  /**
   * The queue were index job are waiting to be run.
   */
  private final IndexJobQueue _indexQueue;

  /**
   * The queue were index job are waiting to be run.
   */
  private final IndexManager _manager;

  /**
   * Simple Constructor.
   *
   * @param cf       the Content Fetcher used to retrieve the content to index.
   * @param listener an object used to record events
   */
  public IndexingThread(IndexManager manager, IndexListener listener, IndexJobQueue queue) {
    this._manager = manager;
    this._listener = listener;
    this._indexQueue = queue;
  }

  /**
   * The thread's main method.
   */
  @Override
  public void run() {
    IndexJob job = null;
    try {
      try {
        job = this._indexQueue.nextJob();
      } catch (InterruptedException ex) {
        this._listener.error(job, "Interrupted indexing: " + ex.getMessage(), ex);
        // the thread was shutdown, let's die then
        return;
      }
      // We've got a job to handle?
      if (job == null) {
        this._listener.error(null, "Found a null job in indexing queue.", null);
        // the thread was shutdown, let's die then
        return;
      }
      // start of batch?
      if (job.isBatch() && !job.getBatch().isStarted()) {
        job.getBatch().startIndexing();
        this._listener.startBatch(job.getBatch());
      }
      this._listener.startJob(job);
      boolean success = false;
      IndexIO io = null;
      try {
        // OK launch the job then load the IO for this job
        try {
          io = this._manager.getIndexIO(job.getIndex());
        } catch (Exception ex) {
          this._listener.error(job, "Failed to retrieve Index: " + ex.getMessage(), ex);
          return;
        }
        // clear job
        if (job.isClearJob()) {
          try {
            success = io.clearIndex();
          } catch (Exception ex) {
            this._listener.error(job, "Failed to clear index", ex);
          }
        // normal content
        } else {
          // retrieve content
          success = indexContent(job, io);
        }
        // set job status
        job.setSuccess(success);
      } catch (Throwable ex) {
        this._listener.error(job, "Unknown error: " + ex.getMessage(), ex);
      } finally {
        // mark job
        job.finish();
        // tell listener that the job ended
        this._listener.endJob(job);
        // end batch?
        if (job.isBatch() && job.getBatch().isFinished()) {
          this._listener.endBatch(job.getBatch());
        }
        // update index reader and searcher
        boolean updateIndex = job.isBatch() ? job.getBatch().isFinished() : success;
        if (io != null && updateIndex) {
          io.maybeReopen();
          // commit if queue has no more jobs for this index
          if (!this._indexQueue.hasJobsForIndex(job.getIndex())) io.maybeCommit();
        }
      }
      // check the number of opened readers then
      OpenIndexManager.closeOldReaders();
      // clear the job
      job = null;
    } catch (Throwable ex) {
      this._listener.error(job, "Unexpected general error: " + ex.getMessage(), ex);
    }
  };

  // private methods
  // ----------------------------------------------------------------------------------------------

  private boolean indexContent(IndexJob job, IndexIO io) throws IndexException {

    // retrieve content
    Content content = this._manager.getContent(job);
    if (content == null) {
      this._listener.error(job, "Failed to retrieve Source content", null);
      return false;
    }

    // check if we should delete the document
    if (content.isDeleted()) {
      try {
        // delete docs from index
        io.deleteDocuments(content.getDeleteRule());
      } catch (Exception ex) {
        this._listener.error(job, "Failed to delete Lucene Documents from Index", ex);
        return false;
      }
      return true;
    }

    // translate content directly into documents
    IndexParser parser = IndexParserFactory.getInstanceForTransformation();
    try {
      translateContent(this._manager, new FlintErrorListener(this._listener, job),
                       job.getIndex(), content, job.getParameters(), parser.getResult());
    } catch (IndexException ex) {
      this._listener.error(job, ex.getMessage(), ex);
      return false;
    }
    List<Document> documents = parser.getDocuments();

//    LOGGER.debug("Found {} document(s) to update", documents.size());

    try {
      // add docs to index index
      io.updateDocuments(content.getDeleteRule(), documents);
    } catch (Exception ex) {
      this._listener.error(job, "Failed to add Lucene Documents to Index", ex);
      return false;
    }

    return true;

  }

  /**
   * Translate the provided content into Flint Index XML
   *
   * @param errorListener   a listener for the XSLT transformation errors
   * @param type            the type of the content
   * @param config          the config used to retrieve the XSLT templates
   * @param content         the content
   * @param params          list of parameters to add to the XSLT templates
   * @param out             where the result should be written to
   * @throws IndexException if anything went wrong
   */
  public static void translateContent(IndexManager manager, FlintErrorListener errorListener,
                                      Index index, Content content, Map<String, String> params, Result result) throws IndexException {
    String mediatype = content.getMediaType();
    // no MIME type found
    if (mediatype == null)
      throw new IndexException("Media Type not found.", null);
    // load translator
    ContentTranslator translator = manager.getTranslator(mediatype);
    // ok translate now
    Reader source = null;
    try {
      source = translator.translate(content);
    } catch (IndexException ex) {
      throw new IndexException("Failed to translate Source content.", ex);
    }
    if (source == null)
      throw new IndexException("Failed to translate Content as the Translator returned a null result.", null);
    // retrieve XSLT script
    Templates templates = index.getTemplates(content.getContentType(), mediatype);
    if (templates == null)
      throw new IndexException("Failed to load XSLT script for Content.", null);
    // run XSLT script
    try {
      // prepare transformer
      Transformer t = templates.newTransformer();
      if (errorListener != null) {
        t.setErrorListener(errorListener);
      }
      // retrieve parameters
      Map<String, String> parameters = new HashMap<String, String>();
      Map<String, String> indexParams = index.getParameters(content);
      if (indexParams != null) parameters.putAll(indexParams);
      if (params != null)      parameters.putAll(params);
      for (Entry<String, String> p : parameters.entrySet()) {
        t.setParameter(p.getKey(), p.getValue());
      }
      // run transform
      t.transform(new StreamSource(source), result);
    } catch (Exception ex) {
      throw new IndexException("Failed to create Index XML from Source content.", ex);
    } finally {
      try {
        source.close();
      } catch (IOException ex) {
        LOGGER.debug("Unable to close source", ex);
      }
    }
  }


}

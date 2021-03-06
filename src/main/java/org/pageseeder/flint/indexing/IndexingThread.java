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
package org.pageseeder.flint.indexing;

import org.pageseeder.flint.*;
import org.pageseeder.flint.content.Content;
import org.pageseeder.flint.content.ContentTranslator;
import org.pageseeder.flint.ixml.IndexParser;
import org.pageseeder.flint.ixml.IndexParserFactory;
import org.pageseeder.flint.templates.FlintErrorListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.Result;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Class used to index the content using iXML XSLT template.
 *
 * @author Jean-Baptiste Reure
 * @author Christophe Lauret
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
   * The queue were index job are waiting to be run.
   */
  private final boolean _singleThread;

  /**
   * Simple Constructor.
   *
   * @param manager  the manager used to retrieve the content to index.
   * @param listener an object used to record events
   */
  public IndexingThread(IndexManager manager, IndexListener listener, IndexJobQueue queue, boolean singleThread) {
    this._manager = manager;
    this._listener = listener;
    this._indexQueue = queue;
    this._singleThread = singleThread;
  }

  /**
   * The thread's main method.
   */
  @Override
  public void run() {
    IndexJob job = null;
    try {
      try {
        job = this._singleThread ? this._indexQueue.nextSingleThreadJob() : this._indexQueue.nextMultiThreadJob();
      } catch (InterruptedException ex) {
        this._listener.error(null, "Interrupted indexing: " + ex.getMessage(), ex);
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
      // OK launch the job then
      boolean success = false;
      IndexIO io = job.getIndex().getIndexIO();
      try {
        // clear job?
        if (job.isClearJob()) {
          if (!Thread.currentThread().isInterrupted()) try {
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
        // end batch?
        IndexBatch batch = job.getBatch();
        boolean batchFinished = batch != null && batch.increaseCurrent();
        // when to update index reader and searcher
        if (io != null) {
          if (batchFinished) {
            io.maybeRefresh();
            // for single jobs, do it if the job worked
          } else if (batch == null && success) {
            io.maybeRefresh();
          }
          // if queue has no more jobs for this index
          if (!this._indexQueue.hasJobsForIndex(job.getIndex())) {
            io.maybeCommit();
          }
        }
        // tell listener that the job ended
        this._listener.endJob(job);
        if (batchFinished) {
          this._listener.endBatch(job.getBatch());
        }
      }
      // check the number of opened readers then
      OpenIndexManager.closeOldReaders();
      // clear the job
      job = null;
    } catch (Throwable ex) {
      this._listener.error(job, "Unexpected general error: " + ex.getMessage(), ex);
    }
  }

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
    boolean deleted;
    try {
      deleted = content.isDeleted();
    } catch (IndexException ex) {
      this._listener.error(job, ex.getMessage(), ex);
      return false;
    }
    if (deleted) {
      if (Thread.currentThread().isInterrupted())
        return false;
      else try {
        // delete docs from index
        io.deleteDocuments(content.getDeleteRule());
      } catch (Exception ex) {
        this._listener.error(job, "Failed to delete Lucene Documents from Index", ex);
        return false;
      }
      return true;
    }

    // translate content directly into documents
    IndexParser parser = IndexParserFactory.getInstanceForTransformation(job.getCatalog());
    try {
      translateContent(this._manager, new FlintErrorListener(this._listener, job),
                       job.getIndex(), content, job.getParameters(), parser.getResult());
    } catch (IndexException ex) {
      this._listener.error(job, ex.getMessage(), ex);
      return false;
    }

    // find documents
    List<FlintDocument> documents = parser.getDocuments();
    if (documents == null || documents.isEmpty()) {
      this._listener.warn(job, "No Lucene Documents to Index!");
      return documents != null; // if no documents defined in ixml, it's not an error
    }

    // add custom fields
    Collection<FlintField> fields = job.getIndex().getFields(content);
    if (fields != null && !fields.isEmpty()) {
      for (FlintDocument doc : documents) {
        // remove existing ones with same name
        for (FlintField field : fields) {
          doc.removeFields(field.name());
        }
        for (FlintField field : fields) {
          // add fields
          doc.add(field);
        }
      }
    }

    if (Thread.currentThread().isInterrupted())
      return false;
    else try {
      // add docs to index
      if (!io.updateDocuments(content.getDeleteRule(), documents))
        this._listener.warn(job, "Failed to add Lucene Documents to Index");
    } catch (Exception ex) {
      this._listener.error(job, "Failed to add Lucene Documents to Index", ex);
      return false;
    }

    return true;

  }

  /**
   * Translate the provided content into Flint Index XML
   *
   * @param manager         the manager
   * @param errorListener   a listener for the XSLT transformation errors
   * @param index           the index used
   * @param content         the content
   * @param params          list of parameters to add to the XSLT templates
   * @param result          where the result should be written to
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
    Reader source;
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
      Map<String, String> parameters = new HashMap<>();
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

/*
 * Copyright 2026 Allette Systems (Australia)
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
package org.pageseeder.flint.berlioz.helper;


import org.pageseeder.flint.indexing.IndexBatch;

/**
 * Interface definition for a callback to be invoked when the {@link AsynchronousIndexer}
 * has finished its file-to-job submission process.
 *
 * <p><strong>Architectural Note:</strong></p>
 * <p>Because the Flint {@code IndexManager} processes jobs using a background
 * worker pool, the invocation of this listener signifies that all files have been
 * scanned, but the Lucene index may still be undergoing updates. Implementations
 * should use the provided {@link IndexBatch} to block or wait until
 * {@code batch.isFinished()} returns {@code true} before performing operations
 * that require the latest index data.</p>
 *
 * @author ccabral
 * @since 06 May 2026
 */
public interface IndexCompletionListener {

  /**
   * Invoked when the indexing submission process is complete.
   *
   * <p>This method is typically executed in a dedicated background thread to
   * prevent blocking the UI status of the indexer. It is ideal for triggering
   * heavy post-indexing tasks such as:</p>
   * <ul>
   *   <li>Rebuilding Autosuggest dictionaries</li>
   *   <li>Updating Spellchecker indexes</li>
   *   <li>Warm-up queries for search caches</li>
   * </ul>
   *
   * @param indexName the name of the Lucene index that was updated
   * @param batch     the batch object tracking the progress of the underlying
   *                  Lucene jobs; may be {@code null} if no changes were detected.
   */
  void onIndexingCompleted(String indexName, IndexBatch batch);
}

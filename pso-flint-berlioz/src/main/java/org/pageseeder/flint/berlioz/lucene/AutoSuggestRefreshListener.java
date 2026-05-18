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
package org.pageseeder.flint.berlioz.lucene;

import org.pageseeder.flint.berlioz.helper.IndexCompletionListener;
import org.pageseeder.flint.berlioz.model.FlintConfig;
import org.pageseeder.flint.berlioz.model.IndexDefinition;
import org.pageseeder.flint.berlioz.model.IndexMaster;
import org.pageseeder.flint.indexing.IndexBatch;
import org.pageseeder.flint.lucene.search.AutoSuggest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ccabral
 * @since 06 May 2026
 */
public class AutoSuggestRefreshListener implements IndexCompletionListener {
  private final static Logger LOGGER = LoggerFactory.getLogger(AutoSuggestRefreshListener.class);

  @Override
  public void onIndexingCompleted(String indexName, IndexBatch batch) {
    // 1. Wait for the asynchronous Flint IndexManager workers to finish
    if (batch != null) {
      try {
        LOGGER.info("Dictionary listener waiting for batch {} to complete...", indexName);
        while (!batch.isFinished()) {
          Thread.sleep(1000); // Check every second
          LOGGER.info("Still indexing {}...", indexName);
        }
        IndexMaster index = FlintConfig.get().getMaster(indexName.trim());
        IndexDefinition indexDefinition = FlintConfig.get().getIndexDefinitionFromIndexName(indexName);
        for(String autosuggestName:indexDefinition.listAutoSuggestNames()) {
          AutoSuggest autoSuggest = index.getAutoSuggest(autosuggestName);
          LOGGER.info("Auto suggest {} found", autosuggestName);
          LOGGER.info("Auto suggest version: {}", autoSuggest != null ? autoSuggest.getLastBuilt() : "");
        }

      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
    }

    // 2. Perform the logic (Autosuggest / Spellcheck)
    // Since we are in a separate thread now, this can take as long as needed.
    buildDictionary(indexName);
  }

  private void buildDictionary(String indexName) {
    // Your Lucene 9 Dictionary/Suggest logic here
    LOGGER.info("Starting dictionary build for index: {}", indexName);
  }
}
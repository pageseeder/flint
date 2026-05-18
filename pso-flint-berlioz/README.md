[ ![Download](https://api.bintray.com/packages/pageseeder/maven/flint/images/download.svg) ](https://bintray.com/pageseeder/maven/flint/_latestVersion)

# berlioz-flint

Berlioz library to use Flint

## Comprehensive Guide to the Flint XML Configuration Architecture

The Flint framework uses a centralized XML file `config-{mode}.xml` to manage Apache Lucene indexing in Java applications. This configuration controls the entire search lifecycle, including:

File system directory polling and thread allocation.

Document transformations using XSLT templates.

Autocomplete setup via AutoSuggest engines.

Automated post-indexing event management.

Understanding this structure is essential for building stable, high-performance search solutions.

### Global Control Elements

The root `<flint>` element contains the global system settings. Before any content parsing begins, two main elements configure the asynchronous indexing environment:

1. `<watcher>` (Directory Monitoring)

2. This element manages filesystem polling to detect changes in source data repositories.

`root`: The absolute base directory containing the source files (XML, PSML, etc.).

`max-folders`: The limit on monitored subdirectories. Set to -1 for unlimited nesting traversal.

`delay`: A "quiet period" buffer in seconds. Flint waits for this duration to pass without new file activity before triggering a job, preventing resource exhaustion from rapid, consecutive writes.

`watch`: A boolean flag (`true`/`false`). Set to `false` if directory updates are handled programmatically by your application, which prevents redundant thread locks.

`excludes`: A comma-separated list of files or folders to ignore during initialization.

2. `<threads>` (Worker Pool Allocation)

This element configures and scales the background thread pool assigned to the Flint IndexManager.

number: The maximum number of concurrent worker threads handling indexing queues.

priority: The JVM thread execution priority, scaling from 1 (lowest) to 10 (highest).

Tip: A balanced setup (e.g., number="10" and priority="5") ensures large batch ingestion runs efficiently without starving front-end web request channels.

### Processing Boundaries: Document Scoping and Type Definitions

The actual work of indexing is governed by the `<index>` element container, which functions as a structural routing matrix. The `<index>` element groups data processing workflows using two defining parameters: `types` and `extensions`. The `types` attribute is a comma-separated list of names mapping directly to internal search profile configurations, while the `extensions` attribute defines a strict whitelist of lowercase file types (such as `xml,psml`) that the ingestion loops will inspect. Any document failing to match these extensions is skipped during traversal.

Inside the `<index>` block, developers declare custom-named inner tags that represent explicit, isolated index profile definitions (such as `<data>`,  or `<static>`). These tags match the identifiers provided in the parent `types` collection and hold the critical parsing rules for those specific spaces. Every index definition profile uses a series of core attributes to bind data to disk targets:

*   **`name`**: The unique system token used to query, optimize, or reference this exact Lucene index space programmatically within the Java backend application.
*   **`path`**: The target directory location containing the source files, evaluated relative to the global baseline root folder established in the `<watcher>` block.
*   **`template`**: The filename or relative path of an iXML/XSLT stylesheet. This template is the core mapping engine; it intercepts the raw source files and transforms them into standard Lucene `Document` and `Field` structures.
*   **`autosuggests`**: A comma-separated list of lookup keys that map directly to underlying custom dictionary child blocks nested within the profile.
*   **`excludes`**: Fine-grained, comma-separated local sub-directories or specific filenames to skip during deep indexing passes.
*   **`no-stop-words`**: A legacy configuration attribute historically used to toggle language-specific stop-word filtration. In modern iterations of the Flint engine, this is typically bypassed as tokenization controls are managed inside the XSLT mapping templates or Lucene Analyzers.

To apply precise constraints on file selection within these index profiles, developers utilize the `<files>` child element. This component uses regular expressions to enforce inclusion and exclusion rules. The `includes` attribute accepts a regex pattern that a file path must match to qualify for indexing, while the `excludes` attribute provides a corresponding regex to reject specific files. This layer is highly effective for filtering out scratch files, backup templates, or draft content that should not be exposed to search indexes.

### Real-Time Search Enhancements: AutoSuggest Sub-Systems

To deliver responsive user experiences, the Flint framework natively embeds autocomplete indexing capabilities within the configuration file via custom suggester blocks. These blocks are declared as inner tags named after the tokens listed in the parent profile's `autosuggests` attribute (for example, a `<data>` index might house a `<medicines>` tag, while a `<static>` index houses `<generic-title>` or `<pdf>` tags).

These elements map directly to Lucene's advanced `AnalyzingInfixSuggester` architectures, turning indexed document fields into searchable autocomplete dictionaries. The behavior of these real-time suggestion blocks is managed using several key attributes:

*   **`fields`**: A comma-separated list of fields from the primary Lucene index that act as the source text for compiling autocomplete lookup dictionaries.
*   **`result-fields`**: Companion metadata fields that are stored directly within the suggestion payload. When a user selects a suggestion, the application can instantly return these values (such as original category strings, codes, or descriptions) to the front-end user interface without executing a separate, resource-intensive lookup against the primary index.
*   **`terms`**: A boolean flag determining lookup evaluation rules. Setting this to `true` enforces exact, literal term matching; setting it to `false` configures predictive prefix or infix analysis, allowing inputs to match internal fragments or mid-phrase terms.
*   **`criteria-fields` & `criteria-values`**: These two attributes work together to provide context-aware filtering. The `criteria-fields` attribute defines the document attribute used as a facet or filter constraint (e.g., `category`), while `criteria-values` defines the hardcoded token it must match. This allows developers to build scoped lookup indexes that only offer autocomplete suggestions relevant to a specific subset of the index.

### Lifecycle Hooks & Concurrency

Because Flint indexing is asynchronous, updates happen in the background. The AsynchronousIndexer scans files quickly and queues them, but the IndexManager worker pool continues writing data to the Lucene index afterward.

If an autocomplete dictionary tries to rebuild while these background writes are active, it can result in a stale dictionary or cause thread collisions like a LockObtainFailedException.

To solve this coordination issue, the `<post-indexing>` configuration element provides an asynchronous callback gateway that triggers only after a primary index update batch is fully finalized and committed to disk.

```XML
<post-indexing listeners="org.pageseeder.flint.berlioz.lucene.AutoSuggestRefreshListener" />
```

**How it Works**

- Initialization: The configuration parser reads the listener class names (FQCN) from the XML and attaches them to the IndexDefinition model.

- Loading: When an AsynchronousIndexer batch task executes, it pulls these listener instances from the definition.

- Execution: Once the background workers finish all writes and the batch officially closes (batch.isFinished() == true), the framework calls the listener's onIndexingCompleted method in a separate thread.

**Key Benefits**

- Batch-Optimized Rebuilding: Instead of rebuilding the suggestion dictionary after every individual document update—which degrades performance during large folder uploads—the listener waits for the entire batch to finish and rebuilds it exactly once.

- Concurrency Safety: Running the refresh in a post-indexing callback thread ensures the primary index writer has already released its directory locks. The listener can safely open a fresh IndexReader, pass it to AutoSuggest.build(reader), and properly close the old suggester instance to release its .lock file without risking collisions.

- Decoupled Architecture: Keeping listener definitions in the XML configuration prevents post-processing tasks from being hardcoded into core execution engines or Berlioz generators. This allows you to seamlessly add new listeners (e.g. spellcheck updates) without modifying application code.

### Full example

```xml

<flint>
    <!--
      - max-folders: Max watch folders
      - delay: Indexing delay in seconds
      - watch: true or false are allowed. It indicates if it should watch or not. we put false because IndexDrug handles it.
      - root: The root folder where the content to be indexed are.
      - excludes: comma separated list of files/folder to ignore.

    -->
    <watcher max-folders="-1" delay="0" watch="false" root="/data" excludes=""/>
    <!--
      - number:
      - priority:
    -->
    <threads number="10" priority="5"/>
    <!--
      - types: comma separeted list with the name of indexes.
      - extensions: comma separated list.
      - Types
      - - data is the extra index
      - - static are the PSMLs index
     -->

<!--    <index types="data,static" extensions="xml,psml">-->
    <index types="data,static" extensions="xml,psml">
      <!--
        - name: The index name,
        - path: The folder where the content to be indexed are.
        - template: The ixml logic for this index
        - autosuggests: comma separated list of fields
        - excludes:  comma separated list of files/folder to ignore.
      -->
      <data name="data"
               path="/data"
               template="default.xsl"
               autosuggests="suggests"
               excludes="">
        <!--
          - includes: regex
          - excludes: regex
        -->
        <files/>
        <!--
          - fields: index field
          - terms: true means only the term sent.
          - result-fields:
          - weight
          - suggesters: it is used for solr.
        -->
        <suggests  fields="field-01,field-02"
                    result-fields="field-01,field-02"
                    terms="false"
                    criteria-fields="field-03"/>

        <post-indexing listeners="org.pageseeder.flint.berlioz.lucene.AutoSuggestRefreshListener" />
      </data>

      <!--
        - name: The index name,
        - path: The folder where the content to be indexed are.
        - template: The ixml logic for this index
        - autosuggests: comma separated list of fields
        - excludes:  comma separated list of files/folder to ignore.
        - no-stop-words: Currently the flint code ignore it and there is not difference if true or false.
      -->
      <static name="static"
              path="/psml"
              template="static.xsl"
              autosuggests="generic-title,generic-keywords,psd"
              excludes=""
              no-stop-words="false">
        <!--
          - includes: regex
          - excludes: regex
        -->
        <files/>
        <!--
          - fields: index field
          - terms: true means only the term sent.
          - result-fields:
          - weight
          - suggesters: it is used for solr.
        -->
        <generic-title fields="title"
                 result-fields="title,description"
                 terms="false"
                 criteria-fields="category"
                 criteria-values="static"/>
        <generic-keywords fields="keywords"
                    result-fields=""
                    terms="false"
                    criteria-fields="category"
                    criteria-values="static"/>
        <pdf fields="pdf-title,pdf-description"
             terms="false"
             criteria-fields="pdf-group"
             criteria-values="pdf" />
      </static>
    </index>
  </flint>

```

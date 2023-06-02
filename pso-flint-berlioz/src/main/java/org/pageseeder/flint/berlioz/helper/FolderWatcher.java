package org.pageseeder.flint.berlioz.helper;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.util.Pair;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.Requester;
import org.pageseeder.flint.berlioz.model.FlintConfig;
import org.pageseeder.flint.berlioz.model.IndexDefinition;
import org.pageseeder.flint.berlioz.model.IndexMaster;
import org.pageseeder.flint.indexing.IndexBatch;
import org.pageseeder.flint.indexing.IndexJob.Priority;
import org.pageseeder.flint.local.LocalFileContentType;
import org.pageseeder.flint.lucene.LuceneLocalIndex;
import org.pageseeder.flint.lucene.query.BasicQuery;
import org.pageseeder.flint.lucene.query.PrefixParameter;
import org.pageseeder.flint.lucene.query.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;

public class FolderWatcher {

  private static final Logger LOGGER = LoggerFactory.getLogger(FolderWatcher.class);

  private final File _root;
  private final int _maxFolders;
  private final DelayedIndexer _delayedIndexer;
  private final ExecutorService _indexThread;
  private final List<Path> _ignore = new ArrayList<>();
  private FileTreeCrawler crawler = null;

  /**
   * @param root          the root folder
   * @param maxFolders    the max nb of folders to watch (-1 means unlimited)
   * @param indexingDelay the delay between a file change and its indexing
   */
  public FolderWatcher(File root, int maxFolders, int indexingDelay) {
    this._maxFolders = maxFolders;
    this._root = root;
    if (indexingDelay > 0) {
      this._delayedIndexer = new DelayedIndexer(indexingDelay);
      this._indexThread = Executors.newSingleThreadExecutor();
    } else {
      this._delayedIndexer = null;
      this._indexThread = null;
    }
  }

  /**
   * Set a list of paths (relative to root) that the watcher should ignore
   *
   * @param toignore list of paths to ignore
   */
  public void setIgnore(List<String> toignore) {
    for (String ig : toignore) {
      if (ig != null && !ig.isEmpty())
        this._ignore.add(new File(this._root, ig).toPath());
    }
  }

  /**
   * Start the folder watcher
   */
  public void start() {
    LOGGER.debug("Starting watcher on root folder {} (max folders is {}) ", this._root.getAbsolutePath(), this._maxFolders);
    // start delayed indexing if setup
    if (this._delayedIndexer != null) {
      this._indexThread.execute(this._delayedIndexer);
    }
    // go through folder hierarchy to add watchers
    this.crawler = new FileTreeCrawler(this._root.toPath(), this._ignore, (path, kind) -> {
      // if deleted, file does not exist anymore so
      // can't check for folder, use filename TODO is this the best?
      if (kind == ENTRY_DELETE && path.getFileName().toString().indexOf('.') == -1) {
        folderDeleted(path.toFile());
      } else if (kind == ENTRY_CREATE && Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
        folderAdded(path.toFile());
      } else if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
        fileChanged(path.toFile());
      }
    }, this._maxFolders);
    // start crawling
    try {
      this.crawler.start();
    } catch (IOException ex) {
      LOGGER.error("Failed to start watcher", ex);
    }
  }

  /**
   * Stop watcher and delayed indexing thread if setup.
   */
  public void stop() {
    if (this.crawler != null)
      this.crawler.stop();
    if (this._delayedIndexer != null) {
      this._delayedIndexer.stop();
      this._indexThread.shutdown();
    }
  }

  /**
   * When a file was changed on the file system.
   *
   * @param file the modified file
   */
  private void fileChanged(File file) {
    LOGGER.debug("File changed {}", file);
    FlintConfig config = FlintConfig.get();
    Collection<IndexMaster> destinations = getLuceneDestinations(file, config);
    // index it if there's a destination
    if (!destinations.isEmpty()) {
      // delayed indexing?
      if (this._delayedIndexer == null) {
        LOGGER.debug("Re-indexing file {}", file);
        for (IndexMaster destination : destinations)
          config.getManager().index(file.getAbsolutePath(), LocalFileContentType.SINGLETON, destination.getIndex(),
                                    new Requester("Berlioz File Watcher"), Priority.HIGH, null);
      } else {
        LOGGER.debug("Delay re-indexing of file {}", file);
        for (IndexMaster destination : destinations)
          this._delayedIndexer.index(destination.getIndex(), file.getAbsolutePath());
      }
    }
  }

  private void folderAdded(File file) {
    File[] files = file.listFiles();
    if (files == null) return;
    LOGGER.debug("Folder added {}", file);
    FlintConfig config = FlintConfig.get();
    // lucene
    Collection<IndexMaster> destinations = getLuceneDestinations(file, config);
    // index it if there's a destination
    if (!destinations.isEmpty()) {
      // delayed indexing?
      if (this._delayedIndexer == null) {
        LOGGER.debug("Re-indexing file {}", file);
        Requester req = new Requester("Berlioz File Watcher");
        for (File afile : files) {
          if (Files.isDirectory(afile.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            folderAdded(afile);
          } else {
            for (IndexMaster destination : destinations) {
              config.getManager().index(afile.getAbsolutePath(), LocalFileContentType.SINGLETON,
                                        destination.getIndex(), req, Priority.HIGH, null);
            }
          }
        }
      } else {
        LOGGER.debug("Delay re-indexing of file {}", file);
        for (File afile : files) {
          if (Files.isDirectory(afile.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            folderAdded(afile);
          } else {
            for (IndexMaster destination : destinations) {
              this._delayedIndexer.index(destination.getIndex(), afile.getAbsolutePath());
            }
          }
        }
      }
    }
  }

  private void folderDeleted(File file) {
    LOGGER.debug("Folder deleted {}", file);
    FlintConfig config = FlintConfig.get();
    // lucene
    Collection<IndexMaster> destinations = getLuceneDestinations(file, config);
    // index it if there's a destination
    for (IndexMaster destination : destinations) {
      Collection<File> toReIndex = new ArrayList<>();
      // find all files located in there
      try {
        String path = destination.getIndex().fileToPath(file);
        SearchResults results = destination.query(BasicQuery.newBasicQuery(new PrefixParameter(new Term("_path", path))));
        for (Document doc : results.documents()) {
          toReIndex.add(destination.getIndex().pathToFile(doc.get("_path")));
        }
        results.terminate();
      } catch (IndexException ex) {
        LOGGER.error("Failed to load files in folder {}", file, ex);
        return;
      }
      // delayed indexing?
      if (this._delayedIndexer == null) {
        LOGGER.debug("Re-indexing file {}", file);
        Requester req = new Requester("Berlioz File Watcher");
        for (File afile : toReIndex) {
          if (Files.isDirectory(afile.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            folderDeleted(afile);
          } else {
            config.getManager().index(afile.getAbsolutePath(), LocalFileContentType.SINGLETON, destination.getIndex(), req, Priority.HIGH, null);
          }
        }
      } else {
        LOGGER.debug("Delay re-indexing of file {}", file);
        for (File afile : toReIndex) {
          if (Files.isDirectory(afile.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            folderDeleted(afile);
          } else {
            this._delayedIndexer.index(destination.getIndex(), afile.getAbsolutePath());
          }
        }
      }
    }
  }

  private Collection<IndexMaster> getLuceneDestinations(File file, FlintConfig config) {
    List<IndexMaster> indexes = new ArrayList<>();
    // find which index that file is in
    for (IndexMaster master : config.listIndexes()) {
      if (master.isInIndex(file)) {
        indexes.add(master);
      }
    }
    // no index, check the configs then
    String path = '/' + org.pageseeder.flint.berlioz.util.Files.path(GlobalSettings.getAppData(), file);
    for (IndexDefinition def : config.listDefinitions()) {
      String name = def.findIndexName(path);
      if (name != null) {
        // create new index
        IndexMaster m = config.getMaster(name);
        if (m != null && !indexes.contains(m))
          indexes.add(m);
      }
    }
    return indexes;
  }
  /**
   * Delayed indexing thread.
   * A loop checks every second for all the files in the list.
   * If a file's delay has expired, it is indexed then removed from the list.
   */
  private static class DelayedIndexer implements Runnable {

    /** if the thread has been stopped */
    private boolean keepGoing = true;
    /** the indexing delay (in seconds) */
    private final int _delay;
    /** the list of delayed files */
    private final Map<Pair<String, LuceneLocalIndex>, AtomicInteger> _delayedLuceneIndexing = new ConcurrentHashMap<>(100, 0.8f);

    /**
     * @param delay the indexing delay in seconds
     */
    DelayedIndexer(int delay) {
      this._delay = delay;
    }

    /**
     * Mark the thread so that is will stop when possible next.
     */
    void stop() {
      this.keepGoing = false;
    }

    /**
     * Add a new file to be indexed
     * @param index the index
     * @param path  the file
     */
    public void index(LuceneLocalIndex index, String path) {
      Pair<String, LuceneLocalIndex> key = new Pair<>(path, index);
      synchronized (this._delayedLuceneIndexing) {
        AtomicInteger delay = this._delayedLuceneIndexing.get(key);
        // add it if not there
        if (delay == null)
          this._delayedLuceneIndexing.put(key, new AtomicInteger(this._delay));
        // reset otherwise
        else delay.set(this._delay);
      }
    }

    @Override
    public void run() {
      FlintConfig config = FlintConfig.get();
      bigloop: while (this.keepGoing) {
        // check every second
        try {
          Thread.sleep(1000);
        } catch (InterruptedException ex) {
          if (!this.keepGoing) break;
          LOGGER.error("Failed to wait 1s in delayed indexer", ex);
          break;
        }
        // one requester
        Requester req = new Requester("Berlioz File Watcher Delayed Indexer");
        // lucene indexes first
        synchronized (this._delayedLuceneIndexing) {
          // do we need batches?
          Map<String, IndexBatch> batches = null;
          if (this._delayedLuceneIndexing.size() > 1) {
            batches = new HashMap<>();
          }
          try {
            // loop through delayed files
            for (Pair<String, LuceneLocalIndex> toIndex : new ArrayList<>(this._delayedLuceneIndexing.keySet())) {
              AtomicInteger timer = this._delayedLuceneIndexing.get(toIndex);
              if (timer.decrementAndGet() == 0) {
                LOGGER.debug("Re-indexing file {} after delay", toIndex.first());
                // index now
                if (batches == null) {
                  config.getManager().index(toIndex.first(), LocalFileContentType.SINGLETON, toIndex.second(), req, Priority.HIGH, null);
                } else {
                  IndexBatch batch = batches.get(toIndex.second().getIndexID());
                  if (batch == null) {
                    batch = new IndexBatch(toIndex.second().getIndexID());
                    batches.put(batch.getIndex(), batch);
                  }
                  batch.increaseTotal();
                  config.getManager().indexBatch(batch, toIndex.first(), LocalFileContentType.SINGLETON, toIndex.second(), req, Priority.HIGH, null);
                }
                // delete it
                this._delayedLuceneIndexing.remove(toIndex);
              }
              if (!this.keepGoing) break bigloop;
            }
          } finally {
            // complete batches
            if (batches != null) {
              for (IndexBatch batch : batches.values()) {
                batch.setComputed();
              }
            }
          }
        }
      }
    }
  }

}

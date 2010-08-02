package org.weborganic.flint.util;

import java.io.File;
import java.io.FileFilter;
import java.util.LinkedList;
import java.util.List;

/**
 * Collects files in a root directory and its sub-directories, using the specified file filter.
 * 
 * <p><b>NOTE:</b> this file does not implement the Lucene {@link org.apache.lucene.search.Collector}
 * interface and bears no relation with it.
 * 
 * @author Christophe Lauret
 * @version 2 August 2010
 */
public final class FileCollector {

  /**
   * The root of all the file to index.
   */
  private final File _root;

  /**
   * Files to filter.
   */
  private final FileFilter _filter;

  /**
   * Creates a new unfiltered file collector.
   * 
   * @param root The root directory to scan.
   */
  public FileCollector(File root) {
    this._root = root;
    this._filter = null;
  }

  /**
   * Returns all the files as specified.
   * 
   * @return all the files as specified.
   */
  public List<File> list() {
    return list(this._root, this._filter);
  }

  /**
   * Lists all the files in the specified directory and its descendants. 
   * 
   * <p>Note: The file filter only affects files, directory are still scanned regardless.
   * 
   * @param root   the root directory to scan.
   * @param filter a file filter to use.
   * 
   * @return all the collected files matching the filter.
   */
  public static List<File> list(File root, FileFilter filter) {
    List<File> files = new LinkedList<File>();
    list(root, filter, files);
    return files; 
  }

  /**
   * Lists all the files in the specified directory and its descendants. 
   * 
   * <p>Note: The file filter affects both files and directories.
   * 
   * @param root   the root directory to scan.
   * 
   * @return all the collected files matching the filter.
   */
  public static List<File> list(File root) {
    return list(root, null);
  }

  // private helpers ------------------------------------------------------------------------------

  /**
   * Lists all the files in the specified directory and its descendants. 
   * 
   * <p>Note: The file filter only affects files, directory are still scanned regardless.
   * 
   * @param dir      the root directory to scan.
   * @param filter    a file filter to use.
   * @param collected files collected so far.
   */
  private static void list(File dir, FileFilter filter, List<File> collected) {
    // get all the files in the current directory
    File[] files = filter != null? dir.listFiles(filter) : dir.listFiles();
    // iterate over the files, collect
    for (File f : files) {
      // scan directories
      if (f.isDirectory()) {
        list(f, filter, collected);
      } else {
        // collect files only
        collected.add(f);
      }
    }
  }

}

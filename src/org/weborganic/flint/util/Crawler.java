/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.util;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

/**
 * Goes through the specified directories and build a list of files.
 *
 * <p>The crawler is a convenience class, given a directory it can produce the corresponding
 * <code>FileList</code>.
 *
 * @author  Christophe Lauret (Weborganic)
 * @version 8 September 2009
 */
public final class Crawler {

  /**
   * A logger for this class.
   */
  private static final Logger LOGGER = Logger.getLogger(Crawler.class);

  /**
   * The root directory to crawl.
   */
  private final File _directory;

  /**
   * Set to <code>true</code> if the crawler should through the directories recursively.
   */
  private final boolean _recurse;

// methods and constructors -----------------------------------------------------------------------

  /**
   * Creates a new Crawler.
   *
   * @param directory  The directory to crawl
   * @param recurse    <code>true</code> to crawl through subdirectories recursively;
   *                   <code>false</code> otherwise.
   */
  public Crawler(File directory, boolean recurse) {
    this._directory = directory;
    this._recurse = recurse;
  }

  /**
   * Crawls through this directory.
   * 
   * @param list The list of files to use. 
   */
  public void crawl(FileList list) {
    crawl(this._directory, list);
  }

  /**
   * Crawls through the specified directory and add the files encountered to the list.
   *
   * <p>This method is recursive and will invoke itself when finding directories.
   *
   * @param file The file to crawl through
   * @param list The list of files to use
   *
   * @throws NullPointerException If the list or the file is null
   */
  public void crawl(File file, FileList list) throws NullPointerException {
    // directories
    if (file.isDirectory()) {
      LOGGER.info("Crawling directory " + file.getName());
      File[] files = file.listFiles();
      for (int i = 0; i < files.length; i++) {
        if (this._recurse)
          crawl(files[i], list);
        else
          if (!files[i].isDirectory()) crawl(files[i], list);
      }
    // files
    } else if (file.isFile()) {
      LOGGER.info("Adding file " + file.getName());
      list.add(file);
    }
  }

// convenience methods ----------------------------------------------------------------------------


  /**
   * Returns the file list corresponding to the given directory.
   *
   * @param file    The directory to crawl.
   * @param recurse <code>true</code> to crawl through subdirectories recursively;
   *                <code>false</code> otherwise.
   * 
   * @return The corresponding file list.
   */
  public static FileList crawlIn(File file, boolean recurse) {
    Crawler crawler = new Crawler(file, true);
    FileList list = new AbsoluteFileList();
    crawler.crawl(list);
    return list;
  }


  /**
   * To invoke the Crawler from the command-line.
   *
   * @see #usage
   * 
   * @param args The commad line parameters.
   *
   * @throws IOException If and error occurs whilst saving the file.
   */
  public static void main(String[] args) throws IOException {
    // get command-line arguments
    String dir = CommandLine.getParameter("-d", args);
    String flist = CommandLine.getParameter("-list", args);
    boolean shallow = CommandLine.hasSwitch("-shallow", args);
    boolean append = CommandLine.hasSwitch("-append", args);

    // checking arguments
    if (dir == null) {
      usage("you must specify a directory");
      System.exit(0);
    }
    File directory = new File(dir);
    if (!directory.exists()) System.exit(0);
    if (flist == null) flist = "crawler.tmp";

    // Create list of documents to index
    Crawler crawler = new Crawler(directory, !shallow);
    FileList list = new AbsoluteFileList();
    crawler.crawl(list);

    // save the list of file to the file system
    list.saveAs(new File(flist), append);
  }

  /**
   * Displays the usage for this main method on System.err.
   *
   * <p>An error message can be specified and will
   * be displayed before the standard usage.
   *
   * <p>The current implementation displays:
   * <pre>
   *     usage: Crawler -d &lt;directory&gt; &lt;options&gt;
   *       &lt;directory&gt;     Specify the directory to index
   *     possible options include:
   *       -list &lt;file&gt;  Specify the filename for the list of files.
   *       -shallow      Do not recurse subdirectories
   *       -append       Append to the filelist to an existing one.
   * </pre>
   * 
   * @param error The error that ocurred.
   */
  public static void usage(String error) {
    if (error != null) System.err.println("error: "+error);
    System.err.println("usage: Crawler -d <directory> <options>");
    System.err.println("  <directory>     Specify the directory to index");
    System.err.println("possible options include:");
    System.err.println("  -list <file>  Specify the filename for the list of files");
    System.err.println("  -shallow      Do not recurse subdirectories");
    System.err.println("  -append       Append to the filelist to an existing one");
  }

}

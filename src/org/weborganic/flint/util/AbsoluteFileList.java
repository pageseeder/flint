/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Implementation of the file list interface storing only files denoted by their abstract
 * path name, but using their absolute path.
 *
 * <p>This list is synchronized.
 *
 * @deprecated This is a pre-Java5 class, use the <code>FileCollector</code> instead.
 *
 * @author  Christophe Lauret (Weborganic)
 * @version 9 August 2004
 */
public class AbsoluteFileList implements FileList {

  /**
   * list maintained by this class
   */
  private final List<File> filelist;

  /**
   * Create a new file list.
   */
  public AbsoluteFileList() {
    this.filelist = Collections.synchronizedList(new LinkedList<File>());
  }

  /**
   * Appends the specified file to the end of this list.
   *
   * <p>If the file does not denote and abstract path name
   * then a new file is created using its absolute path name.
   *
   * <p>Does nothing if the file is <code>null</code>.
   *
   * @see File#isAbsolute
   * @see File#getAbsoluteFile
   *
   * @param file to file to append to the list
   *
   * @return true if the file was added; false otherwise.
   */
  public final boolean add(File file) {
    if (file == null) return false;
    if (file.isAbsolute())
      return this.filelist.add(file);
    else
      return this.filelist.add(file.getAbsoluteFile());
  }

  /**
   * Appends all of the files in the specified file list to the end of this list, in the order 
   * that they are stored.
   *
   * <p>Does nothing if the list is <code>null</code>.
   *
   * @param list a file list
   *
   * @return true if the list was added; false otherwise.
   */
  public final boolean addAll(FileList list) {
    if (list == null) return false;
    for (int i = 0; i < list.size(); i++) {
      this.filelist.add(list.get(i));
    }
    return true;
  }

  /**
   * Returns the file at the specified position in this list.
   *
   * @param index index of file to return.
   *
   * @return Thr file corresponding to the index.
   *
   * @throws IndexOutOfBoundsException if the specified index is is out of range (index < 0 || index >= size()).
   */
  public final File get(int index) throws IndexOutOfBoundsException {
    return (File)this.filelist.get(index);
  }

  /**
   * Returns the number of elements in this list.
   *
   * @return the number of files in the list
   */
  public final int size() {
    return this.filelist.size();
  }

  /**
   * Saves this file list as a the specified file.
   *
   * <p>Every file is stored on a line.
   *
   * <p>Use <code>append</code> to specify whether the file list
   * should be appended to an existing one or not. The file
   * need not exist.
   *
   * @param file   The file this file list list should be saved as.
   * @param append Whether the file list should be appended to an existing one or not.
   *
   * @throws FileNotFoundException If thrown by the {@link FileOutputStream} constructor.
   */
  public final void saveAs(File file, boolean append) throws FileNotFoundException {
    // save the list of file to the file system
    PrintWriter out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(file)));
    for (int i = 0; i < this.filelist.size(); i++) {
      out.println(this.filelist.get(i));
    }
    out.flush();
  }

  /**
   * Loads a file list from a file and make a filelist instance.
   *
   * <p>A FileList file must follow the prolog:
   * <pre>
   *   filelist ::= (line)+
   *   line     ::= (comment | filepath)?  end_of_line_character
   *   comment  ::= ':' (anychar)*
   *   filepath ::= (any_valid_filepath_on_the_platform)
   * </pre>
   *
   * <p>The filename need to denote a valid abstract path name,
   * otherwise an exception will be reported by the <code>File</code>
   * constructor.
   *
   * @see File
   *
   * @param file The file which contains a file list.
   *
   * @throws IOException Should an error occur
   *
   * @return The corresponding file list.
   */
  public static final FileList make(File file) throws IOException {
    FileList list = new AbsoluteFileList();
    BufferedReader in = new BufferedReader(new FileReader(file));
    while (true) {
      String filename = in.readLine();
      if (filename == null) break;
      if (filename.length() > 0 && filename.charAt(0) != ':') {
        list.add(new File(filename));
      }
    }
    return list;
  }

}

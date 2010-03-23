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

/**
 * An interface to define list of files.
 *
 * @author  Christophe Lauret (Weborganic)
 * @version 9 August 2004
 */
public interface FileList {

  /**
   * Appends the specified file to the end of this list.
   *
   * @param file to file to append to the list.
   * 
   * @return <code>true</code> if the file was added; <code>false</code> otherwise.
   */
  boolean add(File file);

  /**
   * Appends all of the files in the specified file list 
   * to the end of this list, in the order that they are 
   * returned by the specified file list iterator.
   *
   * @param list a file list
   * 
   * @return <code>true</code> if the file was added; <code>false</code> otherwise.
   */
  boolean addAll(FileList list);

  /** 
   * Returns the file at the specified position in this list.
   *
   * @param index index of file to return.
   * 
   * @return <code>true</code> if the file was added; <code>false</code> otherwise.
   *
   * @throws IndexOutOfBoundsException If the specified index is is out of range
   *                                   (index < 0 || index >= size()).
   */
  File get(int index) throws IndexOutOfBoundsException;

  /**
   * Returns the number of elements in this list.
   *
   * @return the number of files in the list
   */
  int size();

  /**
   * Saves the file list instance as a the specified file.
   *
   * <p>Every file should be stored on a line.
   *
   * <p>Use <code>append</code> to specify whether the file list
   * should be appended to an existing one or not. The file
   * need not exist.
   *
   * @param file The file this file list should be saved as.
   * @param append Whether the file should be appended to an existing one or not.   
   *
   * @throws IOException should an error occur when saving the file.
   */
  void saveAs(File file, boolean append) throws IOException;

}

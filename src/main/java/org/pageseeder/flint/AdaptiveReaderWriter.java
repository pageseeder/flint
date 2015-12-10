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

import java.io.CharArrayReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * A class which can supply a writer then a reader to be used immediatly after.
 *
 * <p>It is designed so that when there are more than 1M characters, the buffer
 * switches to the file system to store the temporary data.
 *
 * @author Christophe Lauret
 * @version 2 April 2014
 */
final class AdaptiveReaderWriter {

  private final AdaptiveWriter _writer;

  /**
   *
   */
  public AdaptiveReaderWriter() {
    this._writer = new AdaptiveWriter(1024);
  }

  public Writer getWriter() {
    return this._writer;
  }

  public Reader getReader() throws IOException {
    if (this._writer.file != null) return new InputStreamReader(new FileInputStream(this._writer.file), Charset.forName("utf-8"));
    else return new CharArrayReader(this._writer.buffer, 0, this._writer.size());
  }

  public void cleanup() throws IOException {
    File f = this._writer.file;
    if (f != null && f.exists()) {
      f.delete();
    }
  }

  /**
   * A writer
   *
   * @author Christophe Lauret
   * @version 2 April 2014
   */
  public static class AdaptiveWriter extends Writer {

    /** Threshold before writing out to file. */
    private static final int FILE_THRESHOLD = 1024*1024;

    /**
     * The buffer where data is stored.
     */
    private char buffer[];

    /**
     *
     */
    private File file = null;

    /**
     *
     */
    private Writer writer = null;

    /**
     * The number of chars in the buffer.
     */
    private int count;

    /**
     * Creates a new CharArrayWriter with the specified initial size.
     *
     * @param initialSize  an int specifying the initial buffer size.
     * @exception IllegalArgumentException if initialSize is negative
     */
    public AdaptiveWriter(int initialSize) {
      if (initialSize < 0) throw new IllegalArgumentException("Negative initial size: "+ initialSize);
      this.buffer = new char[initialSize];
    }

    @Override
    public void write(int c) throws IOException {
      synchronized (this.lock) {
        int newcount = this.count + 1;
        if (newcount > FILE_THRESHOLD) {
          flushToFile();
          newcount = 1;
        } else if (newcount > this.buffer.length) {
          this.buffer = Arrays.copyOf(this.buffer, Math.max(this.buffer.length << 1, newcount));
        }
        this.buffer[this.count] = (char)c;
        this.count = newcount;
      }
    }

    @Override
    public void write(char c[], int off, int len) throws IOException {
      if ((off < 0) || (off > c.length) || (len < 0) || ((off + len) > c.length) || ((off + len) < 0)) throw new IndexOutOfBoundsException();
      else if (len == 0) return;
      synchronized (this.lock) {
        int newcount = this.count + len;
        if (newcount > FILE_THRESHOLD) {
          flushToFile();
          newcount = len;
        } else if (newcount > this.buffer.length) {
          this.buffer = Arrays.copyOf(this.buffer, Math.max(this.buffer.length << 1, newcount));
        }
        System.arraycopy(c, off, this.buffer, this.count, len);
        this.count = newcount;
      }
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
      synchronized (this.lock) {
        int newcount = this.count + len;
        if (newcount > FILE_THRESHOLD) {
          flushToFile();
          newcount = len;
        } else if (newcount > this.buffer.length) {
          this.buffer = Arrays.copyOf(this.buffer, Math.max(this.buffer.length << 1, newcount));
        }
        str.getChars(off, off + len, this.buffer, this.count);
        this.count = newcount;
      }
    }

    @Override
    public AdaptiveWriter append(CharSequence csq) throws IOException {
      String s = (csq == null ? "null" : csq.toString());
      write(s, 0, s.length());
      return this;
    }

    @Override
    public AdaptiveWriter append(CharSequence csq, int start, int end) throws IOException {
      String s = (csq == null ? "null" : csq).subSequence(start, end).toString();
      write(s, 0, s.length());
      return this;
    }

    @Override
    public AdaptiveWriter append(char c) throws IOException {
      write(c);
      return this;
    }

    /**
     * Returns the current size of the buffer.
     *
     * @return an int representing the current size of the buffer.
     */
    public int size() {
      return this.count;
    }

    @Override
    public void flush() throws IOException {
      if (this.writer != null) {
        flushToFile();
        this.count = 0;
        this.writer.flush();
      }
    }

    @Override
    public void close() throws IOException {
      if (this.writer != null) {
        flushToFile();
        this.writer.close();
        this.writer = null;
      }
    }

    /**
     * Writes the contents of the buffer to another character stream.
     *
     * @param out the output stream to write to
     * @throws IOException If an I/O error occurs.
     */
    private void flushToFile() throws IOException {
      // Setup the temporary file
      if (this.file == null) {
        this.file = File.createTempFile("index_writer", ".ixml");
        this.writer = new OutputStreamWriter(new FileOutputStream(this.file), Charset.forName("utf-8"));
      }
      // flush the buffer to the file
      this.writer.write(this.buffer, 0, this.count);
      this.count = 0;
    }

  }

}

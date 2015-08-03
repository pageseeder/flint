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
package org.pageseeder.flint.content;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.List;

import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.api.Content;
import org.pageseeder.flint.api.ContentTranslator;

/**
 * A simple translator implementation allows XML Media types to be simply forwarded without translation.
 *
 * @author Jean-Baptiste Reure
 * @version 27 February 2013
 */
public final class SourceForwarder implements ContentTranslator {

  /**
   * The list of media types.
   */
  private final List<String> _mediaTypes;

  /**
   * The charset to use to decode byte streams.
   */
  private final Charset _charset;

  /**
   * Forwards data for one MIME type only, will return in any other case.
   *
   * @param mediaType The Media type that this translator can forward.
   * @param charset   The name of the charset to use for decoding byte streams.
   *
   * @throws IllegalCharsetNameException If the given charset name is illegal.
   * @throws IllegalArgumentException    If the given charset name or media type is <code>null</code>.
   * @throws UnsupportedCharsetException If no support for the named charset is available.
   */
  public SourceForwarder(String mediaType, String charset) throws IllegalArgumentException {
    this(mediaType, Charset.forName(charset));
  }

  /**
   * Forwards data for one MIME type only, will return in any other case.
   *
   * @param mediaTypes The list of Media types this translator should forward.
   * @param charset    The name of the charset to use for decoding byte streams.
   *
   * @throws IllegalCharsetNameException If the given charset name is illegal.
   * @throws IllegalArgumentException    If the given charset name or media type is <code>null</code>.
   * @throws UnsupportedCharsetException If no support for the named charset is available.
   */
  public SourceForwarder(List<String> mediaTypes, String charset) throws IllegalArgumentException {
    this(mediaTypes, Charset.forName(charset));
  }

  /**
   * Forwards data for one MIME type only, will return in any other case.
   *
   * @param mediaType The Media type that this translator can forward.
   * @param charset   The charset to use for decoding byte streams.
   *
   * @throws IllegalArgumentException    If the given charset name or media type is <code>null</code>.
   */
  public SourceForwarder(String mediaType, Charset charset) throws IllegalArgumentException {
    if (mediaType == null) throw new IllegalArgumentException("mediaType is null");
    if (charset == null) throw new IllegalArgumentException("charset is null");
    this._mediaTypes = Collections.singletonList(mediaType);
    this._charset = charset;
  }

  /**
   * Forwards data for one MIME type only, will return in any other case.
   *
   * @param mediaTypes The list of Media types this translator should forward.
   * @param charset    The charset to use for decoding byte streams.
   *
   * @throws IllegalArgumentException    If the given charset name or media type is <code>null</code>.
   */
  public SourceForwarder(List<String> mediaTypes, Charset charset) throws IllegalArgumentException {
    if (mediaTypes == null) throw new IllegalArgumentException("mediaTypes is null");
    if (charset == null) throw new IllegalArgumentException("charset is null");
    this._mediaTypes = mediaTypes;
    this._charset = charset;
  }

  /**
   * Returns a new {@link Reader} on the content to translate using the specified charset.
   *
   * @param content The content to translate.
   *
   * @return the reader or <code>null</code> if this forwarder does not support this Media type.
   *
   * @throws IndexException If thrown while trying to access the content methods.
   */
  @Override
  public Reader translate(Content content) throws IndexException {
    // Ignore deleted content
    if (content.isDeleted()) return null;
    // Check that Media type is supported
    if (!this._mediaTypes.contains(content.getMediaType())) return null;
    // Return a new reader
    return new InputStreamReader(content.getSource(), this._charset);
  }

}

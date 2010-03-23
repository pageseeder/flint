/*
 * This file is part of the Flint library.
 * 
 * For licensing information please see the file license.txt included in the release. A copy of this licence can also be
 * found at http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.index;

/**
 * The type of Field of a Lucene indexed document.
 *
 * <p>An enumerated type.
 *
 * @version 9 August 2004
 * @author  Christophe Lauret
 */
public final class FieldType {

  // TODO: use enum

  /**
   * Used for Lucene keywords.
   *
   * <p>Keywords are not tokenised by the indexed and are stored as is
   * by the indexer. The value of this type of field cannot be searched 
   * using full-text, but a search based on the exact value of this field
   * can be performed.
   *
   * <p>A <code>KEYWORD</code> field is appropriate for an enumerated type of 
   * values, such as mime type, type of document, etc...
   */
  public static final FieldType KEYWORD = new FieldType("keyword");

  /**
   * Used for Lucene unindexed fields.
   *
   * <p>Unindexed fields are stored in the index repository, but they
   * are not tokenised into words. The value can retrieved for the documents
   * in the results, but cannot be searched.
   *
   * <p>An <code>UNINDEXED</code> field is appropriate for values associated to 
   * the document but that need not be searched, such as URLs, file size, etc...
   */
  public static final FieldType UNINDEXED = new FieldType("unindexed");

  /**
   * Used for Lucene text fields.
   *
   * <p>Text fields are tokenised into words, and each word is added to the
   * index, the complete value of this field is also stored in the index
   * ad can be retrieved in the results.
   *
   * <p>A <code>TEXT</code> field is appropriate for paragraphs or short texts that
   * need to be searched and retrieved in the results, such as titles, abstracts, etc...
   *
   * <p>Note: Indexing long text as this type of field, can result in big a index,
   * unless the entire value of this field is required it is better to use the
   * <code>UNSTORED</code> field which does not take as much space.
   */
  public static final FieldType TEXT = new FieldType("text");

  /**
   * Used for Lucene text fields that aren't stored.
   *
   * <p>Unstored fields are tokenised into words, and each word is added to
   * the index, but the value of the field itself is not stored and so cannot
   * be retrieved with the results.
   *
   * <p>An <code>UNSTORED</code> field is appropriate for text that need to
   * be indexed and full-text searched.
   */
  public static final FieldType UNSTORED = new FieldType("unstored");

  /**
   * Specific PageSeeder field which is not added to the index
   */
  public static final FieldType SYSTEM = new FieldType("system");

  /**
   * The string name of the FieldType
   */
  public final String type;

  /**
   * Protect constructor, prevent creation of other instances.
   *
   * @param type The type for field.
   */
  private FieldType(String type) {
    this.type = type;
  }

  /**
   * Parse a type of field and return the correponding constant.
   *
   * <p>This method returns:
   * <ul>
   *   <li><code>UNINDEXED</code> if name is equal to "unindexed" in any case.</li>
   *   <li><code>TEXT</code> if name is equal to "text" or "stored" in any case.</li>
   *   <li><code>UNSTORED</code> if name is equal to "unstored" in any case.</li>
   *   <li><code>KEYWORD</code> for any other value.</li>
   * </ul>
   *
   * @param name The name of the field type.
   *
   * @return the corresponding constant.
   */
  public static FieldType parse(String name) {
    if (name == null)                            return KEYWORD;
    else if ("".equals(name))                    return KEYWORD;
    else if ("keyword".equalsIgnoreCase(name))   return KEYWORD;
    else if ("unindexed".equalsIgnoreCase(name)) return UNINDEXED;
    else if ("text".equalsIgnoreCase(name))      return TEXT;
    else if ("stored".equalsIgnoreCase(name))      return TEXT;
    else if ("unstored".equalsIgnoreCase(name))  return UNSTORED;
    else if ("system".equalsIgnoreCase(name))  return SYSTEM;
    else return KEYWORD;
  }

  /**
   * Return the String representation of this object.
   *
   * @return "keyword", "text", "unindexed" or "unstored".
   */
  public String toString() {
    return this.type;
  }

}
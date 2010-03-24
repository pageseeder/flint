/*
 * This file is part of the Flint library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.flint.legacy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TopDocsCollector;

/**
 * An implementation of TopDocCollector. This is used by NPS to uniquely display drug based on brand
 * name and active ingredients (substance).
 * 
 * @author William Liem (Allette Systems)
 * @author Christophe Lauret (Weborganic)
 * 
 * @version 15 October 2009
 * @deprecated
 */
public class GroupingHitCollector extends TopDocsCollector {

  /**
   * The index reader to be used to read the document.
   */
  protected IndexReader reader;

  /**
   * The TreeMap to store key value based on the collection. In NPS treemap, brand name + substance
   * is the key. TreeMap is used for the purpose to sort the Tree alphabetically by the key.
   */
  protected TreeMap hash;

  /**
   * The default unsorted document ScoreDocs output.
   */
  protected List<ScoreDoc> topdocs;

  /**
   * Public constructor for GroupingHitCollector.
   */
  public GroupingHitCollector(int hitsperpage) {
    super(null);
    hash = new TreeMap();
    topdocs = new ArrayList<ScoreDoc>();
  }

  /**
   * Setting the IndexReader.
   * 
   * @param reader The reader for the index.
   */
  public void setReader(IndexReader reader) {
    this.reader = reader;
  }

  /**
   * Returns the default ScoreDocs from collection.
   * 
   * @return The scoreDocs for this class.
   */
  public ScoreDoc[] getScoreDocs() {
    return (ScoreDoc[]) topdocs.toArray(new ScoreDoc[topdocs.size()]);
  }

  /**
   * Returns the sorted ScoreDocs by the TreeMap key alphabetically.
   * 
   * @return The sorted scoreDocs for this class.
   */
  public ScoreDoc[] getSortedKeyScoreDocs() {
    ArrayList scoredocs = new ArrayList();
    Set s = hash.keySet();
    for (Iterator h = s.iterator(); h.hasNext();) {
      String key = (String) h.next();
      scoredocs.add(hash.get(key));
    }
    return (ScoreDoc[]) scoredocs.toArray(new ScoreDoc[topdocs.size()]);
  }
  
  @Override
  public boolean acceptsDocsOutOfOrder() {
    // TODO Auto-generated method stub
    return false;
  }
  
  @Override
  public void collect(int arg0) throws IOException {
    // TODO Auto-generated method stub
    
  }
  
  @Override
  public void setNextReader(IndexReader arg0, int arg1) throws IOException {
    // TODO Auto-generated method stub
    
  }
  
  @Override
  public void setScorer(Scorer arg0) throws IOException {
    // TODO Auto-generated method stub
    
  }

  /**
   * Overwrite collect function to have unique ScoreDocs.
   */
  public void collect(int doc, float score) {
    try {
      subcollect(doc, score);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Collection Sub function to include throw Exception error.
   * 
   * @throws IOException If an I/O error occurs whilst reading the index.
   * @throws Exception For any other exception.
   */
  public void subcollect(int doc, float score) throws IOException, Exception {
    // FIXME: this is project specific!!!
    try {
      Document d = this.reader.document(doc);
      if (d != null) {
        String key = d.getField("brand").stringValue() + "  "
            + d.getField("substance").stringValue();
        if (!hash.containsKey(key)) {
          hash.put(key, new ScoreDoc(doc, score));
          topdocs.add(new ScoreDoc(doc, score));
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
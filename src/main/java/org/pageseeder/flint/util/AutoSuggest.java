package org.pageseeder.flint.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.suggest.Lookup.LookupResult;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.api.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoSuggest {

  private final static Logger LOGGER = LoggerFactory.getLogger(AutoSuggest.class);
  
  private AnalyzingInfixSuggester suggester;

  private final ObjectBuilder _objectBuilder;

  private final Index _index;

  private final boolean _useTerms;
  
  private final List<String> _searchFields = new ArrayList<>();

  private String _withField = null;

  private long lastBuilt = -1;
  
  private AutoSuggest(Index index, Directory dir, boolean useTerms, ObjectBuilder objectBuilder) throws IndexException {
    this._index = index;
    this._useTerms = useTerms;
    this._objectBuilder = objectBuilder;
    try {
      this.suggester = new AnalyzingInfixSuggester(dir, index.getAnalyzer());
    } catch (IOException ex) {
      LOGGER.error("Failed to build autosuggest", ex);
      throw new IndexException("Failed to build autosuggest", ex);
    }
  }

  public List<String> getSearchFields() {
    return this._searchFields;
  }

  public void addSearchField(String field) {
    this._searchFields.add(field);
  }

  public void addSearchFields(Collection<String> fields) {
    this._searchFields.addAll(fields);
  }

  public void setCriteriaField(String field) {
    if (this._useTerms) throw new IllegalStateException("Illogical to use criteria for words suggestions!");
    this._withField = field;
  }

  public long getLastBuilt() {
    return this.lastBuilt;
  }

  public boolean isCurrent(IndexManager mgr) {
    return mgr.getLastTimeUsed(this._index) < this.lastBuilt;
  }

  public void build(IndexReader reader) throws IndexException {
    try {
      if (this._useTerms) {
        for (String field : this._searchFields) {
          org.apache.lucene.index.Terms terms = MultiFields.getTerms(reader, field);
          if (terms == null) continue;
          TermsEnum termsEnum = terms.iterator();
          BytesRef text;
          while ((text = termsEnum.next()) != null) {
            this.suggester.add(text, null, 1, text);
          }
        }
      } else {
        int max = reader.numDocs();
        for (int i = 0; i < max; i++) {
          Document doc = reader.document(i);
          // load criteria values
          Set<BytesRef> contexts = null;
          if (this._withField != null) {
            String[] with = doc.getValues(this._withField);
            if (with != null) {
              contexts = new HashSet<>();
              for (String w : with) {
                contexts.add(new BytesRef(w));
              }
            }
          }
          // create payload
          BytesRef payload = null;
          if (this._objectBuilder != null) {
            byte[] serialized = serialize(this._objectBuilder.documentToObject(doc));
            if (serialized != null) payload = new BytesRef(serialized);
          }
          for (String field : this._searchFields) {
            String[] texts = doc.getValues(field);
            if (texts != null) {
              for (String text : texts) {
                BytesRef bytes = new BytesRef(text);
                this.suggester.add(bytes, contexts, 1, payload);
              }
            }
          }
        }
      }
      this.suggester.refresh();
      this.lastBuilt = System.currentTimeMillis();
    } catch (IOException ex) {
      LOGGER.error("Failed to build autosuggest dictionary", ex);
    }
  }

  private byte[] serialize(Serializable object) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutputStream out = new ObjectOutputStream(bos);
    out.writeObject(object);
    out.close();
    return bos.toByteArray();
  }

  private Serializable deserialize(byte[] bytes) throws IOException {
    ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
    ObjectInputStream in = new ObjectInputStream(bis);
    try {
      return (Serializable) in.readObject();
    } catch (ClassNotFoundException ex) {
      throw new IOException("Class not found when deserializing", ex);
    }
  }
  
  public List<Suggestion> suggest(String text) {
    return suggest(text, 10);
  }

  public List<Suggestion> suggest(String text, int nb) {
    return suggest(text, (Collection<String>) null, nb);
  }

  public List<Suggestion> suggest(String text, String with, int nb) {
    return suggest(text, Collections.singleton(with), nb);
  }

  public List<Suggestion> suggest(String text, Collection<String> criteria, int nb) {
    List<Suggestion> suggestions = new ArrayList<>();
    if (this.suggester == null) return suggestions;
    Set<BytesRef> contexts = null;
    if (criteria != null) {
      if (this._useTerms)
        throw new IllegalStateException("Illogical to use criteria for words suggestions!");
      contexts = new HashSet<>();
      for (String with : criteria) {
        contexts.add(new BytesRef(with));
      }
    }
    List<LookupResult> results = null;
    try {
      results = this.suggester.lookup(text, contexts, false, nb);
    } catch (IOException ex) {
      LOGGER.error("Failed to lookup autosuggest suggestions", ex);
    }
    if (results != null) {
      for (LookupResult result : results) {
        Suggestion suggestion = new Suggestion();
        suggestion.text = result.key.toString();
        suggestion.highlight = result.highlightKey.toString();
        if (this._objectBuilder != null && result.payload != null) {
          try {
            suggestion.object = deserialize(result.payload.bytes);
          } catch (IOException ex) {
            LOGGER.error("Failed to deserialize object", ex);
          }
        }
        if (!suggestions.contains(suggestion))
          suggestions.add(suggestion);
      }
    }
    return suggestions;
  }

  public void close() {
    try {
      this.suggester.close();
    } catch (IOException ex) {
      LOGGER.error("Failed to close autosuggestor", ex);
    }
  }
  // --------------------------------------------------------------------------------------
  // static business
  // --------------------------------------------------------------------------------------

  public static AutoSuggest terms(Index index) throws IndexException {
    return terms(index, new RAMDirectory());
  }

  public static AutoSuggest terms(Index index, Directory dir) throws IndexException {
    return new AutoSuggest(index, dir, true, null);
  }

  public static AutoSuggest fields(Index index) throws IndexException {
    return fields(index, new RAMDirectory());
  }
  
  public static AutoSuggest fields(Index index, Directory dir) throws IndexException {
    return new AutoSuggest(index, dir, false, null);
  }
  
  public static AutoSuggest documents(Index index, ObjectBuilder builder) throws IndexException {
    return documents(index, new RAMDirectory(), builder);
  }
  
  public static AutoSuggest documents(Index index, Directory dir, ObjectBuilder builder) throws IndexException {
    return new AutoSuggest(index, dir, false, builder);
  }

  public static abstract class ObjectBuilder {
    public abstract Serializable documentToObject(Document document);
  };

  public static class Suggestion {
    public String text;
    public String highlight;
    public Serializable object;
    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Suggestion)) return false;
      Suggestion s = (Suggestion) obj;
      return this.text.equals(s.text) &&
             this.highlight.equals(s.highlight) && 
             ((this.object == null && s.object == null) || this.object.equals(s.object));
    }
    @Override
    public int hashCode() {
      return this.text.hashCode() * 3 +
             this.highlight.hashCode() * 11 +
             (this.object != null ? 17 * this.object.hashCode() : 0);
    }
  }
}

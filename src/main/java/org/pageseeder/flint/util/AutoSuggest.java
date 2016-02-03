package org.pageseeder.flint.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
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

  private final List<String> _resultFields = new ArrayList<>();

  private final Index _index;

  private final boolean _useTerms;
  
  private final List<String> _searchFields = new ArrayList<>();

  private String _withField = null;

  private long lastBuilt = -1;
  
  private AutoSuggest(Index index, Directory dir, Analyzer analyzer, boolean useTerms, int minChars) throws IndexException {
    this._index = index;
    this._useTerms = useTerms;
    try {
      this.suggester = new AnalyzingInfixSuggester(dir, index.getAnalyzer(), analyzer, minChars, false, true, true);
    } catch (IOException ex) {
      LOGGER.error("Failed to build autosuggest", ex);
      throw new IndexException("Failed to build autosuggest", ex);
    }
  }

  public List<String> getSearchFields() {
    return this._searchFields;
  }

  public void addSearchField(String field) {
    if (field != null) this._searchFields.add(field);
  }

  public void addSearchFields(Collection<String> fields) {
    if (fields != null) this._searchFields.addAll(fields);
  }

  public void addResultField(String field) {
    if (field != null) this._resultFields.add(field);
  }

  public void addResultFields(Collection<String> fields) {
    if (fields != null) this._resultFields.addAll(fields);
  }

  public void setCriteriaField(String field) {
    if (this._useTerms && field != null)
      throw new IllegalStateException("Illogical to use criteria for words suggestions!");
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
      boolean buildit = false;
      if (this._useTerms) {
        for (String field : this._searchFields) {
          org.apache.lucene.index.Terms terms = MultiFields.getTerms(reader, field);
          if (terms == null) continue;
          TermsEnum termsEnum = terms.iterator();
          BytesRef text;
          while ((text = termsEnum.next()) != null) {
            this.suggester.add(text, null, 1, null);
            buildit = true;
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
          byte[] serialized = serialize(this._resultFields, doc);
          BytesRef payload = serialized == null ? null : new BytesRef(serialized);
          for (String field : this._searchFields) {
            String[] texts = doc.getValues(field);
            if (texts != null) {
              for (String text : texts) {
                BytesRef bytes = new BytesRef(text);
                this.suggester.add(bytes, contexts, 1, payload);
                buildit = true;
              }
            }
          }
        }
      }
      if (buildit) {
        this.suggester.refresh();
        this.lastBuilt = System.currentTimeMillis();
      }
    } catch (IOException | IllegalStateException ex) {
      LOGGER.error("Failed to build autosuggest dictionary", ex);
    }
  }

  private static byte[] serialize(Collection<String> fields, Document doc) throws IOException {
    if (fields.isEmpty()) return null;
    // build map
    Map<String, String[]> result = new HashMap<>();
    for (String field : fields) {
      String[] values = doc.getValues(field);
      if (values != null) result.put(field, values);
    }
    // serialize it
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutputStream out = new ObjectOutputStream(bos);
    out.writeObject(result);
    out.close();
    return bos.toByteArray();
  }

  @SuppressWarnings("unchecked")
  private Map<String, String[]> deserialize(byte[] bytes) throws IOException {
    ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
    ObjectInputStream in = new ObjectInputStream(bis);
    try {
      return (Map<String, String[]>) in.readObject();
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
    if (this.lastBuilt == -1) {
      LOGGER.warn("Loading suggestions with empty suggester!");
      return suggestions;
    }
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
        if (result.payload != null) {
          try {
            suggestion.document = deserialize(result.payload.bytes);
          } catch (IOException ex) {
            LOGGER.error("Failed to deserialize suggestion payload", ex);
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

  public static class Builder {
    private Boolean _terms = null;
    private Index _index = null;
    private Directory _dir = null;
    private Analyzer _analyzer = null;
    private String _criteria = null;
    private int _minChars = 2;
    private Collection<String> _searchFields = new ArrayList<>();
    private Collection<String> _resultFields = new ArrayList<>();
    public Builder index(Index index) {
      this._index = index;
      return this;
    }
    public Builder analyzer(Analyzer analyzer) {
      this._analyzer = analyzer;
      return this;
    }
    public Builder useTerms(boolean terms) {
      this._terms = Boolean.valueOf(terms);
      return this;
    }
    public Builder directory(Directory dir) {
      this._dir = dir;
      return this;
    }
    public Builder minChars(int minChars) {
      this._minChars = minChars;
      return this;
    }
    public Builder searchFields(Collection<String> searchFields) {
      this._searchFields = searchFields;
      return this;
    }
    public Builder resultFields(Collection<String> resultFields) {
      this._resultFields = resultFields;
      return this;
    }
    public Builder criteria(String criteria) {
      this._criteria = criteria;
      return this;
    }
    public AutoSuggest build() throws IndexException {
      if (this._terms == null) throw new IllegalStateException("missing terms");
      if (this._index == null) throw new IllegalStateException("missing index");
      Directory dir = this._dir == null ? new RAMDirectory() : this._dir;
      Analyzer analyzer = this._analyzer == null ? new StandardAnalyzer(CharArraySet.EMPTY_SET) : this._analyzer;
      AutoSuggest as = new AutoSuggest(this._index, dir, analyzer, this._terms.booleanValue(), this._minChars);
      as.setCriteriaField(this._criteria);
      as.addSearchFields(this._searchFields);
      as.addResultFields(this._resultFields);
      return as;
    }
  }

  public static class Suggestion {
    public String text;
    public String highlight;
    public Map<String, String[]> document;
    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Suggestion)) return false;
      Suggestion s = (Suggestion) obj;
      return this.text.equals(s.text) &&
             this.highlight.equals(s.highlight) && 
             ((this.document == null && s.document == null) || this.document.equals(s.document));
    }
    @Override
    public int hashCode() {
      return this.text.hashCode() * 3 +
             this.highlight.hashCode() * 11 +
             (this.document != null ? 17 * this.document.hashCode() : 0);
    }
  }
}

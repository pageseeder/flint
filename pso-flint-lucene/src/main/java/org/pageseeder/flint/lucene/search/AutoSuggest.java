package org.pageseeder.flint.lucene.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.suggest.Lookup.LookupResult;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.store.ByteArrayDataInput;
import org.apache.lucene.store.ByteBuffersDataOutput;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.pageseeder.flint.Index;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

public class AutoSuggest {

  private final static Logger LOGGER = LoggerFactory.getLogger(AutoSuggest.class);

  private final AnalyzingInfixSuggester suggester;

  private final List<String> _resultFields = new ArrayList<>();

  private final String _name;

  private final Index _index;

  private final boolean _useTerms;

  private final List<String> _searchFields = new ArrayList<>();

  //TODO maybe it should be a list
  private String _withField = null;

  private final Map<String, Float> _weights = new HashMap<>();

  private long lastBuilt = -1;

  /**
   * Version of the index reader used for the last build, or -1 if never built.
   *
   * <p>Used instead of timestamps to detect a changed index. An NRT reader's
   * version only changes when the index is refreshed, so it is not affected by
   * the gap between a write and the next commit.
   */
  private volatile long builtVersion = -1;

  private AutoSuggest(String name, Index index, Directory dir, Analyzer indexAnalyzer, Analyzer searchAnalyzer, boolean useTerms, int minChars) throws IndexException {
    this._name = name;
    this._index = index;
    this._useTerms = useTerms;
    try {
      this.suggester = new AnalyzingInfixSuggester(dir, indexAnalyzer, searchAnalyzer, minChars, true, true, true);
    } catch (IOException ex) {
      LOGGER.error("Failed to build autosuggest {}", this._name, ex);
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

  public void setWeight(String field, float weight) {
    this._weights.put(field, weight);
  }

  /**
   * Comma separated list of weights:
   *  level:2,price:10,number:0.5
   * @param weights weights as string
   */
  public void setWeights(String weights) {
    if (weights == null) return;
    for (String weight: weights.split(",")) {
      String[] parts = weight.split(":");
      if (parts.length == 2) {
        try {
          this._weights.put(parts[0], Float.valueOf(parts[1]));
        } catch (NumberFormatException ex) {
          LOGGER.error("Ignoring invalid autosuggest {} weight for field {}: not a number! ({})", this._name, parts[0], parts[1]);
        }
      }
    }
  }

  public long getLastBuilt() {
    return this.lastBuilt;
  }

  /**
   * @deprecated use the method isCurrentFor
   * @return true if current
   */
  public boolean isCurrent() {
    return this._index.getIndexIO().getLastTimeUsed() < this.lastBuilt;
  }

  /**
   * @deprecated use the method with no parameters
   * @param mgr the manager not used
   * @return true if current
   */
  public boolean isCurrent(IndexManager mgr) {
    return this.isCurrent();
  }

  /**
   * Indicates whether this suggester was built from the same index state as the
   * given reader.
   *
   * <p>Unlike {@link #isCurrent()}, which compares timestamps, this compares the
   * {@link DirectoryReader#getVersion() version} of the reader used for the last
   * build with the version of the given reader. It returns <code>false</code> if
   * the suggester was never built or if the reader is not a
   * {@link DirectoryReader}.
   *
   * @param reader the reader representing the current state of the index
   *
   * @return <code>true</code> if the index has not changed since the last build
   */
  public boolean isCurrentFor(IndexReader reader) {
    return reader instanceof DirectoryReader
        && this.builtVersion == ((DirectoryReader) reader).getVersion();
  }

  public void build(IndexReader reader) {
    // can't search while we're building it
    synchronized (this.suggester) {
      try {
        boolean buildit = false;
        if (this._useTerms) {
          for (String field : this._searchFields) {
            org.apache.lucene.index.Terms terms = MultiTerms.getTerms(reader, field);
            if (terms == null) continue;
            TermsEnum termsEnum = terms.iterator();
            BytesRef text;
            while ((text = termsEnum.next()) != null) {
              this.suggester.add(text, null, 1, null);
              buildit = true;
            }
          }
        } else {
          Set<String> fieldsToLoad = new HashSet<>();
          fieldsToLoad.addAll(this._resultFields);
          fieldsToLoad.addAll(this._searchFields);
          fieldsToLoad.addAll(this._weights.keySet());
          if (this._withField != null) fieldsToLoad.add(this._withField);
          for (LeafReaderContext ctxt : reader.leaves()) {
            if (addEntries(ctxt, fieldsToLoad)) {
              buildit = true;
            }
          }
        }
        if (buildit) {
          this.suggester.refresh();
          this.lastBuilt = System.currentTimeMillis();
        }

        // Record the version even if nothing was added.
        // Otherwise, an empty index is seen as "never built" and is rebuilt on every request.
        if (reader instanceof DirectoryReader) {
          this.builtVersion = ((DirectoryReader) reader).getVersion();
        }
      } catch (IOException | IllegalStateException ex) {
        LOGGER.error("Failed to build dictionary for autosuggest {}", this._name, ex);
      }
    }
  }

  /**
   * Add entries to the suggester
   *
   * @param context    where to read the documents from
   * @param fieldsToLoad the fields to add
   *
   * @return <code>true</code> if something was added
   *
   * @throws IOException if reading/adding entries failed
   */
  private boolean addEntries(LeafReaderContext context, Set<String> fieldsToLoad) throws IOException {
    boolean buildit = false;
    // check for leaves
    // We do not use try-with-resources on the subReader here because it is a child of the parent IndexReader (the
    // 'reader' parameter in build()).
    // Closing a leaf reader while the parent is still active invalidates the entire reader hierarchy, which would
    // trigger an 'AlreadyClosedException'.
    // The parent DirectoryReader is responsible for the lifecycle and closure of all its leaf segments.
    LeafReader subReader = context.reader();
    List<LeafReaderContext> leaves = subReader.leaves();
    if (leaves != null && !leaves.isEmpty()) {
      if (leaves.size() > 1 || leaves.get(0) != context) {
        for (LeafReaderContext ctxt : leaves) {
          if (addEntries(ctxt, fieldsToLoad)) {
            buildit = true;
          }
        }
        return buildit;
      }
    }
    // go through our docs then
    Bits live = subReader.getLiveDocs();
    for (int i = 0; i < subReader.maxDoc(); i++) {
      if (live != null && !live.get(i)) continue;
      Document doc = subReader.storedFields().document(i, fieldsToLoad);
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
      // find doc weight
      float weightF = 0;
      for (Entry<String, Float> aweight : this._weights.entrySet()) {
        String val = doc.get(aweight.getKey());
        try {
          // default value is 1 if missing
          weightF += aweight.getValue() * (val == null ? 1 : Float.parseFloat(val));
        } catch (NumberFormatException ex) {
          LOGGER.error("Failed to compute weight as field {} is not a number! ({})", aweight.getKey(), val);
        }
      }
      // mutiply by 100 to turn to long (2 decimal precision)
      long weight = weightF == 0 ? 100 : (long) (weightF * 100);
      // create payload
      // compact payload instead of Java serialization (see encode)
      BytesRef payload = encode(this._resultFields, doc);
      for (String field : this._searchFields) {
        String[] texts = doc.getValues(field);
        if (texts != null) {
          for (String text : texts) {
            try {
              this.suggester.add(new BytesRef(text), contexts, weight, payload);
            } catch (Exception ex) {
              LOGGER.error("Failed to add text for field {} to autosuggest {}", field, this._name);
            }
            buildit = true;
          }
        } else {
          LOGGER.error("Failed to load values for field {} in autosuggest {}", field, this._name);
        }
      }
    }
    return buildit;
  }

  /**
   * Encodes the given fields of a document as the payload of a suggestion.
   *
   * <p>Format: VInt field count, then for each field: the name, a VInt value
   * count, and the values (all as Lucene strings). This replaces Java
   * serialization, which allocated several objects per document and wrote class
   * descriptors into every payload.
   *
   * @param fields the fields to include
   * @param doc    the document to read the values from
   *
   * @return the payload, or <code>null</code> if there are no fields
   *
   * @throws IOException if writing the payload failed
   */
  private static BytesRef encode(Collection<String> fields, Document doc) throws IOException {
    if (fields.isEmpty()) return null;
    ByteBuffersDataOutput out = new ByteBuffersDataOutput();
    out.writeVInt(fields.size());
    for (String f : fields) {
      String[] vals = doc.getValues(f);
      out.writeString(f);
      out.writeVInt(vals.length);
      for (String v : vals) out.writeString(v);
    }
    return new BytesRef(out.toArrayCopy());
  }

  /**
   * Decodes a payload written by {@link #encode(Collection, Document)}.
   *
   * <p>Honours the offset and length of the {@link BytesRef}. The previous
   * implementation used the whole backing array, which is wrong when the
   * reference is a slice of a larger array.
   *
   * @param ref the payload
   *
   * @return the field values by field name
   */
  private static Map<String, String[]> decode(BytesRef ref) throws IOException {
    ByteArrayDataInput in = new ByteArrayDataInput(ref.bytes, ref.offset, ref.length);
    Map<String, String[]> map = new HashMap<>();
    for (int n = in.readVInt(); n > 0; n--) {
      String f = in.readString();
      String[] vals = new String[in.readVInt()];
      for (int k = 0; k < vals.length; k++) vals[k] = in.readString();
      map.put(f, vals);
    }
    return map;
  }

  public List<Suggestion> suggest(String text) {
    return suggest(text, 10);
  }

  public List<Suggestion> suggest(String text, int nb) {
    return suggest(text, (Collection<String>) null, nb);
  }

  public List<Suggestion> suggest(String text, String with, int nb) {
    return suggest(text, with == null ? null : Collections.singleton(with), nb);
  }

  public List<Suggestion> suggest(String text, Collection<String> criteria, int nb) {
    List<Suggestion> suggestions = new ArrayList<>();
    if (this.lastBuilt == -1) {
      LOGGER.warn("Loading suggestions with empty suggester for autosuggest {}!", this._name);
      return suggestions;
    }
    if (this.suggester == null) return suggestions;
    Set<BytesRef> contexts = null;
    if (criteria != null && !criteria.isEmpty()) {
      if (this._useTerms)
        throw new IllegalStateException("Illogical to use criteria for words suggestions!");
      contexts = new HashSet<>();
      for (String with : criteria) {
        contexts.add(new BytesRef(with));
      }
    }
    List<LookupResult> results = null;
    synchronized (this.suggester) {
      try {
        results = this.suggester.lookup(text, contexts, false, nb);
      } catch (IOException ex) {
        LOGGER.error("Failed to lookup suggestions for autosuggest {}", this._name, ex);
      }
    }
    if (results != null) {
      for (LookupResult result : results) {
        Suggestion suggestion = new Suggestion();
        suggestion.text = result.key.toString();
        suggestion.highlight = result.highlightKey.toString();
        suggestion.weight = result.value;
        if (result.payload != null) {
          try {
            //decode honours offset/length (see decode)
            suggestion.document = decode(result.payload);
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
      LOGGER.error("Failed to close autosuggest {}", this._name, ex);
    }
  }
  // --------------------------------------------------------------------------------------
  // static business
  // --------------------------------------------------------------------------------------

  public static class Builder {
    private Boolean _terms = null;
    private String _name = null;
    private Index _index = null;
    private Directory _dir = null;
    private Analyzer _indexAnalyzer = null;
    private Analyzer _searchAnalyzer = null;
    //TODO it should be a list
    private String _criteria = null;
    private final Map<String, Float> _weights = new HashMap<>();
    private int _minChars = 2;
    private Collection<String> _searchFields = new ArrayList<>();
    private Collection<String> _resultFields = new ArrayList<>();
    public Builder index(Index index) {
      this._index = index;
      return this;
    }
    public Builder indexAnalyzer(Analyzer analyzer) {
      this._indexAnalyzer = analyzer;
      return this;
    }
    public Builder searchAnalyzer(Analyzer analyzer) {
      this._searchAnalyzer = analyzer;
      return this;
    }
    public Builder name(String name) {
      this._name = name;
      return this;
    }
    public Builder useTerms(boolean terms) {
      this._terms = terms;
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
    public Builder weights(Map<String, Float> weights) {
      if (weights != null) this._weights.putAll(weights);
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
      if (this._name  == null) throw new IllegalStateException("missing name");
      if (this._index == null) throw new IllegalStateException("missing index");
      Directory dir = this._dir == null ? new ByteBuffersDirectory() : this._dir;
      Analyzer indexAnalyzer  = this._indexAnalyzer  == null ? new StandardAnalyzer(CharArraySet.EMPTY_SET) : this._indexAnalyzer;
      Analyzer searchAnalyzer = this._searchAnalyzer == null ? new StandardAnalyzer(CharArraySet.EMPTY_SET) : this._searchAnalyzer;
      AutoSuggest as = new AutoSuggest(this._name, this._index, dir, indexAnalyzer, searchAnalyzer, this._terms, this._minChars);
      as.setCriteriaField(this._criteria);
      as.addSearchFields(this._searchFields);
      as.addResultFields(this._resultFields);
      for (Entry<String, Float> w : this._weights.entrySet())
        as.setWeight(w.getKey(), w.getValue());
      return as;
    }
  }

  public static class Suggestion {
    public String text;
    public String highlight;
    public Map<String, String[]> document;
    public long weight;
    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Suggestion)) return false;
      Suggestion s = (Suggestion) obj;
      return this.text.equals(s.text) &&
             this.highlight.equals(s.highlight) &&
             ((this.document == null && s.document == null) || (this.document!= null && this.document.equals(s.document)));
    }
    @Override
    public int hashCode() {
      return this.text.hashCode() * 3 +
             this.highlight.hashCode() * 11 +
             (this.document != null ? 17 * this.document.hashCode() : 0);
    }
    @Override
    public String toString() {
      return this.text + (weight != 100 ? "("+weight+")" : "");
    }
  }
}

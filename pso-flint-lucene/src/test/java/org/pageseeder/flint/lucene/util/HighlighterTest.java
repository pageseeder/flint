package org.pageseeder.flint.lucene.util;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.TermQuery;
import org.junit.Assert;
import org.junit.Test;

public class HighlighterTest {

  private static final String lipsum =
      "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. " +
      "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
      "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. " +
      "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum";


  @Test
  public void testHighlightSingleTerm() {

    TermQuery query = new TermQuery(new Term("content", "ipsum"));
    Highlighter highlighter = new Highlighter(query, new StandardAnalyzer());
    Assert.assertEquals("Lorem <term>ipsum</term> dolor", highlighter.highlight("content", lipsum, 20));
    Assert.assertEquals("Lorem <term>ipsum</term> dolor sit", highlighter.highlight("content", lipsum, 22));
    Assert.assertEquals("<term>ipsum</term>", highlighter.highlight("content", lipsum, 8));
    Assert.assertEquals("Lorem <term>ipsum</term>", highlighter.highlight("content", lipsum, 12));

    query = new TermQuery(new Term("content", "lorem"));
    highlighter = new Highlighter(query, new StandardAnalyzer());
    Assert.assertEquals("<term>Lorem</term> ipsum dolor", highlighter.highlight("content", lipsum, 20));
    Assert.assertEquals("<term>Lorem</term>", highlighter.highlight("content", lipsum, 5));
    Assert.assertEquals("<term>Lorem</term>", highlighter.highlight("content", lipsum, 2));

    query = new TermQuery(new Term("content", "laborum"));
    highlighter = new Highlighter(query, new StandardAnalyzer());
    Assert.assertEquals("mollit anim id est <term>laborum</term>", highlighter.highlight("content", lipsum, 20));
    Assert.assertEquals("est <term>laborum</term>", highlighter.highlight("content", lipsum, 2));
  }

  @Test
  public void testHighlightMultiTermsWithBoost() {

    BooleanQuery.Builder query = new BooleanQuery.Builder();
    query.add(new TermQuery(new Term("content", "ipsum")), BooleanClause.Occur.MUST);
    query.add(new BoostQuery(new TermQuery(new Term("content", "laboris")), 1.5f), BooleanClause.Occur.MUST);
    Highlighter highlighter = new Highlighter(query.build(), new StandardAnalyzer());
    Assert.assertEquals("ullamco <term>laboris</term> nisi ut aliquip ex", highlighter.highlight("content", lipsum, 35));

  }

  @Test
  public void testHighlightMultiTerms() {

    BooleanQuery.Builder query = new BooleanQuery.Builder();
    query.add(new TermQuery(new Term("title",   "laboris")),    BooleanClause.Occur.SHOULD);
    query.add(new TermQuery(new Term("filename", "voluptate")), BooleanClause.Occur.SHOULD);
    query.add(new TermQuery(new Term("content",  "ipsum")),     BooleanClause.Occur.SHOULD);
    Highlighter highlighter = new Highlighter(query.build(), new StandardAnalyzer());
    Assert.assertEquals("Lorem <term>ipsum</term> dolor", highlighter.highlight("content", lipsum, 20));
    Assert.assertEquals("in <term>voluptate</term> velit esse", highlighter.highlight("filename", lipsum, 20));
    Assert.assertEquals("ullamco <term>laboris</term> nisi ut aliquip ex", highlighter.highlight("title", lipsum, 35));
    Assert.assertNull(highlighter.highlight("id", lipsum, 20));

  }

  @Test
  public void testHighlightMarker() {

    BooleanQuery.Builder query = new BooleanQuery.Builder();
    query.add(new TermQuery(new Term("content", "ipsum")), BooleanClause.Occur.MUST);
    Highlighter highlighter = new Highlighter(query.build(), new StandardAnalyzer());
    highlighter.setMarkerTag("span");
    Assert.assertEquals("Lorem <span>ipsum</span> dolor", highlighter.highlight("content", lipsum, 20));
  }

  @Test
  public void testHighlightEscape() {

    String content = "test1 & test2 > test3 < test4";

    BooleanQuery.Builder query = new BooleanQuery.Builder();
    query.add(new TermQuery(new Term("content", "test2")), BooleanClause.Occur.MUST);
    Highlighter highlighter = new Highlighter(query.build(), new StandardAnalyzer());
    highlighter.setMarkerTag("m");
    highlighter.setEscape(false);
    Assert.assertEquals("test1 & <m>test2</m> > test3 < test4", highlighter.highlight("content", content, 20));

    highlighter.setEscape(true);
    Assert.assertEquals("test1 &amp; <m>test2</m> &gt; test3 &lt; test4", highlighter.highlight("content", content, 20));
  }
}

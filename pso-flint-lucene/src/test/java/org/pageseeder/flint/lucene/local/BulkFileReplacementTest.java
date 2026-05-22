package org.pageseeder.flint.lucene.local;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.pageseeder.flint.local.LocalIndexManager;
import org.pageseeder.flint.local.LocalIndexManagerFactory;
import org.pageseeder.flint.lucene.LuceneIndexQueries;
import org.pageseeder.flint.lucene.LuceneLocalIndex;
import org.pageseeder.flint.lucene.utils.TestListener;
import org.pageseeder.flint.lucene.utils.TestUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

public class BulkFileReplacementTest {

  private static final int FILE_COUNT = 500;

  private static final File template = new File("src/test/resources/template.xsl");
  private static final File contentRoot = new File("tmp/bulk-file-replacement/content");
  private static final File indexRoot = new File("tmp/bulk-file-replacement/index");

  private LuceneLocalIndex index;
  private LocalIndexManager manager;

  @Before
  public void setup() throws Exception {
    deleteRecursively(new File("tmp/bulk-file-replacement"));

    Assert.assertTrue(contentRoot.mkdirs());
    Assert.assertTrue(indexRoot.mkdirs());

    this.index = new LuceneLocalIndex(indexRoot, "bulk-file-replacement", new StandardAnalyzer(), contentRoot);
    this.index.setTemplate("xml", template.toURI());

    this.manager = LocalIndexManagerFactory.createMultiThreads(5, new TestListener());
  }

  @After
  public void tearDown() {
    if (this.manager != null) {
      this.manager.shutdown();
    }
    deleteRecursively(new File("tmp/bulk-file-replacement"));
  }

  @Test
  public void testManyFilesRemovedThenAddedAtOnce() throws Exception {
    createFiles("initial");

    this.manager.indexNewContent(this.index, contentRoot);
    waitForDocumentCount(FILE_COUNT, 30, TimeUnit.SECONDS);

    deleteFiles();

    for (int i = 0; i < FILE_COUNT; i++) {
      this.manager.indexFile(this.index, file(i), "Bulk delete");
    }

    createFiles("recreated");

    for (int i = 0; i < FILE_COUNT; i++) {
      this.manager.indexFile(this.index, file(i), "Bulk recreate");
    }

    waitForDocumentCount(FILE_COUNT, 60, TimeUnit.SECONDS);

    Assert.assertEquals(FILE_COUNT, getDocumentCount());
  }

  private void createFiles(String value) throws IOException {
    for (int i = 0; i < FILE_COUNT; i++) {
      writeFile(file(i), value + '-' + i);
    }
  }

  private void deleteFiles() throws IOException {
    for (int i = 0; i < FILE_COUNT; i++) {
      Files.deleteIfExists(file(i).toPath());
    }
  }

  private File file(int i) {
    return new File(contentRoot, String.format("file-%03d.xml", i));
  }

  private void writeFile(File file, String value) throws IOException {
    try (FileOutputStream out = new FileOutputStream(file)) {
      String xml = "<documents version=\"5.0\">\n" +
          "  <document>\n" +
          "    <field name=\"field1\">"+value+"-value1</field>\n" +
          "    <field name=\"field2\">"+value+"-value2</field>\n" +
          "  </document>\n" +
          "</documents>";
      out.write(xml.getBytes(StandardCharsets.UTF_8));
    }
  }

  private void waitForDocumentCount(int expected, long timeout, TimeUnit unit) throws Exception {
    long deadline = System.nanoTime() + unit.toNanos(timeout);
    int lastCount = -1;

    while (System.nanoTime() < deadline) {
      lastCount = getDocumentCount();

      if (lastCount == expected && !this.manager.isIndexing()) {
        return;
      }

      TestUtils.waitMs(250);
    }

    Assert.fail("Expected " + expected + " indexed documents but found " + lastCount);
  }

  private int getDocumentCount() throws IOException {
    IndexReader reader = null;
    try {
      reader = LuceneIndexQueries.grabReader(this.index);
      Assert.assertNotNull(reader);
      return reader.numDocs();
    } finally {
      if (reader != null) {
        LuceneIndexQueries.release(this.index, reader);
      }
    }
  }

  private static void deleteRecursively(File file) {
    if (file == null || !file.exists()) return;

    File[] children = file.listFiles();
    if (children != null) {
      for (File child : children) {
        deleteRecursively(child);
      }
    }

    if (!file.delete() && file.exists()) {
      throw new IllegalStateException("Failed to delete " + file);
    }
  }

}

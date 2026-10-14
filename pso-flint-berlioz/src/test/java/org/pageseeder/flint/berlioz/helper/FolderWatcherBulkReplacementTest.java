package org.pageseeder.flint.berlioz.helper;

import org.apache.lucene.index.IndexReader;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.flint.berlioz.model.FlintConfig;
import org.pageseeder.flint.berlioz.model.IndexMaster;
import org.pageseeder.flint.lucene.LuceneIndexQueries;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

public class FolderWatcherBulkReplacementTest {

  private static final int FILE_COUNT = 500;

  private static final File root = new File("tmp/folder-watcher-bulk-replacement");
  private static final File webInf = new File(root, "WEB-INF");
  private static final File indexRoot = new File(webInf, "index");
  private static final File configRoot = new File(webInf, "config");
  private static final File templateRoot = new File(webInf, "ixml");
  private static final File contentRoot = new File(webInf, "content");

  private FlintConfig config;

  @Before
  public void setup() throws Exception {
    deleteRecursively(root);

    Assert.assertTrue(webInf.mkdirs());
    Assert.assertTrue(indexRoot.mkdirs());
    Assert.assertTrue(configRoot.mkdirs());
    Assert.assertTrue(templateRoot.mkdirs());
    Assert.assertTrue(contentRoot.mkdirs());

    copyConfigs();

    GlobalSettings.setup(webInf);

    FlintConfig.setupFlintConfig(indexRoot, templateRoot);
    this.config = FlintConfig.get();

    Assert.assertNotNull(this.config.getMaster("default"));

    // Give the watcher a small amount of time to register the root folder.
    waitMs(1000);
  }

  @After
  public void tearDown() {
    if (this.config != null) {
      this.config.stop();
    }
//    deleteRecursively(root);
  }

  @Test
  public void testWatcherIndexesManyFilesRemovedThenAddedAtOnce() throws Exception {
    createFiles("initial");
    System.out.println("Created " + FILE_COUNT + " files");

    waitForDocumentCount(FILE_COUNT, 60, TimeUnit.SECONDS);

    deleteFiles();
    System.out.println("Deleted files");
    // small wait
    // waitMs(500);

    createFiles("recreated");
    System.out.println("Recreated files");

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

      if (lastCount == expected && this.config.getManager().getStatus().isEmpty()) {
        return;
      }

      waitMs(250);
    }

    Assert.fail("Expected " + expected + " indexed documents but found " + lastCount);
  }

  private int getDocumentCount() throws IOException {
    IndexReader reader = null;
    try {
      IndexMaster master = this.config.getMaster("default");
      Assert.assertNotNull(master);

      reader = LuceneIndexQueries.grabReader(master.getIndex());
      Assert.assertNotNull(reader);
      return reader.numDocs();
    } finally {
      if (reader != null) {
        LuceneIndexQueries.release(this.config.getMaster("default").getIndex(), reader);
      }
    }
  }

  private void copyConfigs() throws IOException {
    // config
    File source = new File("src/test/resources/config.xml");
    File destination = new File(configRoot, "config.xml");

    Assert.assertTrue("Missing test config " + source.getAbsolutePath(), source.exists());

    Files.copy(source.toPath(), destination.toPath());
    // template
    source = new File("src/test/resources/template.xsl");
    destination = new File(templateRoot, "template.xsl");

    Assert.assertTrue("Missing test template " + source.getAbsolutePath(), source.exists());

    Files.copy(source.toPath(), destination.toPath());
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

  public static void waitMs(int timeMs) {
    try {
      Thread.sleep(timeMs);
    } catch (InterruptedException ex) {
      LoggerFactory.getLogger(FolderWatcherBulkReplacementTest.class).error("Something went wrong", ex);
      Thread.currentThread().interrupt();
      Assert.fail();
    }
  }

}

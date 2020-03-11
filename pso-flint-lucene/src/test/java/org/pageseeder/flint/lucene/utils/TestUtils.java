package org.pageseeder.flint.lucene.utils;

import org.junit.Assert;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerScheduler;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.content.Content;
import org.pageseeder.flint.content.ContentType;
import org.pageseeder.flint.content.DeleteRule;
import org.pageseeder.flint.lucene.LuceneDeleteRule;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TestUtils {

  public static void wait(int time) {
    try {
      Thread.sleep(time * 1000);
    } catch (InterruptedException ex) {
      ex.printStackTrace();
      Assert.fail();
    }
  }

  public static File createFile(File root, String name, String content) throws IOException {
    File doc = new File(root, name);
    doc.createNewFile();
    FileOutputStream out = new FileOutputStream(doc);
    out.write(content.getBytes("UTF-8"));
    out.close();
    return doc;
  }

  public static final String ID_FIELD = "id";
  public static final String MEDIA_TYPE = "xml";
  public static final TestContentType TYPE = new TestContentType();
  public static final class TestContentType implements ContentType {
    private TestContentType() {}
  }

  public static final class TestContent implements Content {
    private final String _id;
    private final String _xml;
    private final boolean _deleted;
    public TestContent(String id, String xml) {
      this._id = id;
      this._xml = xml;
      this._deleted = xml == null;
    }
    @Override
    public String getContentID() {
      return this._id;
    }
    @Override
    public ContentType getContentType() {
      return TYPE;
    }
    @Override
    public DeleteRule getDeleteRule() {
      return new LuceneDeleteRule(ID_FIELD, this._id);
    }
    @Override
    public File getFile() throws IndexException {
      return null;
    }
    @Override
    public String getMediaType() throws IndexException {
      return MEDIA_TYPE;
    }
    @Override
    public InputStream getSource() throws IndexException {
      if (this._xml == null) return null;
      return new ByteArrayInputStream(this._xml.getBytes(StandardCharsets.UTF_8));
    }
    @Override
    public boolean isDeleted() throws IndexException {
      return this._deleted;
    }
  }

  public static class ParallelScheduler implements RunnerScheduler {

    private ExecutorService threadPool = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors());

    @Override
    public void schedule(Runnable childStatement) {
      threadPool.submit(childStatement);
    }

    @Override
    public void finished() {
      try {
        threadPool.shutdown();
        threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Got interrupted", e);
      }
    }
  }

  public static class ParallelRunner extends BlockJUnit4ClassRunner {

    public ParallelRunner(Class<?> klass) throws InitializationError {
      super(klass);
      setScheduler(new ParallelScheduler());
    }
  }
}

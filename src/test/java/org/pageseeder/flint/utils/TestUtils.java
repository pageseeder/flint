package org.pageseeder.flint.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Assert;

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
}

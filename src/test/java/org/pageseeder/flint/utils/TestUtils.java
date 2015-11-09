package org.pageseeder.flint.utils;

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
}

package org.pageseeder.flint;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

public class AdaptiveReaderWriterTest {

  // TODO Old tests copied here and to be JUnitified

  private static void test(int size) throws IOException {

    // Write
    AdaptiveReaderWriter buffer = new AdaptiveReaderWriter();
    long a = write(buffer.getWriter(), size);
    long b = read(buffer.getReader(), size);
    System.out.println("A("+size+")\t w="+(a / 1000)+"\t r="+(b / 1000)+"\t t="+((a + b) / 1000000)+"ms");

    // String Writer
    StringWriter w1 = new StringWriter();
    long c = write(w1, size);
    long d = read(new StringReader(w1.toString()), size);
    System.out.println("S("+size+")\t w="+(c / 1000)+"\t r="+(d / 1000)+"\t t="+((c + d) / 1000000)+"ms");

  }

  private static long write(Writer w, int size) throws IOException {
    long start = System.nanoTime();
    char[] array = "abcd_efgh_ijkl_mnop_qrst_uvwx_yz01_2345_6789_ABCD_".toCharArray();
    for (int i=0; i < size; i++) {
      char c = array[i % array.length];
      w.write(c);
    }
    w.close();
    long end = System.nanoTime();
    return (end-start);
  }

  private static long read(Reader r, int size) throws IOException {
    long start = System.nanoTime();
    int count = 0;
    while ((r.read()) != -1) {
      count++;
    }
    r.close();
    long end = System.nanoTime();
    if (size != count) throw new IllegalStateException();
    return (end-start);
  }

  public static void main(String[] args) throws IOException {
    test(10);
    test(100);
    test(1000);
    test(10000);
    test(100000);
    test(1000000);
    test(10000000);
    test(100000000);
    test(1000000000);
  }

}

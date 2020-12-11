package lla.privat.atarixl;

public class ByteUtils {

  public static int getWord(final byte low, final byte high) {
    return getByte(low) + 256 * getByte(high);
  }

  public static int getByte(final byte b) {
    int low = b;
    if (low < 0) {
      low += 256;
    }
    return low;
  }

  public static byte toByte(final int i) {
    byte b = (byte) i;
    return b;
  }

  public static char asciiChar(final byte b) {
    int c = getByte(b);
    if (c > 128) {
      c -= 128;
    }
    return (char) c;
  }

}

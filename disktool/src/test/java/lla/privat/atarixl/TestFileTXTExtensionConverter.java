package lla.privat.atarixl;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestFileTXTExtensionConverter {

  private FileTXTExtensionConverter fileTXTExtensionConverterSUT;

  private String nl;

  @Before
  public void setUp() {
    fileTXTExtensionConverterSUT = new FileTXTExtensionConverter();

    final byte[] nlArray = new byte[1];
    nlArray[0] = (byte) 0x9b;
    nl = new String(nlArray);
  }

  @Test
  public void testConvertToUnix() {

    final List<Byte> content = new ArrayList<>();

    content.add((byte) '1');
    content.add((byte) ' ');
    content.add((byte) 'A');
    content.add((byte) '=');
    content.add((byte) '1');
    content.add((byte) 0x9b);
    content.add((byte) '2');
    content.add((byte) '5');
    content.add((byte) '1');
    content.add((byte) ' ');
    content.add((byte) 'B');
    content.add((byte) '=');
    content.add((byte) '1');
    content.add((byte) 0x9b);

    final byte[] newContent = fileTXTExtensionConverterSUT.convertToUnix(content);

    Assert.assertEquals("1 A=1\n251 B=1\n", new String(newContent));
  }

  @Test
  public void testConvertToUnixWithAtariCarrigeReturnInString() {

    final List<Byte> content = new ArrayList<>();

    content.add((byte) '1');
    content.add((byte) ' ');
    content.add((byte) 'C');
    content.add((byte) '$');
    content.add((byte) '=');
    content.add((byte) '"');
    content.add((byte) 0x9b);
    content.add((byte) '"');
    content.add((byte) 0x9b);

    final byte[] newContent = fileTXTExtensionConverterSUT.convertToUnix(content);

    Assert.assertEquals("1 C$=\"\n\"\n", new String(newContent));
  }

  @Test
  public void testConvertFromUnix() {
    final List<Byte> content = new ArrayList<>();

    content.add((byte) 'A');
    content.add((byte) '=');
    content.add((byte) '1');
    content.add((byte) 0x0a);
    content.add((byte) 'B');
    content.add((byte) '=');
    content.add((byte) '1');
    content.add((byte) 0x0a);

    final byte[] newContent = fileTXTExtensionConverterSUT.convertFromUnix(content);
    Assert.assertEquals("A=1" + nl + "B=1" + nl, new String(newContent));
  }
}

package lla.privat.atarixl;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestFileLSTExtensionConverter {

  private FileLSTExtensionConverter fileLSTExtensionConverterSUT;

  private String nl;

  @Before
  public void setUp() {
    fileLSTExtensionConverterSUT = new FileLSTExtensionConverter();

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

    final byte[] newContent = fileLSTExtensionConverterSUT.convertToUnix(content);

    Assert.assertEquals("A=1\nB=1\n", new String(newContent));
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

    final byte[] newContent = fileLSTExtensionConverterSUT.convertToUnix(content);

    Assert.assertEquals(7, newContent.length);
    Assert.assertEquals("C$=\"", new String(newContent).subSequence(0, 4));

    // in the String we will not convert
    Assert.assertEquals(0x9b, ByteUtils.getByte(newContent[4]));
    Assert.assertEquals('"', ByteUtils.getByte(newContent[5]));
    Assert.assertEquals(0x0a, ByteUtils.getByte(newContent[6]));
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
    content.add((byte) 0x3b);
    content.add((byte) 0x0a);
    content.add((byte) ' ');
    content.add((byte) ' ');
    content.add((byte) ' ');
    content.add((byte) ' ');
    content.add((byte) '.');
    content.add((byte) 'M');
    content.add((byte) 'A');
    content.add((byte) 'C');
    content.add((byte) 'R');
    content.add((byte) 'O');
    content.add((byte) 0x0a);

    final byte[] newContent = fileLSTExtensionConverterSUT.convertFromUnix(content);
    Assert.assertEquals("1 A=1" + nl + "2 B=1" + nl + "3 ;" + nl + "4     .MACRO" + nl, new String(newContent));
  }

  @Test
  public void testConvertFromUnix_Spezial() {
    final List<Byte> content = new ArrayList<>();

    content.add((byte) '!'); // do not add line number, could be nice for 'run' or 'go#start', if this works as expected
    content.add((byte) 'T');
    content.add((byte) 'e');
    content.add((byte) 's');
    content.add((byte) 't');
    content.add((byte) 0x0a);

    final byte[] newContent = fileLSTExtensionConverterSUT.convertFromUnix(content);
    Assert.assertEquals("Test" + nl, new String(newContent));
  }

  @Test
  public void testConvertFromUnix_EmptyLine() {
    final List<Byte> content = new ArrayList<>();

    content.add((byte) 0x0a);
    content.add((byte) 0x0a);
    content.add((byte) 0x0d); // windows lineend
    content.add((byte) 0x0a);
    content.add((byte) 'a');
    content.add((byte) 0x0a);
    content.add((byte) 0x0a);

    final byte[] newContent = fileLSTExtensionConverterSUT.convertFromUnix(content);
    Assert.assertEquals("1 a" + nl, new String(newContent));
  }

  @Test
  public void testConvertFromUnix_WindowsLineendings() {
    final List<Byte> content = new ArrayList<>();

    content.add((byte) 'A');
    content.add((byte) '=');
    content.add((byte) '1');
    content.add((byte) 0x0d);
    content.add((byte) 0x0a);

    final byte[] newContent = fileLSTExtensionConverterSUT.convertFromUnix(content);
    Assert.assertEquals("1 A=1" + nl, new String(newContent));
  }

  @Test
  public void testConvertFromUnix_remove_added_rem() {
    final List<Byte> content = new ArrayList<>();

    content.add((byte) 'A');
    content.add((byte) '=');
    content.add((byte) '1');
    content.add((byte) ':');
    content.add((byte) 'R');
    content.add((byte) 'E');
    content.add((byte) 'M');
    content.add((byte) ' ');
    content.add((byte) 'b');
    content.add((byte) 'l');
    content.add((byte) 'a');
    content.add((byte) 'h');
    content.add((byte) 0x0a);
    content.add((byte) 'B');
    content.add((byte) '=');
    content.add((byte) '1');
    content.add((byte) ':');
    content.add((byte) '.');
    content.add((byte) 0x0a);
    content.add((byte) 'C');
    content.add((byte) ':');
    content.add((byte) 'R');
    content.add((byte) 'E');
    content.add((byte) 'M');
    content.add((byte) 'O');
    content.add((byte) 'V');
    content.add((byte) 'E');
    content.add((byte) 0x0a);

    final byte[] newContent = fileLSTExtensionConverterSUT.convertFromUnix(content);
    Assert.assertEquals("1 A=1" + nl + "2 B=1:." + nl + "3 C:REMOVE" + nl, new String(newContent));
  }

  @Test
  public void testConvertFromUnix_remove_first_rem() {
    final List<Byte> content = new ArrayList<>();

    content.add((byte) 'R');
    content.add((byte) 'E');
    content.add((byte) 'M');
    content.add((byte) ' ');
    content.add((byte) 'b');
    content.add((byte) 'l');
    content.add((byte) 'a');
    content.add((byte) 'h');
    content.add((byte) 0x0a);
    content.add((byte) 'B');
    content.add((byte) '=');
    content.add((byte) '1');
    content.add((byte) ':');
    content.add((byte) '.');
    content.add((byte) 0x0a);
    final byte[] newContent = fileLSTExtensionConverterSUT.convertFromUnix(content);
    Assert.assertEquals("1 " + nl + "2 B=1:." + nl, new String(newContent));
  }

}

package lla.privat.atarixl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestFileExtensionConverter {

  private FileExtensionConverter fileExtensionConverterSUT;

  @Before
  public void setUp() {
    fileExtensionConverterSUT = new FileExtensionConverter();

  }

  @Test
  public void testConvertToUnix_LST() {
    final byte[] content = new byte[2];
    content[0] = (byte) 'A';
    content[1] = (byte) 0x9b;

    final String extension = "LST";
    final byte[] newContent = fileExtensionConverterSUT.convertToUnix(content, extension);

    Assert.assertEquals(2, newContent.length);
    Assert.assertEquals('A', ByteUtils.getByte(newContent[0]));
    Assert.assertEquals(0x0a, ByteUtils.getByte(newContent[1]));
  }

  @Test
  public void testConvertToUnix_COM() {
    final byte[] content = new byte[2];
    content[0] = (byte) 'A';
    content[1] = (byte) 0x9b;

    final String extension = "COM";
    final byte[] newContent = fileExtensionConverterSUT.convertToUnix(content, extension);

    Assert.assertEquals(2, newContent.length);
    Assert.assertEquals('A', ByteUtils.getByte(newContent[0]));
    Assert.assertEquals(0x9b, ByteUtils.getByte(newContent[1]));
  }

}

package lla.privat.atarixl;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;


public class ITCreateFilesOnED {

  private HighLevelDisk highleveldisk;

  byte[] content;

  @Before
  public void setUp() throws IOException {
    highleveldisk = new HighLevelDisk("src/test/resources/lla/privat/atarixl/dos-ed.atr");
  }

  @Test
  public void testCreateFiles() throws IOException {
    final String name = "NAME_";
    for (int i = 0; i < 64 - 2; i++) {
      final String filename = name + i + ".DAT";
      createFile(filename, i + 1, 2000 - 125);
    }

    highleveldisk.getFilesystem().showFreeAndUsedTracks(highleveldisk.getFilesystem());
  }

  @Test
  public void testCreateOneBigFile() throws IOException {
    final String name = "NAME_";
    final String filename = name + 0 + ".DAT";
    createFile(filename, 65, 930 * 125);

    highleveldisk.getFilesystem().showFreeAndUsedTracks(highleveldisk.getFilesystem());
  }

  @Test
  public void testCreateTwoOneSektorFilesWithSameName() throws IOException {
    final String name = "NAME_";
    final String filename = name + 0 + ".DAT";
    createFile(filename, 65, 1 * 125);

    createFile(filename, 66, 1 * 125);

    highleveldisk.getFilesystem().showFreeAndUsedTracks(highleveldisk.getFilesystem());
  }
  
  @Ignore
  @Test
  public void testCreateTwoBigFileSameName() throws IOException {
    final String name = "NAME_";
    final String filename = name + 0 + ".DAT";
    createFile(filename, 65, 931 * 125);

    createFile(filename, 66, 931 * 125);

    highleveldisk.getFilesystem().showFreeAndUsedTracks(highleveldisk.getFilesystem());
  }

  @Test(expected = IOException.class)
  public void testCreateFilesMoreThanFreeDirectoryEntries() throws IOException {
    final String name = "NAME_";
    for (int i = 0; i < 65 - 2; i++) {
      final String filename = name + i + ".DAT";
      createFile(filename, i + 1, 1);
    }
  }

  @Test(expected = IOException.class)
  public void testCreateFilesUntilDiskFull() throws IOException {
    final String name = "NAME_";
    for (int i = 0; i < 62 - 2; i++) {
      final String filename = name + i + ".DAT";
      createFile(filename, i + 1, 2000);
    }
  }

  private void createFile(final String filename, final int value, final int size) throws IOException {
    content = new byte[size];
    for (int i = 0; i < content.length; i++) {
      content[i] = (byte) value;
    }
    highleveldisk.createFile(filename, content);
  }

}

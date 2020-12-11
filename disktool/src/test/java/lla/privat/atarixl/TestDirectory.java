package lla.privat.atarixl;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestDirectory {

  private Directory directorySUT;
  private Diskette diskette;

  @Before
  public void setUp() throws IOException {
    diskette = new Diskette("src/test/resources/lla/privat/atarixl/turbobasic-on-dd.atr");
    diskette.read();
    final Filesystem filesystem = new FilesystemDos25(diskette);

    directorySUT = new Directory(diskette, filesystem);
    directorySUT.read();
  }

  @Test
  public void testRead() throws IOException {
    final List<DirectoryEntry> entries = directorySUT.getEntries();
    Assert.assertEquals(6, entries.size());

    Assert.assertEquals("DOS.SYS", entries.get(0).getFilename());
    Assert.assertEquals("DUP.SYS", entries.get(1).getFilename());
    Assert.assertEquals("AUTORUN.SYS", entries.get(2).getFilename());
    Assert.assertEquals("AUTORUN.TUR", entries.get(3).getFilename());
    Assert.assertEquals("PLAYMISL.TUR", entries.get(4).getFilename());
    Assert.assertEquals("TEST.TUR", entries.get(5).getFilename());

    Assert.assertEquals(2 + 64, entries.get(0).getFlag());

    // File is marked as deleted
    Assert.assertEquals(128, entries.get(5).getFlag());

  }

  @Test
  public void testFirstFreeSektor() throws IOException {
    final List<DirectoryEntry> entries = directorySUT.getEntries();
    int firstFreeSektor = 4;
    for (int i = 0; i < 5; i++) {
      firstFreeSektor += entries.get(i).getCount();
    }
    Assert.assertEquals(125, firstFreeSektor);
  }

  @Test
  public void testShow() throws IOException {
    directorySUT.show();
  }

  @Test
  public void testFindFreeDirectoryEntry() throws IOException {
    final DirectoryEntry foundEntry = directorySUT.findFreeDirectoryEntry();

    Assert.assertEquals(5, foundEntry.getIndex());
  }
  
}

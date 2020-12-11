package lla.privat.atarixl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ITHighLevelDisk {

  private HighLevelDisk highLevelDiskSUT;

  byte[] helloWorldContent;
  byte[] bigContent;

  @Before
  public void setUp() throws IOException {
    highLevelDiskSUT = new HighLevelDisk("src/test/resources/lla/privat/atarixl/turbobasic-on-dd.atr");

    helloWorldContent = new byte[] {'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd', (byte) 0x9b};
    bigContent = new byte[256];
    for (int i = 0; i < bigContent.length; i++) {
      bigContent[i] = '.';
    }

  }

  @Test(expected = IOException.class)
  public void testCreateFile_with_tooLongName() throws IOException {
    final String filename = "HELLO_WITH_TOO_LONG_NAME.TXT";
    highLevelDiskSUT.createFile(filename, helloWorldContent);
  }

  @Test(expected = IOException.class)
  public void testCreateFile_with_tooLongFirstName() throws IOException {
    final String filename = "HELLOWORLD.1";
    highLevelDiskSUT.createFile(filename, helloWorldContent);
  }

  @Test(expected = IOException.class)
  public void testCreateFile_with_tooLongExtension() throws IOException {
    final String filename = "A.EXTENSION";
    highLevelDiskSUT.createFile(filename, helloWorldContent);
  }

  
  @Test
  public void testCreateFile_with_helloWorld() throws IOException {
    final String filename = "HELLO.TXT";
    highLevelDiskSUT.createFile(filename, helloWorldContent);

    final Directory directory = highLevelDiskSUT.getDirectory();
    directory.read();

    final DirectoryEntry entry = directory.getEntryByFilename(filename);
    Assert.assertNotNull(entry);
    Assert.assertEquals(125, entry.getStartsektor());
    Assert.assertEquals(1, entry.getCount());
    Assert.assertEquals("HELLO.TXT", entry.getFilename());
    Assert.assertEquals(12, entry.getStatus().getFileSize());
  }

  @Test
  public void testCreateFile_with_bigContent() throws IOException {

    final int calculateFreeSektors = highLevelDiskSUT.getFilesystem().calculateFreeSektors();
    int freeSektors = highLevelDiskSUT.getFilesystem().getFreeSektors();
    if (freeSektors != calculateFreeSektors) {
      highLevelDiskSUT.getFilesystem().setFreeSektors(calculateFreeSektors);
    }

    freeSektors = highLevelDiskSUT.getFilesystem().getFreeSektors();

    final String filename = "BIG.DAT";
    highLevelDiskSUT.createFile(filename, bigContent);

    final Directory directory = highLevelDiskSUT.getDirectory();
    directory.read();

    final DirectoryEntry entry = directory.getEntryByFilename(filename);
    Assert.assertEquals(125, entry.getStartsektor());
    Assert.assertEquals(2, entry.getCount());
    Assert.assertEquals("BIG.DAT", entry.getFilename());
    Assert.assertEquals(256, entry.getStatus().getFileSize());

    Assert.assertEquals(freeSektors - 2, highLevelDiskSUT.getFilesystem().getFreeSektors());
  }

  @Test
  public void testDeleteFile() throws IOException {
    final String filename = "DUP.SYS";
    highLevelDiskSUT.deleteFile(filename);

    final Filesystem filesystem = highLevelDiskSUT.getFilesystem();
    Assert.assertTrue(filesystem.isSektorFree(23));

    final Directory directory = highLevelDiskSUT.getDirectory();
    directory.read();

    final DirectoryEntry entry = directory.getEntryByFilename("DUP.SYS");
    Assert.assertEquals(128, entry.getFlag());
    Assert.assertEquals(20, entry.getCount());
    Assert.assertEquals(18, entry.getStartsektor());

    Assert.assertEquals(18, filesystem.findFirstFreeSektor());
  }

  @Test(expected = FileNotFoundException.class)
  public void testInsertFile_away() throws Exception {

    final String path = "src/test/resources/lla/privat/atarixl";
    highLevelDiskSUT.insert(path, "indexfile fehlt.txt");
  }

  @Test
  public void testInsert_fileDoNotExist() throws Exception {

    final String path = "src/test/resources/lla/privat/atarixl";
    highLevelDiskSUT.insert(path, "indexfile-fileDoNotExists.txt");
    // TODO: check that content of indexfile-fileDoNotExists.txt (SCHNUBBEL.TXT) do not exist on testdisk.atr

    // reread
    highLevelDiskSUT.getDirectory().read();
    // TODO: a contains would be nice for checks
  }

  @Test
  public void testInsert() throws Exception {

    final String path = "src/test/resources/lla/privat/atarixl";
    highLevelDiskSUT.insert(path, "indexfile1.txt");
  }

  @Test
  public void testInsert_LongName() throws Exception {

    final String path = "src/test/resources/lla/privat/atarixl";
    highLevelDiskSUT.insert(path, "indexfile2.txt");
  }

  @Test
  public void testInsertWithConvert() throws Exception {

    final String path = "src/test/resources/lla/privat/atarixl";
    highLevelDiskSUT.insert(path, "indexfile3.txt");

    // after disk change, a reread of the directory is need
    highLevelDiskSUT.getDirectory().read();

    highLevelDiskSUT.extract("LINEEND.TXT");
    final File file = new File("LINEEND.TXT");
    Assert.assertTrue(file.exists());
  }

  @Test(expected = IOException.class)
  public void testInsert_TooBigFile_DiskFull() throws Exception {

    final String path = "src/test/resources/lla/privat/atarixl";
    highLevelDiskSUT.insert(path, "indexfile_diskfull.txt");
  }

  @Test
  public void testShow() throws Exception {
    highLevelDiskSUT.showDirectory("");
  }

  @Test
  public void testShowAll() throws Exception {
    highLevelDiskSUT.showDirectory("all");
  }

  @Ignore("Reason: there is something stupid with this disk. Seems, atari800 can't handle ED Disks in the right way.")
  @Test
  public void testDosEdDisk_show() throws IOException {
    final HighLevelDisk highleveldisk = new HighLevelDisk("src/test/resources/lla/privat/atarixl/dos-ed.atr");
    highleveldisk.showDirectory("");

    // highleveldisk.getDiskette().setDebugOn();

//     final int free = highleveldisk.getFilesystem().getFreeSektors();
    final int freeCalculated = highleveldisk.getFilesystem().calculateFreeSektors();
    Assert.assertEquals(930, freeCalculated);
    // TODO: write a file
    // check if it is the right vtoc (vtoc on sektor 1024 starts at position 0)
  }

  @Test
  public void testDosEdDisk_showFree() throws IOException {
    final HighLevelDisk highleveldisk = new HighLevelDisk("src/test/resources/lla/privat/atarixl/dos-ed.atr", true);

    highleveldisk.getFilesystem().showFreeAndUsedTracks(highleveldisk.getFilesystem());
  }
}

package lla.privat.atarixl;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import lla.privat.atarixl.Filesystem.FreeUsed;

public class TestFilesystem {

  private Directory directory;
  private Diskette diskette;
  private Filesystem filesystemSUT;

  @Before
  public void setUp() throws IOException {
    diskette = new Diskette("src/test/resources/lla/privat/atarixl/turbobasic-on-dd.atr");
    diskette.read();
    filesystemSUT = new FilesystemDos25(diskette);

    directory = new Directory(diskette, filesystemSUT);
    directory.read();
  }

  @Test
  public void testParameter() throws IOException {
    Assert.assertEquals(2, filesystemSUT.getVtocId());
    Assert.assertEquals(360, filesystemSUT.getSektorOfVtocInfo());
    Assert.assertEquals(361, filesystemSUT.getSektorOfDirectory());
    Assert.assertEquals(8, filesystemSUT.getSektorCountOfDirectory());
  }

  @Test
  public void testGetVtocSektor() {
  }

  @Test
  public void testVtocTable() throws IOException {

    final int vtocTable = diskette.getSektorPositionInFulldisk(filesystemSUT.getSektorOfVtocInfo());
    final int vtoc = ByteUtils.getByte(diskette.readFromDisk(vtocTable, IOType.VTOC_INFO));
    Assert.assertEquals(2, vtoc);
  }

  @Test
  public void testGesamtanzahlSektoren() {
    final int gesamtzahlSektoren = filesystemSUT.getAllOverSektors();
    Assert.assertEquals(707, gesamtzahlSektoren);
  }

  @Test
  public void testFreeSektors() {
    final int anzahlFreieSektoren = filesystemSUT.getFreeSektors();
    Assert.assertEquals(586, anzahlFreieSektoren);

    final int vtocTable = diskette.getSektorPositionInFulldisk(filesystemSUT.getSektorOfVtocInfo());
    Assert.assertEquals(0b00000000, ByteUtils.getByte(diskette.readFromDisk(vtocTable + 10 + 45, IOType.VTOC)));
    Assert.assertEquals(0b01111111, ByteUtils.getByte(diskette.readFromDisk(vtocTable + 10 + 46, IOType.VTOC)));

    Assert.assertEquals(0, ByteUtils.getByte(diskette.readFromDisk(vtocTable + 10 + 0, IOType.VTOC)));
    Assert.assertEquals(0, ByteUtils.getByte(diskette.readFromDisk(vtocTable + 10 + 1, IOType.VTOC)));
    Assert.assertEquals(0b00000000, ByteUtils.getByte(diskette.readFromDisk(vtocTable + 10 + 14, IOType.VTOC)));
    Assert.assertEquals(0b00000111, ByteUtils.getByte(diskette.readFromDisk(vtocTable + 10 + 15, IOType.VTOC)));
    Assert.assertEquals(0b11111111, ByteUtils.getByte(diskette.readFromDisk(vtocTable + 10 + 16, IOType.VTOC)));
    Assert.assertEquals(0b11111111, ByteUtils.getByte(diskette.readFromDisk(vtocTable + 10 + 17, IOType.VTOC)));
    
    Assert.assertEquals(0b11111111, ByteUtils.getByte(diskette.readFromDisk(vtocTable + 10 + 89, IOType.VTOC)));
    Assert.assertEquals(0b00000000, ByteUtils.getByte(diskette.readFromDisk(vtocTable + 10 + 90, IOType.VTOC)));
  }

  @Test
  public void testExtractSingleFileTurbobasic() throws IOException {
    final List<DirectoryEntry> entries = directory.getEntries();

    final DirectoryEntry turbobasicEntry = entries.get(2);
    Assert.assertEquals("AUTORUN.SYS", turbobasicEntry.getFilename());

    final Status fileStatus = filesystemSUT.getFileStatus(turbobasicEntry);
    final int lengthOfBytesOfTurboBasic = 17806;
    Assert.assertEquals(lengthOfBytesOfTurboBasic, fileStatus.getFileSize());
  }

  @Test
  public void testVtocTableBits() throws IOException {

    Assert.assertFalse(filesystemSUT.isSektorFree(0));
    Assert.assertFalse(filesystemSUT.isSektorFree(1));
    Assert.assertFalse(filesystemSUT.isSektorFree(2));
    Assert.assertFalse(filesystemSUT.isSektorFree(3));
    Assert.assertTrue(filesystemSUT.isSektorFree(125));

    Assert.assertTrue(filesystemSUT.isSektorFree(130));
    
    Assert.assertFalse(filesystemSUT.isSektorFree(filesystemSUT.getSektorOfVtocInfo()));
    Assert.assertFalse(filesystemSUT.isSektorFree(Filesystem.SEKTOR_OF_DIRECTORY));
    Assert.assertFalse(filesystemSUT.isSektorFree(Filesystem.SEKTOR_OF_DIRECTORY + 1));
    Assert.assertFalse(filesystemSUT.isSektorFree(Filesystem.SEKTOR_OF_DIRECTORY + 2));
    Assert.assertFalse(filesystemSUT.isSektorFree(Filesystem.SEKTOR_OF_DIRECTORY + 3));
    Assert.assertFalse(filesystemSUT.isSektorFree(Filesystem.SEKTOR_OF_DIRECTORY + 4));
    Assert.assertFalse(filesystemSUT.isSektorFree(Filesystem.SEKTOR_OF_DIRECTORY + 5));
    Assert.assertFalse(filesystemSUT.isSektorFree(Filesystem.SEKTOR_OF_DIRECTORY + 6));
    Assert.assertFalse(filesystemSUT.isSektorFree(Filesystem.SEKTOR_OF_DIRECTORY + 7));
    Assert.assertTrue(filesystemSUT.isSektorFree(369));
    Assert.assertTrue(filesystemSUT.isSektorFree(719));
    Assert.assertFalse(filesystemSUT.isSektorFree(720));

    filesystemSUT.setSektorAsUsed(160);
    Assert.assertFalse(filesystemSUT.isSektorFree(160));

    filesystemSUT.setSektorAsFree(160);
    Assert.assertTrue(filesystemSUT.isSektorFree(160));

    final FreeUsed freeUsed = filesystemSUT.showFreeAndUsedTracks(filesystemSUT);
    Assert.assertEquals(720 - 1 - freeUsed.free, freeUsed.used);
  }

  @Test
  public void testFindNextFreeSektor() throws IOException {
    Assert.assertEquals(125, filesystemSUT.findFirstFreeSektor());
  }

  @Test
  public void testExtractSingleFile() throws IOException {
    final DirectoryEntry entry = directory.getEntries().get(2);
    Assert.assertEquals(66, entry.getFlag());
    Assert.assertEquals("AUTORUN.SYS", entry.getFilename());

    final File file = new File("AUTORUN.SYS");

    if (file.exists()) {
      file.delete();
    }

    filesystemSUT.extractSingleFile(entry, false);

    final String currentPath = file.getAbsolutePath();
    Assert.assertTrue("File " + currentPath + " already exist.", file.exists());
    Assert.assertEquals(17806L, file.length());

    file.delete();
  }

  @Test
  public void testExtractSingleFileLineEnding() throws IOException {
    final Diskette diskette = new Diskette("src/test/resources/lla/privat/atarixl/dos-sd.atr");
    diskette.read();
    final Filesystem filesystem = new FilesystemDos25(diskette);

    final Directory directory = new Directory(diskette, filesystem);
    directory.read();

    Assert.assertEquals(3,  directory.getEntries().size());
    final DirectoryEntry entry = directory.getEntries().get(1);
    Assert.assertEquals(66, entry.getFlag());
    Assert.assertEquals("DUP.SYS", entry.getFilename());

    final File file = new File("DUP.SYS");

    if (file.exists()) {
      file.delete();
    }

    filesystemSUT.extractSingleFile(entry, false);

    final String currentPath = file.getAbsolutePath();
    Assert.assertTrue("File " + currentPath + " already exist.", file.exists());
    Assert.assertEquals(4966L, file.length());

    file.delete();
  }

  @Ignore
  @Test
  public void testExtractSingleFile_deleted_file() throws IOException {
    final DirectoryEntry entry = directory.getEntries().get(8);
    Assert.assertEquals(128, entry.getFlag());

    final File file = new File("ATMASII.COM");
    if (file.exists()) {
      file.delete();
    }

    filesystemSUT.extractSingleFile(entry, false);

    final String currentPath = file.getAbsolutePath();
    Assert.assertFalse("File " + currentPath + " already exist.", file.exists());
  }

  @Test
  public void testGetNextSektorOfSektor() throws IOException {
    Assert.assertEquals(5, filesystemSUT.getNextSektorOfSektorByVtoc(4, 0));
    Assert.assertEquals(6, filesystemSUT.getNextSektorOfSektorByVtoc(5, 0));
    Assert.assertEquals(7, filesystemSUT.getNextSektorOfSektorByVtoc(6, 0));
    // ...
    Assert.assertEquals(17, filesystemSUT.getNextSektorOfSektorByVtoc(16, 0));
    // end of file
    Assert.assertEquals(0, filesystemSUT.getNextSektorOfSektorByVtoc(17, 0));

    // next file
    Assert.assertEquals(19, filesystemSUT.getNextSektorOfSektorByVtoc(18, 1));
    Assert.assertEquals(20, filesystemSUT.getNextSektorOfSektorByVtoc(19, 1));
  }

  @Test(expected = IOException.class)
  public void testGetNextSektorOfSektor_wrong_entryNumber() throws IOException {
    Assert.assertEquals(24, filesystemSUT.getNextSektorOfSektorByVtoc(23, 2));
  }

  @Test(expected = IOException.class)
  public void testGetNextSektorOfSektorByVtoc_wrong_entryNumber() throws IOException {
    Assert.assertEquals(24, filesystemSUT.getNextSektorOfSektorByVtoc(23, 2));
  }

  @Ignore
  @Test
  public void checkFreeDisk_ED() throws IOException {
    final Diskette diskette = new Diskette("src/test/resources/lla/privat/atarixl/dos-ed.atr");
    diskette.read();

    final Filesystem filesystem = new FilesystemDos25(diskette);

    Assert.assertFalse(filesystem.isSektorFree(720));

    final FreeUsed freeUsed = filesystem.showFreeAndUsedTracks(filesystem);

    final int usableFree = filesystem.getAllOverSektors();
    Assert.assertEquals(1024 - 4 - 8 - 1 - 1, usableFree);

    final int calculatedFree = filesystem.calculateFreeSektors();
    final int free = filesystem.getFreeSektors();
    Assert.assertEquals(calculatedFree, freeUsed.free);

    Assert.assertEquals(calculatedFree, free);
  }

  @Ignore
  @Test
  public void checkFreeDisk_DD() throws IOException {
    final int usableFree = filesystemSUT.getAllOverSektors();
//    final FreeUsed freeUsed = filesystemSUT.showFreeAndUsedTracks(filesystemSUT);

    Assert.assertEquals(720 - 4 - 8 - 1, usableFree);

    final int calculatedFree = filesystemSUT.calculateFreeSektors();
    final int free = filesystemSUT.getFreeSektors();
    Assert.assertEquals(calculatedFree, free);
  }
}

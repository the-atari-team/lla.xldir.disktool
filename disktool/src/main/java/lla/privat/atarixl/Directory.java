package lla.privat.atarixl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Directory {

  private static Logger LOGGER = LoggerFactory.getLogger(Directory.class);

  private List<DirectoryEntry> entries;
  private final Diskette disk;
  private final Filesystem filesystem;
  private boolean hasDirectory;

  public Directory(final Diskette disk, final Filesystem filesystem) {
    this.disk = disk;
    this.filesystem = filesystem;
    entries = null;
    hasDirectory = false;
  }

  public List<DirectoryEntry> getEntries() {
    return entries;
  }

  public boolean hasDirectory() {
    return hasDirectory;
  }

  public void extractAll() throws IOException {
    if (!hasDirectory) {
      throw new IOException("There exist no directory to extract all");
    }
    LOGGER.info("Extract all files from: {}", disk.getFilename());

    for (final DirectoryEntry entry : entries) {
      try {
        filesystem.extractSingleFile(entry, true /* alsoDeletedFiles */);
      }
      catch (final IOException e) {
        LOGGER.error("Caught IOException: {}", e.getMessage());
      }
    }
  }

  public void showAll() {
    LOGGER.info("Show directory, all files from: {}", disk.getFilename());

    for (final DirectoryEntry entry : entries) {
      showEntry(entry);
    }
    showFreeSectors();
  }

  public void show() {
    LOGGER.info("Show directory, only existing files from: {}", disk.getFilename());

    for (final DirectoryEntry entry : entries) {
      final int flag = entry.getFlag();
      if (!((flag & 128) == 128)) {
        showEntry(entry);
      }
    }
    showFreeSectors();
  }

  private void showFreeSectors() {
    // show free sektors
    LOGGER.info("   free sectors: {}", filesystem.getFreeSektors());
    LOGGER.info("calculated free: {}", filesystem.calculateFreeSektors());
  }

  public void showEntry(final DirectoryEntry entry) {
    final int flag = entry.getFlag();
    String filetype;
    if ((flag & 128) == 128) {
      filetype = "XX";
    }
    else if ((flag & 32) == 32) {
      filetype="r ";
    }
    else {
      filetype = "rw";
    }
    if ((flag & 2) == 2 || flag == 128 || flag == 96) {
      final int size = entry.getCount();
      final String sizeAsString = indentWithSpace(size, 3);
      LOGGER.info("{} {} {} {}",filetype,entry.getFilenameNotTrimed(), sizeAsString, entry.getStatus());
    }
    else {
      LOGGER.info("           .    {} ",0, entry.getStatus());
    }
  }

  private String indentWithSpace(final int size, final int n) {
    final String value = Integer.toString(size);
    final int countOfSpace = n - value.length();
    if (countOfSpace < 1) {
      return value;
    }
    final StringBuilder valueBuffer = new StringBuilder();
    for (int i = 0; i < countOfSpace; i++) {
      valueBuffer.append(" ");
    }
    valueBuffer.append(value);
    return valueBuffer.toString();
  }

  public void read() throws IOException {
    final int sektorSize = disk.getSektorSize();
    // final byte[] fulldisk = disk.getFulldisk();

    entries = new ArrayList<>();

    if (filesystem.getSektorOfDirectory() * sektorSize > disk.getDiskSize()) {
      LOGGER.warn("Seems, disk is not big enough to contain a directory. Use extract!");
      return;
    }
    final int vtocId = filesystem.getVtocId();
    if (vtocId >= 128) {
    	LOGGER.info("LiteDOS format recognized!");
    }
    else if (vtocId != 2 && vtocId != 7 && vtocId != 3) {
      LOGGER.error("Unknown VTOC format for disk: {}", disk.getFilename());
      System.exit(1);
    }

    int index = 0;
    final int firstSektor = filesystem.getSektorOfDirectory();
    final int lastSektor = firstSektor + filesystem.getSektorCountOfDirectory();

    for (int sektor = firstSektor; sektor < lastSektor; sektor++) {
      int flag = 0;
      for (int line = 0; line < 128; line += 16) {
        final int fulldiskPosition = disk.getSektorPositionInFulldisk(sektor) + line;
        flag = ByteUtils.getByte(disk.readFromDisk(fulldiskPosition, IOType.DIR_FLAG));
        if (flag != 0) {
          final DirectoryEntry entry = createDirectoryEntry(fulldiskPosition, flag, index);
          entries.add(entry);
        }
        else if (flag == 0) {
          break;
        }
        index++;
      }
      if (flag == 0) {
        break;
      }
      // Absolut spezieller Sonderfall
      // Normalerweilse enthält das Directory immer nur 128 bytes an Daten
      // daraus folgt es gibt max. 64 Dateien, mehr ist mit der vtocId==2 auch nicht
      // darstellbar
      // weil die EntryPosition ja mit im NextSektor vergraben ist und geprüft wird.
      if (sektorSize > 128) {
        for (int line = 128; line < 256; line += 16) {
          final int fulldiskPosition = disk.getSektorPositionInFulldisk(sektor) + line;
          flag = ByteUtils.getByte(disk.readFromDisk(fulldiskPosition, IOType.DIR_FLAG));
          if ((flag & 128 + 64 + 2) == 66 || (flag & 128 + 64 + 2) == 128 + 66) {
            final DirectoryEntry entry = createDirectoryEntry(fulldiskPosition, flag, index);
            entries.add(entry);
          }
          else if (flag == 0) {
            flag = 1;
            break;
          }
          index++;
        }
      }
      if (flag == 0) {
        break;
      }
    }
    if (entries.size() > 0) {
      hasDirectory = true;
    }
  }

  private DirectoryEntry createDirectoryEntry(final int direktoryPosition, final int flag, final int index) {
    // final byte[] fulldisk = disk.getFulldisk();
    String statusText = "";

    final int sektorCount = ByteUtils.getWord(disk.readFromDisk(direktoryPosition + 1, IOType.DIR_FILE_SIZE),
        disk.readFromDisk(direktoryPosition + 2, IOType.DIR_FILE_SIZE));
    final int start = ByteUtils.getWord(disk.readFromDisk(direktoryPosition + 3, IOType.DIR_SECTOR_START),
        disk.readFromDisk(direktoryPosition + 4, IOType.DIR_SECTOR_START));
    if (start < 4 || start > disk.getSektors()) {
      statusText = "illegal sector start";
    }

    final String fullname = getFullName(direktoryPosition);

    final DirectoryEntry entry = new DirectoryEntry(flag, sektorCount, start, index, fullname);
    final Status status = filesystem.getFileStatus(entry);
    status.addStatus(statusText);
    entry.setStatus(status);
    return entry;
  }

  private String getFullName(final int direktoryPosition) {
    final StringBuilder name = new StringBuilder();
    for (int i = 0; i < 8; i++) {
      name.append(ByteUtils.asciiChar(disk.readFromDisk(direktoryPosition + 5 + i, IOType.DIR_NAME)));
    }
    final StringBuilder extend = new StringBuilder();
    for (int i = 0; i < 3; i++) {
      extend.append(ByteUtils.asciiChar(disk.readFromDisk(direktoryPosition + 5 + 8 + i, IOType.DIR_EXT)));
    }

    final String fullname = name.toString() + "." + extend.toString();
    return fullname;
  }

  public DirectoryEntry getEntryByFilename(final String filename) {

    for (final DirectoryEntry entry : entries) {
      if (entry.getFilename().equalsIgnoreCase(filename)) {
        return entry;
      }
    }
    return null;
  }

  public DirectoryEntry findFreeDirectoryEntry() throws IOException {
    int index = 0;
    for (final DirectoryEntry entry : entries) {
      if ((entry.getFlag() & 128) == 128) {
        return entry;
      }
      index++;
    }
    if (index < 64) {
      final DirectoryEntry newEntry = new DirectoryEntry(128, 0, 0, index, "");
      entries.add(newEntry);
      return newEntry;
    }
    LOGGER.error("Directory is full");
    throw new IOException("Directory full.");
  }

  public void copyToDirectory(final DirectoryEntry entry) {
    final int index = entry.getIndex();
    final int firstSektor = filesystem.getSektorOfDirectory();
    int low = index & 0b111;
    int high = index / 8;
    final int directoryPosition = disk.getSektorPositionInFulldisk(firstSektor) + high * disk.getSektorSize() + low * 16;
    disk.writeToDisk(directoryPosition, (byte) entry.getFlag(), IOType.DIR_FLAG);

    low = entry.getCount() & 0b11111111;
    high = entry.getCount() / 256;
    disk.writeToDisk(directoryPosition + 1, (byte) low, IOType.DIR_FILE_SIZE);
    disk.writeToDisk(directoryPosition + 2, (byte) high, IOType.DIR_FILE_SIZE);

    low = entry.getStartsektor() & 0b11111111;
    high = entry.getStartsektor() / 256;
    disk.writeToDisk(directoryPosition + 3, (byte) low, IOType.DIR_SECTOR_START);
    disk.writeToDisk(directoryPosition + 4, (byte) high, IOType.DIR_SECTOR_START);

    // empty full name
    for (int i = 0; i < 8 + 3; i++) {
      disk.writeToDisk(directoryPosition + 5 + i, (byte) ' ', IOType.DIR_NAME);
    }

    final String filename = entry.getFilename();
    int ext = 0;
    // fill name until dot
    for (int i = 0; i < filename.length(); i++) {
      final char charAt = filename.charAt(i);
      if (charAt == '.') {
        ext = i;
        break;
      }
      disk.writeToDisk(directoryPosition + 5 + i, (byte) charAt, IOType.DIR_NAME);
    }
    if (ext > 0) {
      int extPosOnDisk = 8;
      // file extension
      for (int i = ext + 1; i < filename.length(); i++) {
        final char charAt = filename.charAt(i);
        disk.writeToDisk(directoryPosition + 5 + extPosOnDisk++, (byte) charAt, IOType.DIR_EXT);
      }
    }
  }
}

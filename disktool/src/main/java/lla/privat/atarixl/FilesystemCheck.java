package lla.privat.atarixl;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilesystemCheck {

  private static Logger LOGGER = LoggerFactory.getLogger(FilesystemCheck.class);

  private final Diskette diskette;
  private final Filesystem filesystem;
  private final Directory directory;

  private final int[] sektorStatus;

  private int countOfErrors;
  private int countOfWarnings;

  public FilesystemCheck(final String filename) throws IOException {
    this(filename, false);
  }

  public int getCountOfErrors() {
    return countOfErrors;
  }

  public int getCountOfWarnings() {
    return countOfWarnings;
  }

  public FilesystemCheck(final String filename, final boolean isDebug) throws IOException {
    LOGGER.info("File system check on disk: {}", filename);

    this.diskette = new Diskette(filename);
    if (isDebug) {
      diskette.setDebugOn();
    }
    diskette.read();

    this.filesystem = new FilesystemDos25(diskette);

    this.directory = new Directory(diskette, filesystem);
    directory.read();

    sektorStatus = new int[diskette.getSektors() + 1];
    // Welche Sektoren immer belegt sind
    sektorStatus[0] = 255; // nicht existent Atari zählt von 1-720 oder 1-1040
    sektorStatus[1] = 255; // bootsektoren
    sektorStatus[2] = 255;
    sektorStatus[3] = 255;

    sektorStatus[360] = 255; // vtoc
    for (int i = 361; i <= 368; i++) { // directory
      sektorStatus[i] = 255;
    }
    sektorStatus[720] = 255; // Sektor 720 ist immer belegt
    if (diskette.getSektorSize() == 128 && diskette.getSektors() > 1024) {
      for (int i = 1024; i <= 1040; i++) {
        sektorStatus[i] = 255;
      }
    }
  }

  public void checkDirectoryEntries() {
    LOGGER.info("Check every directory entry");
    int countOfUsedSectors = 0;
    for (final DirectoryEntry entry : directory.getEntries()) {
      final boolean deleted = (entry.getFlag() & 128) == 128;
      showStatusOfDirectoryEntry(entry);

      if (!deleted) {
        markUsedSektorsByCheckFiles(entry);
        countOfUsedSectors += entry.getCount();
      }
    }

    checkWholeVtoc();

    checkSizeInDirectory(countOfUsedSectors);

    if (countOfErrors > 0) {
      LOGGER.error("Errors found. Count: {}", countOfErrors);
    }
    if (countOfWarnings > 0) {
      LOGGER.warn("Recoverable warnings found. Count: {}", countOfWarnings);
    }
    if (countOfErrors == 0 && countOfWarnings == 0) {
      LOGGER.info("No errors and warnings found, ok.");
    }
  }

  private int freeSectors() {
    int freeSectors = 0;
    for (int sector = 0; sector <= diskette.getSektors(); sector++) {
      if (sektorStatus[sector] == 0) {
        freeSectors++;
      }
    }
    return freeSectors;
  }

  private void checkSizeInDirectory(final int countOfUsedSectors) {
    final int freeSectorsOnDisk = filesystem.getFreeSektors();
    final int allOverFreeSectors = filesystem.getAllOverSektors();
    LOGGER.info("Check disk size");
    LOGGER.info("Disk has {} sectors", diskette.getSektors());

    final int freeSectors = freeSectors();
    if (freeSectors != freeSectorsOnDisk) {
      LOGGER.warn("Real free sectors {} differ from vtoc-freeSectorsOnDisk {}", freeSectors, freeSectorsOnDisk);
    }
    else {
      LOGGER.warn("Real free sectors {} and vtoc-freeSectorsOnDisk are equal", freeSectors);
    }

    if (diskette.getSektors() == 1040) {
      if (allOverFreeSectors != 1024 - 3 - 1 - 8 - 1) {
        LOGGER.warn("Given over all free sectors are wrong, should be {} sectors, but is {}", 1024 - 3 - 1 - 8 - 1, allOverFreeSectors);
        countOfWarnings++;
      }
    }
    else if (diskette.getSektors() == 720) {
	    if (allOverFreeSectors != 720 - 3 - 1 - 8) {
	        LOGGER.warn("Given over all free sectors are wrong, should be {} sectors, but is {}", 720 - 3 - 1 - 8, allOverFreeSectors);
	        countOfWarnings++;
	      }
	    }
    else if (diskette.getSektors() == 1440) {
	    if (allOverFreeSectors != 1440 - 3 - 1 - 8) {
	        LOGGER.warn("Given over all free sectors are wrong, should be {} sectors, but is {}", 1440 - 3 - 1 - 8, allOverFreeSectors);
	        countOfWarnings++;
	      }
	    }

    else {
      LOGGER.error("Unsupported disk size, found {} sectors", diskette.getSektors());
      countOfErrors++;
    }

    LOGGER.info("Check count of sectors");

    final int calculatedUsedSectors = allOverFreeSectors - freeSectorsOnDisk;
    if (countOfUsedSectors != calculatedUsedSectors) {
      LOGGER.warn("Count of used sectors wrong really used {} calculated {}", countOfUsedSectors, calculatedUsedSectors);
      countOfWarnings++;
    }
  }

  public void checkWholeVtoc() {
    for (int i = 0; i <= diskette.getUseableSektors(); i++) {
      if (sektorStatus[i] != 0) {
        // sektor is in use
        if (filesystem.isSektorFree(i)) {
          LOGGER.warn("Sektor {} must be in use by entry {} but is marked free.", i, sektorStatus[i] & 0b00111111);
          countOfWarnings++;
        }
      }
      else {
        // sektor is free
        if (filesystem.isSektorInUse(i)) {
          LOGGER.warn("Sektor {} should be free but is in use.", i);
          countOfWarnings++;
        }
      }
    }
  }

  public void showStatusOfDirectoryEntry(final DirectoryEntry entry) {
    final StringBuilder entryFlag = new StringBuilder();

    final boolean deleted = (entry.getFlag() & 128) == 128;

    entryFlag.append(deleted
        ? " deleted"
        : "        ");
    entryFlag.append((entry.getFlag() & 64) == 64
        ? " used"
        : "     ");
    entryFlag.append((entry.getFlag() & 32) == 32
        ? " locked"
        : "       ");
    // status.append((entry.getFlag() & 16) == 16 ? " " : " ".repeat(1));
    // status.append((entry.getFlag() & 8) == 8 ? " " : " ".repeat(1));
    // status.append((entry.getFlag() & 4) == 4 ? " " : " ".repeat(1));
    // status.append((entry.getFlag() & 2) != 2
    // ? " !D2"
    // : " ".repeat(4));
    // status.append((entry.getFlag() & 1) == 1 ? " " : " ".repeat(1));

    final int errorCount = entry.getStatus().getErrorCount();

    if (deleted) {
      if (errorCount == 0) {
        LOGGER.info("{} {} file recoverable", entry.getFilename(), entryFlag.toString());
      }
      else {
        LOGGER.info("{} {} errors in structure, file unrecoverable, but deleted", entry.getFilename(), entryFlag.toString());
      }
    }
    else {
      // ! deleted
      if (errorCount == 0) {
        LOGGER.info("{} {} no errors in file", entry.getFilename(), entryFlag.toString());
      }
      else {
        LOGGER.error("{} {} errors in structure, file unrecoverable", entry.getFilename(), entryFlag.toString());
        countOfErrors++;
      }
    }
  }

  // jeder Sektor wird markiert
  public void markUsedSektorsByCheckFiles(final DirectoryEntry entry) {
    LOGGER.info("Sektor count: {}", entry.getCount());
    LOGGER.info("Sektor start: {}", entry.getStartsektor());

    int sektor = entry.getStartsektor();
    final int index = entry.getIndex();
    int count = entry.getCount();
    int sectorCount = 0;
    int size = 0;
    int status = 0;

    try {
      int nextSektor;
      do {
        sectorCount++;
        final int contentSize = filesystem.getContentSizeOfSektor(sektor);
        if (contentSize > diskette.getSektorSize() - 3) {
          LOGGER.error("Content size is bigger than sector size. Impossible.");
          countOfErrors++;
          status = 2;
        }

        if (filesystem.isSektorFree(sektor)) {
          LOGGER.warn("VTOC is wrong. Sector (" + sektor + ") marked as free");
          countOfWarnings++;
          status = 1;
          sektorStatus[sektor] = index | 128;
        }

        if (filesystem.isSektorInUse(sektor)) {
          sektorStatus[sektor] = index | 64;
        }

        size += contentSize;
        nextSektor = filesystem.getNextSektorOfSektorByVtoc(sektor, index);
        --count;
        if (count < 0) {
          status = 4;
          break;
        }
        sektor = nextSektor;
      }
      while (nextSektor != 0);

      if (count == 0) {
        if (status == 0) {
          LOGGER.info("File structure ok.");
        }
      }
      else if (count > 0) {
        LOGGER.error("File is longer in directory entry, miss {} sectors", count);
        countOfErrors++;
      }
      else {
        LOGGER.warn("Given sector count in directory is wrong, should be {} sectors", sectorCount);
        countOfWarnings++;
      }
    }
    catch (final IOException e) {
      LOGGER.error("Exception caught: {}", e.getMessage());
    }
    LOGGER.info("File uses really {} bytes", size);
  }
}

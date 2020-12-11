package lla.privat.atarixl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HighLevelDisk {

  private static Logger LOGGER = LoggerFactory.getLogger(HighLevelDisk.class);

  private final Diskette diskette;
  private Filesystem filesystem;
  private final Directory directory;
  private final static boolean DO_NOT_DEBUG = false;

  private final FileExtensionConverter fileExtensionConverter;

  public HighLevelDisk(final String filename) throws IOException {
    this(filename, DO_NOT_DEBUG);
  }

  public HighLevelDisk(final String filename, final boolean isDebug) throws IOException {
    this.diskette = new Diskette(filename);
    if (isDebug) {
      diskette.setDebugOn();
    }
    diskette.read();

    int vtocInfoPosition = diskette.getSektorPositionInFulldisk(360);
    if (vtocInfoPosition < diskette.getDiskSize()) {
      int vtocId = ByteUtils.getByte(diskette.readFromDisk(vtocInfoPosition, IOType.VTOC_INFO));
      if (vtocId == 2 || vtocId == 3 || vtocId == 7) {
    	  this.filesystem = new FilesystemDos25(diskette);
      }
      else if (vtocId > 128) {
    	  this.filesystem = new FilesystemLiteDOS(diskette);    	  
      }
      else {
    	  LOGGER.error("vtocId {} not supported!", vtocId);
          throw new IOException("vtocId problem.");
      }
    }
    this.directory = new Directory(diskette, filesystem);
    directory.read();

    fileExtensionConverter = new FileExtensionConverter();
  }

  public Directory getDirectory() {
    return directory;
  }

  public Filesystem getFilesystem() {
    return filesystem;
  }

  public Diskette getDiskette() {
    return diskette;
  }

  /**
   * Create a file on ATR Disk
   * * delete old file from ATR
   * * write file to disk, sektor by sektor
   * * insert in directory entry
   * * write new directory
   * * write used sektors in vtoc
   *
   * @param filename
   * @param content
   * @throws IOException
   */
  public void createFile(final String filename, final byte[] content) throws IOException {

    LOGGER.info("Create file: {} length: {}", filename, content.length);

    deleteFile(filename);
    final DirectoryEntry freeEntry = directory.findFreeDirectoryEntry();

    int count = 0;
    int nextSektor = 0;
    int contentCursor = 0;
    int sektor = filesystem.findFirstFreeSektor();

    freeEntry.setFilename(filename);
    freeEntry.setStartSektor(sektor);
    do {
      count++;
      filesystem.setSektorAsUsed(sektor);

      int countBytes = diskette.getSektorSize() - 3;
      if (content.length - contentCursor <= countBytes) {
        countBytes = content.length - contentCursor;
        nextSektor = 0;
      }
      else {
        nextSektor = filesystem.findNextFreeSektor(sektor);
      }
      filesystem.copyToDisk(sektor, countBytes, content, contentCursor);

      contentCursor += countBytes;
      filesystem.createSektorHeader(sektor, nextSektor, freeEntry.getIndex(), countBytes);
      sektor = nextSektor;
    }
    while (contentCursor < content.length);

    freeEntry.setCount(count);
    freeEntry.setFlag(64 + 2);
    directory.copyToDirectory(freeEntry);

    // TODO: we use the expensive way but the result is always right to get free sektors
    final int currentFreeSektors = filesystem.calculateFreeSektors(); // TODO: .getFreeSektors();
    filesystem.setFreeSektors(currentFreeSektors); // wir brauchen nichts abzuziehen, weil es ja schon geschrieben wurde
    // diskette.write();
  }

  public void deleteFile(final String filename) throws IOException {
    final DirectoryEntry entry = directory.getEntryByFilename(filename);
    if (entry == null) {
      return;
    }

    int sektor = entry.getStartsektor();
    int count = entry.getCount();
    final int index = entry.getIndex();

    final List<Integer> sektorsToFree = new ArrayList<>();

    int nextSektor;
    do {
      nextSektor = filesystem.getNextSektorOfSektorByVtoc(sektor, index);
      --count;
      if (count < 0) {
        break;
      }
      sektorsToFree.add(sektor);
      sektor = nextSektor;
    }
    while (nextSektor != 0);

    if (count == 0) {
      // ok, delete file
      sektorsToFree.forEach((sec) -> filesystem.setSektorAsFree(sec));
      entry.setFlag(128);
      directory.copyToDirectory(entry);
    }
    else if (count > 0) {
      throw new IOException("Can't delete file: " + filename + " count > 0");
    }
    else {
      throw new IOException("Can't delete file: " + filename + " count < 0");
    }
  }

  public void showDirectory(final String file) {
    if ("all".equals(file)) {
      directory.showAll();
    }
    else {
      directory.show();
    }
  }

  public void extract(final String file) throws IOException {
    if (directory.hasDirectory()) {
      if ("all".equals(file)) {
        LOGGER.info("extract all files");
        directory.extractAll();
      }
      else {
        final DirectoryEntry entry = directory.getEntryByFilename(file);
        if (entry != null) {
          LOGGER.info("extract one file: {}", entry.getFilename());
          filesystem.extractSingleFile(entry, true);
        }
      }
    }
    else {
      diskette.extractDiskWithoutBootsektor(file);
    }
  }

  public void insert(String path, final String indexFile) throws IOException {
    final File file = new File(path, indexFile);
    // final String filePath = file.getAbsolutePath();
    if (file.exists()) {
      @SuppressWarnings("resource")
      final Scanner scanner = new Scanner(file);
      if (".".equals(path)) {
        path = file.getCanonicalFile().getParent();
      }
      while (scanner.hasNextLine()) {
        final String line = scanner.nextLine();
        if (line.startsWith("#")) {
          continue;
        }
        if (line.length() == 0) {
          continue;
        }

        String filenameToInsert;
        String filenameToRead;

        boolean asciiToAtari = true;
        boolean asList = false;

        if (line.contains("->")) {
          final String[] convert = line.split("->");
          filenameToRead = convert[0].trim();
          filenameToInsert = convert[1].trim();
          if ("plain".equals(filenameToInsert)) {
            asciiToAtari = false;
            filenameToInsert = convert[2].trim();
          }
          if ("list".equalsIgnoreCase(filenameToInsert)) {
            asciiToAtari = false;
            asList = true;
            filenameToInsert = convert[2].trim();
          }
        }
        else {
          filenameToRead = line;
          filenameToInsert = line;
        }

        final File fileToRead = new File(path, filenameToRead);
        if (!fileToRead.exists()) {
          LOGGER.warn("File to insert {} doesn't exist. Will exclude this file.", line);
          continue;
        }
        try {
          byte[] content = readContent(fileToRead);
          if (asciiToAtari) {
            final String[] filenameSplit = filenameToInsert.split("\\.");
            if (filenameSplit.length > 1) {
              final String extension = filenameSplit[1];
              content = fileExtensionConverter.convertFromUnix(content, extension);
            }
            else {
              LOGGER.error("No filename extension given in '{}', will not convert", filenameSplit[0]);
            }
          }
          if (asList) {
            content = fileExtensionConverter.convertFromUnix(content, "LST");
          }
          createFile(filenameToInsert, content);
        }
        catch (final IOException e) {
          final String errorText = String.format("Can't create file %s on disk. Message: %s", filenameToInsert, e.getMessage());
          LOGGER.error(errorText);
          throw new IOException(errorText);
        }
      }
      scanner.close();
    }
    else {
      throw new FileNotFoundException("Insert file doesn't exist. " + indexFile);
    }
  }

  private byte[] readContent(final File file) {

    try (FileInputStream inputStream = new FileInputStream(file)) {
      final int bytes = (int) file.length();
      final byte[] buffer = new byte[bytes];

      inputStream.read(buffer, 0, bytes);
      return buffer;
    }
    catch (final IOException e) {

      LOGGER.error("ERROR: can't read file: {} message: {}", file.getAbsolutePath(), e.getMessage());
    }
    return null;
  }
}

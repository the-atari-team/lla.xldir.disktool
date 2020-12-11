package lla.privat.atarixl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Atari 8bit Disk Tool
 *
 */
public class Main {

  private static Logger LOGGER = LoggerFactory.getLogger(Main.class);

  private final String filename;

  public Main(final String filename) throws IOException {
    this.filename = filename;
  }

  public void showDirectory(final String file) throws IOException {
    final HighLevelDisk disk = new HighLevelDisk(filename);
    disk.showDirectory(file);
  }

  public void extract(final String file) throws IOException {
    if (file == null || "".equals(file)) {
      throw new IllegalArgumentException("No file to extract given.");
    }
    final HighLevelDisk disk = new HighLevelDisk(filename);
    disk.extract(file);
  }

  public void hexdump(final String writeFile) throws IOException {
    final HighLevelDisk disk = new HighLevelDisk(filename);
    disk.getDiskette().hexdump(writeFile);
  }

  public void makeDD() throws IOException {
	    final Diskette newDisk = new Diskette("empty");
	    newDisk.makeEmptyDDDisk(filename);
	  }

  public void makeED() throws IOException {
    final Diskette newDisk = new Diskette("empty");
    newDisk.makeEmptyEDDisk(filename);
  }

  public void makeSD() throws IOException {
    final Diskette newDisk = new Diskette("empty");
    newDisk.makeEmptySDDisk(filename);
  }

  public void fsck(final String file) throws IOException {

    final FilesystemCheck fsck = new FilesystemCheck(filename);
    fsck.checkDirectoryEntries();
  }

  public void insert(final String file, final String writeDisk) throws IOException {
    final HighLevelDisk disk = new HighLevelDisk(filename);
    disk.insert(".", file);
    String diskfile;
    if ("".equals(writeDisk)) {
      LOGGER.warn("No name given, will write to testdisk.atr");
      diskfile = "testdisk.atr";
    }
    else {
      diskfile = writeDisk;
    }
    LOGGER.info("write new disk: {}", diskfile);
    disk.getDiskette().write(diskfile);
  }

  public static void usage() {
    LOGGER.info("Usage:");
    LOGGER.info("java -jar xldir [diskfile] [command] [file]");
    LOGGER.info("Helper program, to handle ATR files in a simple batchable way.");
    LOGGER.info("");
    LOGGER.info("Commands:");
    LOGGER.info("list (all):      list normal files, if 'all' given, show also deleted files.");
    LOGGER.info("fsck:            check disk integrity. All directory entries, there places on disk, free and used sectors.");
    LOGGER.info("extract (file):  extract 'file' from disk, if 'all', extract all also deleted files.");
    LOGGER.info("hexdump:         output a hex dump of all sectors");
    LOGGER.info(
        "insert filelist newdisk: insert all files out of the given file list into the given disk but write it to a new disk image. The first given disk file will not change.");
    LOGGER.info("");
    LOGGER.info("makedd:          create an new empty disk of type DD (256 bytes per sector, 18 per track)");
    LOGGER.info("makeed:          create an new empty disk of type ED (128 bytes per sector, 26 per track)");
    LOGGER.info("makesd:          create an new empty disk of type SD (128 bytes per sector, 18 per track)");
    LOGGER.info("make will not overwrite a existing file.");
  }

  public static void main(final String[] args) throws IOException {
    if (args.length < 1) {
      LOGGER.error("No parameter given");
      usage();
      return;
    }

    final String diskname = args[0];
    final String command = args.length > 1
        ? args[1]
        : "list";

    final String file = args.length > 2
        ? args[2]
        : "";

    LOGGER.info("Disk name: {} command:{} file:{}", diskname, command, file);

    final Main main = new Main(diskname);
    try {
      switch (command) {
        case "list":
          checkDiskname(diskname);
          main.showDirectory(file);
          break;

        case "extract":
          checkDiskname(diskname);
          main.extract(file);
          break;

        case "hexdump":
          checkDiskname(diskname);
          main.hexdump(file);
          break;

        case "makedd":
          main.makeDD();
          break;

        case "makeed":
          main.makeED();
          break;

        case "makesd":
          main.makeSD();
          break;

        case "fsck":
          checkDiskname(diskname);
          main.fsck(file);
          break;

        case "insert":
          checkDiskname(diskname);
          final String writeDisk = args.length > 3
              ? args[3]
              : "";

          main.insert(file, writeDisk);
          break;
      }

      LOGGER.info("Done.");
    }
    catch (final FileNotFoundException e) {
      LOGGER.error("ERROR: {}", e.getMessage());
      throw new FileNotFoundException(e.getMessage());
    }
    catch (final IOException e) {
      LOGGER.error("ERROR: {}", e.getMessage());
      throw new IOException(e);
    }
    catch (final IllegalArgumentException e) {
      LOGGER.error("ERROR: {}", e.getMessage());
      throw new IllegalArgumentException(e);
    }
  }

  private static void checkDiskname(final String diskname) throws FileNotFoundException {
    final File diskfile = new File(diskname);
    if (!diskfile.exists()) {
      LOGGER.error("Given disk file: '{}' doesn't exists.", diskfile.getAbsoluteFile());
      throw new FileNotFoundException("Given disk file doesn't exist.");
    }

  }
}

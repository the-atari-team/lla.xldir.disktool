package lla.privat.atarixl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enthält höhere Funktionen auf der Diskette, kann Dateien im Filesystem lesen
 *
 * @author lars
 *
 */
abstract public class Filesystem {

  private final static Logger LOGGER = LoggerFactory.getLogger(Filesystem.class);

  protected final Diskette diskette;

  private final int vtocInfoPosition;
  private final int vtocId;
  private final int allOverSektors;

  private final FileExtensionConverter fileConverter;

  final static int SEKTOR_OF_DIRECTORY = 361;
  final static int SEKTOR_COUNT_OF_DIRECTORY = 8;

  public Filesystem(final Diskette diskette) {
    this.diskette = diskette;

    vtocInfoPosition = diskette.getSektorPositionInFulldisk(360);
    if (vtocInfoPosition < diskette.getDiskSize()) {
      vtocId = ByteUtils.getByte(diskette.readFromDisk(vtocInfoPosition, IOType.VTOC_INFO));
      // TODO hier mal etwas Schmalz versenken, damit dieser Wert aktualisiert werden kann.
      allOverSektors = ByteUtils.getWord(diskette.readFromDisk(vtocInfoPosition + 1, IOType.VTOC_INFO),
          diskette.readFromDisk(vtocInfoPosition + 2, IOType.VTOC_INFO));

    }
    else {
      vtocId = 0;
      allOverSektors = 0;
      LOGGER.warn("This disk has no real filesystem, it is to small for directory entries.");
    }
    fileConverter = new FileExtensionConverter();
  }

  public int getAllOverSektors() {
    return allOverSektors;
  }

  // TODO: get/set FreeSektors has to be rewritten


  abstract public void setFreeSektors(final int freeSektors);

  abstract public int getFreeSektors();
  
  abstract public int calculateFreeSektors();
  
  public int getVtocId() {
    return vtocId;
  }

  protected int getSektorOfVtocInfo() {
    return 360; // SEKTOR_OF_VTOC
  }

  public int getSektorOfDirectory() {
    return SEKTOR_OF_DIRECTORY;
  }

  public int getSektorCountOfDirectory() {
    return SEKTOR_COUNT_OF_DIRECTORY;
  }

  public Status getFileStatus(final DirectoryEntry entry) {
    int sektor = entry.getStartsektor();
    int count = entry.getCount();
    final int index = entry.getIndex();

    final byte[] fileContentSectorSized = new byte[count * diskette.getSektorSize()];
    final Status status = new Status();
    if (sektor < 4) {
      status.addStatus("Start sector too low (<4)");
    }
    else {

      try {
        int size = 0;
        int nextSektor;
        do {
          final int contentSize = getContentSizeOfSektor(sektor);
          // Nicht die feine Art gleich die komplette Datei im Array abzulegen, aber die
          // Dateien sind so klein (max 180kb)
          // das es den Aufwand nicht lohnt hier besseres zu erstellen.
          copyFromDisk(sektor, contentSize, fileContentSectorSized, size);
          size += contentSize;

          nextSektor = getNextSektorOfSektorByVtoc(sektor, index);
          --count;
          if (count < 0) {
            break;
          }
          sektor = nextSektor;
        }
        while (nextSektor != 0);
        if (count == 0) {
          status.setSize(size);
          final byte[] realSized = new byte[size];
          for (int i = 0; i < size; i++) {
            realSized[i] = fileContentSectorSized[i];
          }
          status.setFileContent(realSized);
        }
        else if (count > 0) {
          status.addStatus("Count not ended.");
        }
        else {
          status.addStatus("Count < 0, file defect.");
        }
      }
      catch (final IOException e) {
        status.addStatus(e.getMessage());
      }
    }
    return status;
  }

  public void copyFromDisk(final int startSektor, final int contentSize, final byte[] fileContent, final int index) {

    final int startPosition = diskette.getSektorPositionInFulldisk(startSektor);
    for (int i = 0; i < contentSize; i++) {
      final byte readFromDisk = diskette.readFromDisk(startPosition + i, IOType.DATA);
      try {
        fileContent[index + i] = readFromDisk;
      }
      catch (final ArrayIndexOutOfBoundsException e) {
        LOGGER.error("something stupid has happen:", e);
      }
    }
  }

  public void copyToDisk(final int startSektor, final int contentSize, final byte[] fileContent, final int index) {

    final int startPosition = diskette.getSektorPositionInFulldisk(startSektor);
    for (int i = 0; i < contentSize; i++) {
      diskette.writeToDisk(startPosition + i, fileContent[index + i], IOType.DATA);
    }
  }

  protected boolean is1050Mode() {
    return diskette.getSektorsOfTrack() == 26
        ? true
        : false;
  }


  abstract protected boolean isSektorInUse(final int sektor);

  abstract public boolean isSektorFree(final int sektor);
  
  abstract public void setSektorAsUsed(final int sektor);

  abstract public void setSektorAsFree(final int sektor);
  
  abstract public int findFirstFreeSektor() throws IOException;
  
  abstract public int findNextFreeSektor(int oldsektor) throws IOException;
  
  public int getContentSizeOfSektor(final int sektornummer) {
    final int size = ByteUtils.getByte(diskette.readFromDisk(sektornummer, diskette.getSektorSize() - 1, IOType.DATA_CONTENT_SIZE));
    return size;
  }

  /**
   * Holt aus einem Sektor den nächsten möglichen Sektor
   *
   * Ist dieser 0, ist die Datei hier zu ende Bei vtocId == 2 wird zusätzlich die
   * entryPosition geprüft ob es sich überhaupt um die richtige Datei handelt
   *
   * @param sektornummer
   *          4 - 1024
   * @param entryPosition
   *          0-63
   * @return nächster möglicher Sektor, 0 falls die Datei hier zu Ende ist.
   * @throws IOException
   * @throws IllegalArgumentException,
   *           falls vtoc nicht 2 oder 7 ist
   */
  public int getNextSektorOfSektorByVtoc(final int sektornummer, final int entryPosition) throws IOException {
    final int position = diskette.getSektorPositionInFulldisk(sektornummer) + diskette.getSektorSize();
    final int a = ByteUtils.getByte(diskette.readFromDisk(position - 3, IOType.DATA_NEXT_SECTOR));
    final int b = ByteUtils.getByte(diskette.readFromDisk(position - 2, IOType.DATA_NEXT_SECTOR));

    if (vtocId == 2) {
      if (a / 4 == entryPosition) {
        return b + 256 * (a & 3);
      }
      LOGGER.error("Error: Given Entry number {} is not equal to found entry {}, file is defect.", entryPosition, a / 4);
      throw new IOException("Error: Given entry number (" + entryPosition + ") is not equal to found entry (" + a / 4 + "), file is defect.");
    }
    if (vtocId == 3) {
      return b + 256 * (a & 31);
    }
    // on Megadisk!
    if (vtocId == 7) {
      return b + 256 * (a & 31);
    }
    // LiteDOS
    // TODO: fuer < sektor 1024 soll angeblich auch die entry position gesetzt sein.
    if (vtocId > 128) {
        return b + 256 * (a & 3);
    }
    throw new IllegalArgumentException("vtocId (" + vtocId + ") is not supported.");
  }

  public void extractSingleFile(final DirectoryEntry entry, final boolean alsoDeletedFiles) throws IOException {
    final int flag = entry.getFlag();

    String filenameMarker = "";
    if ((flag & 128) == 128) {
      filenameMarker = ".deleted";

      if (!alsoDeletedFiles) {
        LOGGER.info("Do not write deleted file.");
        return;
      }
    }

    if (entry.getStatus().getErrorCount() != 0) {
      throw new IOException("There exist errors. Will not create file. " + entry.getStatus());
    }

    try {
      final String filename = entry.getFilename() + filenameMarker;
      final File file = new File(filename);

      if (!file.exists()) {
        final byte[] convertedFileContent = fileConverter.convertToUnix(entry.getStatus().getFileContent(), entry.getExtension());

        final FileOutputStream stream = new FileOutputStream(file);
        stream.write(convertedFileContent, 0, convertedFileContent.length);
        stream.close();
      }
      else {
        LOGGER.warn("File with same name already exists.");
      }
    }
    catch (final IOException e) {
      LOGGER.warn("Can't create Directory: {} ", diskette.getFilename());
    }
  }

  public void createSektorHeader(final int sektor, final int nextSektor, final int index, final int countBytes) {

    final int startPosition = diskette.getSektorPositionInFulldisk(sektor) + diskette.getSektorSize();
    final int high = nextSektor / 256;
    final int low = nextSektor & 0b11111111;

    final int highAndIndex = high & 3 | index * 4;

    diskette.writeToDisk(startPosition - 3, (byte) highAndIndex, IOType.DATA_NEXT_SECTOR);
    diskette.writeToDisk(startPosition - 2, (byte) low, IOType.DATA_NEXT_SECTOR);
    diskette.writeToDisk(startPosition - 1, (byte) countBytes, IOType.DATA_CONTENT_SIZE);
  }

  public static class FreeUsed {

    public final int free;
    public final int used;

    public FreeUsed(final int free, final int used) {
      this.free = free;
      this.used = used;
    }
  }

  public FreeUsed showFreeAndUsedTracks(final Filesystem filesystem) {

    int free = 0;
    int used = 0;
    LOGGER.info("Every line is a track, a star shows sector is in use.");
    // final Diskette diskette = filesystem.getDiskette();
    diskette.setDebugOff();
    StringBuilder sectors=new StringBuilder();
    for (int sektor = 0; sektor < diskette.getSektorsOfTrack(); sektor++) {
      for (int track = 0; track < diskette.getTracks(); track++) {
        final int disksektor = track * diskette.getSektorsOfTrack() + sektor + 1;
        if (disksektor >= diskette.getUseableSektors()) {
          sectors.append("X");
        }
        else {
          if (filesystem.isSektorFree(disksektor)) {
            sectors.append(".");
            free++;
          }
          else {
            sectors.append("*");
            used++;
          }
        }
      }
      sectors.append("\n");
    }
    LOGGER.info("{}", sectors.toString());
    return new FreeUsed(free, used);
  }

}

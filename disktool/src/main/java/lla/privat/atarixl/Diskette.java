package lla.privat.atarixl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Low Level Funktionalität auf einer Diskette - Hält die Daten der Diskette -
 * Kennt die Größe der Sektoren - Anzahl der Sektoren - Namen auf der Linux
 * Platte - Kann eine Diskette komplett einlesen - Kann eine Diskette komplett
 * schreiben
 *
 * @author develop
 *
 */
public class Diskette {

  private static Logger LOGGER = LoggerFactory.getLogger(Diskette.class);

  private final String filename;

  private int sektors;
  private int useableSektors;
  private int sektorsize;
  private int disksize;

  private int tracks;
  private int sektorsOfTrack;

  private byte[] wholeDisk;
  private boolean fatBootsector;
  
  private boolean isDebug = false;

  public Diskette(final String name) {
    this.filename = name;
  }

  public String getFilename() {
    return filename;
  }

  public int getSektors() {
    return sektors;
  }

  public int getUseableSektors() {
    return useableSektors;
  }

  public int getSektorSize() {
    return sektorsize;
  }

  // private byte[] getFulldisk() {
  // return fulldisk;
  // }

  public void writeToDisk(final int sektor, final int positionInSektor, final byte value, final IOType artOfWrite) {
    writeToDisk(calculatePosition(sektor, positionInSektor), value, artOfWrite);
  }

  public void writeToDisk(final int position, final byte value, final IOType artOfWrite) {
    if (isDebug) {
      final int sektor = position / sektorsize + 1;
      final int index = position & sektorsize - 1;

      LOGGER.info("write to disk! Sektor {} index {} {}", sektor, index, artOfWrite);
    }
    wholeDisk[position] = value;
  }

  public byte readFromDisk(final int sektor, final int positionInSektor, final IOType artOfRead) {
    return readFromDisk(calculatePosition(sektor, positionInSektor), artOfRead);
  }

  public void setDebugOn() {
    isDebug = true;
  }

  public void setDebugOff() {
    isDebug = false;
  }

  public byte readFromDisk(final int position, final IOType artOfRead) {
    if (isDebug) {
      final int sektor = position / sektorsize + 1;
      final int index = position & sektorsize - 1;

      LOGGER.info("Read from disk. Sektor {} index {} {}", sektor, index, artOfRead);
    }
    return wholeDisk[position];
  }

  private int calculatePosition(final int sektor, final int positionInSektor) {
    return (sektor - 1) * sektorsize + positionInSektor;
  }

  public int getDiskSize() {
    return wholeDisk.length;
  }

  public int getTracks() {
    return tracks;
  }

  public int getSektorsOfTrack() {
    return sektorsOfTrack;
  }

  public void show() {
    LOGGER.info("Disk name: {} size: {}",filename, disksize);
  }

  public void makeEmptyDDDisk(final String newFilename) throws IOException {
    sektorsize = 256;
    sektorsOfTrack = 18;
    fatBootsector = false;
    makeEmptyDisk(newFilename);
  }

  public void makeEmptyEDDisk(final String newFilename) throws IOException {
    sektorsize = 128;
    sektorsOfTrack = 26;
    makeEmptyDisk(newFilename);
  }

  public void makeEmptySDDisk(final String newFilename) throws IOException {
    sektorsize = 128;
    sektorsOfTrack = 18;
    makeEmptyDisk(newFilename);
  }

  private void makeEmptyDisk(final String newFilename) throws IOException {
    tracks = 40;
    sektors = sektorsOfTrack * tracks;
    disksize = sektors * sektorsize;
    useableSektors = sektors;

    wholeDisk = new byte[disksize];

    final boolean written = write(newFilename);

    if (written) {
      LOGGER.info("Empty disk created. {} sectors, {} bytes per sector", sektors, sektorsize);
    }
  }

  private void sleepInSeconds(final int milliseconds) {
    try {
      Thread.sleep(milliseconds);
    }
    catch (final InterruptedException e) {
    }
  }

  public boolean write(final String diskname) throws IOException {
    try {
      final File file = new File(diskname);

      if (file.exists()) {
        file.delete();
        LOGGER.info("Disk {} already exist. Will delete it.", diskname);
        sleepInSeconds(50);
      }

      if (!file.exists()) {
        final FileOutputStream stream = new FileOutputStream(file);
        // TODO: create a right header!
        if (diskname.toLowerCase().endsWith(".atr")) {
          final byte[] header = new byte[16];
          header[0] = (byte) 0x96; // nick
          header[1] = (byte) 0x02;

          int pages = wholeDisk.length / 16;
          // HACK
          if (sektorsize == 256) {
        	  if (sektorsOfTrack == 18) {
        		  if (fatBootsector == true) {
        			  pages = disksize / 16;
        		  }
        		  else {
                	  pages = (717 * sektorsize + 3 * 128) / 16;
        		  }
        	  }
          }
          if (sektorsOfTrack == 2*18) {
    		  if (fatBootsector == true) {
    			  pages = disksize / 16;
    		  }
    		  else {
            	  pages = ((1440 - 3) * sektorsize + 3 * 128) / 16;
            	  // pages = 0x59e8;
    		  }
          }
          int low = pages & 0b11111111;
          int high = pages / 256;
          header[2] = (byte) low;
          header[3] = (byte) high;

          low = sektorsize & 0b11111111;
          high = sektorsize / 256;
          header[4] = (byte) low;
          header[5] = (byte) high;

          stream.write(header, 0, header.length);
        }
        if (sektorsize == 256 && fatBootsector == false) {
            stream.write(wholeDisk, 0, 128);
            stream.write(wholeDisk, 256, 128);
            stream.write(wholeDisk, 512, 128);
            stream.write(wholeDisk, 768, sektors * sektorsize - 768);
        }
        else {
        	stream.write(wholeDisk, 0, wholeDisk.length);
        }
        stream.close();
        LOGGER.info("Disk {} written.", diskname);
      }
      else {
        LOGGER.warn("Disk with same name already exists.");
        return false;
      }
    }
    catch (final IOException e) {
      LOGGER.warn("Can't create Disk: {} ", diskname);
      return false;
    }
    return true;
  }

  public void hexdump(final String diskname) throws IOException {
    if (diskname == null || "".equals(diskname)) {
      throw new IllegalArgumentException("No disk name for hex dump given.");
    }

    try {
      final File file = new File(diskname);

      if (file.exists()) {
        LOGGER.error("hex dump file {} already exist. Will not overwritten.", diskname);
        throw new IOException("hex dump file exist.");
      }

      final byte[] hex = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
      if (!file.exists()) {
        final FileOutputStream stream = new FileOutputStream(file);
        // TODO: create a right header!
        for (int sektor = 0; sektor < getSektors(); sektor++) {
          final int sektorPos = sektor * getSektorSize();

          final String sektorAsString = "Sektor " + (sektor + 1) + "\n";
          sektorAsString.chars().forEach(a -> {
            try {
              stream.write((byte) a);
            }
            catch (final IOException e) {
            }
          });

          final int n = 32;
          for (int pos = 0; pos < getSektorSize(); pos += n) {

            final StringBuilder line = new StringBuilder();
            line.append("  ");
            for (int i = 0; i < 32; i++) {
              final int value = ByteUtils.getByte(wholeDisk[sektorPos + pos + i]);
              if (value >= ' ' && value <= 'z') {
                line.append((char) value);
              }
              else {
                line.append('.');
              }
              final byte highHex = hex[value / 16];
              final byte lowHex = hex[value & 0b1111];
              stream.write(highHex);
              stream.write(lowHex);
              stream.write((byte) ' ');
            }
            line.toString().chars().forEach(a -> {
              try {
                stream.write((byte) a);
              }
              catch (final IOException e) {
              }
            });

            stream.write((byte) 0xa);
          }

          stream.write((byte) 0xa);
        }

        stream.close();
      }
      else {
        LOGGER.warn("Disk with same name already exists.");
      }
    }
    catch (final IOException e) {
      LOGGER.warn("Can't create Disk: {} ", diskname);
    }
  }

  public void read() throws IOException {
    final File file = new File(filename);

    if (!file.exists()) {
      final String absolutePath = file.getAbsolutePath();
      LOGGER.info("Search in absolute path: {}", absolutePath);
      throw new FileNotFoundException("Can't find file: " + filename);
    }

    int header = 0;
    if (filename.toLowerCase().endsWith(".atr")) {
      LOGGER.debug("Assume ATR Format");
      header = 16;
    }
    final int bytes = (int) file.length();
    LOGGER.info("Disk size in bytes: {}", bytes);
//    final int filesize = bytes;

    final byte headerBuffer[] = new byte[16];

    tracks = 40;

    // try to realize the size values via file size
    sektorsize = 128;
    sektorsOfTrack = 18;
    sektors = sektorsOfTrack * tracks;
    if (bytes - header > 720 * 256 + 18) {    	
        LOGGER.info("Disk size is too big, will not supported: {}", bytes);
    	System.exit(1);
    }
    if (bytes - header >= ((720 - 3) * 256) + 3 * 128 && bytes-header <= 720*256+1) {
      sektorsize = 256;
      disksize = sektors * sektorsize;
      useableSektors = sektors;
    }
    else if (bytes - header >= 1040 * 128 && bytes-header <= 1040*128+1) {
      sektorsize = 128;
      sektorsOfTrack = 26;
      sektors = sektorsOfTrack * tracks;
      disksize = sektors * sektorsize;
      useableSektors = 1024;
    }
    else if (bytes - header >= 720 * 128 && bytes-header <= 720*128 + 1) {
      sektorsize = 128;
      sektorsOfTrack = 18;
      sektors = sektorsOfTrack * tracks;
      disksize = sektors * sektorsize;
      useableSektors = sektors;
    }
    else if (header == 16) {
      LOGGER.warn("File size unknown, try to use ATR header");
    }
    else {
      LOGGER.error("Unknown disk format is currently unsupported");
      throw new IOException("File format unsupported");        	  
    }
    
    wholeDisk = new byte[disksize];

    try (FileInputStream inputStream = new FileInputStream(file)) {
      if (header > 0) {
        inputStream.read(headerBuffer, 0, header);
        final int nick = ByteUtils.getWord(headerBuffer[0], headerBuffer[1]);
        if (nick != 0x296) {
          throw new IOException("Header wrong ('nick' 0x296 expected)");
        }
        else {
          final int pages = ByteUtils.getWord(headerBuffer[2], headerBuffer[3]);
//          final int size = pages * 0x10;

          sektorsize = ByteUtils.getWord(headerBuffer[4], headerBuffer[5]);
//          sektors = disksize / sektorsize;
//          useableSektors = sektors == 1040
//              ? 1024
//              : sektors;
          // final int highpages = ByteUtils.getWord(headerBuffer[6], headerBuffer[7]);
          if ((pages * 16) > (bytes - header) ) {
        	  LOGGER.error("ATR-Pages * 16 " + pages * 16 + " passen nicht, sollten: " + ( bytes - header ) + " sein!");
              throw new IOException("Pages wrong");        	  
          }
          LOGGER.debug("ATR-Pages * 16 {} <= {} passen",pages * 16,  (bytes - header));
        }
      }
      if (bytes < disksize) {
        // Hat wieder ein schlaumeier die Bootsektoren verkleinert :-(
    	fatBootsector=false;
    	LOGGER.info("read boot sectors separate (small 128b boot sectors)");
        inputStream.read(wholeDisk, 0, 128);
        inputStream.read(wholeDisk, 256, 128);
        inputStream.read(wholeDisk, 512, 128);
        
        final int bytesToRead = (useableSektors - 3) * sektorsize;
        inputStream.read(wholeDisk, 768, bytesToRead);
      }
      else {
      	LOGGER.info("read full disk. (fat boot sectors)");
      	fatBootsector = true;
        inputStream.read(wholeDisk, 0, disksize);
      }
    }
    catch (final IOException e) {
      LOGGER.error("ERROR: {}", e.getMessage());
      System.exit(3);
    }
  }

  public void extractDiskWithoutBootsektor(final String filename) {
    try {
      final File file = new File(filename);

      if (!file.exists()) {
        final FileOutputStream stream = new FileOutputStream(file);
        final int bootsektorsize = 384;
        stream.write(wholeDisk, bootsektorsize, wholeDisk.length - bootsektorsize);
        stream.close();
      }
      else {
        LOGGER.warn("File with same name already exists.");
      }
    }
    catch (final IOException e) {
      LOGGER.warn("Can't create file: {} ", filename);
    }
  }

  public int getSektorPositionInFulldisk(final int sektornummer) {
	    return (sektornummer - 1) * getSektorSize();
  }


}

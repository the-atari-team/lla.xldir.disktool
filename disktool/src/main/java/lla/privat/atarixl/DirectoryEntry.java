package lla.privat.atarixl;

import java.io.IOException;

public class DirectoryEntry {

  private String filename;
  private String extension;

  private final String filenameNotTrimed;
  private int sektorCount;
  private int startsektor;
  private int flag;
  private final int index;
  private Status status;

  public DirectoryEntry(final int flag, final int count, final int startsektor, final int index, final String filenameNotTrimed) {
    this.flag = flag;
    this.sektorCount = count;
    this.startsektor = startsektor;
    this.index = index;
    this.filenameNotTrimed = filenameNotTrimed;
    final String[] split = filenameNotTrimed.split("\\.");
    if (split.length == 0) {
      filename = "";
    }
    else if (split.length == 1) {
      filename = split[0].trim() + ".   ";
    }
    else {
      extension = split[1].trim();
      filename = split[0].trim() + "." + extension;
    }
  }

  public void setStatus(final Status status) {
    this.status = status;
  }

  public Status getStatus() {
    return status;
  }

  public int getIndex() {
    return index;
  }

  public String getFilename() {
    return filename;
  }

  public String getExtension() {
    return extension;
  }

  public void setFilename(final String filename) throws IOException {
    if (filename.length() > 8 + 1 + 3) {
      throw new IOException("Name too long");
    }
    final String[] splittedFilename = filename.split("\\.");
    if (splittedFilename[0].length() > 8) {
      throw new IOException("First name too long");
    }
    if (splittedFilename.length > 1 && splittedFilename[1].length() > 3) {
      throw new IOException("Extension Name too long");
    }
    this.filename = filename;
  }

  public String getFilenameNotTrimed() {
    return filenameNotTrimed;
  }

  public int getCount() {
    return sektorCount;
  }

  public void setCount(final int count) {
    this.sektorCount = count;
  }

  public int getStartsektor() {
    return startsektor;
  }

  public void setStartSektor(final int startsektor) {
    this.startsektor = startsektor;
  }

  public int getFlag() {
    return flag;
  }

  public void setFlag(final int flag) {
    this.flag = flag;
  }
}

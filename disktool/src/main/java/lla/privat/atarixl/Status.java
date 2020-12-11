package lla.privat.atarixl;

public class Status {

  private final StringBuilder statusText;
  private int errorCount;
  private int fileSize;
  private byte[] fileContent;

  public Status() {
    statusText = new StringBuilder();
    errorCount = 0;
    fileSize = 0;
  }

  public void addStatus(final String statusText) {
    if (statusText.length() == 0) {
      return;
    }
    if (this.statusText.length() > 0) {
      this.statusText.append(", ");
    }
    this.statusText.append(statusText);
    ++errorCount;
  }

  public void setSize(final int size) {
    this.fileSize = size;
  }

  public int getSize() {
    return fileSize;
  }

  public int getErrorCount() {
    return errorCount;
  }

  public int getFileSize() {
    return fileSize;
  }

  public void setFileContent(final byte[] fileContent) {
    this.fileContent = fileContent;
  }

  public byte[] getFileContent() {
    return fileContent;
  }

  @Override
  public String toString() {
    return errorCount == 0
        ? "OK"
        : statusText.toString();
  }
}

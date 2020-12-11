package lla.privat.atarixl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileExtensionConverter {

  private final static Logger LOGGER = LoggerFactory.getLogger(FileExtensionConverter.class);

  public FileExtensionConverter() {
  }

  public byte[] convertFromUnix(final byte[] content, final String extension) {
    if ("lst".equalsIgnoreCase(extension)) {
      return new FileLSTExtensionConverter().convertFromUnix(toList(content));
    }
    if ("txt".equalsIgnoreCase(extension)) {
      return new FileTXTExtensionConverter().convertFromUnix(toList(content));
    }
    if ("wnf".equalsIgnoreCase(extension)) {
      return new FileTXTExtensionConverter().convertFromUnix(toList(content));
    }

    LOGGER.warn("Given Extension {} is not supported, do not convert.", extension);
    return content;
  }

  public byte[] convertToUnix(final byte[] content, final String extension) {
    if ("lst".equalsIgnoreCase(extension)) {
      return new FileLSTExtensionConverter().convertToUnix(toList(content));
    }
    if ("txt".equalsIgnoreCase(extension)) {
      return new FileTXTExtensionConverter().convertToUnix(toList(content));
    }
    if ("wnf".equalsIgnoreCase(extension)) {
      return new FileTXTExtensionConverter().convertToUnix(toList(content));
    }
    if ("asm".equalsIgnoreCase(extension)) {
      return new FileTXTExtensionConverter().convertToUnix(toList(content));
    }
    LOGGER.warn("Given Extension {} is not supported, do not convert.", extension);
    return content;
  }

  public static List<Byte> toList(final byte[] content) {
    final List<Byte> contentAsList = new ArrayList<>();

    for (int i = 0; i < content.length; i++) {
      final byte value = content[i];
      contentAsList.add(value);
    }
    return contentAsList;
  }

  public static byte[] toArray(final List<Byte> byteList) {
    final byte[] newContent = new byte[byteList.size()];

    for (int i = 0; i < byteList.size(); i++) {
      final byte value = ByteUtils.toByte(byteList.get(i));
      newContent[i] = value;
    }
    return newContent;
  }
}

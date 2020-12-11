package lla.privat.atarixl;

import java.util.ArrayList;
import java.util.List;

public class FileTXTExtensionConverter implements IFileExtensionConverter {

  public FileTXTExtensionConverter() {
  }

  @Override
  public byte[] convertFromUnix(final List<Byte> content) {
    final List<Byte> newByteStream = new ArrayList<>();

    for (int i = 0; i < content.size(); i++) {
      byte value = content.get(i);

      // Windows line endings with 0x0d 0x0a
      if (value == (byte) 0x0d) {
        if (content.get(i + 1) == 0x0a) {
          i++;
          value = content.get(i);
        }
      }

      if (value == (byte) 0x0a) {
        value = (byte) 0x9b;
      }
      newByteStream.add(value);
    }
    return FileExtensionConverter.toArray(newByteStream);
  }

  // replace 0x9b by 0xa (Atari Carrige Return) to unix new line

  @Override
  public byte[] convertToUnix(final List<Byte> content) {
    final List<Byte> newByteStream = new ArrayList<>();

    for (int i = 0; i < content.size(); i++) {
      byte value = content.get(i);

      if (value == (byte) 0x9b) {
        value = 0x0a;
      }
      newByteStream.add(value);
    }

    return FileExtensionConverter.toArray(newByteStream);
  }

}

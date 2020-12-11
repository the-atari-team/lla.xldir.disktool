package lla.privat.atarixl;

import java.util.ArrayList;
import java.util.List;

public class FileLSTExtensionConverter implements IFileExtensionConverter {

  public FileLSTExtensionConverter() {
  }

  @Override
  public byte[] convertFromUnix(final List<Byte> content) {
    final List<Byte> newByteStream = new ArrayList<>();
    boolean startNewLine = true;
    boolean isRemark = false;
    int lineNumber = 1;

    for (int i = 0; i < content.size(); i++) {
      byte value = content.get(i);

      if (startNewLine) {
        // overread a '!' at line position 0, insert without line numbers
        if (value == (byte) '!') {
          startNewLine = false;
          continue;
        }

        // overread empty lines
        if (value == (byte) 0x0a) {
          continue;
        }
        if (value == (byte) 0x0d) {
          if (content.get(i + 1) == 0x0a) {
            i++;
            continue;
          }
        }
        if (value == (byte) 0x09) {
          // overread tab key
          continue;
        }
      }

      if (startNewLine) {
        final String lineNumberAsString = String.valueOf(lineNumber);
        for (int j = 0; j < lineNumberAsString.length(); j++) {
          newByteStream.add(ByteUtils.toByte(lineNumberAsString.charAt(j)));
        }
        newByteStream.add((byte) ' ');

        lineNumber++;
        startNewLine = false;
      }

      // Windows line endings with 0x0d 0x0a
      if (value == (byte) 0x0d) {
        if (content.get(i + 1) == 0x0a) {
          i++;
          value = content.get(i);
        }
      }

      if (value == (byte) 0x0a) {
        value = (byte) 0x9b;
        startNewLine = true;
        isRemark = false;
      }

      if (value == (byte) ':') {
        if (content.get(i + 1) == 'R' && content.get(i + 2) == 'E' && content.get(i + 3) == 'M' && content.get(i + 4) == ' ') {
          isRemark = true;
        }
      }

      if (value == (byte) 'R') {
          if (content.get(i + 1) == 'E' && content.get(i + 2) == 'M' && content.get(i + 3) == ' ') {
            isRemark = true;
          }
        }

      if (!isRemark) {
        newByteStream.add(value);
      }
    }
    return FileExtensionConverter.toArray(newByteStream);
  }

  // replace 0x9b by 0xa (Atari Carrige Return) to unix new line
  // replace line numbers
  @Override
  public byte[] convertToUnix(final List<Byte> content) {
    boolean startNewLine = true;
    boolean inString = false;

    final List<Byte> newByteStream = new ArrayList<>();
    byte valueOld = 0;
    byte valueOlder = 0;
    boolean remZeile = false;

    for (int i = 0; i < content.size(); i++) {
      byte value = content.get(i);

      if (startNewLine) {
        if (value >= '0' && value <= '9') {
          continue;
        }
        if (value == ' ') {
          startNewLine = false;
          continue;
        }
      }

      if (inString) {
        if (value == '"') {
          inString = false;
        }
      }
      else {
        // not in String
        if (value == '"') {
          inString = true;
        }

        if (value == (byte) 77 && valueOld == (byte) 69 && valueOlder == (byte) 82) {
          remZeile = true;
        }
        if (value == (byte) 0x9b) {
          value = 0x0a;
          startNewLine = true;
          remZeile = false;
        }
      }
      if (remZeile) {
        value = (byte) (value & 0x7f);
      }
      newByteStream.add(value);
      valueOlder = valueOld;
      valueOld = value;
    }

    return FileExtensionConverter.toArray(newByteStream);
  }

}

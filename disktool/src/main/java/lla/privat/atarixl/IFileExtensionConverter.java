package lla.privat.atarixl;

import java.util.List;

public interface IFileExtensionConverter {

  byte[] convertFromUnix(final List<Byte> content);

  byte[] convertToUnix(final List<Byte> content);

}

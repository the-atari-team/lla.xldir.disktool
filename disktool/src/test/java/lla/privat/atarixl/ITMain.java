package lla.privat.atarixl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ITMain {

  @Before
  public void setUp() {
  }

  @Test
  public void test_no_parameter() throws IOException {
    Main.main(new String[0]);
  }

  @Test(expected = FileNotFoundException.class)
  public void test_with_not_exist_disk_parameter() throws IOException {
    final String[] params = {"schnubbel"};
    Main.main(params);
  }

  @Test
  public void test_with_disk_parameter() throws IOException {
    final String[] params = {"src/test/resources/lla/privat/atarixl/dos-sd.atr"};
    Main.main(params);
  }

  @Test
  public void test_with_disk_list_parameter() throws IOException {
    final String[] params = {"src/test/resources/lla/privat/atarixl/dos-sd.atr", "list"};
    Main.main(params);
  }

  @Test(expected = IllegalArgumentException.class)
  public void test_with_disk_extract_parameter() throws IOException {
    final String[] params = {"src/test/resources/lla/privat/atarixl/dos-sd.atr", "extract"};
    Main.main(params);
  }
  
  @Test
  public void test_with_disk_extract_file_parameter() throws IOException {
    final String[] params = {"src/test/resources/lla/privat/atarixl/dos-sd.atr", "extract", "playmisl.lst"};
    Main.main(params);

    final File file = new File("PLAYMISL.LST");
    if (!file.exists()) {
      throw new FileNotFoundException("PLAYMISL.LST does not exist.");
    }
  }

  @Test
  public void test_with_disk_insert_file_parameter() throws IOException {
    final File file = new File("testdisk.atr");
    if (file.exists()) {
      file.delete();
    }

    final String[] params = {"src/test/resources/lla/privat/atarixl/dos-sd.atr", "insert", "to-insert.txt"};
    Main.main(params);

    // we expect a new disk testdisk.atr will be created
    if (!file.exists()) {
      throw new FileNotFoundException("testdisk.atr does not exist.");
    }
    Assert.assertEquals(92176L, file.length());
  }

  @Test
  public void test_with_disk_insert_file_todisk_parameter() throws IOException {
    final File file = new File("test-new-disk.atr");
    if (file.exists()) {
      file.delete();
    }

    final String[] params = {"src/test/resources/lla/privat/atarixl/dos-sd.atr", "insert", "to-insert.txt", "test-new-disk.atr"};
    Main.main(params);

    // we expect a new disk testdisk.atr will be created
    if (!file.exists()) {
      throw new FileNotFoundException("new-disk.atr does not exist.");
    }
    Assert.assertEquals(92176L, file.length());

    final String[] paramsForList = {"test-new-disk.atr", "list"};
    Main.main(paramsForList);
  }

  @Test
  public void test_makesd() throws IOException {
    final String[] params = {"sd.atr", "makesd"};
    Main.main(params);
  }

  @Test
  public void test_makeed() throws IOException {
    final String[] params = {"ed.atr", "makeed"};
    Main.main(params);
  }

  @Test
  public void test_makedd() throws IOException {
    final String[] params = {"dd.atr", "makedd"};
    Main.main(params);
  }

  @Test
  public void test_hexdump() throws IOException {
    final String[] params = {"src/test/resources/lla/privat/atarixl/dos-sd.atr", "hexdump", "dos-sd.atr.hexdump"};
    Main.main(params);
  }

}

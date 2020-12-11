package lla.privat.atarixl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ITLiteDOS_ed {
	private String disk;

	@Before
	public void setUp() {
		disk = "src/test/resources/lla/privat/atarixl/LiteDOS-ed.atr";
		File file = new File(disk);
		Assert.assertTrue(file.exists());
	}

	@Test
	public void test_with_disk_parameter() throws IOException {
		final String[] params = { disk };
		Main.main(params);
	}

	@Test
	public void test_with_disk_list_parameter() throws IOException {
		final String[] params = { disk, "list" };
		Main.main(params);
	}

	@Test(expected = IllegalArgumentException.class)
	public void test_with_disk_extract_parameter() throws IOException {
		final String[] params = { disk, "extract" };
		Main.main(params);
	}

	@Ignore
	@Test
	public void test_with_disk_extract_file_parameter() throws IOException {
		final String[] params = { disk, "extract", "playmisl.lst" };
		Main.main(params);

		final File file = new File("PLAYMISL.LST");
		if (!file.exists()) {
			throw new FileNotFoundException("PLAYMISL.LST does not exist.");
		}
	}

	@Ignore
	@Test
	public void test_with_disk_insert_file_parameter() throws IOException {
		final File file = new File("testdisk.atr");
		if (file.exists()) {
			file.delete();
		}

		final String[] params = { disk, "insert", "to-insert.txt" };
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

		final String[] params = { disk, "insert", "to-insert.txt", "test-new-disk.atr" };
		Main.main(params);

		// we expect a new disk testdisk.atr will be created
		if (!file.exists()) {
			throw new FileNotFoundException("test-new-disk.atr does not exist.");
		}
		Assert.assertEquals(133136L, file.length());

		final String[] paramsForList = { "test-new-disk.atr", "list" };
		Main.main(paramsForList);
	}

	@Ignore
	@Test
	public void test_hexdump() throws IOException {
		final String[] params = { disk, "hexdump", "dos-sd.atr.hexdump" };
		Main.main(params);
	}

}

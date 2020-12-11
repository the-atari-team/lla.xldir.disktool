package lla.privat.atarixl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TestDiskette {

	private Diskette disketteSUT;

	@Before
	public void setUp() {
	}

	@Test
	public void testGetFilename() {
		disketteSUT = new Diskette("src/test/resources/lla/privat/atarixl/turbobasic-on-dd.atr");
		Assert.assertEquals("src/test/resources/lla/privat/atarixl/turbobasic-on-dd.atr", disketteSUT.getFilename());
	}

	@Test
	public void testParameter_dd() throws IOException {
		disketteSUT = new Diskette("src/test/resources/lla/privat/atarixl/turbobasic-on-dd.atr");
		disketteSUT.read();

		Assert.assertEquals(720, disketteSUT.getSektors());
		Assert.assertEquals(256, disketteSUT.getSektorSize());
		Assert.assertEquals(720 * 256, disketteSUT.getDiskSize());
		Assert.assertEquals(40, disketteSUT.getTracks());
		Assert.assertEquals(18, disketteSUT.getSektorsOfTrack());
	}

	@Test
	public void testParameter_ed() throws IOException {
		disketteSUT = new Diskette("src/test/resources/lla/privat/atarixl/dos-ed.atr");
		disketteSUT.read();

		Assert.assertEquals(1040, disketteSUT.getSektors());
		Assert.assertEquals(128, disketteSUT.getSektorSize());
		Assert.assertEquals(1040 * 128, disketteSUT.getDiskSize());
	}

	@Test
	public void testParameter_sd() throws IOException {
		disketteSUT = new Diskette("src/test/resources/lla/privat/atarixl/dos-sd.atr");
		disketteSUT.read();

		Assert.assertEquals(720, disketteSUT.getSektors());
		Assert.assertEquals(128, disketteSUT.getSektorSize());
		Assert.assertEquals(720 * 128, disketteSUT.getDiskSize());
	}

	@Test
	public void testShow() {
		disketteSUT = new Diskette("src/test/resources/lla/privat/atarixl/dos-sd.atr");
		disketteSUT.show();
	}

	@Test(expected = FileNotFoundException.class)
	public void testReadWithException() throws IOException {
		disketteSUT = new Diskette("file do not exist.xfd");
		disketteSUT.read();
	}

	@Test
	public void testmakeEmptySDDisk() throws IOException {
		final String filename = "sd-disk-test.atr";
		File file = new File(filename);
		if (file.exists())
			file.delete();

		Assert.assertFalse(file.exists());

		disketteSUT = new Diskette("");
		disketteSUT.makeEmptySDDisk(filename);

		Assert.assertTrue(file.exists());
		Assert.assertEquals(720 * 128 + 16, file.length());
	}

	@Test
	public void testmakeEmptyEDDisk() throws IOException {
		final String filename = "ed-disk-test.atr";
		File file = new File(filename);
		if (file.exists())
			file.delete();

		Assert.assertFalse(file.exists());

		disketteSUT = new Diskette("");
		disketteSUT.makeEmptyEDDisk(filename);

		Assert.assertTrue(file.exists());
		Assert.assertEquals(1040 * 128 + 16, file.length());
	}

	@Test
	public void testmakeEmptyDDDisk() throws IOException {
		final String filename = "dd-disk-test.atr";
		File file = new File(filename);
		if (file.exists())
			file.delete();

		Assert.assertFalse(file.exists());

		disketteSUT = new Diskette("");
		disketteSUT.makeEmptyDDDisk(filename);

		Assert.assertTrue(file.exists());
		Assert.assertEquals((720 - 3) * 256 + 3 * 128 + 16, file.length());
	}

}

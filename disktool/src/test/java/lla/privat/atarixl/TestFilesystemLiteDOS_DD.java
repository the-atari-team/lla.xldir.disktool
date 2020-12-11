package lla.privat.atarixl;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestFilesystemLiteDOS_DD {

	private FilesystemLiteDOS filesystemSUT;

	@Before
	public void setUp() throws IOException {
		final Diskette diskette = new Diskette("src/test/resources/lla/privat/atarixl/LiteDOS-DD-starter.atr");
		diskette.read();

		filesystemSUT = new FilesystemLiteDOS(diskette);
	}

	@Test
	public void testIsSektorFree() throws IOException {

		Assert.assertEquals(8, filesystemSUT.clustersize);
		Assert.assertEquals(1024, filesystemSUT.clusterInBytes);
		Assert.assertEquals(4, filesystemSUT.countOfSektors);

		Assert.assertEquals(720, filesystemSUT.getAllOverSektors());
		Assert.assertEquals(0x87, filesystemSUT.getVtocId());

		Assert.assertFalse(filesystemSUT.isSektorFree(0));
		Assert.assertFalse(filesystemSUT.isSektorFree(360));
		Assert.assertFalse(filesystemSUT.isSektorFree(720));

		Assert.assertTrue(filesystemSUT.isSektorFree(716));
		Assert.assertTrue(filesystemSUT.isSektorFree(712));
		Assert.assertTrue(filesystemSUT.isSektorFree(708));

		Assert.assertFalse(filesystemSUT.isSektorFree(8));
	}

	@Test
	public void testFindFirstFreeSektor() throws IOException {
		int sektor = filesystemSUT.findFirstFreeSektor();
		Assert.assertEquals(104, sektor);
	}

}

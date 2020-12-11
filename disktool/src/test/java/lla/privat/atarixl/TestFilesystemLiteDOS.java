package lla.privat.atarixl;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestFilesystemLiteDOS {

	private FilesystemLiteDOS filesystemSUT;

	@Before
	public void setUp() throws IOException {
		final Diskette diskette = new Diskette("src/test/resources/lla/privat/atarixl/LiteDOS-SD-V301.atr");
		diskette.read();

		filesystemSUT = new FilesystemLiteDOS(diskette);
	}

	@Test
	public void testIsSektorFree() throws IOException {

		Assert.assertEquals(4, filesystemSUT.clustersize);
		Assert.assertEquals(512, filesystemSUT.clusterInBytes);
		Assert.assertEquals(4, filesystemSUT.countOfSektors);

		Assert.assertEquals(720, filesystemSUT.getAllOverSektors());
		Assert.assertEquals(0x83, filesystemSUT.getVtocId());

		Assert.assertTrue(filesystemSUT.isSektorFree(716)); // The last 'f' in VTOC
		Assert.assertTrue(filesystemSUT.isSektorFree(712));
		Assert.assertTrue(filesystemSUT.isSektorFree(708));
		Assert.assertTrue(filesystemSUT.isSektorFree(704));

		Assert.assertTrue(filesystemSUT.isSektorFree(700)); // The pre last 'f' in VTOC
		Assert.assertTrue(filesystemSUT.isSektorFree(696));
		Assert.assertTrue(filesystemSUT.isSektorFree(692));
		Assert.assertTrue(filesystemSUT.isSektorFree(688));

		Assert.assertTrue(filesystemSUT.isSektorFree(684)); // The pre pre last 'f' in VTOC
		Assert.assertTrue(filesystemSUT.isSektorFree(680));
		Assert.assertTrue(filesystemSUT.isSektorFree(676));
		Assert.assertTrue(filesystemSUT.isSektorFree(672));

		Assert.assertTrue(filesystemSUT.isSektorFree(668)); // The pre pre pre last '7' in VTOC
		Assert.assertTrue(filesystemSUT.isSektorFree(664));
		Assert.assertTrue(filesystemSUT.isSektorFree(660));
		Assert.assertFalse(filesystemSUT.isSektorFree(656));

		Assert.assertFalse(filesystemSUT.isSektorFree(360));
	}

	@Test
	public void testFindFirstFreeSektor() throws IOException {
		int sektor = filesystemSUT.findFirstFreeSektor();
		Assert.assertEquals(660, sektor);
	}

	@Test
	public void testFindNextFreeSektor() throws IOException {
		int sektor = filesystemSUT.findFirstFreeSektor();
		int nextSektor = filesystemSUT.findNextFreeSektor(sektor);
		Assert.assertEquals(661, nextSektor);
	}
	
	@Test
	public void testSetSektorUsed() {
		int sektor = 712;
		filesystemSUT.setSektorAsUsed(sektor);
		Assert.assertFalse(filesystemSUT.isSektorFree(sektor));
		filesystemSUT.setSektorAsFree(sektor);
		Assert.assertTrue(filesystemSUT.isSektorFree(sektor));
	}
	
	@Test
	public void testCalculateFreeSektors() {
		int free = filesystemSUT.calculateFreeSektors();
		Assert.assertEquals(60, free);
	}

}

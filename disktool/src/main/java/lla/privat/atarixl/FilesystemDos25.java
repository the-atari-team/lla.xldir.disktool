package lla.privat.atarixl;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilesystemDos25 extends Filesystem {

	private final static Logger LOGGER = LoggerFactory.getLogger(FilesystemDos25.class);

	public FilesystemDos25(Diskette diskette) {
		super(diskette);
	}

	public int findNextFreeSektor(int oldsektor) throws IOException {
		// der alte Sektor ist uns egal, wir suchen einfach den naechsten freien Sektor
		return findFirstFreeSektor();
	}

	public int findFirstFreeSektor() throws IOException {
		int firstFreeSektor = 4;
		boolean foundFree = false;
		for (int sektor = 4; sektor < diskette.getSektors(); sektor++) {
			if (isSektorFree(sektor)) {
				foundFree = true;
				firstFreeSektor = sektor;
				break;
			}
		}
		if (!foundFree) {
			LOGGER.error("Disk is full");
			throw new IOException("Disk is full");
		}
		return firstFreeSektor;
	}

	private final int[] bit = { 128, 64, 32, 16, 8, 4, 2, 1 };

	protected boolean isSektorInUse(final int sektor) {
		return !isSektorFree(sektor);
	}

	public boolean isSektorFree(final int sektor) {
		boolean isFree720 = false;
		boolean isFree1050 = false;

		int byteInVtoc = sektor / 8;
		final int bitInVToc = bit[sektor & 0b111];

		if (!is1050Mode() || sektor < 720) {
			final int position = 10 + byteInVtoc;

			final int vtocPositionOnDisk = diskette.getSektorPositionInFulldisk(360);
			int byte1 = ByteUtils.getByte(diskette.readFromDisk(vtocPositionOnDisk + position, IOType.VTOC));
			isFree720 = (byte1 & bitInVToc) == bitInVToc;
		}
		if (is1050Mode() && sektor > 6 * 8) {
			byteInVtoc = (sektor - 6 * 8) / 8;
			final int position = 0 + byteInVtoc;

			final int vtocPositionOnDisk = diskette.getSektorPositionInFulldisk(1024);
			isFree1050 = (ByteUtils.getByte(diskette.readFromDisk(vtocPositionOnDisk + position, IOType.VTOC))
					& bitInVToc) == bitInVToc;
		}
		if (is1050Mode() && sektor <= 720 && isFree720 != isFree1050) {
			return isFree720;
		}
		if (is1050Mode() && sektor > 720) {
			return isFree1050;
		}
		return isFree720;
	}

	public void setSektorAsUsed(final int sektor) {
		int byteInVtoc = sektor / 8;
		final int bitToMaskFromVToc = bit[sektor & 0b111] ^ 255;
		if (sektor < 720) {
			final int position = 10 + byteInVtoc;

			final int vtocPositionOnDisk = diskette.getSektorPositionInFulldisk(360);
			final int newByte = ByteUtils.getByte(diskette.readFromDisk(vtocPositionOnDisk + position, IOType.VTOC))
					& bitToMaskFromVToc;
			diskette.writeToDisk(vtocPositionOnDisk + position, ByteUtils.toByte(newByte), IOType.VTOC);
		}
		if (is1050Mode() && sektor > 6 * 8) {
			byteInVtoc = (sektor - 6 * 8) / 8;
			final int position = 0 + byteInVtoc;

			final int vtocPositionOnDisk = diskette.getSektorPositionInFulldisk(1024);

			final int newByte = ByteUtils.getByte(diskette.readFromDisk(vtocPositionOnDisk + position, IOType.VTOC))
					& bitToMaskFromVToc;
			diskette.writeToDisk(vtocPositionOnDisk + position, ByteUtils.toByte(newByte), IOType.VTOC);

		}
	}

	public void setSektorAsFree(final int sektor) {
		int byteInVtoc = sektor / 8;
		final int bitInVtoc = bit[sektor & 0b111];
		if (sektor < 720) {
			final int position = 10 + byteInVtoc;

			final int vtocPositionOnDisk = diskette.getSektorPositionInFulldisk(360);
			final int newByte = ByteUtils.getByte(diskette.readFromDisk(vtocPositionOnDisk + position, IOType.VTOC))
					| bitInVtoc;
			diskette.writeToDisk(vtocPositionOnDisk + position, ByteUtils.toByte(newByte), IOType.VTOC);
		}
		if (is1050Mode() && sektor > 6 * 8) {
			byteInVtoc = (sektor - 6 * 8) / 8;
			final int position = 0 + byteInVtoc;

			final int vtocPositionOnDisk = diskette.getSektorPositionInFulldisk(1024);

			final int newByte = ByteUtils.getByte(diskette.readFromDisk(vtocPositionOnDisk + position, IOType.VTOC))
					| bitInVtoc;
			diskette.writeToDisk(vtocPositionOnDisk + position, ByteUtils.toByte(newByte), IOType.VTOC);

		}
	}

	// TODO: this has to be rewritten
	public int calculateFreeSektors() {
		int count = 0;
		// if (is1050Mode()) {
		// TODO: we need to do this twice
		// }

		for (int sector = 0; sector < diskette.getUseableSektors(); sector++) {
			if (isSektorFree(sector)) {
				count++;
			}
		}
		return count;
	}

	public void setFreeSektors(final int freeSektors) {
		int vtocInfoPosition = diskette.getSektorPositionInFulldisk(360);

		final int low = freeSektors & 0b11111111;
		final int high = freeSektors / 256;

		diskette.writeToDisk(vtocInfoPosition + 3, ByteUtils.toByte(low), IOType.VTOC_INFO);
		diskette.writeToDisk(vtocInfoPosition + 4, ByteUtils.toByte(high), IOType.VTOC_INFO);
		if (is1050Mode()) {
			LOGGER.warn("Can't set free sektors right on 1050 disks.");
			// // TODO: calculate für 1050 mode
			// final int vtocInfo1050Position = getSektorPositionInFulldisk(1024);
			// final int low1050 = 0;
			// final int high1050 = 0 / 256;
			// diskette.writeToDisk(vtocInfo1050Position + 122, ByteUtils.toByte(low1050),
			// IOType.VTOC_INFO);
			// diskette.writeToDisk(vtocInfo1050Position + 123, ByteUtils.toByte(high1050),
			// IOType.VTOC_INFO);
		}
	}

	public int getFreeSektors() {
		int vtocInfoPosition = diskette.getSektorPositionInFulldisk(360);
		final int freeSektors720 = ByteUtils.getWord(diskette.readFromDisk(vtocInfoPosition + 3, IOType.VTOC_INFO),
				diskette.readFromDisk(vtocInfoPosition + 4, IOType.VTOC_INFO));
		int freeSektors1050 = 0;
		if (is1050Mode()) {
			final int vtocInfo1050Position = diskette.getSektorPositionInFulldisk(1024);
			freeSektors1050 = ByteUtils.getWord(diskette.readFromDisk(vtocInfo1050Position + 122, IOType.VTOC_INFO),
					diskette.readFromDisk(vtocInfo1050Position + 123, IOType.VTOC_INFO));
		}
		return freeSektors720 + freeSektors1050;
	}

}

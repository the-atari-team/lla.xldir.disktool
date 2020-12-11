package lla.privat.atarixl;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LiteDOS ist sehr neu. Es ist Clusterbasiert, verschwendet also wieder etwas
 * Platz dafür ist das DOS sehr klein, es belegt den Speicher von $700 - $fff.
 * Der Freie Speicher beginnt ab $1000 Es existiert ein Turbo Basic mit 38500
 * Bytes freien Speicher (fast 4kb mehr)
 *
 * getVtocId() liefert einen Wert >$80 (negative Bit ist gesetzt)
 * 
 * Das untere Halbbyte (Nibble) enthält die Clustergröße - 1 Ein Cluster ist
 * min. 4 * 128 Byte groß also 512 Byte
 * 
 * Die eigentliche vtoc nimmt 64 Bytes ein, beginnt in Sektor 360 bei byte 32
 * bietet Platz für 512 freie Cluster In Sektor 360 das erste Byte ist der
 * vtocId die nächsten beiden Byte die gesammte Anzahl der Sektoren $71,2
 * enthält die Anzahl der freien Sektoren, nicht Cluster!
 * 
 * Einschränkungen Ein Cluster kann nur von einer Datei belegt werden Es
 * existieren immer noch Sektoren mit dessen Header (3 Byte), damit ist die
 * volle "lesende" Kompatiblität zu Dos 2.5 gewahrt Die Anzahl anlegbaren
 * Dateien ist durch die Clustergröße bestimmt
 * 
 * @author develop
 *
 */
public class FilesystemLiteDOS extends Filesystem {

	private final static Logger LOGGER = LoggerFactory.getLogger(FilesystemLiteDOS.class);

	protected int clustersize;
	protected int clusterInBytes;
	protected int countOfClusters;
	protected int countOfSektors;

	public FilesystemLiteDOS(Diskette diskette) {
		super(diskette);

		clustersize = (getVtocId() - 0x80) + 1;
		clusterInBytes = clustersize * 128;

		countOfClusters = getAllOverSektors() / clustersize;

		countOfSektors = clusterInBytes / diskette.getSektorSize();
	}

	// Find next Sektor in Cluster
	// Da der Cluster sofort in Benutzung gesetzt wird, geben wir einfach den
	// nächsten Sektor zurück
	// solange wir uns im gleichen Cluster befinden. Sonst nächster freier Cluster
	public int findNextFreeSektor(int oldSektor) throws IOException {
		int nextSektor = oldSektor + 1;
		int currentCluster = oldSektor / clustersize;
		int nextCluster = nextSektor / clustersize;
		if (currentCluster == nextCluster) {
			return nextSektor;
		}
		return findFirstFreeSektor();
	}

	// Wir liefern den ersten freien Sektor des ersten freien Clusters
	public int findFirstFreeSektor() throws IOException {

		int firstFreeSektor = 0;
		boolean foundFree = false;
		for (int sektor = firstFreeSektor; sektor < diskette.getSektors(); sektor += clustersize) {
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
		if (sektor == 0)
			return false;

		final int vtocPositionOnDisk = diskette.getSektorPositionInFulldisk(360);

		int cluster = sektor / clustersize;
		int byteInVtoc = cluster / 8;
		final int bitInVToc = bit[cluster & 0b111];

		final int position = 32 + byteInVtoc;

		int byte1 = ByteUtils.getByte(diskette.readFromDisk(vtocPositionOnDisk + position, IOType.VTOC));
		boolean isFree = (byte1 & bitInVToc) == bitInVToc;
		return isFree;
	}

	public void setSektorAsUsed(final int sektor) {
		if (sektor == 0)
			return;

		final int vtocPositionOnDisk = diskette.getSektorPositionInFulldisk(360);

		int cluster = sektor / clustersize;
		int byteInVtoc = cluster / 8;

		final int position = 32 + byteInVtoc;

		final int bitToMaskFromVToc = bit[cluster & 0b111] ^ 255;

		final int newByte = ByteUtils.getByte(diskette.readFromDisk(vtocPositionOnDisk + position, IOType.VTOC))
				& bitToMaskFromVToc;
		diskette.writeToDisk(vtocPositionOnDisk + position, ByteUtils.toByte(newByte), IOType.VTOC);
	}

	public void setSektorAsFree(final int sektor) {
		int cluster = sektor / clustersize;
		int byteInVtoc = cluster / 8;
		final int bitInVtoc = bit[cluster & 0b111];

		final int position = 32 + byteInVtoc;

		final int vtocPositionOnDisk = diskette.getSektorPositionInFulldisk(360);
		final int newByte = ByteUtils.getByte(diskette.readFromDisk(vtocPositionOnDisk + position, IOType.VTOC))
				| bitInVtoc;
		diskette.writeToDisk(vtocPositionOnDisk + position, ByteUtils.toByte(newByte), IOType.VTOC);
	}

	// TODO: this has to be rewritten
	public int calculateFreeSektors() {
		int count = 0;

		for (int sector = 0; sector < diskette.getUseableSektors(); sector += clustersize) {
			if (isSektorFree(sector)) {
				count++;
			}
		}
		return count * clustersize;
	}

	public void setFreeSektors(final int freeSektors) {
		int vtocInfoPosition = diskette.getSektorPositionInFulldisk(360);
		
		final int low = freeSektors & 0b11111111;
		final int high = freeSektors / 256;

		diskette.writeToDisk(vtocInfoPosition + 0x71, ByteUtils.toByte(low), IOType.VTOC_INFO);
		diskette.writeToDisk(vtocInfoPosition + 0x72, ByteUtils.toByte(high), IOType.VTOC_INFO);
	}
	
	public int getFreeSektors() {
		int vtocInfoPosition = diskette.getSektorPositionInFulldisk(360);
		
		final int freeSektors = ByteUtils.getWord(diskette.readFromDisk(vtocInfoPosition + 0x71, IOType.VTOC_INFO),
				diskette.readFromDisk(vtocInfoPosition + 0x72, IOType.VTOC_INFO));
		return freeSektors;
	}
}

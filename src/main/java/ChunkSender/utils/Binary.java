package ChunkSender.utils;

/*
 * This code is created by CloudburstMC/Nukkit
 *
 * We are only using a small part of the binary system but the origin is this file:
 * https://github.com/CloudburstMC/Nukkit/blob/master/src/main/java/cn/nukkit/utils/Binary.java
 */
public class Binary {

	public static int readInt(byte[] bytes) {
		return ((bytes[0] & 0xff) << 24) +
				((bytes[1] & 0xff) << 16) +
				((bytes[2] & 0xff) << 8) +
				(bytes[3] & 0xff);
	}

	public static byte[] writeInt(int i) {
		return new byte[]{
				(byte) ((i >>> 24) & 0xFF),
				(byte) ((i >>> 16) & 0xFF),
				(byte) ((i >>> 8) & 0xFF),
				(byte) (i & 0xFF)
		};
	}
}
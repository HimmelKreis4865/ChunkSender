package ChunkSender.utils;

/*
 * This code is created by CloudburstMC/Nukkit
 *
 * We are only using a small part of the binary system but the origin is this file:
 * https://github.com/CloudburstMC/Nukkit/blob/master/src/main/java/cn/nukkit/utils/VarInt.java
 */
public final class VarInt {

	private VarInt() {}

	private static long read(BinaryStream stream) {
		long value = 0;
		int size = 0;
		int b;
		while (((b = stream.getByte()) & 0x80) == 0x80) {
			value |= (long) (b & 0x7F) << (size++ * 7);
			if (size >= 5) {
				throw new IllegalArgumentException("VarLong too big");
			}
		}

		return value | ((long) (b & 0x7F) << (size * 7));
	}

	public static long readUnsignedVarInt(BinaryStream stream) {
		return read(stream);
	}

	private static void write(BinaryStream stream, long value) {
		do {
			byte temp = (byte) (value & 0b01111111);
			value >>>= 7;
			if (value != 0) {
				temp |= 0b10000000;
			}
			stream.putByte(temp);
		} while (value != 0);
	}

	public static void writeUnsignedVarInt(BinaryStream stream, long value) {
		write(stream, value);
	}
}
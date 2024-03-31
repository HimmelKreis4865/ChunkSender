package ChunkSender.utils;

import java.nio.charset.StandardCharsets;
import java.util.*;

/*
 * This code is created by CloudburstMC/Nukkit
 *
 * We are only using a small part of the binary system but the origin is this file:
 * https://github.com/CloudburstMC/Nukkit/blob/master/src/main/java/cn/nukkit/utils/BinaryStream.java
 */
public class BinaryStream {

	public int offset;
	private byte[] buffer;
	protected int count;

	private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

	public BinaryStream() {
		this.buffer = new byte[32];
		this.offset = 0;
		this.count = 0;
	}

	public BinaryStream(byte[] buffer) {
		this(buffer, 0);
	}

	public BinaryStream(byte[] buffer, int offset) {
		this.buffer = buffer;
		this.offset = offset;
		this.count = buffer.length;
	}

	public byte[] getBuffer() {
		return Arrays.copyOf(buffer, count);
	}

	public int getCount() {
		return count;
	}

	public byte[] get(int len) {
		if (len < 0) {
			this.offset = this.count - 1;
			return new byte[0];
		}
		len = Math.min(len, this.getCount() - this.offset);
		this.offset += len;
		return Arrays.copyOfRange(this.buffer, this.offset - len, this.offset);
	}

	public void put(byte[] bytes) {
		if (bytes == null) {
			return;
		}

		this.ensureCapacity(this.count + bytes.length);

		System.arraycopy(bytes, 0, this.buffer, this.count, bytes.length);
		this.count += bytes.length;
	}

	public int getInt() {
		return Binary.readInt(this.get(4));
	}

	public void putInt(int i) {
		this.put(Binary.writeInt(i));
	}

	public int getByte() {
		return this.buffer[this.offset++] & 0xff;
	}

	public void putByte(byte b) {
		this.put(new byte[]{b});
	}

	public byte[] getByteArray() {
		return this.get((int) this.getUnsignedVarInt());
	}

	public void putByteArray(byte[] b) {
		this.putUnsignedVarInt(b.length);
		this.put(b);
	}

	public String getString() {
		return new String(this.getByteArray(), StandardCharsets.UTF_8);
	}

	public void putString(String string) {
		byte[] b = string.getBytes(StandardCharsets.UTF_8);
		this.putByteArray(b);
	}

	public long getUnsignedVarInt() {
		return VarInt.readUnsignedVarInt(this);
	}

	public void putUnsignedVarInt(long v) {
		VarInt.writeUnsignedVarInt(this, v);
	}

	private void ensureCapacity(int minCapacity) {
		// overflow-conscious code
		if (minCapacity - buffer.length > 0) {
			grow(minCapacity);
		}
	}

	private void grow(int minCapacity) {
		// overflow-conscious code
		int oldCapacity = buffer.length;
		int newCapacity = oldCapacity << 1;

		if (newCapacity - minCapacity < 0) {
			newCapacity = minCapacity;
		}

		if (newCapacity - MAX_ARRAY_SIZE > 0) {
			newCapacity = hugeCapacity(minCapacity);
		}
		this.buffer = Arrays.copyOf(buffer, newCapacity);
	}

	private static int hugeCapacity(int minCapacity) {
		if (minCapacity < 0) { // overflow
			throw new OutOfMemoryError();
		}
		return (minCapacity > MAX_ARRAY_SIZE) ?
				Integer.MAX_VALUE :
				MAX_ARRAY_SIZE;
	}
}
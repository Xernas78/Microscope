package dev.xernas.microscope.reader;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class BinaryReader implements AutoCloseable {


    public final List<Integer> list = new ArrayList<Integer>();
    private ByteBuffer byteBuffer;
    private FileChannel channel;

    public BinaryReader(byte[] data, ByteOrder order) {
        byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(order);
    }

    public BinaryReader(FileInputStream fileInputStream, ByteOrder order) throws IOException {
        channel = fileInputStream.getChannel();
        byteBuffer = channel.map(
                FileChannel.MapMode.READ_ONLY,
                0,
                channel.size());
        byteBuffer.order(order);
    }

    public BinaryReader(ByteBuffer byteBuffer, ByteOrder order) {
        this.byteBuffer = byteBuffer;
        this.byteBuffer.order(order);
    }

    public void skip(int bytes) {
        byteBuffer.position(byteBuffer.position() + bytes);
    }

    public void setPos(int pos) {
        byteBuffer.position(pos);
    }

    public int getPos() {
        return byteBuffer.position();
    }

    public byte readByte() {
        return byteBuffer.get();
    }

    public short readInt16() {
        return byteBuffer.getShort();
    }

    public int readUInt16() {
        short s = byteBuffer.getShort();
        int intVal = s >= 0 ? s : 0x10000 + s;
        return intVal;
    }

    public long readUInt32() {
        int i = byteBuffer.getInt();
        long longVal = i >= 0 ? i : 0x100000000L + i;
        return longVal;
    }

    public int readInt32() {
        return byteBuffer.getInt();
    }

    public boolean readBoolean() {
        return byteBuffer.get() != 0;
    }

    public byte[] readBytes(final int length) {
        byte[] bytes = new byte[length];
        byteBuffer.get(bytes);
        return bytes;
    }

    @Override
    public void close() {
        byteBuffer = null;
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException ex) {
                // Do nothing.
            }
        }
    }
}
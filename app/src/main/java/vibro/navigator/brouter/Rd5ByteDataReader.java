package vibro.navigator.brouter;

import androidx.annotation.NonNull;

// Adapted from BRouter btools.util.ByteDataReader (MIT) for read-only rd5 extraction.
final class Rd5ByteDataReader {
    @NonNull
    private final byte[] data;
    private int offset;

    Rd5ByteDataReader(@NonNull byte[] data) {
        this(data, 0);
    }

    Rd5ByteDataReader(@NonNull byte[] data, int offset) {
        this.data = data;
        this.offset = offset;
    }

    int readInt() {
        int b3 = data[offset++] & 0xff;
        int b2 = data[offset++] & 0xff;
        int b1 = data[offset++] & 0xff;
        int b0 = data[offset++] & 0xff;
        return (b3 << 24) + (b2 << 16) + (b1 << 8) + b0;
    }

    long readLong() {
        long b7 = data[offset++] & 0xffL;
        long b6 = data[offset++] & 0xffL;
        long b5 = data[offset++] & 0xffL;
        long b4 = data[offset++] & 0xffL;
        long b3 = data[offset++] & 0xffL;
        long b2 = data[offset++] & 0xffL;
        long b1 = data[offset++] & 0xffL;
        long b0 = data[offset++] & 0xffL;
        return (b7 << 56) + (b6 << 48) + (b5 << 40) + (b4 << 32)
                + (b3 << 24) + (b2 << 16) + (b1 << 8) + b0;
    }

    byte readByte() {
        return (byte) (data[offset++] & 0xff);
    }
}

package vibro.navigator.brouter;

import androidx.annotation.NonNull;

final class Rd5VarBitsWriter {
    @NonNull
    private final byte[] data;
    private int index = -1;
    private int bits;
    private int buffer;

    Rd5VarBitsWriter(@NonNull byte[] data) {
        this.data = data;
    }

    void reset() {
        index = -1;
        bits = 0;
        buffer = 0;
    }

    void encodeVarBits(int value) {
        int range = 0;
        int remaining = value;
        while (remaining > range) {
            encodeBit(false);
            remaining -= range + 1;
            range = 2 * range + 1;
        }
        encodeBit(true);
        encodeBounded(range, remaining);
    }

    int closeAndGetEncodedLength() {
        flushBuffer();
        if (bits > 0) {
            data[++index] = (byte) (buffer & 0xff);
        }
        return index + 1;
    }

    private void encodeBit(boolean value) {
        if (bits > 31) {
            data[++index] = (byte) (buffer & 0xff);
            buffer >>>= 8;
            bits -= 8;
        }
        if (value) {
            buffer |= 1 << bits;
        }
        bits++;
    }

    private void encodeBounded(int max, int value) {
        int mask = 1;
        int remainingMax = max;
        while (mask <= remainingMax) {
            if ((value & mask) != 0) {
                encodeBit(true);
                remainingMax -= mask;
            } else {
                encodeBit(false);
            }
            mask <<= 1;
        }
    }

    private void flushBuffer() {
        while (bits > 7) {
            data[++index] = (byte) (buffer & 0xff);
            buffer >>>= 8;
            bits -= 8;
        }
    }
}

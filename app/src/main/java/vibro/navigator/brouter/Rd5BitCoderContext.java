package vibro.navigator.brouter;

import androidx.annotation.NonNull;

// Adapted from BRouter btools.util.BitCoderContext (MIT) for rd5 tag/stat decoding.
class Rd5BitCoderContext {
    private static final int LOOKUP_SIZE = 4096;
    private static final int[] VAR_LENGTH_VALUES = new int[LOOKUP_SIZE];
    private static final int[] VAR_LENGTH_BITS = new int[LOOKUP_SIZE];
    private static final int[] REVERSE_BYTE = new int[256];

    static {
        initLookups();
    }

    @NonNull
    private byte[] data;
    private int maxIndex;
    private int index = -1;
    private int bits;
    private int buffer;

    Rd5BitCoderContext(@NonNull byte[] data) {
        this.data = data;
        maxIndex = data.length - 1;
    }

    final void reset(@NonNull byte[] bytes) {
        data = bytes;
        maxIndex = bytes.length - 1;
        reset();
    }

    final void reset() {
        index = -1;
        bits = 0;
        buffer = 0;
    }

    final boolean decodeBit() {
        if (bits == 0) {
            bits = 8;
            buffer = data[++index] & 0xff;
        }
        boolean value = (buffer & 1) != 0;
        buffer >>>= 1;
        bits--;
        return value;
    }

    final int decodeVarBits() {
        fillBuffer();
        int b12 = buffer & 0xfff;
        int len = VAR_LENGTH_BITS[b12];
        if (len <= 12) {
            buffer >>>= len;
            bits -= len;
            return VAR_LENGTH_VALUES[b12];
        }
        if (len <= 23) {
            int len2 = len >> 1;
            buffer >>>= len2 + 1;
            int mask = 0xffffffff >>> (32 - len2);
            mask += buffer & mask;
            buffer >>>= len2;
            bits -= len;
            return mask;
        }
        if ((buffer & 0xffffff) != 0) {
            buffer >>>= 12;
            int len3 = 1 + (VAR_LENGTH_BITS[buffer & 0xfff] >> 1);
            buffer >>>= len3;
            int len2 = 11 + len3;
            bits -= len2 + 1;
            fillBuffer();
            int mask = 0xffffffff >>> (32 - len2);
            mask += buffer & mask;
            buffer >>>= len2;
            bits -= len2;
            return mask;
        }
        return decodeVarBitsSlow();
    }

    final int decodeBounded(int max) {
        int value = 0;
        int mask = 1;
        while ((value | mask) <= max) {
            if (bits == 0) {
                bits = 8;
                buffer = data[++index] & 0xff;
            }
            if ((buffer & 1) != 0) {
                value |= mask;
            }
            buffer >>>= 1;
            bits--;
            mask <<= 1;
        }
        return value;
    }

    final int decodeBits(int count) {
        fillBuffer();
        int mask = 0xffffffff >>> (32 - count);
        int value = buffer & mask;
        buffer >>>= count;
        bits -= count;
        return value;
    }

    final int decodeBitsReverse(int count) {
        fillBuffer();
        int value = 0;
        int remaining = count;
        while (remaining > 8) {
            value = (value << 8) | REVERSE_BYTE[buffer & 0xff];
            buffer >>>= 8;
            remaining -= 8;
            bits -= 8;
            fillBuffer();
        }
        value = (value << remaining) | REVERSE_BYTE[buffer & 0xff] >> (8 - remaining);
        bits -= remaining;
        buffer >>>= remaining;
        return value;
    }

    final int getReadingBitPosition() {
        return (index << 3) + 8 - bits;
    }

    private int decodeVarBitsSlow() {
        int range = 0;
        while (!decodeBit()) {
            range = 2 * range + 1;
        }
        return range + decodeBounded(range);
    }

    private void fillBuffer() {
        while (bits < 24) {
            if (index++ < maxIndex) {
                buffer |= (data[index] & 0xff) << bits;
            }
            bits += 8;
        }
    }

    private static void initLookups() {
        Rd5BitCoderContext context = new Rd5BitCoderContext(new byte[4]);
        for (int i = 0; i < LOOKUP_SIZE; i++) {
            context.reset();
            context.bits = 14;
            context.buffer = 0x1000 + i;
            int start = context.getReadingBitPosition();
            VAR_LENGTH_VALUES[i] = context.decodeVarBitsSlow();
            VAR_LENGTH_BITS[i] = context.getReadingBitPosition() - start;
        }
        for (int i = 0; i < 1024; i++) {
            context.reset();
            context.bits = 14;
            context.buffer = 0x1000 + i;
            int start = context.getReadingBitPosition();
            VAR_LENGTH_VALUES[i] = context.decodeVarBitsSlow();
            VAR_LENGTH_BITS[i] = context.getReadingBitPosition() - start;
        }
        for (int value = 0; value < REVERSE_BYTE.length; value++) {
            REVERSE_BYTE[value] = reverseByte(value);
        }
    }

    private static int reverseByte(int value) {
        int reversed = 0;
        for (int bit = 0; bit < 8; bit++) {
            if ((value & (1 << bit)) != 0) {
                reversed |= 1 << (7 - bit);
            }
        }
        return reversed;
    }
}

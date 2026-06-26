package vibro.navigator.brouter;

import androidx.annotation.NonNull;

final class Rd5Crc {
    private static final int[] TABLE = new int[256];

    static {
        for (int i = 0; i < TABLE.length; i++) {
            int value = i;
            for (int bit = 0; bit < 8; bit++) {
                value = (value & 1) == 0 ? value >>> 1 : (value >>> 1) ^ 0xedb88320;
            }
            TABLE[i] = value;
        }
    }

    private Rd5Crc() {
    }

    static int crc(@NonNull byte[] bytes, int offset, int length) {
        int crc = 0xffffffff;
        int end = offset + length;
        for (int i = offset; i < end; i++) {
            crc = (crc >>> 8) ^ TABLE[(crc ^ bytes[i]) & 0xff];
        }
        return crc;
    }
}

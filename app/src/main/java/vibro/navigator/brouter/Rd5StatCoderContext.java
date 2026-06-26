package vibro.navigator.brouter;

import androidx.annotation.NonNull;

// Adapted from BRouter btools.codec.StatCoderContext (MIT) for rd5 decoding.
final class Rd5StatCoderContext extends Rd5BitCoderContext {
    private static final int[] NOISY_BITS = new int[1024];

    static {
        for (int i = 0; i < NOISY_BITS.length; i++) {
            int value = i;
            int noisyBits = 0;
            while (value > 2) {
                noisyBits++;
                value >>= 1;
            }
            NOISY_BITS[i] = noisyBits;
        }
    }

    Rd5StatCoderContext(@NonNull byte[] data) {
        super(data);
    }

    int decodeNoisyNumber(int noisyBits) {
        int value = decodeBits(noisyBits);
        return value | (decodeVarBits() << noisyBits);
    }

    int decodeNoisyDiff(int noisyBits) {
        int value = 0;
        if (noisyBits > 0) {
            value = decodeBits(noisyBits) - (1 << (noisyBits - 1));
        }
        int upper = decodeVarBits() << noisyBits;
        if (upper != 0 && decodeBit()) {
            upper = -upper;
        }
        return value + upper;
    }

    int decodePredictedValue(int predictor) {
        int p = Math.abs(predictor);
        int noisyBits = 0;
        while (p > 1023) {
            noisyBits++;
            p >>= 1;
        }
        return predictor + decodeNoisyDiff(noisyBits + NOISY_BITS[p]);
    }

    void decodeSortedArray(int[] values, int offset, int size, int nextBitPosition, int value) {
        if (size == 1) {
            values[offset] = decodeLeafValue(nextBitPosition, value);
            return;
        }
        if (nextBitPosition < 0) {
            fillValues(values, offset, size, value);
            return;
        }

        int size1 = decodeBounded(size);
        int size2 = size - size1;
        if (size1 > 0) {
            decodeSortedArray(values, offset, size1, nextBitPosition - 1, value);
        }
        if (size2 > 0) {
            decodeSortedArray(values, offset + size1, size2, nextBitPosition - 1, value | (1 << nextBitPosition));
        }
    }

    private int decodeLeafValue(int nextBitPosition, int value) {
        return nextBitPosition < 0 ? value : value | decodeBitsReverse(nextBitPosition + 1);
    }

    private static void fillValues(int[] values, int offset, int size, int value) {
        for (int i = 0; i < size; i++) {
            values[offset + i] = value;
        }
    }
}

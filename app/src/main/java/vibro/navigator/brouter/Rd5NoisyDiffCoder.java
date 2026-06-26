package vibro.navigator.brouter;

import androidx.annotation.NonNull;

// Adapted from BRouter btools.codec.NoisyDiffCoder (MIT) for rd5 decoding.
final class Rd5NoisyDiffCoder {
    private final int noisyBits;
    @NonNull
    private final Rd5StatCoderContext context;

    Rd5NoisyDiffCoder(@NonNull Rd5StatCoderContext context) {
        noisyBits = context.decodeVarBits();
        this.context = context;
    }

    int decodeSignedValue() {
        return context.decodeNoisyDiff(noisyBits);
    }
}

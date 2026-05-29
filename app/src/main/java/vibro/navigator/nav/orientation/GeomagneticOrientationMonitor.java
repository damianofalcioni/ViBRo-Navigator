package vibro.navigator.nav.orientation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class GeomagneticOrientationMonitor {

    public interface Callback {
        void onSampleUpdated(@NonNull Sample sample);
    }

    private GeomagneticOrientationMonitor() {
    }

    public static final class Sample {
        private static final double MAX_FLAT_TILT_DEGREES = 25.0;

        final double headingDegrees;
        final double pitchDegrees;
        final double rollDegrees;
        final int accuracy;
        @Nullable
        final Integer legacyOrientationAccuracy;
        final long legacyOrientationAccuracyElapsedRealtimeMs;
        @Nullable
        final Double headingAccuracyDegrees;
        final long elapsedRealtimeMs;

        public Sample(
                double headingDegrees,
                double pitchDegrees,
                double rollDegrees,
                int accuracy,
                @Nullable Double headingAccuracyDegrees,
                long elapsedRealtimeMs
        ) {
            this(
                    headingDegrees,
                    pitchDegrees,
                    rollDegrees,
                    accuracy,
                    null,
                    -1L,
                    headingAccuracyDegrees,
                    elapsedRealtimeMs
            );
        }

        public Sample(
                double headingDegrees,
                double pitchDegrees,
                double rollDegrees,
                int accuracy,
                @Nullable Integer legacyOrientationAccuracy,
                long legacyOrientationAccuracyElapsedRealtimeMs,
                @Nullable Double headingAccuracyDegrees,
                long elapsedRealtimeMs
        ) {
            this.headingDegrees = headingDegrees;
            this.pitchDegrees = pitchDegrees;
            this.rollDegrees = rollDegrees;
            this.accuracy = accuracy;
            this.legacyOrientationAccuracy = legacyOrientationAccuracy;
            this.legacyOrientationAccuracyElapsedRealtimeMs = legacyOrientationAccuracyElapsedRealtimeMs;
            this.headingAccuracyDegrees = headingAccuracyDegrees;
            this.elapsedRealtimeMs = elapsedRealtimeMs;
        }

        @NonNull
        public Sample withLegacyOrientationAccuracy(
                @Nullable Integer updatedLegacyOrientationAccuracy,
                long updatedLegacyOrientationAccuracyElapsedRealtimeMs
        ) {
            return new Sample(
                    headingDegrees,
                    pitchDegrees,
                    rollDegrees,
                    accuracy,
                    updatedLegacyOrientationAccuracy,
                    updatedLegacyOrientationAccuracyElapsedRealtimeMs,
                    headingAccuracyDegrees,
                    elapsedRealtimeMs
            );
        }

        public boolean isFlatEnough() {
            return Math.abs(pitchDegrees) <= MAX_FLAT_TILT_DEGREES
                    && Math.abs(rollDegrees) <= MAX_FLAT_TILT_DEGREES;
        }

        public boolean isAccuracyHighEnough(long nowElapsedRealtimeMs) {
            return HeadingAccuracyPolicy.isAccuracyHighEnough(
                    accuracy,
                    legacyOrientationAccuracy,
                    legacyOrientationAccuracyElapsedRealtimeMs,
                    nowElapsedRealtimeMs
            );
        }

        public boolean isHeadingAccuracyHighEnough(
                double absoluteTurnDegrees,
                double minimumTurnDegrees,
                long nowElapsedRealtimeMs
        ) {
            Double effectiveHeadingAccuracyDegrees = effectiveHeadingAccuracyDegrees(nowElapsedRealtimeMs);
            return effectiveHeadingAccuracyDegrees == null
                    || absoluteTurnDegrees - effectiveHeadingAccuracyDegrees >= minimumTurnDegrees;
        }

        @Nullable
        public Double effectiveHeadingAccuracyDegrees(long nowElapsedRealtimeMs) {
            return HeadingAccuracyPolicy.effectiveHeadingAccuracyDegrees(
                    accuracy,
                    headingAccuracyDegrees,
                    legacyOrientationAccuracy,
                    legacyOrientationAccuracyElapsedRealtimeMs,
                    nowElapsedRealtimeMs
            );
        }
    }
}

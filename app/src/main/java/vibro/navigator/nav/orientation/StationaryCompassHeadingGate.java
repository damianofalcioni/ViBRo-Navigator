package vibro.navigator.nav.orientation;

import androidx.annotation.Nullable;

import vibro.navigator.geo.GeoMath;

/** Confirms a deliberate turn relative to the compass reading at the start of a stop. */
public final class StationaryCompassHeadingGate {
    private static final double MIN_TURN_DEGREES = 30.0;
    private static final float MAX_ACCURACY_DEGREES = 25f;
    private static final double MAX_CONFIRMATION_JITTER_DEGREES = 10.0;
    private static final long TURN_CONFIRMATION_MS = 1_000L;
    private static final long MAX_CONFIRMATION_SAMPLE_GAP_MS = 1_500L;

    @Nullable
    private Double referenceDegrees;
    @Nullable
    private Double candidateDegrees;
    private long candidateSinceMs;
    private long lastEvaluationMs;
    private boolean activated;

    public void reset() {
        activated = false;
        resetConfirmation();
    }

    private void resetConfirmation() {
        referenceDegrees = null;
        candidateDegrees = null;
        candidateSinceMs = 0L;
        lastEvaluationMs = 0L;
    }

    public boolean accept(@Nullable Double headingDegrees, @Nullable Float accuracyDegrees, long nowMs) {
        if (!isUsable(headingDegrees, accuracyDegrees)) {
            resetConfirmation();
            return false;
        }
        if (activated) {
            return true;
        }
        if (nowMs < lastEvaluationMs || nowMs - lastEvaluationMs > MAX_CONFIRMATION_SAMPLE_GAP_MS) {
            candidateDegrees = null;
        }
        lastEvaluationMs = nowMs;
        if (referenceDegrees == null) {
            referenceDegrees = headingDegrees;
            return false;
        }
        double turnThreshold = Math.max(MIN_TURN_DEGREES, 2.0 * accuracyDegrees);
        if (GeoMath.angularDiffDegrees(headingDegrees, referenceDegrees) < turnThreshold) {
            candidateDegrees = null;
            return false;
        }
        return confirmTurn(headingDegrees, nowMs);
    }

    private boolean confirmTurn(double headingDegrees, long nowMs) {
        if (candidateDegrees == null
                || GeoMath.angularDiffDegrees(headingDegrees, candidateDegrees) > MAX_CONFIRMATION_JITTER_DEGREES) {
            candidateDegrees = headingDegrees;
            candidateSinceMs = nowMs;
            return false;
        }
        if (nowMs - candidateSinceMs < TURN_CONFIRMATION_MS) {
            return false;
        }
        referenceDegrees = headingDegrees;
        candidateDegrees = null;
        activated = true;
        return true;
    }

    public static boolean isUsable(@Nullable Double headingDegrees, @Nullable Float accuracyDegrees) {
        return headingDegrees != null && Double.isFinite(headingDegrees)
                && accuracyDegrees != null && Float.isFinite(accuracyDegrees)
                && accuracyDegrees >= 0f && accuracyDegrees <= MAX_ACCURACY_DEGREES;
    }
}

package vibro.navigator.nav.guidance;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.nav.location.NavigationLocation;

/** Closely repeated uncertain positions are not independent departure evidence. */
final class OffTrackEvidence {
    private static final long UNCERTAIN_CONFIRMATION_MS = 6_000L;
    private static final long MAX_SAMPLE_GAP_MS = 10_000L;
    private NavigationLocation anchor;
    private long beganMs;
    private long lastMs;
    private double anchorExcess;

    void clear() {
        anchor = null;
        anchorExcess = 0;
    }

    boolean supportsDeparture(NavigationLocation fix, double excessMeters, long nowMs) {
        updateAnchor(fix, excessMeters, nowMs);
        lastMs = nowMs;
        double accuracy = fix.hasAccuracy() ? fix.getAccuracy() : Double.POSITIVE_INFINITY;
        double moved = GeoMath.distanceMeters(anchor.getLatitude(), anchor.getLongitude(),
                fix.getLatitude(), fix.getLongitude());
        boolean independent = moved >= Math.max(4, accuracy);
        boolean clearlyOutside = Double.isFinite(accuracy) && excessMeters > accuracy;
        return (independent && clearlyOutside) || (Double.isFinite(accuracy)
                && nowMs - beganMs >= UNCERTAIN_CONFIRMATION_MS);
    }

    private void updateAnchor(NavigationLocation fix, double excessMeters, long nowMs) {
        if (anchor == null || nowMs <= lastMs || nowMs - lastMs > MAX_SAMPLE_GAP_MS) {
            anchor = new NavigationLocation(fix);
            beganMs = nowMs;
            anchorExcess = excessMeters;
        }
        if (anchor != null && excessMeters + 3 < anchorExcess) {
            // Returning positions are correction evidence, even when their filtered center still lies outside.
            anchor = new NavigationLocation(fix);
            beganMs = nowMs;
            anchorExcess = excessMeters;
        }
    }
}

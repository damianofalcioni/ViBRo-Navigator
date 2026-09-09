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

    void clear() {
        anchor = null;
    }

    boolean supportsDeparture(NavigationLocation fix, double excessMeters, long nowMs) {
        if (anchor == null || nowMs <= lastMs || nowMs - lastMs > MAX_SAMPLE_GAP_MS) {
            anchor = new NavigationLocation(fix);
            beganMs = nowMs;
        }
        lastMs = nowMs;
        double accuracy = fix.hasAccuracy() ? fix.getAccuracy() : Double.POSITIVE_INFINITY;
        double moved = GeoMath.distanceMeters(anchor.getLatitude(), anchor.getLongitude(),
                fix.getLatitude(), fix.getLongitude());
        boolean independent = moved >= Math.max(4, accuracy);
        boolean clearlyOutside = Double.isFinite(accuracy) && excessMeters > accuracy;
        return (independent && clearlyOutside) || nowMs - beganMs >= UNCERTAIN_CONFIRMATION_MS;
    }
}

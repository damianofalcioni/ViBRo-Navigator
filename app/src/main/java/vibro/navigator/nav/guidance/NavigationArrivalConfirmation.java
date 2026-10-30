package vibro.navigator.nav.guidance;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationMotionState;
import java.util.Objects;

/** A near-target observation starts confirmation; uncertainty alone cannot establish arrival. */
public final class NavigationArrivalConfirmation {
    private LatLon target;
    private long firstTimeMs = -1;
    private long lastTimeMs = -1;
    private boolean confirmed;
    private String provider;

    public void reset() {
        target = null;
        firstTimeMs = -1;
        lastTimeMs = -1;
        confirmed = false;
        provider = null;
    }

    public static double radiusMeters(float accuracy) {
        return Float.isFinite(accuracy) && accuracy >= 0 ? Math.min(20, Math.max(10, accuracy + 8)) : 10;
    }

    public boolean observe(NavigationLocation location, float accuracy, LatLon destination, long nowMs) {
        if (!sameTarget(destination)) {
            reset();
            target = destination;
        }
        if (!usable(location, accuracy, destination, nowMs)) {
            reset();
            return false;
        }
        long timeMs = location.getElapsedRealtimeOrTimeMs();
        if (!Objects.equals(provider, location.getProvider())) {
            firstTimeMs = -1;
            lastTimeMs = -1;
            confirmed = false;
            provider = location.getProvider();
        }
        if (timeMs <= lastTimeMs) {
            return confirmed && timeMs == lastTimeMs;
        }
        if (lastTimeMs < 0 || timeMs - lastTimeMs > 10_000L) {
            firstTimeMs = timeMs;
            confirmed = false;
        }
        lastTimeMs = timeMs;
        confirmed = confirmed || timeMs - firstTimeMs >= 3_000L;
        return confirmed;
    }

    private boolean sameTarget(LatLon destination) {
        return target != null && target.lat == destination.lat && target.lon == destination.lon;
    }

    private static boolean usable(NavigationLocation location, float accuracy, LatLon destination, long nowMs) {
        return NavigationMotionState.isFresh(location.getElapsedRealtimeOrTimeMs(), nowMs)
                && hasUsableAccuracy(location, accuracy)
                && GeoMath.distanceMeters(location.getLatitude(), location.getLongitude(),
                        destination.lat, destination.lon) <= radiusMeters(accuracy);
    }

    /** Departure evidence must meet the same quality gate before it can unlock a loop finish. */
    public static boolean hasUsableAccuracy(NavigationLocation location, float accuracy) {
        return Float.isFinite(accuracy) && accuracy >= 0 && accuracy <= 25
                && location.hasAccuracy() && Float.isFinite(location.getAccuracy())
                && location.getAccuracy() >= 0 && location.getAccuracy() <= 25;
    }
}

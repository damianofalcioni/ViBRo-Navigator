package vibro.navigator.nav.session;

import androidx.annotation.Nullable;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.nav.location.NavigationLocation;

/** Display-only disagreement confirmation; never supplies reroute evidence. */
final class NavigationRouteHeadingFallback {
    private static final double ENTER_DISAGREEMENT_DEGREES = 120.0;
    private static final double EXIT_DISAGREEMENT_DEGREES = 60.0;
    private static final double MIN_CONFIDENT_DISAGREEMENT_DEGREES = 90.0;
    private static final double MAX_COURSE_JITTER_DEGREES = 30.0;
    private static final float GOOD_ACCURACY_DEGREES = 25f;
    private static final long MIN_SAMPLE_SEPARATION_MS = 750L;
    // Cover the longest normal acquisition interval plus acquisition slack.
    private static final long MAX_SAMPLE_GAP_MS = 90_000L;

    @Nullable
    private NavigationLocation candidateLocation;
    private double candidateHeading;
    private float candidateAccuracy;
    private long lastSampleMs = -1L;
    private boolean active;

    void reset() {
        candidateLocation = null;
        lastSampleMs = -1L;
        active = false;
    }

    boolean useLocationHeading(NavigationLocation location, double heading, float accuracy, double routeHeading) {
        long sampleMs = location.getElapsedRealtimeOrTimeMs();
        if (sampleMs <= lastSampleMs) {
            return active;
        }
        if (lastSampleMs >= 0L && sampleMs - lastSampleMs > MAX_SAMPLE_GAP_MS) {
            reset();
        }
        lastSampleMs = sampleMs;
        double difference = GeoMath.angularDiffDegrees(heading, routeHeading);
        if (!shouldChangeSource(difference, accuracy)) {
            candidateLocation = null;
            return active;
        }
        return confirmChange(location, heading, accuracy);
    }

    private boolean shouldChangeSource(double difference, float accuracy) {
        return active
                ? difference < EXIT_DISAGREEMENT_DEGREES && difference + accuracy < MIN_CONFIDENT_DISAGREEMENT_DEGREES
                : difference > ENTER_DISAGREEMENT_DEGREES && difference - accuracy > MIN_CONFIDENT_DISAGREEMENT_DEGREES;
    }

    private boolean confirmChange(NavigationLocation location, double heading, float accuracy) {
        if (candidateLocation == null
                || location.getElapsedRealtimeOrTimeMs() - candidateLocation.getElapsedRealtimeOrTimeMs() > MAX_SAMPLE_GAP_MS
                || GeoMath.angularDiffDegrees(heading, candidateHeading) > MAX_COURSE_JITTER_DEGREES) {
            candidateLocation = new NavigationLocation(location);
            candidateHeading = heading;
            candidateAccuracy = accuracy;
            return active;
        }
        long separationMs = location.getElapsedRealtimeOrTimeMs() - candidateLocation.getElapsedRealtimeOrTimeMs();
        if (separationMs >= MIN_SAMPLE_SEPARATION_MS && hasMovementSupport(location, heading, accuracy)) {
            active = !active;
            candidateLocation = null;
        }
        return active;
    }

    private boolean hasMovementSupport(NavigationLocation location, double heading, float accuracy) {
        if (accuracy <= GOOD_ACCURACY_DEGREES && candidateAccuracy <= GOOD_ACCURACY_DEGREES) {
            return true;
        }
        if (!hasPositionAccuracy(location) || !hasPositionAccuracy(candidateLocation)) {
            return false;
        }
        double distance = GeoMath.distanceMeters(candidateLocation.getLatitude(), candidateLocation.getLongitude(),
                location.getLatitude(), location.getLongitude());
        double displacementHeading = GeoMath.bearingDegrees(candidateLocation.getLatitude(), candidateLocation.getLongitude(),
                location.getLatitude(), location.getLongitude());
        return distance > Math.max(3.0, (double) candidateLocation.getAccuracy() + location.getAccuracy())
                && GeoMath.angularDiffDegrees(displacementHeading, heading) <= MAX_COURSE_JITTER_DEGREES;
    }

    private static boolean hasPositionAccuracy(NavigationLocation location) {
        return location.hasAccuracy() && Float.isFinite(location.getAccuracy()) && location.getAccuracy() >= 0f;
    }
}

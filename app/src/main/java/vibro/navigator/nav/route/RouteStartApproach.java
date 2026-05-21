package vibro.navigator.nav.route;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.guidance.RouteDeviationPolicy;

public final class RouteStartApproach {
    public static final int BEELINE_COMMAND = 16;
    private static final double DEFAULT_APPROACH_SPEED_MPS = 1.4;
    private static final double MIN_LIVE_APPROACH_SPEED_MPS = 0.2;

    private RouteStartApproach() {
    }

    @NonNull
    public static Plan plan(
            @NonNull GeoJsonRoute route,
            @NonNull LatLon requestedStart,
            float accuracyMeters
    ) {
        if (route.track.isEmpty()) {
            return Plan.inactive(0.0, 0.0);
        }
        LatLon target = route.track.get(0);
        double distanceMeters = distanceMeters(requestedStart, target);
        double thresholdMeters = RouteDeviationPolicy.resolveOffTrackThresholdMeters(accuracyMeters);
        if (distanceMeters <= thresholdMeters) {
            return Plan.inactive(distanceMeters, thresholdMeters);
        }
        return Plan.active(new LatLon(target.lat, target.lon), distanceMeters, thresholdMeters);
    }

    public static boolean isInsideOriginalRouteThreshold(
            @NonNull PolylineIndex.Match match,
            double accuracyMeters
    ) {
        return match.distanceToTrackMeters <= RouteDeviationPolicy.resolveOffTrackThresholdMeters(accuracyMeters);
    }

    public static double distanceMeters(@NonNull LatLon first, @NonNull LatLon second) {
        return GeoMath.distanceMeters(first.lat, first.lon, second.lat, second.lon);
    }

    public static double estimateApproachTimeSeconds(
            double distanceMeters,
            float speedMps,
            boolean likelyStationary
    ) {
        double resolvedSpeedMps = !likelyStationary && Float.isFinite(speedMps) && speedMps >= MIN_LIVE_APPROACH_SPEED_MPS
                ? speedMps
                : DEFAULT_APPROACH_SPEED_MPS;
        return Math.max(0.0, distanceMeters) / resolvedSpeedMps;
    }

    public static final class Plan {
        @Nullable
        public final LatLon target;
        public final boolean active;
        public final double distanceMeters;
        public final double thresholdMeters;

        private Plan(
                @Nullable LatLon target,
                boolean active,
                double distanceMeters,
                double thresholdMeters
        ) {
            this.target = target;
            this.active = active;
            this.distanceMeters = distanceMeters;
            this.thresholdMeters = thresholdMeters;
        }

        @NonNull
        private static Plan inactive(double distanceMeters, double thresholdMeters) {
            return new Plan(null, false, distanceMeters, thresholdMeters);
        }

        @NonNull
        private static Plan active(
                @NonNull LatLon target,
                double distanceMeters,
                double thresholdMeters
        ) {
            return new Plan(target, true, distanceMeters, thresholdMeters);
        }
    }
}

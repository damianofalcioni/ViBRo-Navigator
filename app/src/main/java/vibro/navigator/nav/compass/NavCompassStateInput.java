package vibro.navigator.nav.compass;

import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.policy.NavigationSpeedBucket;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.NavigationRouteGeometryState;
import vibro.navigator.nav.route.PolylineIndex;

import java.util.Collections;
import java.util.List;

public final class NavCompassStateInput {
    @NonNull
    public final GeoJsonRoute route;
    @NonNull
    public final PolylineIndex index;
    public final double alongTrackMeters;
    @NonNull
    public final NavigationLocation currentLocation;
    public final float speedMps;
    public final boolean likelyStationary;
    public final float accuracyMeters;
    public final float destinationReachedRadiusMeters;
    @Nullable
    public final Double headingDegrees;
    @Nullable
    public final Float headingAccuracyDegrees;
    @Nullable
    public final Float previousVisibleRadiusMeters;
    @Nullable
    public final Float previousReliableMovingVisibleRadiusMeters;
    @Nullable
    public final NavigationSpeedBucket previousMovingSpeedBucket;
    public final long radiusUpdateDeltaMs;
    @Nullable
    public final CompassRouteGeometry routeGeometry;
    @Nullable
    public final CompassRadiusTransition radiusTransition;
    @Nullable
    public final CompassOrientationCue orientationCue;
    @Nullable
    public final LatLon routeStartApproachTarget;
    @NonNull
    public final List<CompassBlockedArea> blockedAreas;
    public final long nowMs;
    public final boolean stationaryFullRouteZoomEnabled;

    private NavCompassStateInput(@NonNull Builder builder) {
        route = builder.route;
        index = builder.index;
        alongTrackMeters = builder.alongTrackMeters;
        currentLocation = builder.currentLocation;
        speedMps = builder.speedMps;
        likelyStationary = builder.likelyStationary;
        accuracyMeters = builder.accuracyMeters;
        destinationReachedRadiusMeters = builder.destinationReachedRadiusMeters;
        headingDegrees = builder.headingDegrees;
        headingAccuracyDegrees = builder.headingAccuracyDegrees;
        previousVisibleRadiusMeters = builder.previousVisibleRadiusMeters;
        previousReliableMovingVisibleRadiusMeters = builder.previousReliableMovingVisibleRadiusMeters;
        previousMovingSpeedBucket = builder.previousMovingSpeedBucket;
        radiusUpdateDeltaMs = builder.radiusUpdateDeltaMs;
        routeGeometry = builder.routeGeometry;
        radiusTransition = builder.radiusTransition;
        orientationCue = builder.orientationCue;
        routeStartApproachTarget = builder.routeStartApproachTarget;
        blockedAreas = builder.blockedAreas;
        nowMs = builder.nowMs;
        stationaryFullRouteZoomEnabled = builder.stationaryFullRouteZoomEnabled;
    }

    @NonNull
    public static Builder builder(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            @NonNull NavigationLocation currentLocation
    ) {
        return new Builder(route, index, currentLocation);
    }

    public static final class Builder {
        @NonNull
        private final GeoJsonRoute route;
        @NonNull
        private final PolylineIndex index;
        @NonNull
        private final NavigationLocation currentLocation;
        private double alongTrackMeters;
        private float speedMps;
        private boolean likelyStationary;
        private float accuracyMeters;
        private float destinationReachedRadiusMeters;
        @Nullable
        private Double headingDegrees;
        @Nullable
        private Float headingAccuracyDegrees;
        @Nullable
        private Float previousVisibleRadiusMeters;
        @Nullable
        private Float previousReliableMovingVisibleRadiusMeters;
        @Nullable
        private NavigationSpeedBucket previousMovingSpeedBucket;
        private long radiusUpdateDeltaMs;
        @Nullable
        private CompassRouteGeometry routeGeometry;
        @Nullable
        private CompassRadiusTransition radiusTransition;
        @Nullable
        private CompassOrientationCue orientationCue;
        @Nullable
        private LatLon routeStartApproachTarget;
        @NonNull
        private List<CompassBlockedArea> blockedAreas = Collections.emptyList();
        private long nowMs;
        private boolean stationaryFullRouteZoomEnabled = true;

        private Builder(
                @NonNull GeoJsonRoute route,
                @NonNull PolylineIndex index,
                @NonNull NavigationLocation currentLocation
        ) {
            this.route = route;
            this.index = index;
            this.currentLocation = currentLocation;
        }

        @NonNull
        public Builder routeProgress(double alongTrackMeters) {
            this.alongTrackMeters = alongTrackMeters;
            return this;
        }

        @NonNull
        public Builder motion(float speedMps, boolean likelyStationary, float accuracyMeters) {
            this.speedMps = speedMps;
            this.likelyStationary = likelyStationary;
            this.accuracyMeters = accuracyMeters;
            this.destinationReachedRadiusMeters =
                    (float) NavigationRouteGeometryState.resolveDestinationReachedRadiusMeters(accuracyMeters);
            return this;
        }

        @NonNull
        public Builder destinationReachedRadiusMeters(float destinationReachedRadiusMeters) {
            this.destinationReachedRadiusMeters = destinationReachedRadiusMeters;
            return this;
        }

        @NonNull
        public Builder heading(@Nullable Double headingDegrees, @Nullable Float headingAccuracyDegrees) {
            this.headingDegrees = headingDegrees;
            this.headingAccuracyDegrees = headingAccuracyDegrees;
            return this;
        }

        @NonNull
        public Builder radiusMemory(
                @Nullable Float previousVisibleRadiusMeters,
                @Nullable Float previousReliableMovingVisibleRadiusMeters,
                long radiusUpdateDeltaMs
        ) {
            this.previousVisibleRadiusMeters = previousVisibleRadiusMeters;
            this.previousReliableMovingVisibleRadiusMeters = previousReliableMovingVisibleRadiusMeters;
            this.radiusUpdateDeltaMs = radiusUpdateDeltaMs;
            return this;
        }

        @NonNull
        public Builder speedBucketMemory(@Nullable NavigationSpeedBucket previousMovingSpeedBucket) {
            this.previousMovingSpeedBucket = previousMovingSpeedBucket;
            return this;
        }

        @NonNull
        public Builder geometry(
                @Nullable CompassRouteGeometry routeGeometry,
                @Nullable CompassRadiusTransition radiusTransition
        ) {
            this.routeGeometry = routeGeometry;
            this.radiusTransition = radiusTransition;
            return this;
        }

        @NonNull
        public Builder orientationCue(@Nullable CompassOrientationCue orientationCue) {
            this.orientationCue = orientationCue;
            return this;
        }

        @NonNull
        public Builder routeStartApproachTarget(@Nullable LatLon routeStartApproachTarget) {
            this.routeStartApproachTarget = routeStartApproachTarget;
            return this;
        }

        @NonNull
        public Builder blockedAreas(@NonNull List<CompassBlockedArea> blockedAreas) {
            this.blockedAreas = blockedAreas;
            return this;
        }

        @NonNull
        public Builder stationaryFullRouteZoomEnabled(boolean enabled) {
            stationaryFullRouteZoomEnabled = enabled;
            return this;
        }

        @NonNull
        public Builder nowMs(long nowMs) {
            this.nowMs = nowMs;
            return this;
        }

        @NonNull
        public NavCompassStateInput build() {
            return new NavCompassStateInput(this);
        }
    }
}

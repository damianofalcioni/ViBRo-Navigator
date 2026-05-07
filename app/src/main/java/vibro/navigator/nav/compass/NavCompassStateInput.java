package vibro.navigator.nav.compass;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;

public final class NavCompassStateInput {
    @NonNull
    public final GeoJsonRoute route;
    @NonNull
    public final PolylineIndex index;
    public final double alongTrackMeters;
    @NonNull
    public final Location currentLocation;
    public final float speedMps;
    public final boolean likelyStationary;
    public final float accuracyMeters;
    @Nullable
    public final Double headingDegrees;
    @Nullable
    public final Float headingAccuracyDegrees;
    @Nullable
    public final Float previousVisibleRadiusMeters;
    @Nullable
    public final Float previousReliableMovingVisibleRadiusMeters;
    public final long radiusUpdateDeltaMs;
    @Nullable
    public final CompassRouteGeometry routeGeometry;
    @Nullable
    public final CompassRadiusTransition radiusTransition;
    @Nullable
    public final CompassOrientationCue orientationCue;
    public final long nowMs;

    private NavCompassStateInput(@NonNull Builder builder) {
        route = builder.route;
        index = builder.index;
        alongTrackMeters = builder.alongTrackMeters;
        currentLocation = builder.currentLocation;
        speedMps = builder.speedMps;
        likelyStationary = builder.likelyStationary;
        accuracyMeters = builder.accuracyMeters;
        headingDegrees = builder.headingDegrees;
        headingAccuracyDegrees = builder.headingAccuracyDegrees;
        previousVisibleRadiusMeters = builder.previousVisibleRadiusMeters;
        previousReliableMovingVisibleRadiusMeters = builder.previousReliableMovingVisibleRadiusMeters;
        radiusUpdateDeltaMs = builder.radiusUpdateDeltaMs;
        routeGeometry = builder.routeGeometry;
        radiusTransition = builder.radiusTransition;
        orientationCue = builder.orientationCue;
        nowMs = builder.nowMs;
    }

    @NonNull
    public static Builder builder(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            @NonNull Location currentLocation
    ) {
        return new Builder(route, index, currentLocation);
    }

    public static final class Builder {
        @NonNull
        private final GeoJsonRoute route;
        @NonNull
        private final PolylineIndex index;
        @NonNull
        private final Location currentLocation;
        private double alongTrackMeters;
        private float speedMps;
        private boolean likelyStationary;
        private float accuracyMeters;
        @Nullable
        private Double headingDegrees;
        @Nullable
        private Float headingAccuracyDegrees;
        @Nullable
        private Float previousVisibleRadiusMeters;
        @Nullable
        private Float previousReliableMovingVisibleRadiusMeters;
        private long radiusUpdateDeltaMs;
        @Nullable
        private CompassRouteGeometry routeGeometry;
        @Nullable
        private CompassRadiusTransition radiusTransition;
        @Nullable
        private CompassOrientationCue orientationCue;
        private long nowMs;

        private Builder(
                @NonNull GeoJsonRoute route,
                @NonNull PolylineIndex index,
                @NonNull Location currentLocation
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

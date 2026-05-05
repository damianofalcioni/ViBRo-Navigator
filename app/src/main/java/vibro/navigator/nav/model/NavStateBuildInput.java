package vibro.navigator.nav.model;

import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.nav.compass.CompassRadiusTransition;
import vibro.navigator.nav.compass.CompassRouteGeometry;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;

public final class NavStateBuildInput {
    @NonNull
    public final GeoJsonRoute route;
    @NonNull
    public final PolylineIndex index;
    public final double alongTrackMeters;
    public final int nextHintIndex;
    public final int currentSegmentIndex;
    public final float speedMps;
    public final float etaSpeedMps;
    public final boolean likelyStationary;
    public final float accuracyMeters;
    public final float compassAccuracyMeters;
    @NonNull
    public final Location currentLocation;
    @Nullable
    public final Integer fixedSatelliteCount;
    @Nullable
    public final Double headingDegrees;
    @Nullable
    public final Float headingAccuracyDegrees;
    @Nullable
    public final Float previousCompassVisibleRadiusMeters;
    @Nullable
    public final Float previousReliableMovingCompassVisibleRadiusMeters;
    public final long compassRadiusUpdateDeltaMs;
    @Nullable
    public final CompassRouteGeometry compassRouteGeometry;
    @Nullable
    public final CompassRadiusTransition compassRadiusTransition;
    public final long nextEvaluationDeadlineElapsedMs;
    public final long nowMs;
    public final boolean destinationReached;
    @NonNull
    public final List<NavTarget> targets;
    @NonNull
    public final Context context;

    private NavStateBuildInput(@NonNull Builder builder) {
        route = builder.route;
        index = builder.index;
        alongTrackMeters = builder.alongTrackMeters;
        nextHintIndex = builder.nextHintIndex;
        currentSegmentIndex = builder.currentSegmentIndex;
        speedMps = builder.speedMps;
        etaSpeedMps = builder.etaSpeedMps;
        likelyStationary = builder.likelyStationary;
        accuracyMeters = builder.accuracyMeters;
        compassAccuracyMeters = builder.compassAccuracyMeters;
        currentLocation = builder.currentLocation;
        fixedSatelliteCount = builder.fixedSatelliteCount;
        headingDegrees = builder.headingDegrees;
        headingAccuracyDegrees = builder.headingAccuracyDegrees;
        previousCompassVisibleRadiusMeters = builder.previousCompassVisibleRadiusMeters;
        previousReliableMovingCompassVisibleRadiusMeters = builder.previousReliableMovingCompassVisibleRadiusMeters;
        compassRadiusUpdateDeltaMs = builder.compassRadiusUpdateDeltaMs;
        compassRouteGeometry = builder.compassRouteGeometry;
        compassRadiusTransition = builder.compassRadiusTransition;
        nextEvaluationDeadlineElapsedMs = builder.nextEvaluationDeadlineElapsedMs;
        nowMs = builder.nowMs;
        destinationReached = builder.destinationReached;
        targets = Collections.unmodifiableList(new ArrayList<>(builder.targets));
        context = builder.context;
    }

    @NonNull
    public static Builder builder(
            @NonNull Context context,
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            @NonNull Location currentLocation
    ) {
        return new Builder(context, route, index, currentLocation);
    }

    public static final class Builder {
        @NonNull
        private final Context context;
        @NonNull
        private final GeoJsonRoute route;
        @NonNull
        private final PolylineIndex index;
        @NonNull
        private final Location currentLocation;
        private double alongTrackMeters;
        private int nextHintIndex;
        private int currentSegmentIndex;
        private float speedMps;
        private float etaSpeedMps;
        private boolean likelyStationary;
        private float accuracyMeters;
        private float compassAccuracyMeters;
        @Nullable
        private Integer fixedSatelliteCount;
        @Nullable
        private Double headingDegrees;
        @Nullable
        private Float headingAccuracyDegrees;
        @Nullable
        private Float previousCompassVisibleRadiusMeters;
        @Nullable
        private Float previousReliableMovingCompassVisibleRadiusMeters;
        private long compassRadiusUpdateDeltaMs;
        @Nullable
        private CompassRouteGeometry compassRouteGeometry;
        @Nullable
        private CompassRadiusTransition compassRadiusTransition;
        private long nextEvaluationDeadlineElapsedMs = NavState.NO_DEADLINE;
        private long nowMs;
        private boolean destinationReached;
        @NonNull
        private List<NavTarget> targets = Collections.emptyList();

        private Builder(
                @NonNull Context context,
                @NonNull GeoJsonRoute route,
                @NonNull PolylineIndex index,
                @NonNull Location currentLocation
        ) {
            this.context = context;
            this.route = route;
            this.index = index;
            this.currentLocation = currentLocation;
        }

        @NonNull
        public Builder routeProgress(double alongTrackMeters, int nextHintIndex, int currentSegmentIndex) {
            this.alongTrackMeters = alongTrackMeters;
            this.nextHintIndex = nextHintIndex;
            this.currentSegmentIndex = currentSegmentIndex;
            return this;
        }

        @NonNull
        public Builder motion(
                float speedMps,
                float etaSpeedMps,
                boolean likelyStationary,
                float accuracyMeters,
                float compassAccuracyMeters
        ) {
            this.speedMps = speedMps;
            this.etaSpeedMps = etaSpeedMps;
            this.likelyStationary = likelyStationary;
            this.accuracyMeters = accuracyMeters;
            this.compassAccuracyMeters = compassAccuracyMeters;
            return this;
        }

        @NonNull
        public Builder gps(@Nullable Integer fixedSatelliteCount) {
            this.fixedSatelliteCount = fixedSatelliteCount;
            return this;
        }

        @NonNull
        public Builder heading(@Nullable Double headingDegrees, @Nullable Float headingAccuracyDegrees) {
            this.headingDegrees = headingDegrees;
            this.headingAccuracyDegrees = headingAccuracyDegrees;
            return this;
        }

        @NonNull
        public Builder compassMemory(
                @Nullable Float previousVisibleRadiusMeters,
                @Nullable Float previousReliableMovingVisibleRadiusMeters,
                long radiusUpdateDeltaMs
        ) {
            previousCompassVisibleRadiusMeters = previousVisibleRadiusMeters;
            previousReliableMovingCompassVisibleRadiusMeters = previousReliableMovingVisibleRadiusMeters;
            compassRadiusUpdateDeltaMs = radiusUpdateDeltaMs;
            return this;
        }

        @NonNull
        public Builder compassGeometry(
                @Nullable CompassRouteGeometry routeGeometry,
                @Nullable CompassRadiusTransition radiusTransition
        ) {
            compassRouteGeometry = routeGeometry;
            compassRadiusTransition = radiusTransition;
            return this;
        }

        @NonNull
        public Builder timing(long nextEvaluationDeadlineElapsedMs, long nowMs) {
            this.nextEvaluationDeadlineElapsedMs = nextEvaluationDeadlineElapsedMs;
            this.nowMs = nowMs;
            return this;
        }

        @NonNull
        public Builder destinationReached(boolean destinationReached) {
            this.destinationReached = destinationReached;
            return this;
        }

        @NonNull
        public Builder targets(@NonNull List<NavTarget> targets) {
            this.targets = targets;
            return this;
        }

        @NonNull
        public NavStateBuildInput build() {
            return new NavStateBuildInput(this);
        }
    }
}

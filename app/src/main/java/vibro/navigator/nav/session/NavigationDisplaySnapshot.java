package vibro.navigator.nav.session;

import android.content.Context;
import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.compass.CompassOrientationCue;
import vibro.navigator.nav.format.AndroidNavigationTextResources;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.brouter.NogoPoint;

import java.util.Collections;
import java.util.List;

public final class NavigationDisplaySnapshot {
    @NonNull
    final NavigationTextResources textResources;
    @Nullable
    final NavigationLocation lastFiltered;
    final float speedMps;
    final float displaySpeedMps;
    final boolean likelyStationary;
    final float accuracyMeters;
    @Nullable
    final Integer fixedSatelliteCount;
    final int acquiredFixCount;
    @Nullable
    final Double headingDegrees;
    @Nullable
    final Float headingAccuracyDegrees;
    @Nullable
    final CompassOrientationCue orientationCue;
    @NonNull
    final List<NogoPoint> blockedPoints;
    final long nextEvaluationDeadlineElapsedMs;
    final long nowMs;
    final boolean routeCalculationInProgress;
    @Nullable
    final String routeCalculationNotice;
    @Nullable
    final Throwable lastRouteFailure;
    final boolean compassZoomAnimationEnabled;
    final boolean stationaryFullRouteZoomEnabled;

    private NavigationDisplaySnapshot(@NonNull Builder builder) {
        textResources = builder.textResources;
        lastFiltered = builder.lastFiltered;
        speedMps = builder.speedMps;
        displaySpeedMps = builder.displaySpeedMps;
        likelyStationary = builder.likelyStationary;
        accuracyMeters = builder.accuracyMeters;
        fixedSatelliteCount = builder.fixedSatelliteCount;
        acquiredFixCount = builder.acquiredFixCount;
        headingDegrees = builder.headingDegrees;
        headingAccuracyDegrees = builder.headingAccuracyDegrees;
        orientationCue = builder.orientationCue;
        blockedPoints = builder.blockedPoints;
        nextEvaluationDeadlineElapsedMs = builder.nextEvaluationDeadlineElapsedMs;
        nowMs = builder.nowMs;
        routeCalculationInProgress = builder.routeCalculationInProgress;
        routeCalculationNotice = builder.routeCalculationNotice;
        lastRouteFailure = builder.lastRouteFailure;
        compassZoomAnimationEnabled = builder.compassZoomAnimationEnabled;
        stationaryFullRouteZoomEnabled = builder.stationaryFullRouteZoomEnabled;
    }

    @NonNull
    static Builder builder(@NonNull Context context) {
        return builder(new AndroidNavigationTextResources(context));
    }

    @NonNull
    static Builder builder(@NonNull NavigationTextResources textResources) {
        return new Builder(textResources);
    }

    static final class Builder {
        @NonNull
        private final NavigationTextResources textResources;
        @Nullable
        private NavigationLocation lastFiltered;
        private float speedMps;
        private float displaySpeedMps;
        private boolean likelyStationary;
        private float accuracyMeters = Float.MAX_VALUE;
        @Nullable
        private Integer fixedSatelliteCount;
        private int acquiredFixCount;
        @Nullable
        private Double headingDegrees;
        @Nullable
        private Float headingAccuracyDegrees;
        @Nullable
        private CompassOrientationCue orientationCue;
        @NonNull
        private List<NogoPoint> blockedPoints = Collections.emptyList();
        private long nextEvaluationDeadlineElapsedMs;
        private long nowMs;
        private boolean routeCalculationInProgress;
        @Nullable
        private String routeCalculationNotice;
        @Nullable
        private Throwable lastRouteFailure;
        private boolean compassZoomAnimationEnabled = true;
        private boolean stationaryFullRouteZoomEnabled = true;

        private Builder(@NonNull NavigationTextResources textResources) {
            this.textResources = textResources;
        }

        @NonNull
        Builder location(
                @Nullable NavigationLocation lastFiltered,
                float speedMps,
                boolean likelyStationary,
                float accuracyMeters
        ) {
            return location(lastFiltered, speedMps, speedMps, likelyStationary, accuracyMeters);
        }

        @NonNull
        Builder location(
                @Nullable NavigationLocation lastFiltered,
                float speedMps,
                float displaySpeedMps,
                boolean likelyStationary,
                float accuracyMeters
        ) {
            this.lastFiltered = lastFiltered;
            this.speedMps = speedMps;
            this.displaySpeedMps = displaySpeedMps;
            this.likelyStationary = likelyStationary;
            this.accuracyMeters = accuracyMeters;
            return this;
        }

        @NonNull
        Builder gps(@Nullable Integer fixedSatelliteCount, int acquiredFixCount) {
            this.fixedSatelliteCount = fixedSatelliteCount;
            this.acquiredFixCount = acquiredFixCount;
            return this;
        }

        @NonNull
        Builder heading(@Nullable Double headingDegrees, @Nullable Float headingAccuracyDegrees) {
            this.headingDegrees = headingDegrees;
            this.headingAccuracyDegrees = headingAccuracyDegrees;
            return this;
        }

        @NonNull
        Builder orientationCue(@Nullable CompassOrientationCue orientationCue) {
            this.orientationCue = orientationCue;
            return this;
        }

        @NonNull
        Builder blockedPoints(@NonNull List<NogoPoint> blockedPoints) {
            this.blockedPoints = blockedPoints;
            return this;
        }

        @NonNull
        Builder timing(long nextEvaluationDeadlineElapsedMs, long nowMs) {
            this.nextEvaluationDeadlineElapsedMs = nextEvaluationDeadlineElapsedMs;
            this.nowMs = nowMs;
            return this;
        }

        @NonNull
        Builder routeCalculation(
                boolean routeCalculationInProgress,
                @Nullable String routeCalculationNotice,
                @Nullable Throwable lastRouteFailure
        ) {
            this.routeCalculationInProgress = routeCalculationInProgress;
            this.routeCalculationNotice = routeCalculationNotice;
            this.lastRouteFailure = lastRouteFailure;
            return this;
        }

        @NonNull
        Builder compassZoomAnimationEnabled(boolean enabled) {
            compassZoomAnimationEnabled = enabled;
            return this;
        }

        @NonNull
        Builder stationaryFullRouteZoomEnabled(boolean enabled) {
            stationaryFullRouteZoomEnabled = enabled;
            return this;
        }

        @NonNull
        NavigationDisplaySnapshot build() {
            return new NavigationDisplaySnapshot(this);
        }
    }
}

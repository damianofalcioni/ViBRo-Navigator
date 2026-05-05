package vibro.navigator.nav.presentation;

import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.nav.compass.NavCompassStateInput;
import vibro.navigator.nav.model.NavTarget;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;

public final class NavStateBuildInput {
    private static final long NO_DEADLINE = -1L;

    @NonNull
    public final GeoJsonRoute route;
    @NonNull
    public final PolylineIndex index;
    @NonNull
    public final Location currentLocation;
    @NonNull
    public final Context context;
    @NonNull
    public final RouteProgress routeProgress;
    @NonNull
    public final Motion motion;
    @NonNull
    public final Gps gps;
    @NonNull
    public final Heading heading;
    @Nullable
    public final NavCompassStateInput compassInput;
    @NonNull
    public final Timing timing;
    public final boolean destinationReached;
    @NonNull
    public final List<NavTarget> targets;

    private NavStateBuildInput(@NonNull Builder builder) {
        route = builder.route;
        index = builder.index;
        currentLocation = builder.currentLocation;
        context = builder.context;
        routeProgress = builder.routeProgress;
        motion = builder.motion;
        gps = builder.gps;
        heading = builder.heading;
        compassInput = builder.compassInput;
        timing = builder.timing;
        destinationReached = builder.destinationReached;
        targets = Collections.unmodifiableList(new ArrayList<>(builder.targets));
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

    public static final class RouteProgress {
        public final double alongTrackMeters;
        public final int nextHintIndex;
        public final int currentSegmentIndex;

        private RouteProgress(double alongTrackMeters, int nextHintIndex, int currentSegmentIndex) {
            this.alongTrackMeters = alongTrackMeters;
            this.nextHintIndex = nextHintIndex;
            this.currentSegmentIndex = currentSegmentIndex;
        }
    }

    public static final class Motion {
        public final float speedMps;
        public final float etaSpeedMps;
        public final boolean likelyStationary;
        public final float accuracyMeters;
        public final float compassAccuracyMeters;

        private Motion(
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
        }
    }

    public static final class Gps {
        @Nullable
        public final Integer fixedSatelliteCount;

        private Gps(@Nullable Integer fixedSatelliteCount) {
            this.fixedSatelliteCount = fixedSatelliteCount;
        }
    }

    public static final class Heading {
        @Nullable
        public final Double headingDegrees;
        @Nullable
        public final Float headingAccuracyDegrees;

        private Heading(@Nullable Double headingDegrees, @Nullable Float headingAccuracyDegrees) {
            this.headingDegrees = headingDegrees;
            this.headingAccuracyDegrees = headingAccuracyDegrees;
        }
    }

    public static final class Timing {
        public final long nextEvaluationDeadlineElapsedMs;
        public final long nowMs;

        private Timing(long nextEvaluationDeadlineElapsedMs, long nowMs) {
            this.nextEvaluationDeadlineElapsedMs = nextEvaluationDeadlineElapsedMs;
            this.nowMs = nowMs;
        }
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
        @NonNull
        private RouteProgress routeProgress = new RouteProgress(0.0, 0, 0);
        @NonNull
        private Motion motion = new Motion(0f, 0f, false, 0f, 0f);
        @NonNull
        private Gps gps = new Gps(null);
        @NonNull
        private Heading heading = new Heading(null, null);
        @Nullable
        private NavCompassStateInput compassInput;
        @NonNull
        private Timing timing = new Timing(NO_DEADLINE, 0L);
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
            routeProgress = new RouteProgress(alongTrackMeters, nextHintIndex, currentSegmentIndex);
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
            motion = new Motion(speedMps, etaSpeedMps, likelyStationary, accuracyMeters, compassAccuracyMeters);
            return this;
        }

        @NonNull
        public Builder gps(@Nullable Integer fixedSatelliteCount) {
            gps = new Gps(fixedSatelliteCount);
            return this;
        }

        @NonNull
        public Builder heading(@Nullable Double headingDegrees, @Nullable Float headingAccuracyDegrees) {
            heading = new Heading(headingDegrees, headingAccuracyDegrees);
            return this;
        }

        @NonNull
        public Builder compass(@Nullable NavCompassStateInput compassInput) {
            this.compassInput = compassInput;
            return this;
        }

        @NonNull
        public Builder timing(long nextEvaluationDeadlineElapsedMs, long nowMs) {
            timing = new Timing(nextEvaluationDeadlineElapsedMs, nowMs);
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

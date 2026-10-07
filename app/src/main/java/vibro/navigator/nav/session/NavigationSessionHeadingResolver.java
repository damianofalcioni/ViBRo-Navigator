package vibro.navigator.nav.session;

import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.orientation.StationaryCompassHeadingGate;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class NavigationSessionHeadingResolver {
    // A retained orientation is useful, but a moving fix older than 10 seconds is no longer live.
    private static final long MAX_LIVE_TRAVEL_HEADING_AGE_MS = 10_000L;
    private static final float HELD_HEADING_UNCERTAINTY_DEGREES = 90f;

    private final NavigationSessionLocationState locationState;
    private final StationaryCompassHeadingGate stationaryCompassGate = new StationaryCompassHeadingGate();
    private final NavigationRouteHeadingFallback routeHeadingFallback = new NavigationRouteHeadingFallback();
    private Selection lastHeading = Selection.none();
    private Selection lastTravelHeading = Selection.none();
    private boolean movementStarted;
    @Nullable
    private GeoJsonRoute headingRoute;
    private int motionRevision;

    public NavigationSessionHeadingResolver(@NonNull NavigationSessionLocationState locationState) {
        this.locationState = locationState;
    }

    public void reset() {
        stationaryCompassGate.reset();
        routeHeadingFallback.reset();
        lastHeading = Selection.none();
        lastTravelHeading = Selection.none();
        movementStarted = false;
        headingRoute = null;
    }

    void synchronizeRoute(@Nullable GeoJsonRoute route) {
        if (headingRoute != route) {
            routeHeadingFallback.reset();
            headingRoute = route;
        }
    }

    void clearRouteDisagreement() {
        routeHeadingFallback.reset();
    }

    private void synchronizeMotion() {
        if (motionRevision != locationState.motionRevision()) {
            routeHeadingFallback.reset();
            motionRevision = locationState.motionRevision();
        }
    }

    @NonNull
    public Selection selectHeading(
            @Nullable NavigationLocation lastFiltered,
            boolean likelyStationary,
            @Nullable Double displayHeadingDegrees,
            @Nullable Float displayHeadingAccuracyDegrees,
            long nowMs,
            @Nullable Double routeHeadingDegrees,
            boolean beelineGuidance
    ) {
        synchronizeMotion();
        if (!movementStarted) {
            selectStartupHeading(displayHeadingDegrees, displayHeadingAccuracyDegrees);
        }
        if (lastFiltered == null) {
            return lastHeading;
        }
        if (!likelyStationary) {
            movementStarted = true;
            stationaryCompassGate.reset();
            lastHeading = selectMovingHeading(lastFiltered, nowMs, routeHeadingDegrees, beelineGuidance,
                    displayHeadingDegrees, displayHeadingAccuracyDegrees);
        } else if (movementStarted) {
            routeHeadingFallback.reset();
            lastHeading = selectStationaryHeading(displayHeadingDegrees, displayHeadingAccuracyDegrees, nowMs);
        }
        return lastHeading;
    }

    @NonNull
    private Selection selectStartupHeading(@Nullable Double headingDegrees, @Nullable Float accuracyDegrees) {
        if (StationaryCompassHeadingGate.isUsable(headingDegrees, accuracyDegrees)) {
            lastHeading = new Selection(headingDegrees, accuracyDegrees);
        } else {
            lastHeading = uncertain(lastHeading);
        }
        return lastHeading;
    }

    @NonNull
    private Selection selectStationaryHeading(@Nullable Double headingDegrees, @Nullable Float accuracyDegrees, long nowMs) {
        if (stationaryCompassGate.accept(headingDegrees, accuracyDegrees, nowMs)) {
            return new Selection(headingDegrees, accuracyDegrees);
        }
        return StationaryCompassHeadingGate.isUsable(headingDegrees, accuracyDegrees)
                ? lastHeading : uncertain(lastHeading);
    }

    @NonNull
    private Selection selectMovingHeading(
            @NonNull NavigationLocation location,
            long nowMs,
            @Nullable Double routeHeadingDegrees,
            boolean beelineGuidance,
            @Nullable Double compassHeadingDegrees,
            @Nullable Float compassAccuracyDegrees
    ) {
        Selection travelHeading = selectTravelHeading(location, nowMs, routeHeadingDegrees, beelineGuidance);
        if (travelHeading.hasHeading()) {
            lastTravelHeading = travelHeading;
            return travelHeading;
        }
        if (StationaryCompassHeadingGate.isUsable(compassHeadingDegrees, compassAccuracyDegrees)) {
            return new Selection(compassHeadingDegrees, compassAccuracyDegrees);
        }
        return uncertain(lastTravelHeading.hasHeading() ? lastTravelHeading : lastHeading);
    }

    @NonNull
    private Selection selectTravelHeading(
            @NonNull NavigationLocation location,
            long nowMs,
            @Nullable Double routeHeadingDegrees,
            boolean beelineGuidance
    ) {
        if (!beelineGuidance) {
            return routeHeadingDegrees != null && Double.isFinite(routeHeadingDegrees)
                    ? selectRouteHeading(location, nowMs, routeHeadingDegrees) : noRouteHeading();
        }
        routeHeadingFallback.reset();
        long ageMs = nowMs - location.getElapsedRealtimeOrTimeMs();
        return ageMs >= 0L && ageMs <= MAX_LIVE_TRAVEL_HEADING_AGE_MS
                ? selectLocationHeading(location) : Selection.none();
    }

    private Selection noRouteHeading() {
        routeHeadingFallback.reset();
        return Selection.none();
    }

    private Selection selectRouteHeading(NavigationLocation location, long nowMs, double routeHeading) {
        long ageMs = nowMs - location.getElapsedRealtimeOrTimeMs();
        if (ageMs < 0L || ageMs > MAX_LIVE_TRAVEL_HEADING_AGE_MS) {
            // Sparse acquisitions must not confirm a source change through repeated UI refreshes.
            return new Selection(routeHeading, 0f);
        }
        NavigationSessionLocationState.HeadingEstimate heading = locationState.routeDisagreementHeading(location);
        if (heading == null) {
            routeHeadingFallback.reset();
            return new Selection(routeHeading, 0f);
        }
        return routeHeadingFallback.useLocationHeading(location, heading.headingDegrees,
                heading.headingAccuracyDegrees, routeHeading)
                ? new Selection(heading.headingDegrees, heading.headingAccuracyDegrees)
                : new Selection(routeHeading, 0f);
    }

    @NonNull
    private static Selection uncertain(@NonNull Selection heading) {
        return heading.hasHeading()
                ? new Selection(heading.headingDegrees, HELD_HEADING_UNCERTAINTY_DEGREES)
                : heading;
    }

    @NonNull
    private Selection selectLocationHeading(@NonNull NavigationLocation lastFiltered) {
        NavigationSessionLocationState.HeadingEstimate locationHeading =
                locationState.preferredLocationHeading(lastFiltered, false);
        if (locationHeading == null) {
            return Selection.none();
        }
        return StationaryCompassHeadingGate.isUsable(locationHeading.headingDegrees, locationHeading.headingAccuracyDegrees)
                ? new Selection(locationHeading.headingDegrees, locationHeading.headingAccuracyDegrees)
                : Selection.none();
    }

    public static final class Selection {
        @Nullable
        public final Double headingDegrees;
        @Nullable
        public final Float headingAccuracyDegrees;

        private Selection(@Nullable Double headingDegrees, @Nullable Float headingAccuracyDegrees) {
            this.headingDegrees = headingDegrees;
            this.headingAccuracyDegrees = headingAccuracyDegrees;
        }

        @NonNull
        public static Selection none() {
            return new Selection(null, null);
        }

        public boolean hasHeading() {
            return headingDegrees != null;
        }
    }
}

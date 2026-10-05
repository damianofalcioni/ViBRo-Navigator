package vibro.navigator.nav.session;

import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.orientation.StationaryCompassHeadingGate;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class NavigationSessionHeadingResolver {
    // A retained orientation is useful, but a moving fix older than 10 seconds is no longer live.
    private static final long MAX_LIVE_TRAVEL_HEADING_AGE_MS = 10_000L;
    private static final float HELD_HEADING_UNCERTAINTY_DEGREES = 90f;

    private final NavigationSessionLocationState locationState;
    private final StationaryCompassHeadingGate stationaryCompassGate = new StationaryCompassHeadingGate();
    private Selection lastHeading = Selection.none();
    private Selection lastTravelHeading = Selection.none();
    private boolean movementStarted;

    public NavigationSessionHeadingResolver(@NonNull NavigationSessionLocationState locationState) {
        this.locationState = locationState;
    }

    public void reset() {
        stationaryCompassGate.reset();
        lastHeading = Selection.none();
        lastTravelHeading = Selection.none();
        movementStarted = false;
    }

    @NonNull
    public Selection selectHeading(
            @Nullable NavigationLocation lastFiltered,
            boolean likelyStationary,
            @Nullable Double displayHeadingDegrees,
            @Nullable Float displayHeadingAccuracyDegrees,
            long nowMs
    ) {
        if (!movementStarted) {
            selectStartupHeading(displayHeadingDegrees, displayHeadingAccuracyDegrees);
        }
        if (lastFiltered == null) {
            return lastHeading;
        }
        if (!likelyStationary) {
            movementStarted = true;
            stationaryCompassGate.reset();
            lastHeading = selectMovingHeading(lastFiltered, nowMs);
        } else if (movementStarted) {
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
    private Selection selectMovingHeading(@NonNull NavigationLocation location, long nowMs) {
        long ageMs = nowMs - location.getElapsedRealtimeOrTimeMs();
        Selection locationHeading = ageMs >= 0L && ageMs <= MAX_LIVE_TRAVEL_HEADING_AGE_MS
                ? selectLocationHeading(location) : Selection.none();
        if (locationHeading.hasHeading()) {
            lastTravelHeading = locationHeading;
            return locationHeading;
        }
        return uncertain(lastTravelHeading.hasHeading() ? lastTravelHeading : lastHeading);
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
        return new Selection(
                locationHeading.headingDegrees,
                locationHeading.headingAccuracyDegrees
        );
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

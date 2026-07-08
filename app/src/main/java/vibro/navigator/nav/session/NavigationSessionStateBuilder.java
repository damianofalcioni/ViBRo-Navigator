package vibro.navigator.nav.session;

import android.content.Context;
import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.compass.CompassOrientationCue;
import vibro.navigator.nav.format.AndroidNavigationTextResources;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.presentation.NavStateComposer;
import vibro.navigator.nav.presentation.NavStateResourceComposer;
import vibro.navigator.nav.routing.NavigationRouteRequestManager;

final class NavigationSessionStateBuilder {
    @NonNull
    private final NavigationSessionLocationState locationState;
    @NonNull
    private final NavigationSessionHeadingResolver headingResolver;
    @NonNull
    private final NavigationSessionRouteState routeState;
    @NonNull
    private final StraightLineNavigationState straightLineState;
    @NonNull
    private final NavigationRouteRequestManager routeRequestManager;
    @NonNull
    private final NavigationTripStatsTracker tripStatsTracker;
    @NonNull
    private final NavigationAcceptedFixHistory acceptedFixHistory;

    NavigationSessionStateBuilder(
            @NonNull NavigationSessionLocationState locationState,
            @NonNull NavigationSessionHeadingResolver headingResolver,
            @NonNull NavigationSessionRouteState routeState,
            @NonNull StraightLineNavigationState straightLineState,
            @NonNull NavigationRouteRequestManager routeRequestManager,
            @NonNull NavigationTripStatsTracker tripStatsTracker,
            @NonNull NavigationAcceptedFixHistory acceptedFixHistory
    ) {
        this.locationState = locationState;
        this.headingResolver = headingResolver;
        this.routeState = routeState;
        this.straightLineState = straightLineState;
        this.routeRequestManager = routeRequestManager;
        this.tripStatsTracker = tripStatsTracker;
        this.acceptedFixHistory = acceptedFixHistory;
    }

    @NonNull
    NavState build(
            @NonNull Context context,
            @NonNull NavigationRequest currentRequest,
            long nextEvaluationDeadlineElapsedMs,
            long nowMs,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Double displayHeadingDegrees,
            @Nullable Float displayHeadingAccuracyDegrees,
            @Nullable CompassOrientationCue orientationCue,
            int acquiredFixCount,
            boolean paused
    ) {
        return build(
                new AndroidNavigationTextResources(context),
                currentRequest,
                nextEvaluationDeadlineElapsedMs,
                nowMs,
                fixedSatelliteCount,
                displayHeadingDegrees,
                displayHeadingAccuracyDegrees,
                orientationCue,
                acquiredFixCount,
                paused
        );
    }

    @NonNull
    NavState build(
            @NonNull NavigationTextResources textResources,
            @NonNull NavigationRequest currentRequest,
            long nextEvaluationDeadlineElapsedMs,
            long nowMs,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Double displayHeadingDegrees,
            @Nullable Float displayHeadingAccuracyDegrees,
            @Nullable CompassOrientationCue orientationCue,
            int acquiredFixCount,
            boolean paused
    ) {
        NavigationDisplaySnapshot snapshot = buildSnapshot(
                textResources,
                nextEvaluationDeadlineElapsedMs,
                nowMs,
                fixedSatelliteCount,
                displayHeadingDegrees,
                displayHeadingAccuracyDegrees,
                orientationCue,
                acquiredFixCount
        );
        if (currentRequest.isStraightLine()) {
            NavState baseState = NavStateResourceComposer.withPauseState(
                    textResources,
                    straightLineState.buildState(currentRequest, snapshot, acceptedFixHistory.snapshot()),
                    paused
            );
            return NavStateComposer.withTripStatus(baseState, tripStatsTracker.snapshot());
        }
        NavState baseState = routeState.advanceDisplayState(snapshot, currentRequest.isRoundTrip());
        if (currentRequest.isRoundTrip()) {
            baseState = NavStateComposer.withBlockedRoadActionAvailable(baseState, false);
        }
        return NavStateComposer.withTripStatus(
                NavStateResourceComposer.withPauseState(textResources, baseState, paused),
                tripStatsTracker.snapshot()
        );
    }

    @NonNull
    private NavigationDisplaySnapshot buildSnapshot(
            @NonNull NavigationTextResources textResources,
            long nextEvaluationDeadlineElapsedMs,
            long nowMs,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Double displayHeadingDegrees,
            @Nullable Float displayHeadingAccuracyDegrees,
            @Nullable CompassOrientationCue orientationCue,
            int acquiredFixCount
    ) {
        NavigationLocation lastFiltered = locationState.getLastFilteredLocation();
        float speedMps = lastFiltered != null ? locationState.speedMps(lastFiltered) : 0f;
        float displaySpeedMps = lastFiltered != null ? locationState.displaySpeedMps(lastFiltered) : 0f;
        boolean likelyStationary = locationState.isLikelyStationary();
        float accuracyMeters = lastFiltered != null
                ? locationState.accuracyMeters(lastFiltered)
                : Float.MAX_VALUE;
        NavigationSessionHeadingResolver.Selection heading = headingResolver.selectHeading(
                lastFiltered,
                likelyStationary,
                displayHeadingDegrees,
                displayHeadingAccuracyDegrees
        );
        return NavigationDisplaySnapshot.builder(textResources)
                .location(lastFiltered, speedMps, displaySpeedMps, likelyStationary, accuracyMeters)
                .gps(fixedSatelliteCount, acquiredFixCount)
                .heading(heading.headingDegrees, heading.headingAccuracyDegrees)
                .orientationCue(orientationCue)
                .blockedPoints(routeState.copyBlockedPoints())
                .timing(nextEvaluationDeadlineElapsedMs, nowMs)
                .routeCalculation(
                        routeRequestManager.isRouteCalculationInProgress(),
                        routeRequestManager.getInProgressNotice(),
                        routeRequestManager.getLastRouteFailure()
                )
                .build();
    }
}

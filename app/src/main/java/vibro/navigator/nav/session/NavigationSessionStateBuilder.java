package vibro.navigator.nav.session;

import android.content.Context;
import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.compass.CompassOrientationCue;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.presentation.NavStateComposer;
import vibro.navigator.nav.routing.NavigationRouteRequestManager;

final class NavigationSessionStateBuilder {
    @NonNull
    private final NavigationSessionLocationState locationState;
    @NonNull
    private final NavigationSessionHeadingResolver headingResolver;
    @NonNull
    private final NavigationSessionRouteState routeState;
    @NonNull
    private final NavigationRouteRequestManager routeRequestManager;

    NavigationSessionStateBuilder(
            @NonNull NavigationSessionLocationState locationState,
            @NonNull NavigationSessionHeadingResolver headingResolver,
            @NonNull NavigationSessionRouteState routeState,
            @NonNull NavigationRouteRequestManager routeRequestManager
    ) {
        this.locationState = locationState;
        this.headingResolver = headingResolver;
        this.routeState = routeState;
        this.routeRequestManager = routeRequestManager;
    }

    @NonNull
    NavState build(
            @NonNull Context context,
            long nextEvaluationDeadlineElapsedMs,
            long nowMs,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Double displayHeadingDegrees,
            @Nullable Float displayHeadingAccuracyDegrees,
            @Nullable CompassOrientationCue orientationCue,
            int acquiredFixCount,
            boolean paused
    ) {
        NavigationLocation lastFiltered = locationState.getLastFilteredLocation();
        float speedMps = lastFiltered != null ? locationState.speedMps(lastFiltered) : 0f;
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
        NavigationDisplaySnapshot snapshot = NavigationDisplaySnapshot.builder(context)
                .location(lastFiltered, speedMps, likelyStationary, accuracyMeters)
                .gps(fixedSatelliteCount, acquiredFixCount)
                .heading(heading.headingDegrees, heading.headingAccuracyDegrees)
                .orientationCue(orientationCue)
                .timing(nextEvaluationDeadlineElapsedMs, nowMs)
                .routeCalculation(
                        routeRequestManager.isRouteCalculationInProgress(),
                        routeRequestManager.getInProgressNotice(),
                        routeRequestManager.getLastRouteFailure()
                )
                .build();
        NavState baseState = routeState.advanceDisplayState(snapshot);
        return NavStateComposer.withPauseState(context, baseState, paused);
    }
}

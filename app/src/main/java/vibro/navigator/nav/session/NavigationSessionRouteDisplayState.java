package vibro.navigator.nav.session;


import vibro.navigator.nav.routing.NavigationRouteFailureFormatter;
import vibro.navigator.nav.guidance.NavigationRouteProgressTracker;
import vibro.navigator.nav.guidance.NavigationTurnState;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.model.NavStateBuildInput;
import vibro.navigator.nav.model.NavStateComposer;
import vibro.navigator.nav.model.NavTarget;
import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;

import java.util.ArrayList;
import java.util.List;

public final class NavigationSessionRouteDisplayState {

    @NonNull
    private List<NavTarget> targets = new ArrayList<>();
    @NonNull
    private final CompassDisplayMemory compassMemory = new CompassDisplayMemory();

    public void reset() {
        targets = new ArrayList<>();
        compassMemory.reset();
    }

    public void rememberSmoothedAccuracyMeters(float smoothedAccuracyMeters) {
        compassMemory.rememberSmoothedAccuracyMeters(smoothedAccuracyMeters);
    }

    public void onRouteApplied(
            @NonNull Context context,
            @NonNull NavigationRequest request,
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex
    ) {
        compassMemory.onRouteApplied(route, polylineIndex);
        targets = buildTargets(context, request.stops, polylineIndex);
    }

    @NonNull
    public NavState buildState(
            @NonNull Context context,
            @Nullable GeoJsonRoute route,
            @Nullable PolylineIndex polylineIndex,
            int lastSegmentIndex,
            @NonNull NavigationTurnState turnState,
            @NonNull NavigationRouteProgressTracker progressTracker,
            @Nullable Location lastFiltered,
            float speedMps,
            boolean likelyStationary,
            float accuracyMeters,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Double headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            long nextEvaluationDeadlineElapsedMs,
            long nowMs,
            boolean routeCalculationInProgress,
            @Nullable String routeCalculationNotice,
            @Nullable Throwable lastRouteFailure
    ) {
        String gpsStatusLine = buildGpsStatusLine(context, lastFiltered, speedMps, accuracyMeters, fixedSatelliteCount);
        if (lastFiltered == null) {
            return buildStateWithoutLocation(context, nextEvaluationDeadlineElapsedMs, lastRouteFailure, gpsStatusLine);
        }

        if (routeCalculationInProgress) {
            return buildCalculatingState(context, nextEvaluationDeadlineElapsedMs, routeCalculationNotice, gpsStatusLine);
        }

        if (route == null || polylineIndex == null) {
            return buildStateWithoutRoute(context, nextEvaluationDeadlineElapsedMs, lastRouteFailure, gpsStatusLine);
        }

        PolylineIndex.Match match = polylineIndex.match(
                new LatLon(lastFiltered.getLatitude(), lastFiltered.getLongitude()),
                lastSegmentIndex
        );
        if (match == null) {
            return NavStateComposer.withGpsStatus(NavStateComposer.waiting(context), gpsStatusLine);
        }

        float etaSpeedMps = progressTracker.resolveEtaSpeedMps(
                lastFiltered,
                match.alongTrackMeters,
                accuracyMeters,
                likelyStationary
        );
        NavState state = NavStateComposer.from(NavStateBuildInput
                .builder(context, route, polylineIndex, lastFiltered)
                .routeProgress(match.alongTrackMeters, turnState.getNextHintIdx(), match.segmentIndex)
                .motion(
                        speedMps,
                        etaSpeedMps,
                        likelyStationary,
                        accuracyMeters,
                        compassMemory.resolveAccuracyMeters(accuracyMeters)
                )
                .gps(fixedSatelliteCount)
                .heading(headingDegrees, headingAccuracyDegrees)
                .compassMemory(
                        compassMemory.lastVisibleRadiusMeters(),
                        compassMemory.lastReliableMovingVisibleRadiusMeters(),
                        compassMemory.resolveRadiusUpdateDeltaMs(nowMs)
                )
                .compassGeometry(compassMemory.routeGeometry(), compassMemory.radiusTransition())
                .timing(nextEvaluationDeadlineElapsedMs, nowMs)
                .destinationReached(turnState.isDestinationReached())
                .targets(targets)
                .build());
        compassMemory.rememberCompassState(state, nowMs, lastFiltered, likelyStationary);
        return withLastRouteFailureNotice(context, state, lastRouteFailure);
    }

    @NonNull
    private String buildGpsStatusLine(
            @NonNull Context context,
            @Nullable Location lastFiltered,
            float speedMps,
            float accuracyMeters,
            @Nullable Integer fixedSatelliteCount
    ) {
        if (lastFiltered == null) {
            return NavStateComposer.buildGpsStatusLine(Float.NaN, null, Float.NaN, fixedSatelliteCount, context);
        }
        return NavStateComposer.buildGpsStatusLine(speedMps, lastFiltered, accuracyMeters, fixedSatelliteCount, context);
    }

    @NonNull
    private NavState buildStateWithoutLocation(
            @NonNull Context context,
            long nextEvaluationDeadlineElapsedMs,
            @Nullable Throwable lastRouteFailure,
            @NonNull String gpsStatusLine
    ) {
        if (lastRouteFailure != null) {
            return NavStateComposer.withGpsStatus(NavStateComposer.routeUnavailable(
                    context,
                    NavigationRouteFailureFormatter.format(context, lastRouteFailure, false),
                    nextEvaluationDeadlineElapsedMs
            ), gpsStatusLine);
        }
        return NavStateComposer.withGpsStatus(
                NavStateComposer.waitingForLocation(context, nextEvaluationDeadlineElapsedMs),
                gpsStatusLine
        );
    }

    @NonNull
    private NavState buildCalculatingState(
            @NonNull Context context,
            long nextEvaluationDeadlineElapsedMs,
            @Nullable String routeCalculationNotice,
            @NonNull String gpsStatusLine
    ) {
        NavState calculatingState = NavStateComposer.calculatingRoute(context, nextEvaluationDeadlineElapsedMs);
        if (routeCalculationNotice != null && !routeCalculationNotice.trim().isEmpty()) {
            calculatingState = NavStateComposer.withNotice(calculatingState, routeCalculationNotice);
        }
        return NavStateComposer.withGpsStatus(calculatingState, gpsStatusLine);
    }

    @NonNull
    private NavState buildStateWithoutRoute(
            @NonNull Context context,
            long nextEvaluationDeadlineElapsedMs,
            @Nullable Throwable lastRouteFailure,
            @NonNull String gpsStatusLine
    ) {
        if (lastRouteFailure != null) {
            return NavStateComposer.withGpsStatus(NavStateComposer.routeUnavailable(
                    context,
                    NavigationRouteFailureFormatter.format(context, lastRouteFailure, false),
                    nextEvaluationDeadlineElapsedMs
            ), gpsStatusLine);
        }
        return NavStateComposer.withGpsStatus(
                NavStateComposer.calculatingRoute(context, nextEvaluationDeadlineElapsedMs),
                gpsStatusLine
        );
    }

    @NonNull
    private NavState withLastRouteFailureNotice(
            @NonNull Context context,
            @NonNull NavState state,
            @Nullable Throwable lastRouteFailure
    ) {
        return lastRouteFailure != null
                ? NavStateComposer.withNotice(state, NavigationRouteFailureFormatter.format(context, lastRouteFailure, true))
                : state;
    }

    @NonNull
    private List<NavTarget> buildTargets(
            @NonNull Context context,
            @NonNull List<LatLon> intermediates,
            @NonNull PolylineIndex index
    ) {
        List<NavTarget> out = new ArrayList<>();
        for (int i = 0; i < intermediates.size(); i++) {
            PolylineIndex.Match match = index.match(intermediates.get(i), -1);
            if (match != null) {
                out.add(new NavTarget(context.getString(R.string.format_stop_label, i + 1), match.alongTrackMeters));
            }
        }
        out.add(new NavTarget(context.getString(R.string.nav_destination_label), index.totalLengthMeters()));
        return out;
    }

}

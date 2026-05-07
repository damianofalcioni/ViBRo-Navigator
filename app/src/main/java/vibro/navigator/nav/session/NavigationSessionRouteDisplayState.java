package vibro.navigator.nav.session;


import vibro.navigator.nav.routing.NavigationRouteFailureFormatter;
import vibro.navigator.nav.guidance.NavigationRouteProgressTracker;
import vibro.navigator.nav.guidance.NavigationTurnState;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.presentation.NavStateBuildInput;
import vibro.navigator.nav.presentation.NavStateComposer;
import vibro.navigator.nav.model.NavTarget;
import vibro.navigator.nav.compass.NavCompassStateInput;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.NavigationRouteGeometryState;
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
            @NonNull NavigationDisplaySnapshot snapshot,
            @Nullable GeoJsonRoute route,
            @Nullable PolylineIndex polylineIndex,
            int lastSegmentIndex,
            @NonNull NavigationTurnState turnState,
            @NonNull NavigationRouteProgressTracker progressTracker
    ) {
        String gpsStatusLine = buildGpsStatusLine(snapshot);
        if (snapshot.lastFiltered == null) {
            return buildStateWithoutLocation(snapshot, gpsStatusLine);
        }

        if (snapshot.routeCalculationInProgress) {
            return buildCalculatingState(snapshot, gpsStatusLine);
        }

        if (route == null || polylineIndex == null) {
            return buildStateWithoutRoute(snapshot, gpsStatusLine);
        }

        PolylineIndex.Match match = polylineIndex.match(
                new LatLon(snapshot.lastFiltered.getLatitude(), snapshot.lastFiltered.getLongitude()),
                lastSegmentIndex
        );
        if (match == null) {
            return NavStateComposer.withGpsStatus(NavStateComposer.waiting(snapshot.context), gpsStatusLine);
        }

        float etaSpeedMps = progressTracker.resolveEtaSpeedMps(
                snapshot.lastFiltered,
                match.alongTrackMeters,
                snapshot.accuracyMeters,
                snapshot.likelyStationary
        );
        NavCompassStateInput compassInput = NavCompassStateInput.builder(route, polylineIndex, snapshot.lastFiltered)
                .routeProgress(match.alongTrackMeters)
                .motion(
                        snapshot.speedMps,
                        snapshot.likelyStationary,
                        compassMemory.resolveAccuracyMeters(snapshot.accuracyMeters)
                )
                .destinationReachedRadiusMeters((float)
                        NavigationRouteGeometryState.resolveDestinationReachedRadiusMeters(snapshot.accuracyMeters))
                .heading(snapshot.headingDegrees, snapshot.headingAccuracyDegrees)
                .radiusMemory(
                        compassMemory.lastVisibleRadiusMeters(),
                        compassMemory.lastReliableMovingVisibleRadiusMeters(),
                        compassMemory.resolveRadiusUpdateDeltaMs(snapshot.nowMs)
                )
                .geometry(compassMemory.routeGeometry(), compassMemory.radiusTransition())
                .orientationCue(snapshot.orientationCue)
                .nowMs(snapshot.nowMs)
                .build();
        NavState state = NavStateComposer.from(NavStateBuildInput
                .builder(snapshot.context, route, polylineIndex, snapshot.lastFiltered)
                .routeProgress(match.alongTrackMeters, turnState.getNextHintIdx(), match.segmentIndex)
                .motion(
                        snapshot.speedMps,
                        etaSpeedMps,
                        snapshot.likelyStationary,
                        snapshot.accuracyMeters,
                        compassMemory.resolveAccuracyMeters(snapshot.accuracyMeters)
                )
                .gps(snapshot.fixedSatelliteCount)
                .heading(snapshot.headingDegrees, snapshot.headingAccuracyDegrees)
                .compass(compassInput)
                .timing(snapshot.nextEvaluationDeadlineElapsedMs, snapshot.nowMs)
                .destinationReached(turnState.isDestinationReached())
                .intermediateDestinationReachedTrackIndex(turnState.getIntermediateDestinationReachedTrackIndex())
                .targets(targets)
                .build());
        return withLastRouteFailureNotice(snapshot, state);
    }

    @NonNull
    public void rememberRenderedState(@NonNull NavState state, @NonNull NavigationDisplaySnapshot snapshot) {
        if (snapshot.lastFiltered != null) {
            compassMemory.rememberCompassState(
                    state,
                    snapshot.nowMs,
                    snapshot.lastFiltered,
                    snapshot.likelyStationary
            );
        }
    }

    @NonNull
    private String buildGpsStatusLine(@NonNull NavigationDisplaySnapshot snapshot) {
        if (snapshot.lastFiltered == null) {
            return NavStateComposer.buildGpsStatusLine(
                    Float.NaN,
                    null,
                    Float.NaN,
                    snapshot.fixedSatelliteCount,
                    snapshot.context
            );
        }
        return NavStateComposer.buildGpsStatusLine(
                snapshot.speedMps,
                snapshot.lastFiltered,
                snapshot.accuracyMeters,
                snapshot.fixedSatelliteCount,
                snapshot.context
        );
    }

    @NonNull
    private NavState buildStateWithoutLocation(
            @NonNull NavigationDisplaySnapshot snapshot,
            @NonNull String gpsStatusLine
    ) {
        if (snapshot.lastRouteFailure != null) {
            return NavStateComposer.withGpsStatus(NavStateComposer.routeUnavailable(
                    snapshot.context,
                    NavigationRouteFailureFormatter.format(snapshot.context, snapshot.lastRouteFailure, false),
                    snapshot.nextEvaluationDeadlineElapsedMs
            ), gpsStatusLine);
        }
        return NavStateComposer.withGpsStatus(
                NavStateComposer.waitingForLocation(snapshot.context, snapshot.nextEvaluationDeadlineElapsedMs),
                gpsStatusLine
        );
    }

    @NonNull
    private NavState buildCalculatingState(
            @NonNull NavigationDisplaySnapshot snapshot,
            @NonNull String gpsStatusLine
    ) {
        NavState calculatingState = NavStateComposer.calculatingRoute(
                snapshot.context,
                snapshot.nextEvaluationDeadlineElapsedMs
        );
        if (snapshot.routeCalculationNotice != null && !snapshot.routeCalculationNotice.trim().isEmpty()) {
            calculatingState = NavStateComposer.withNotice(calculatingState, snapshot.routeCalculationNotice);
        }
        return NavStateComposer.withGpsStatus(calculatingState, gpsStatusLine);
    }

    @NonNull
    private NavState buildStateWithoutRoute(
            @NonNull NavigationDisplaySnapshot snapshot,
            @NonNull String gpsStatusLine
    ) {
        if (snapshot.lastRouteFailure != null) {
            return NavStateComposer.withGpsStatus(NavStateComposer.routeUnavailable(
                    snapshot.context,
                    NavigationRouteFailureFormatter.format(snapshot.context, snapshot.lastRouteFailure, false),
                    snapshot.nextEvaluationDeadlineElapsedMs
            ), gpsStatusLine);
        }
        return NavStateComposer.withGpsStatus(
                NavStateComposer.calculatingRoute(snapshot.context, snapshot.nextEvaluationDeadlineElapsedMs),
                gpsStatusLine
        );
    }

    @NonNull
    private NavState withLastRouteFailureNotice(
            @NonNull NavigationDisplaySnapshot snapshot,
            @NonNull NavState state
    ) {
        return snapshot.lastRouteFailure != null
                ? NavStateComposer.withNotice(
                state,
                NavigationRouteFailureFormatter.format(snapshot.context, snapshot.lastRouteFailure, true)
        )
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

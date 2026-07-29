package vibro.navigator.nav.session;


import vibro.navigator.nav.compass.CompassOrientationCue;
import vibro.navigator.nav.guidance.NavigationRouteProgressTracker;
import vibro.navigator.nav.guidance.NavigationTurnState;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.presentation.NavStateBuildInput;
import vibro.navigator.nav.presentation.NavStateComposer;
import vibro.navigator.nav.presentation.NavStateResourceComposer;
import vibro.navigator.nav.model.NavTarget;
import vibro.navigator.nav.compass.NavCompassStateInput;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.NavigationRouteGeometryState;
import vibro.navigator.nav.route.PolylineIndex;

import java.util.ArrayList;
import java.util.List;

public final class NavigationSessionRouteDisplayState {
    private static final double TRACK_INDEX_TOLERANCE_METERS = 1.0;

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
            @NonNull NavigationTextResources textResources,
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            @NonNull List<LatLon> intermediateStops
    ) {
        onRouteApplied(textResources, route, polylineIndex, intermediateStops, null, true);
    }

    public void onRouteApplied(
            @NonNull NavigationTextResources textResources,
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            @NonNull List<LatLon> intermediateStops,
            @Nullable PolylineIndex.Match previousRouteMatch,
            boolean appendDirectBridge
    ) {
        compassMemory.onRouteApplied(
                route,
                polylineIndex,
                intermediateStops,
                previousRouteMatch,
                appendDirectBridge
        );
        targets = buildTargets(textResources, intermediateStops, route.track.size(), polylineIndex);
    }

    void appendRecalculationBridgeSegment(@NonNull List<LatLon> segment) {
        compassMemory.appendRecalculationBridgeSegment(segment);
    }

    @NonNull
    public NavState buildState(
            @NonNull NavigationDisplaySnapshot snapshot,
            @NonNull NavigationRouteGeometryState geometryState,
            @NonNull NavigationTurnState turnState,
            @NonNull NavigationRouteProgressTracker progressTracker
    ) {
        return buildState(
                snapshot,
                geometryState,
                turnState,
                progressTracker,
                null,
                null,
                false
        );
    }

    @NonNull
    NavState buildState(
            @NonNull NavigationDisplaySnapshot snapshot,
            @NonNull NavigationRouteGeometryState geometryState,
            @NonNull NavigationTurnState turnState,
            @NonNull NavigationRouteProgressTracker progressTracker,
            @Nullable LatLon directGuidanceTarget,
            @Nullable PolylineIndex.Match routeMatchOverride,
            boolean showNextManeuverCue
    ) {
        if (snapshot.lastFiltered == null) {
            return buildStateWithoutLocation(snapshot);
        }

        if (snapshot.routeCalculationInProgress) {
            return buildCalculatingState(snapshot);
        }

        GeoJsonRoute route = geometryState.route();
        PolylineIndex polylineIndex = geometryState.polylineIndex();
        if (route == null || polylineIndex == null) {
            return buildStateWithoutRoute(snapshot);
        }

        PolylineIndex.Match match = routeMatchOverride != null
                ? routeMatchOverride
                : geometryState.match(snapshot.lastFiltered, snapshot.accuracyMeters);
        if (match == null) {
            return NavigationDisplayGpsStatusFactory.withSnapshotGpsStatus(
                    NavStateResourceComposer.waiting(snapshot.textResources),
                    snapshot
            );
        }

        float etaSpeedMps = progressTracker.resolveEtaSpeedMps(
                snapshot.lastFiltered,
                match.alongTrackMeters,
                snapshot.accuracyMeters,
                snapshot.likelyStationary
        );
        float compassAccuracyMeters = compassMemory.resolveAccuracyMeters(snapshot.accuracyMeters);
        CompassOrientationCue orientationCue = resolveOrientationCue(
                snapshot,
                turnState,
                polylineIndex,
                match,
                showNextManeuverCue
        );
        NavCompassStateInput compassInput = NavCompassStateInput.builder(route, polylineIndex, snapshot.lastFiltered)
                .routeProgress(match.alongTrackMeters)
                .motion(
                        snapshot.displaySpeedMps,
                        snapshot.likelyStationary,
                        compassAccuracyMeters
                )
                .destinationReachedRadiusMeters((float)
                        NavigationRouteGeometryState.resolveDestinationReachedRadiusMeters(compassAccuracyMeters))
                .heading(snapshot.headingDegrees, snapshot.headingAccuracyDegrees)
                .radiusMemory(
                        CompassZoomAnimationPolicy.previousVisibleRadius(
                                snapshot.compassZoomAnimationEnabled,
                                compassMemory.lastVisibleRadiusMeters()
                        ),
                        compassMemory.lastReliableMovingVisibleRadiusMeters(),
                        CompassZoomAnimationPolicy.updateDeltaMs(
                                snapshot.compassZoomAnimationEnabled,
                                compassMemory.resolveRadiusUpdateDeltaMs(snapshot.nowMs)
                        )
                )
                .speedBucketMemory(compassMemory.lastReliableMovingSpeedBucket())
                .geometry(
                        compassMemory.routeGeometry(),
                        CompassZoomAnimationPolicy.transition(
                                snapshot.compassZoomAnimationEnabled,
                                compassMemory.radiusTransition
                        )
                )
                .stationaryFullRouteZoomEnabled(snapshot.stationaryFullRouteZoomEnabled)
                .routeStartApproachTarget(directGuidanceTarget)
                .orientationCue(orientationCue)
                .blockedAreas(NavigationBlockedCompassAreas.project(snapshot.lastFiltered, snapshot.blockedPoints))
                .nowMs(snapshot.nowMs)
                .build();
        NavState state = NavStateComposer.from(NavStateBuildInput
                .builder(snapshot.textResources, route, polylineIndex, snapshot.lastFiltered)
                .routeProgress(match.alongTrackMeters, turnState.getNextHintIdx(), match.segmentIndex)
                .motion(
                        snapshot.displaySpeedMps,
                        etaSpeedMps,
                        snapshot.likelyStationary,
                        snapshot.accuracyMeters,
                        compassAccuracyMeters
                )
                .gps(snapshot.fixedSatelliteCount, snapshot.acquiredFixCount)
                .heading(snapshot.headingDegrees, snapshot.headingAccuracyDegrees)
                .compass(compassInput)
                .timing(snapshot.nextEvaluationDeadlineElapsedMs, snapshot.nowMs)
                .destinationReached(turnState.isDestinationReached())
                .intermediateDestinationReachedTrackIndex(turnState.getIntermediateDestinationReachedTrackIndex())
                .targets(targets)
                .build());
        return withLastRouteFailureNotice(
                snapshot,
                withDirectGuidanceLines(state, snapshot, directGuidanceTarget)
        );
    }

    @NonNull
    public void rememberRenderedState(@NonNull NavState state, @NonNull NavigationDisplaySnapshot snapshot) {
        if (snapshot.lastFiltered != null) {
            compassMemory.rememberCompassState(
                    state,
                    snapshot.nowMs,
                    snapshot.lastFiltered,
                    snapshot.displaySpeedMps,
                    snapshot.likelyStationary
            );
        }
    }

    @NonNull
    List<NavTarget> targetsForDetails() {
        return targets;
    }

    @NonNull
    private NavState buildStateWithoutLocation(
            @NonNull NavigationDisplaySnapshot snapshot
    ) {
        return NavigationNoRouteDisplayState.waitingForLocation(snapshot);
    }

    @NonNull
    private NavState buildCalculatingState(@NonNull NavigationDisplaySnapshot snapshot) {
        NavState calculatingState = NavStateResourceComposer.calculatingRoute(
                snapshot.textResources,
                snapshot.nextEvaluationDeadlineElapsedMs
        );
        if (snapshot.routeCalculationNotice != null && !snapshot.routeCalculationNotice.trim().isEmpty()) {
            calculatingState = NavStateComposer.withNotice(calculatingState, snapshot.routeCalculationNotice);
        }
        return NavigationDisplayGpsStatusFactory.withSnapshotGpsStatus(calculatingState, snapshot);
    }

    @NonNull
    private NavState buildStateWithoutRoute(@NonNull NavigationDisplaySnapshot snapshot) {
        return NavigationNoRouteDisplayState.build(snapshot);
    }

    @NonNull
    private NavState withLastRouteFailureNotice(
            @NonNull NavigationDisplaySnapshot snapshot,
            @NonNull NavState state
    ) {
        return NavigationNoRouteDisplayState.withLastRouteFailureNotice(snapshot, state);
    }

    @NonNull
    private NavState withDirectGuidanceLines(
            @NonNull NavState state,
            @NonNull NavigationDisplaySnapshot snapshot,
            @Nullable LatLon directGuidanceTarget
    ) {
        return directGuidanceTarget == null
                ? state
                : NavStateComposer.withGuidanceLines(
                        state,
                        NavigationRouteStartApproachText.buildLine(snapshot, directGuidanceTarget),
                        state.routeStatus.guidance.nextLine
                );
    }

    @NonNull
    private List<NavTarget> buildTargets(
            @NonNull NavigationTextResources textResources,
            @NonNull List<LatLon> intermediates,
            int trackSize,
            @NonNull PolylineIndex index
    ) {
        List<NavTarget> out = new ArrayList<>();
        for (int i = 0; i < intermediates.size(); i++) {
            PolylineIndex.Match match = index.match(intermediates.get(i), -1);
            if (match != null) {
                out.add(new NavTarget(
                        textResources.getString(R.string.format_stop_label, i + 1),
                        match.alongTrackMeters,
                        trackIndexAtOrAfter(index, trackSize, match.alongTrackMeters)
                ));
            }
        }
        out.add(new NavTarget(
                textResources.getString(R.string.nav_destination_label),
                index.totalLengthMeters(),
                -1
        ));
        return out;
    }

    private static int trackIndexAtOrAfter(@NonNull PolylineIndex index, int trackSize, double alongTrackMeters) {
        int lastTrackIndex = Math.max(0, trackSize - 1);
        for (int i = 0; i <= lastTrackIndex; i++) {
            if (index.distanceAtPointIndex(i) + TRACK_INDEX_TOLERANCE_METERS >= alongTrackMeters) {
                return i;
            }
        }
        return lastTrackIndex;
    }

    private static boolean isCueTrackStillAhead(
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            @Nullable Integer trackIndex
    ) {
        return trackIndex != null && alongTrackMeters < index.distanceAtPointIndex(trackIndex);
    }

    @Nullable
    private CompassOrientationCue resolveOrientationCue(
            @NonNull NavigationDisplaySnapshot snapshot,
            @NonNull NavigationTurnState turnState,
            @NonNull PolylineIndex polylineIndex,
            @NonNull PolylineIndex.Match match,
            boolean showNextManeuverCue
    ) {
        Integer turnManeuverDegrees = turnState.getActiveTurnManeuverDegrees();
        Integer turnManeuverTrackIndex = turnState.getActiveTurnManeuverTrackIndex();
        if (turnManeuverDegrees == null && showNextManeuverCue) {
            Integer nextTrackIndex = turnState.getNextTurnManeuverTrackIndex();
            if (isCueTrackStillAhead(polylineIndex, match.alongTrackMeters, nextTrackIndex)) {
                turnManeuverDegrees = turnState.getNextTurnManeuverDegrees();
                turnManeuverTrackIndex = nextTrackIndex;
            }
        }
        return compassMemory.resolveOrientationCue(
                snapshot.orientationCue,
                turnManeuverDegrees,
                turnManeuverTrackIndex,
                polylineIndex,
                snapshot.headingDegrees
        );
    }

}

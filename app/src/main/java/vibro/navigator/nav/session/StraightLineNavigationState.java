package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.R;
import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.compass.NavCompassStateFactory;
import vibro.navigator.nav.compass.StraightLineNavCompassStateFactory;
import vibro.navigator.nav.guidance.NavigationArrivalTurnEvents;
import vibro.navigator.nav.guidance.NavigationTurnEvent;
import vibro.navigator.nav.guidance.NavigationUpdateScheduler;
import vibro.navigator.nav.guidance.NavigationWrongDirectionNotice;
import vibro.navigator.nav.guidance.StraightLineWrongDirectionDetector;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavGpsStatus;
import vibro.navigator.nav.model.NavGuidanceStatus;
import vibro.navigator.nav.model.NavPauseStatus;
import vibro.navigator.nav.model.NavProgressStatus;
import vibro.navigator.nav.model.NavRouteStatus;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.route.GeoJsonRoute;

final class StraightLineNavigationState {
    private static final long NO_SUGGESTED_INTERVAL = -1L;

    private final NavigationUpdateScheduler updateScheduler = new NavigationUpdateScheduler();
    private final StraightLineWrongDirectionDetector wrongDirectionDetector =
            new StraightLineWrongDirectionDetector();

    private int nextStopIndex;
    private boolean destinationReached;

    void reset() {
        nextStopIndex = 0;
        destinationReached = false;
        wrongDirectionDetector.reset();
    }

    void onRequestStarted(@NonNull NavigationRequest request) {
        nextStopIndex = 0;
        destinationReached = request.destination == null;
        wrongDirectionDetector.reset();
    }

    @NonNull
    NavigationRouteEvaluation evaluateLocation(
            @NonNull NavigationRequest request,
            @NonNull NavigationLocation location,
            float speedMps,
            boolean likelyStationary,
            float accuracyMeters,
            @Nullable Double actualBearingDegrees,
            long nowMs,
            long fastChecksUntilMs
    ) {
        if (request.destination == null || destinationReached) {
            return keepDirectGuidance();
        }
        List<NavigationTurnEvent> turnEvents = advanceReachedStops(request, location, accuracyMeters);
        if (!turnEvents.isEmpty()) {
            wrongDirectionDetector.reset();
        }
        if (nextStopIndex >= request.stops.size()
                && StraightLineNavigationProgress.isWithinReachedRadius(
                        location,
                        accuracyMeters,
                        request.destination
                )) {
            destinationReached = true;
            turnEvents.addAll(NavigationArrivalTurnEvents.destinationArrival(nextStopIndex));
            wrongDirectionDetector.reset();
            return keepDirectGuidance(turnEvents);
        }
        LatLon target = StraightLineNavigationProgress.nextTarget(request, false, nextStopIndex);
        if (target == null) {
            return keepDirectGuidance(turnEvents);
        }
        double distanceToTargetMeters = StraightLineNavigationProgress.distanceMeters(location, target);
        Double timeToTargetSeconds = StraightLineNavigationProgress.estimateSeconds(
                distanceToTargetMeters,
                speedMps,
                likelyStationary
        );
        long suggestedUpdateIntervalMs = updateScheduler.suggestDirectTargetUpdateInterval(
                nowMs,
                fastChecksUntilMs,
                timeToTargetSeconds
        );
        NavigationWrongDirectionNotice wrongDirectionNotice = wrongDirectionDetector.evaluate(
                distanceToTargetMeters,
                accuracyMeters,
                speedMps,
                targetBearingDegrees(location, target),
                actualBearingDegrees
        );
        return keepDirectGuidance(turnEvents, suggestedUpdateIntervalMs, wrongDirectionNotice);
    }

    @Nullable
    Double currentTargetBearingDegrees(
            @NonNull NavigationRequest request,
            @Nullable NavigationLocation location
    ) {
        LatLon target = StraightLineNavigationProgress.nextTarget(request, destinationReached, nextStopIndex);
        if (target == null || location == null) {
            return null;
        }
        return GeoMath.bearingDegrees(
                location.getLatitude(),
                location.getLongitude(),
                target.lat,
                target.lon
        );
    }

    @NonNull
    NavState buildState(@NonNull NavigationRequest request, @NonNull NavigationDisplaySnapshot snapshot) {
        String gpsStatusLine = buildGpsStatusLine(snapshot);
        if (snapshot.lastFiltered == null) {
            return buildWaitingForLocationState(snapshot, gpsStatusLine);
        }

        NavCompassState compassState = buildCompassState(request, snapshot);
        return new NavState(
                new NavRouteStatus(
                        StraightLineNavigationGuidanceText.buildStatus(
                                request,
                                snapshot,
                                destinationReached,
                                nextStopIndex
                        ),
                        new NavProgressStatus(
                                StraightLineNavigationProgressText.buildDestinationLine(
                                        request,
                                        snapshot,
                                        destinationReached,
                                        nextStopIndex
                                ),
                                StraightLineNavigationProgressText.buildStopProgressBlock(
                                        request,
                                        snapshot,
                                        destinationReached,
                                        nextStopIndex
                                ),
                                ""
                        ),
                        compassState
                ),
                new NavGpsStatus(gpsStatusLine, snapshot.nextEvaluationDeadlineElapsedMs),
                new NavPauseStatus(false)
        );
    }

    @NonNull
    private List<NavigationTurnEvent> advanceReachedStops(
            @NonNull NavigationRequest request,
            @NonNull NavigationLocation location,
            float accuracyMeters
    ) {
        List<NavigationTurnEvent> turnEvents = new ArrayList<>();
        while (nextStopIndex < request.stops.size()
                && StraightLineNavigationProgress.isWithinReachedRadius(
                        location,
                        accuracyMeters,
                        request.stops.get(nextStopIndex)
                )) {
            turnEvents.addAll(NavigationArrivalTurnEvents.intermediateArrival(nextStopIndex));
            nextStopIndex++;
        }
        return turnEvents;
    }

    @NonNull
    private static NavigationRouteEvaluation keepDirectGuidance() {
        return keepDirectGuidance(Collections.emptyList());
    }

    @NonNull
    private static NavigationRouteEvaluation keepDirectGuidance(@NonNull List<NavigationTurnEvent> turnEvents) {
        return keepDirectGuidance(turnEvents, NO_SUGGESTED_INTERVAL, null);
    }

    @NonNull
    private static NavigationRouteEvaluation keepDirectGuidance(
            @NonNull List<NavigationTurnEvent> turnEvents,
            long suggestedUpdateIntervalMs,
            @Nullable NavigationWrongDirectionNotice wrongDirectionNotice
    ) {
        return NavigationRouteEvaluation.keepRoute(
                turnEvents,
                suggestedUpdateIntervalMs,
                wrongDirectionNotice == null,
                wrongDirectionNotice
        );
    }

    @Nullable
    GeoJsonRoute buildExportRoute(
            @NonNull NavigationRequest request,
            @Nullable NavigationLocation location
    ) {
        if (location == null || request.destination == null) {
            return null;
        }
        return buildExportRoute(location, exportTargets(request));
    }

    @Nullable
    private NavCompassState buildCompassState(
            @NonNull NavigationRequest request,
            @NonNull NavigationDisplaySnapshot snapshot
    ) {
        LatLon target = destinationReached
                ? request.destination
                : StraightLineNavigationProgress.nextTarget(request, destinationReached, nextStopIndex);
        if (target == null || snapshot.lastFiltered == null) {
            return null;
        }
        List<LatLon> remainingTargetsAfterNext = destinationReached
                ? Collections.emptyList()
                : StraightLineNavigationProgress.remainingTargetsAfterNext(request, nextStopIndex);
        return StraightLineNavCompassStateFactory.buildTargetCompassState(
                snapshot.lastFiltered,
                snapshot.displaySpeedMps,
                snapshot.likelyStationary,
                snapshot.accuracyMeters,
                target,
                remainingTargetsAfterNext,
                request.stops,
                snapshot.headingDegrees,
                snapshot.headingAccuracyDegrees,
                snapshot.nowMs
        );
    }

    @NonNull
    private List<LatLon> exportTargets(@NonNull NavigationRequest request) {
        List<LatLon> targets = StraightLineNavigationProgress.remainingTargets(
                request,
                destinationReached,
                nextStopIndex
        );
        return targets.isEmpty() && request.destination != null
                ? Collections.singletonList(request.destination)
                : targets;
    }

    @NonNull
    private static GeoJsonRoute buildExportRoute(
            @NonNull NavigationLocation location,
            @NonNull List<LatLon> targets
    ) {
        List<LatLon> track = StraightLineNavigationProgress.routeTrack(location, targets);
        return new GeoJsonRoute(
                track,
                Collections.emptyList(),
                0.0,
                StraightLineNavigationProgress.trackDistanceMeters(track)
        );
    }

    @NonNull
    private static String buildGpsStatusLine(@NonNull NavigationDisplaySnapshot snapshot) {
        return NavCompassStateFactory.buildGpsStatusLine(
                snapshot.displaySpeedMps,
                snapshot.lastFiltered,
                snapshot.accuracyMeters,
                snapshot.fixedSatelliteCount,
                snapshot.acquiredFixCount,
                snapshot.textResources
        );
    }

    @NonNull
    private static NavState buildWaitingForLocationState(
            @NonNull NavigationDisplaySnapshot snapshot,
            @NonNull String gpsStatusLine
    ) {
        return new NavState(
                new NavRouteStatus(
                        new NavGuidanceStatus(
                                snapshot.textResources.getString(R.string.nav_waiting_for_location_title),
                                ""
                        ),
                        new NavProgressStatus(
                                "",
                                "",
                                snapshot.textResources.getString(R.string.nav_waiting_for_location_straight_line_body)
                        ),
                        null
                ),
                new NavGpsStatus(gpsStatusLine, snapshot.nextEvaluationDeadlineElapsedMs),
                new NavPauseStatus(false)
        );
    }

    private static double targetBearingDegrees(
            @NonNull NavigationLocation location,
            @NonNull LatLon target
    ) {
        return GeoMath.bearingDegrees(
                location.getLatitude(),
                location.getLongitude(),
                target.lat,
                target.lon
        );
    }

}

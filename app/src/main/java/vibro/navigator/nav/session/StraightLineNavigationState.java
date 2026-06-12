package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;

import vibro.navigator.R;
import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.compass.NavCompassStateFactory;
import vibro.navigator.nav.format.NavStateTextFactory;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavGpsStatus;
import vibro.navigator.nav.model.NavGuidanceStatus;
import vibro.navigator.nav.model.NavPauseStatus;
import vibro.navigator.nav.model.NavProgressStatus;
import vibro.navigator.nav.model.NavRouteStatus;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.model.NavigationRequest;

final class StraightLineNavigationState {
    private static final long NO_SUGGESTED_INTERVAL = -1L;

    private int nextStopIndex;
    private boolean destinationReached;

    void reset() {
        nextStopIndex = 0;
        destinationReached = false;
    }

    void onRequestStarted(@NonNull NavigationRequest request) {
        nextStopIndex = 0;
        destinationReached = request.destination == null;
    }

    @NonNull
    NavigationRouteEvaluation evaluateLocation(
            @NonNull NavigationRequest request,
            @NonNull NavigationLocation location,
            float accuracyMeters
    ) {
        if (request.destination == null || destinationReached) {
            return keepDirectGuidance();
        }
        advanceReachedStops(request, location, accuracyMeters);
        if (nextStopIndex >= request.stops.size()
                && StraightLineNavigationProgress.isWithinReachedRadius(
                        location,
                        accuracyMeters,
                        request.destination
                )) {
            destinationReached = true;
        }
        return keepDirectGuidance();
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
                        new NavGuidanceStatus("", ""),
                        new NavProgressStatus(
                                buildDestinationLine(request, snapshot),
                                buildStopProgressBlock(request, snapshot),
                                ""
                        ),
                        compassState
                ),
                new NavGpsStatus(gpsStatusLine, snapshot.nextEvaluationDeadlineElapsedMs),
                new NavPauseStatus(false)
        );
    }

    private void advanceReachedStops(
            @NonNull NavigationRequest request,
            @NonNull NavigationLocation location,
            float accuracyMeters
    ) {
        while (nextStopIndex < request.stops.size()
                && StraightLineNavigationProgress.isWithinReachedRadius(
                        location,
                        accuracyMeters,
                        request.stops.get(nextStopIndex)
                )) {
            nextStopIndex++;
        }
    }

    @NonNull
    private static NavigationRouteEvaluation keepDirectGuidance() {
        return NavigationRouteEvaluation.keepRoute(
                Collections.emptyList(),
                NO_SUGGESTED_INTERVAL,
                true
        );
    }

    @NonNull
    private String buildDestinationLine(
            @NonNull NavigationRequest request,
            @NonNull NavigationDisplaySnapshot snapshot
    ) {
        if (destinationReached) {
            return snapshot.textResources.getString(R.string.nav_destination_reached);
        }
        if (request.destination == null || snapshot.lastFiltered == null) {
            return "";
        }
        double distanceMeters = StraightLineNavigationProgress.remainingDistanceToDestination(
                request,
                snapshot.lastFiltered,
                nextStopIndex
        );
        return NavStateTextFactory.buildProgressLine(
                snapshot.textResources,
                snapshot.textResources.getString(R.string.nav_destination_label),
                distanceMeters,
                StraightLineNavigationProgress.estimateSeconds(
                        distanceMeters,
                        snapshot.speedMps,
                        snapshot.likelyStationary
                ),
                snapshot.nowMs
        );
    }

    @NonNull
    private String buildStopProgressBlock(
            @NonNull NavigationRequest request,
            @NonNull NavigationDisplaySnapshot snapshot
    ) {
        if (destinationReached || nextStopIndex >= request.stops.size() || snapshot.lastFiltered == null) {
            return "";
        }
        LatLon stop = request.stops.get(nextStopIndex);
        double distanceMeters = StraightLineNavigationProgress.distanceMeters(snapshot.lastFiltered, stop);
        return NavStateTextFactory.buildProgressLine(
                snapshot.textResources,
                snapshot.textResources.getString(R.string.format_stop_label, nextStopIndex + 1),
                distanceMeters,
                StraightLineNavigationProgress.estimateSeconds(
                        distanceMeters,
                        snapshot.speedMps,
                        snapshot.likelyStationary
                ),
                snapshot.nowMs
        );
    }

    @Nullable
    private NavCompassState buildCompassState(
            @NonNull NavigationRequest request,
            @NonNull NavigationDisplaySnapshot snapshot
    ) {
        LatLon target = StraightLineNavigationProgress.nextTarget(request, destinationReached, nextStopIndex);
        if (target == null || snapshot.lastFiltered == null) {
            return null;
        }
        return NavCompassStateFactory.buildStraightLineTargetCompassState(
                snapshot.lastFiltered,
                snapshot.speedMps,
                snapshot.likelyStationary,
                snapshot.accuracyMeters,
                target,
                StraightLineNavigationProgress.remainingTargetsAfterNext(request, nextStopIndex),
                snapshot.headingDegrees,
                snapshot.headingAccuracyDegrees,
                snapshot.nowMs
        );
    }

    @NonNull
    private static String buildGpsStatusLine(@NonNull NavigationDisplaySnapshot snapshot) {
        return NavCompassStateFactory.buildGpsStatusLine(
                snapshot.speedMps,
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
}

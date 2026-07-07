package vibro.navigator.nav.session;

import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.guidance.NavigationRouteDeviationHandler;
import vibro.navigator.nav.guidance.NavigationRouteProgressTracker;
import vibro.navigator.nav.guidance.NavigationTurnEvent;
import vibro.navigator.nav.guidance.NavigationTurnState;
import vibro.navigator.nav.route.NavigationRouteGeometryState;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.RouteStartApproach;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.logging.AppLogger;

final class NavigationRouteResultApplier {
    private static final String TAG = "NavSessionRoute";

    @NonNull
    private final NavigationRouteGeometryState geometryState;
    @NonNull
    private final NavigationSessionRouteDisplayState displayState;
    @NonNull
    private final NavigationRouteDeviationHandler deviationHandler;
    @NonNull
    private final NavigationRouteProgressTracker progressTracker;
    @NonNull
    private final NavigationTurnState turnState;
    @NonNull
    private final NavigationArrivalDetector arrivalDetector;
    @NonNull
    private final NavigationIntermediateArrivalTracker intermediateArrivalTracker;
    @NonNull
    private final RouteStartApproachState routeStartApproachState;

    NavigationRouteResultApplier(
            @NonNull NavigationRouteGeometryState geometryState,
            @NonNull NavigationSessionRouteDisplayState displayState,
            @NonNull NavigationRouteDeviationHandler deviationHandler,
            @NonNull NavigationRouteProgressTracker progressTracker,
            @NonNull NavigationTurnState turnState,
            @NonNull NavigationArrivalDetector arrivalDetector,
            @NonNull NavigationIntermediateArrivalTracker intermediateArrivalTracker,
            @NonNull RouteStartApproachState routeStartApproachState
    ) {
        this.geometryState = geometryState;
        this.displayState = displayState;
        this.deviationHandler = deviationHandler;
        this.progressTracker = progressTracker;
        this.turnState = turnState;
        this.arrivalDetector = arrivalDetector;
        this.intermediateArrivalTracker = intermediateArrivalTracker;
        this.routeStartApproachState = routeStartApproachState;
    }

    @NonNull
    List<NavigationTurnEvent> applyRouteResult(@NonNull NavigationRouteResultInput input) {
        float accuracyMeters = accuracyOf(input.lastFiltered);
        RouteStartApproach.Plan approachPlan = RouteStartApproach.plan(
                input.route,
                input.snapshot.start,
                accuracyMeters
        );
        GeoJsonRoute route = input.route;
        routeStartApproachState.apply(
                approachPlan,
                routeStartRequestLocation(input),
                !input.snapshot.isRoundTrip()
        );
        logRouteStartApproachIfNeeded(approachPlan);
        PolylineIndex.Match previousRouteMatch = previousRouteMatch(input.lastFiltered, accuracyMeters);
        geometryState.loadRoute(route, input.snapshot.isRoundTrip());
        displayState.onRouteApplied(
                input.textResources,
                route,
                geometryState.polylineIndex(),
                input.snapshot.intermediates,
                routeStartApproachState.target(),
                previousRouteMatch
        );
        intermediateArrivalTracker.onRouteApplied(input.snapshot.intermediates, route, geometryState.polylineIndex());
        deviationHandler.clearDeviationEvidence();
        progressTracker.reset();
        float initialSpeedMps = input.likelyStationary ? 0f : input.speedMps;

        List<NavigationTurnEvent> turnEvents = buildRouteAppliedTurnEvents(
                input,
                route,
                initialSpeedMps,
                accuracyMeters,
                routeStartApproachState.isActive()
        );
        AppLogger.i(TAG, "Route recalculation #" + input.snapshot.requestNumber
                + " succeeded durationMs=" + (System.currentTimeMillis() - input.beganAt)
                + " trackPoints=" + route.track.size()
                + " voiceHints=" + route.voiceHints.size()
                + " lengthMeters=" + route.trackLengthMeters);
        return turnEvents;
    }

    private void logRouteStartApproachIfNeeded(@NonNull RouteStartApproach.Plan approachPlan) {
        if (!approachPlan.active) {
            return;
        }
        AppLogger.i(TAG, "Holding route-start approach target distance="
                + approachPlan.distanceMeters
                + " threshold=" + approachPlan.thresholdMeters);
    }

    @Nullable
    private PolylineIndex.Match previousRouteMatch(
            @Nullable NavigationLocation lastFiltered,
            float accuracyMeters
    ) {
        if (lastFiltered == null || geometryState.isRouteUnavailable()) {
            return null;
        }
        return geometryState.match(lastFiltered, accuracyMeters);
    }

    @NonNull
    private List<NavigationTurnEvent> buildRouteAppliedTurnEvents(
            @NonNull NavigationRouteResultInput input,
            @NonNull GeoJsonRoute route,
            float initialSpeedMps,
            float accuracyMeters,
            boolean suppressInitialTurnEvent
    ) {
        if (!input.snapshot.isRoundTrip()
                && input.lastFiltered != null
                && arrivalDetector.isDestinationReached(input.lastFiltered, accuracyMeters)) {
            return turnState.onDestinationReached(route);
        }

        List<NavigationTurnEvent> initialEvents = turnState.onRouteApplied(
                route,
                geometryState.polylineIndex(),
                input.snapshot.intermediates,
                initialGuidanceLocation(input, route),
                initialSpeedMps,
                suppressInitialTurnEvent ? Float.MAX_VALUE : accuracyMeters
        );
        if (input.lastFiltered == null) {
            return initialEvents;
        }
        Integer reachedIntermediateTrackIndex = intermediateArrivalTracker.reachedTrackIndex(
                input.lastFiltered,
                accuracyMeters
        );
        return reachedIntermediateTrackIndex != null
                ? turnState.onIntermediateDestinationReached(reachedIntermediateTrackIndex)
                : initialEvents;
    }

    private float accuracyOf(@Nullable NavigationLocation location) {
        return location != null && location.hasAccuracy() ? location.getAccuracy() : Float.MAX_VALUE;
    }

    @Nullable
    private static NavigationLocation routeStartRequestLocation(@NonNull NavigationRouteResultInput input) {
        if (input.lastFiltered == null) {
            return null;
        }
        NavigationLocation requestLocation = new NavigationLocation(input.lastFiltered);
        requestLocation.setLatitude(input.snapshot.start.lat);
        requestLocation.setLongitude(input.snapshot.start.lon);
        return requestLocation;
    }

    @Nullable
    private static LatLon toLatLon(@Nullable NavigationLocation location) {
        return location == null ? null : new LatLon(location.getLatitude(), location.getLongitude());
    }

    @Nullable
    private static LatLon initialGuidanceLocation(
            @NonNull NavigationRouteResultInput input,
            @NonNull GeoJsonRoute route
    ) {
        if (input.snapshot.isRoundTrip() && !route.track.isEmpty()) {
            LatLon start = route.track.get(0);
            return new LatLon(start.lat, start.lon);
        }
        return toLatLon(input.lastFiltered);
    }
}

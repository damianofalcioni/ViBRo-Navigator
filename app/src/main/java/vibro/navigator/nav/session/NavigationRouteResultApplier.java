package vibro.navigator.nav.session;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import vibro.navigator.nav.guidance.NavigationRouteDeviationHandler;
import vibro.navigator.nav.guidance.NavigationRouteProgressTracker;
import vibro.navigator.nav.guidance.NavigationTurnEvent;
import vibro.navigator.nav.guidance.NavigationTurnState;
import vibro.navigator.nav.route.NavigationRouteGeometryState;
import vibro.navigator.nav.route.RouteStartConnector;
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

    NavigationRouteResultApplier(
            @NonNull NavigationRouteGeometryState geometryState,
            @NonNull NavigationSessionRouteDisplayState displayState,
            @NonNull NavigationRouteDeviationHandler deviationHandler,
            @NonNull NavigationRouteProgressTracker progressTracker,
            @NonNull NavigationTurnState turnState,
            @NonNull NavigationArrivalDetector arrivalDetector,
            @NonNull NavigationIntermediateArrivalTracker intermediateArrivalTracker
    ) {
        this.geometryState = geometryState;
        this.displayState = displayState;
        this.deviationHandler = deviationHandler;
        this.progressTracker = progressTracker;
        this.turnState = turnState;
        this.arrivalDetector = arrivalDetector;
        this.intermediateArrivalTracker = intermediateArrivalTracker;
    }

    @NonNull
    List<NavigationTurnEvent> applyRouteResult(@NonNull NavigationRouteResultInput input) {
        float accuracyMeters = accuracyOf(input.lastFiltered);
        RouteStartConnector.Result connectedRoute = RouteStartConnector.apply(
                input.route,
                input.snapshot.start,
                accuracyMeters
        );
        GeoJsonRoute route = connectedRoute.route;
        logConnectedStartIfNeeded(connectedRoute);
        geometryState.loadRoute(route);
        displayState.onRouteApplied(
                input.context,
                route,
                geometryState.polylineIndex(),
                input.snapshot.intermediates
        );
        intermediateArrivalTracker.onRouteApplied(input.snapshot.intermediates, route, geometryState.polylineIndex());
        deviationHandler.clearDeviationEvidence();
        progressTracker.reset();
        float initialSpeedMps = input.likelyStationary ? 0f : input.speedMps;

        List<NavigationTurnEvent> turnEvents = buildRouteAppliedTurnEvents(
                input,
                route,
                initialSpeedMps,
                accuracyMeters
        );
        AppLogger.i(TAG, "Route recalculation #" + input.snapshot.requestNumber
                + " succeeded durationMs=" + (System.currentTimeMillis() - input.beganAt)
                + " trackPoints=" + route.track.size()
                + " voiceHints=" + route.voiceHints.size()
                + " lengthMeters=" + route.trackLengthMeters);
        return turnEvents;
    }

    private void logConnectedStartIfNeeded(@NonNull RouteStartConnector.Result connectedRoute) {
        if (!connectedRoute.connectorAdded) {
            return;
        }
        AppLogger.i(TAG, "Prepended synthetic beeline route-start connector distance="
                + connectedRoute.connectorDistanceMeters
                + " threshold=" + connectedRoute.thresholdMeters);
    }

    @NonNull
    private List<NavigationTurnEvent> buildRouteAppliedTurnEvents(
            @NonNull NavigationRouteResultInput input,
            @NonNull GeoJsonRoute route,
            float initialSpeedMps,
            float accuracyMeters
    ) {
        if (input.lastFiltered != null && arrivalDetector.isDestinationReached(input.lastFiltered, accuracyMeters)) {
            return turnState.onDestinationReached(route);
        }

        List<NavigationTurnEvent> initialEvents = turnState.onRouteApplied(
                route,
                geometryState.polylineIndex(),
                input.snapshot.intermediates,
                input.lastFiltered,
                initialSpeedMps,
                accuracyMeters
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

    private float accuracyOf(@Nullable Location location) {
        return location != null && location.hasAccuracy() ? location.getAccuracy() : Float.MAX_VALUE;
    }
}

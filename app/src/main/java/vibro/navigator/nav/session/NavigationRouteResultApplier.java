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
        geometryState.loadRoute(input.route);
        displayState.onRouteApplied(
                input.context,
                input.route,
                geometryState.polylineIndex(),
                input.snapshot.intermediates
        );
        intermediateArrivalTracker.onRouteApplied(input.snapshot.intermediates, input.route, geometryState.polylineIndex());
        deviationHandler.clearDeviationEvidence();
        progressTracker.reset();
        float initialSpeedMps = input.likelyStationary ? 0f : input.speedMps;

        float accuracyMeters = accuracyOf(input.lastFiltered);
        List<NavigationTurnEvent> turnEvents = buildRouteAppliedTurnEvents(input, initialSpeedMps, accuracyMeters);
        AppLogger.i(TAG, "Route recalculation #" + input.snapshot.requestNumber
                + " succeeded durationMs=" + (System.currentTimeMillis() - input.beganAt)
                + " trackPoints=" + input.route.track.size()
                + " voiceHints=" + input.route.voiceHints.size()
                + " lengthMeters=" + input.route.trackLengthMeters);
        return turnEvents;
    }

    @NonNull
    private List<NavigationTurnEvent> buildRouteAppliedTurnEvents(
            @NonNull NavigationRouteResultInput input,
            float initialSpeedMps,
            float accuracyMeters
    ) {
        if (input.lastFiltered != null && arrivalDetector.isDestinationReached(input.lastFiltered, accuracyMeters)) {
            return turnState.onDestinationReached(input.route);
        }

        List<NavigationTurnEvent> initialEvents = turnState.onRouteApplied(
                input.route,
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

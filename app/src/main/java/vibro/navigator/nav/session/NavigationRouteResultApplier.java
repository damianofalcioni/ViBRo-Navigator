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

    NavigationRouteResultApplier(
            @NonNull NavigationRouteGeometryState geometryState,
            @NonNull NavigationSessionRouteDisplayState displayState,
            @NonNull NavigationRouteDeviationHandler deviationHandler,
            @NonNull NavigationRouteProgressTracker progressTracker,
            @NonNull NavigationTurnState turnState,
            @NonNull NavigationArrivalDetector arrivalDetector
    ) {
        this.geometryState = geometryState;
        this.displayState = displayState;
        this.deviationHandler = deviationHandler;
        this.progressTracker = progressTracker;
        this.turnState = turnState;
        this.arrivalDetector = arrivalDetector;
    }

    @NonNull
    List<NavigationTurnEvent> applyRouteResult(@NonNull NavigationRouteResultInput input) {
        geometryState.loadRoute(input.route);
        displayState.onRouteApplied(input.context, input.request, input.route, geometryState.polylineIndex());
        deviationHandler.clearDeviationEvidence();
        progressTracker.reset();
        float initialSpeedMps = input.likelyStationary ? 0f : input.speedMps;

        List<NavigationTurnEvent> turnEvents = input.lastFiltered != null
                && arrivalDetector.isDestinationReached(input.lastFiltered, accuracyOf(input.lastFiltered))
                ? turnState.onDestinationReached(input.route)
                : turnState.onRouteApplied(
                        input.route,
                        geometryState.polylineIndex(),
                        input.lastFiltered,
                        initialSpeedMps,
                        accuracyOf(input.lastFiltered)
                );
        AppLogger.i(TAG, "Route recalculation #" + input.snapshot.requestNumber
                + " succeeded durationMs=" + (System.currentTimeMillis() - input.beganAt)
                + " trackPoints=" + input.route.track.size()
                + " voiceHints=" + input.route.voiceHints.size()
                + " lengthMeters=" + input.route.trackLengthMeters);
        return turnEvents;
    }

    private float accuracyOf(@Nullable Location location) {
        return location != null && location.hasAccuracy() ? location.getAccuracy() : Float.MAX_VALUE;
    }
}

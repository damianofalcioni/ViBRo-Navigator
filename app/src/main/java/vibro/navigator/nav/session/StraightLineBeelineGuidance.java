package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.guidance.BeelineNotificationTracker;
import vibro.navigator.nav.guidance.NavigationTurnEvent;
import vibro.navigator.nav.guidance.NavigationUpdateScheduler;
import vibro.navigator.nav.guidance.NavigationWrongDirectionNotice;
import vibro.navigator.nav.guidance.StraightLineWrongDirectionDetector;
import vibro.navigator.nav.location.NavigationLocation;

final class StraightLineBeelineGuidance {
    @NonNull
    private final NavigationUpdateScheduler updateScheduler = new NavigationUpdateScheduler();
    @NonNull
    private final BeelineNotificationTracker notificationTracker = new BeelineNotificationTracker();
    @NonNull
    private final StraightLineWrongDirectionDetector wrongDirectionDetector =
            new StraightLineWrongDirectionDetector();

    void reset() {
        wrongDirectionDetector.reset();
        notificationTracker.reset();
    }

    @NonNull
    NavigationRouteEvaluation evaluate(
            @NonNull List<NavigationTurnEvent> turnEvents,
            @NonNull LatLon target,
            @NonNull NavigationLocation location,
            float speedMps,
            boolean likelyStationary,
            float accuracyMeters,
            @Nullable Double actualBearingDegrees,
            long nowMs,
            long fastChecksUntilMs
    ) {
        double distanceMeters = StraightLineNavigationProgress.distanceMeters(location, target);
        Double timeSeconds = StraightLineNavigationProgress.estimateSeconds(
                distanceMeters,
                speedMps,
                likelyStationary
        );
        long updateIntervalMs = updateScheduler.suggestDirectTargetUpdateInterval(
                nowMs,
                fastChecksUntilMs,
                timeSeconds
        );
        updateIntervalMs = BeelineNotificationTracker.limitSuggestedUpdateInterval(updateIntervalMs);
        if (notificationTracker.shouldNotify(target, distanceMeters, nowMs)) {
            turnEvents.add(NavigationTurnEvent.beeline(
                    distanceMeters,
                    timeSeconds == null ? Double.NaN : timeSeconds
            ));
        }
        NavigationWrongDirectionNotice wrongDirectionNotice = wrongDirectionDetector.evaluate(
                distanceMeters,
                accuracyMeters,
                speedMps,
                targetBearingDegrees(location, target),
                actualBearingDegrees
        );
        return NavigationRouteEvaluation.keepRoute(
                turnEvents,
                updateIntervalMs,
                wrongDirectionNotice == null,
                wrongDirectionNotice
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

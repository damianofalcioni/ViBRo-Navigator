package vibro.navigator.nav.session;

import androidx.annotation.NonNull;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.NavigationTextFormatter;
import vibro.navigator.nav.route.RouteStartApproach;
import vibro.navigator.nav.route.VoiceHint;

final class NavigationRouteStartApproachText {
    private NavigationRouteStartApproachText() {
    }

    @NonNull
    static String buildLine(
            @NonNull NavigationDisplaySnapshot snapshot,
            @NonNull LatLon target
    ) {
        double distanceMeters = RouteStartApproach.distanceMeters(
                new LatLon(snapshot.lastFiltered.getLatitude(), snapshot.lastFiltered.getLongitude()),
                target
        );
        double timeSeconds = RouteStartApproach.estimateApproachTimeSeconds(
                distanceMeters,
                snapshot.speedMps,
                snapshot.likelyStationary
        );
        return NavigationTextFormatter.formatTurnNotification(
                snapshot.textResources,
                new VoiceHint(0, RouteStartApproach.BEELINE_COMMAND, 0, 0.0, 0),
                distanceMeters,
                timeSeconds
        );
    }
}

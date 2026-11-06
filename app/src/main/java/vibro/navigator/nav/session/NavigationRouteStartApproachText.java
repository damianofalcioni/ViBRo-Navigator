package vibro.navigator.nav.session;

import androidx.annotation.NonNull;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.NavigationTextFormatter;
import vibro.navigator.nav.route.RouteStartApproach;
import vibro.navigator.nav.route.VoiceHint;
import vibro.navigator.nav.model.NavManeuver;

final class NavigationRouteStartApproachText {
    private NavigationRouteStartApproachText() {
    }

    @NonNull
    static String buildLine(
            @NonNull NavigationDisplaySnapshot snapshot,
            @NonNull LatLon target
    ) {
        NavManeuver maneuver = maneuver(snapshot, target);
        return NavigationTextFormatter.formatTurnNotification(
                snapshot.textResources,
                maneuver.hint,
                maneuver.distanceMeters,
                maneuver.timeSeconds
        );
    }

    @NonNull
    static NavManeuver maneuver(@NonNull NavigationDisplaySnapshot snapshot, @NonNull LatLon target) {
        double distance = RouteStartApproach.distanceMeters(
                new LatLon(snapshot.lastFiltered.getLatitude(), snapshot.lastFiltered.getLongitude()), target);
        double seconds = snapshot.hasFreshLocation() ? RouteStartApproach.estimateApproachTimeSeconds(
                distance, snapshot.speedMps, snapshot.likelyStationary) : Double.NaN;
        return maneuver(snapshot, distance, seconds);
    }

    private static NavManeuver maneuver(NavigationDisplaySnapshot snapshot,
            double distance, double seconds) {
        VoiceHint hint = new VoiceHint(0, RouteStartApproach.BEELINE_COMMAND, 0, 0, 0);
        return new NavManeuver(hint, NavigationTextFormatter.maneuverInstruction(snapshot.textResources, hint),
                distance, seconds);
    }
}

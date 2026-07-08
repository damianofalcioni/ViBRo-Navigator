package vibro.navigator.nav.session;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.NavigationTextFormatter;
import vibro.navigator.nav.guidance.NavigationArrivalTurnEvents;
import vibro.navigator.nav.model.NavGuidanceStatus;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.route.VoiceHint;

final class StraightLineNavigationGuidanceText {
    private StraightLineNavigationGuidanceText() {
    }

    @NonNull
    static NavGuidanceStatus buildStatus(
            @NonNull NavigationRequest request,
            @NonNull NavigationDisplaySnapshot snapshot,
            boolean destinationReached,
        int nextStopIndex
    ) {
        if (destinationReached) {
            return new NavGuidanceStatus(formatReachedDestinationLine(snapshot), "");
        }
        if (snapshot.lastFiltered == null) {
            return new NavGuidanceStatus("", "");
        }
        List<LatLon> targets = StraightLineNavigationProgress.remainingTargets(
                request,
                false,
                nextStopIndex
        );
        if (targets.isEmpty()) {
            return new NavGuidanceStatus("", "");
        }
        return new NavGuidanceStatus(
                formatNextTargetLine(snapshot, targets),
                formatFollowingTargetLine(snapshot, targets)
        );
    }

    @NonNull
    static List<String> buildDetailLines(
            @NonNull NavigationRequest request,
            @NonNull NavigationDisplaySnapshot snapshot,
            boolean destinationReached,
            int nextStopIndex
    ) {
        if (destinationReached) {
            List<String> lines = new ArrayList<>(1);
            lines.add(formatReachedDestinationLine(snapshot));
            return lines;
        }
        if (snapshot.lastFiltered == null) {
            return new ArrayList<>();
        }
        List<LatLon> targets = StraightLineNavigationProgress.remainingTargets(request, false, nextStopIndex);
        return targets.isEmpty() ? new ArrayList<>() : formatRemainingTargetLines(snapshot, targets);
    }

    @NonNull
    private static String formatReachedDestinationLine(@NonNull NavigationDisplaySnapshot snapshot) {
        return NavigationTextFormatter.formatTurnNotification(
                snapshot.textResources,
                new VoiceHint(0, NavigationArrivalTurnEvents.DESTINATION_ARRIVAL_COMMAND, 0, 0.0, 0),
                0.0,
                0.0
        );
    }

    @NonNull
    private static String formatNextTargetLine(
            @NonNull NavigationDisplaySnapshot snapshot,
            @NonNull List<LatLon> targets
    ) {
        return formatTargetDirectionLine(
                snapshot,
                StraightLineNavigationProgress.distanceMeters(snapshot.lastFiltered, targets.get(0)),
                commandForTarget(targets, 0)
        );
    }

    @NonNull
    private static String formatFollowingTargetLine(
            @NonNull NavigationDisplaySnapshot snapshot,
            @NonNull List<LatLon> targets
    ) {
        if (targets.size() <= 1) {
            return "";
        }
        return formatTargetDirectionLine(
                snapshot,
                distanceMeters(targets.get(0), targets.get(1)),
                commandForTarget(targets, 1)
        );
    }

    @NonNull
    private static List<String> formatRemainingTargetLines(
            @NonNull NavigationDisplaySnapshot snapshot,
            @NonNull List<LatLon> targets
    ) {
        List<String> lines = new ArrayList<>(targets.size());
        LatLon cursor = new LatLon(snapshot.lastFiltered.getLatitude(), snapshot.lastFiltered.getLongitude());
        for (int i = 0; i < targets.size(); i++) {
            LatLon target = targets.get(i);
            lines.add(formatTargetDirectionLine(
                    snapshot,
                    distanceMeters(cursor, target),
                    commandForTarget(targets, i)
            ));
            cursor = target;
        }
        return lines;
    }

    @NonNull
    private static String formatTargetDirectionLine(
            @NonNull NavigationDisplaySnapshot snapshot,
            double distanceMeters,
            int command
    ) {
        Double seconds = StraightLineNavigationProgress.estimateSeconds(
                distanceMeters,
                snapshot.speedMps,
                snapshot.likelyStationary
        );
        return NavigationTextFormatter.formatTurnNotification(
                snapshot.textResources,
                new VoiceHint(0, command, 0, 0.0, 0),
                distanceMeters,
                seconds != null ? seconds : Double.NaN
        );
    }

    private static int commandForTarget(@NonNull List<LatLon> targets, int targetIndex) {
        return targetIndex == targets.size() - 1
                ? NavigationArrivalTurnEvents.DESTINATION_ARRIVAL_COMMAND
                : NavigationArrivalTurnEvents.INTERMEDIATE_ARRIVAL_COMMAND;
    }

    private static double distanceMeters(@NonNull LatLon from, @NonNull LatLon to) {
        return GeoMath.distanceMeters(from.lat, from.lon, to.lat, to.lon);
    }
}

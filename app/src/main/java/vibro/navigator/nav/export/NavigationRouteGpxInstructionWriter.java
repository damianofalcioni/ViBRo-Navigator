package vibro.navigator.nav.export;

import androidx.annotation.NonNull;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.directions.DirectionInfo;
import vibro.navigator.nav.directions.VoiceHintMapper;
import vibro.navigator.nav.format.NavigationTextFormatter;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.VoiceHint;

final class NavigationRouteGpxInstructionWriter {
    private static final String TAG_WAYPOINT = "wpt";
    private static final String TYPE_TURN_INSTRUCTION = "vibro.navigator.turn";
    private static final int ARRIVAL_COMMAND = 100;
    private static final int INTERMEDIATE_ARRIVAL_COMMAND = 101;

    private NavigationRouteGpxInstructionWriter() {
    }

    static void appendWaypoints(
            @NonNull StringBuilder out,
            @NonNull NavigationTextResources textResources,
            @NonNull GeoJsonRoute route
    ) {
        if (route.track.isEmpty()) {
            return;
        }
        for (int i = 0; i < route.voiceHints.size(); i++) {
            appendWaypoint(out, textResources, route, route.voiceHints.get(i), i);
        }
        if (!hasArrivalHint(route)) {
            VoiceHint arrival = new VoiceHint(route.track.size() - 1, ARRIVAL_COMMAND, 0, 0.0, 0);
            appendWaypoint(out, textResources, route, arrival, route.voiceHints.size());
        }
    }

    private static void appendWaypoint(
            @NonNull StringBuilder out,
            @NonNull NavigationTextResources textResources,
            @NonNull GeoJsonRoute route,
            @NonNull VoiceHint hint,
            int hintPosition
    ) {
        LatLon point = route.track.get(boundedTrackIndex(route, hint.indexInTrack));
        NavigationRouteGpxXmlWriter.appendPointStart(out, 1, TAG_WAYPOINT, point);
        out.append(">").append(NavigationRouteGpxXmlWriter.LINE_END);
        NavigationRouteGpxXmlWriter.appendSimpleElement(
                out,
                2,
                NavigationRouteGpxXmlWriter.TAG_NAME,
                formatName(textResources, hint)
        );
        NavigationRouteGpxXmlWriter.appendSimpleElement(
                out,
                2,
                NavigationRouteGpxXmlWriter.TAG_DESC,
                formatDescription(textResources, route, hint, hintPosition)
        );
        NavigationRouteGpxXmlWriter.appendSimpleElement(
                out,
                2,
                NavigationRouteGpxXmlWriter.TAG_TYPE,
                TYPE_TURN_INSTRUCTION
        );
        out.append("  </wpt>").append(NavigationRouteGpxXmlWriter.LINE_END);
    }

    @NonNull
    private static String formatName(@NonNull NavigationTextResources textResources, @NonNull VoiceHint hint) {
        DirectionInfo direction = VoiceHintMapper.toDirection(hint);
        return direction.exitNumber > 0
                ? textResources.getString(direction.labelRes, direction.exitNumber)
                : textResources.getString(direction.labelRes);
    }

    @NonNull
    private static String formatDescription(
            @NonNull NavigationTextResources textResources,
            @NonNull GeoJsonRoute route,
            @NonNull VoiceHint hint,
            int hintPosition
    ) {
        double distanceMeters = isArrivalCommand(hint.command)
                ? 0.0
                : sanitizeDistanceMeters(hint.distanceToNextMeters);
        double timeSeconds = isArrivalCommand(hint.command)
                ? 0.0
                : estimateInstructionTimeSeconds(route, hint, hintPosition);
        return NavigationTextFormatter.formatTurnNotification(textResources, hint, distanceMeters, timeSeconds);
    }

    private static double sanitizeDistanceMeters(double distanceMeters) {
        return Double.isFinite(distanceMeters) ? Math.max(0.0, distanceMeters) : 0.0;
    }

    private static double estimateInstructionTimeSeconds(
            @NonNull GeoJsonRoute route,
            @NonNull VoiceHint hint,
            int hintPosition
    ) {
        if (!hasAlignedTrackTimes(route)) {
            return Double.NaN;
        }
        int startIndex = boundedTrackIndex(route, hint.indexInTrack);
        int endIndex = nextInstructionTrackIndex(route, hintPosition, startIndex);
        return Math.max(0.0, route.timesSeconds.get(endIndex) - route.timesSeconds.get(startIndex));
    }

    private static int nextInstructionTrackIndex(
            @NonNull GeoJsonRoute route,
            int hintPosition,
            int startIndex
    ) {
        for (int i = hintPosition + 1; i < route.voiceHints.size(); i++) {
            int candidate = boundedTrackIndex(route, route.voiceHints.get(i).indexInTrack);
            if (candidate >= startIndex) {
                return candidate;
            }
        }
        return route.track.size() - 1;
    }

    private static boolean hasAlignedTrackTimes(@NonNull GeoJsonRoute route) {
        return !route.track.isEmpty()
                && !route.timesSeconds.isEmpty()
                && route.timesSeconds.size() == route.track.size();
    }

    private static int boundedTrackIndex(@NonNull GeoJsonRoute route, int indexInTrack) {
        int maxIndex = Math.max(0, route.track.size() - 1);
        return Math.max(0, Math.min(maxIndex, indexInTrack));
    }

    private static boolean hasArrivalHint(@NonNull GeoJsonRoute route) {
        for (VoiceHint hint : route.voiceHints) {
            if (isArrivalCommand(hint.command)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isArrivalCommand(int command) {
        return command == ARRIVAL_COMMAND || command == INTERMEDIATE_ARRIVAL_COMMAND;
    }
}

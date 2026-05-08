package vibro.navigator.nav.format;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.guidance.RouteTimeEstimator;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;

final class NavArrivalHintFactory {
    static final int ARRIVAL_COMMAND = 100;
    static final int INTERMEDIATE_ARRIVAL_COMMAND = 101;

    private NavArrivalHintFactory() {
    }

    @Nullable
    static ArrivalHint buildSyntheticArrivalHint(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            int currentSegmentIndex,
            float speedMps,
            int hintIdx,
            int collectedHintCount
    ) {
        if (collectedHintCount >= 2 || hasUpcomingArrivalHint(route, hintIdx) || route.track.isEmpty()) {
            return null;
        }
        int destinationTrackIndex = route.track.size() - 1;
        double destinationDistanceAtTrack = index.distanceAtPointIndex(destinationTrackIndex);
        double distanceMeters = Math.max(0.0, destinationDistanceAtTrack - alongTrackMeters);
        if (distanceMeters <= 0.0) {
            return null;
        }
        Double timeSeconds = RouteTimeEstimator.estimateSecondsToTrackPoint(
                route,
                index,
                alongTrackMeters,
                currentSegmentIndex,
                destinationTrackIndex,
                speedMps
        );
        return new ArrivalHint(
                new VoiceHint(destinationTrackIndex, ARRIVAL_COMMAND, 0, 0.0, 0),
                distanceMeters,
                timeSeconds != null ? timeSeconds : Double.NaN
        );
    }

    private static boolean hasUpcomingArrivalHint(@NonNull GeoJsonRoute route, int hintIdx) {
        int startIndex = Math.max(0, hintIdx);
        for (int i = startIndex; i < route.voiceHints.size(); i++) {
            if (route.voiceHints.get(i).command == ARRIVAL_COMMAND) {
                return true;
            }
        }
        return false;
    }

    static final class ArrivalHint {
        @NonNull
        final VoiceHint hint;
        final double distanceMeters;
        final double timeSeconds;

        ArrivalHint(@NonNull VoiceHint hint, double distanceMeters, double timeSeconds) {
            this.hint = hint;
            this.distanceMeters = distanceMeters;
            this.timeSeconds = timeSeconds;
        }
    }
}

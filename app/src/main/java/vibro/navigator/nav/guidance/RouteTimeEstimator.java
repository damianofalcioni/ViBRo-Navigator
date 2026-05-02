package vibro.navigator.nav.guidance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;

import java.util.List;

public final class RouteTimeEstimator {

    static final double MIN_LIVE_SPEED_METERS_PER_SECOND = 0.5;

    private RouteTimeEstimator() {
    }

    @Nullable
    public static Double estimateSecondsToTrackPoint(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            double alongTrackMeters,
            int currentSegmentIndex,
            int trackPointIndex,
            float speedMps
    ) {
        if (trackPointIndex < 0 || trackPointIndex >= route.track.size()) {
            return null;
        }
        return estimateSecondsToAlongTrack(
                route,
                polylineIndex,
                alongTrackMeters,
                currentSegmentIndex,
                polylineIndex.distanceAtPointIndex(trackPointIndex),
                speedMps
        );
    }

    @Nullable
    public static Double estimateSecondsBetweenTrackPoints(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            int startTrackPointIndex,
            int endTrackPointIndex
    ) {
        if (startTrackPointIndex < 0
                || endTrackPointIndex < startTrackPointIndex
                || endTrackPointIndex >= route.track.size()) {
            return null;
        }
        return estimateSecondsBetweenAlongTrack(
                route,
                polylineIndex,
                polylineIndex.distanceAtPointIndex(startTrackPointIndex),
                polylineIndex.distanceAtPointIndex(endTrackPointIndex)
        );
    }

    @Nullable
    public static Double estimateSecondsToAlongTrack(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            double alongTrackMeters,
            int currentSegmentIndex,
            double targetAlongTrackMeters,
            float speedMps
    ) {
        double totalLengthMeters = polylineIndex.totalLengthMeters();
        double clampedAlongTrackMeters = clampAlongTrackMeters(alongTrackMeters, totalLengthMeters);
        double clampedTargetAlongTrackMeters = clampAlongTrackMeters(targetAlongTrackMeters, totalLengthMeters);
        if (clampedTargetAlongTrackMeters <= clampedAlongTrackMeters) {
            return 0.0;
        }

        Double routeModelSeconds = estimateSecondsBetweenAlongTrack(
                route,
                polylineIndex,
                clampedAlongTrackMeters,
                clampedTargetAlongTrackMeters
        );
        double currentSegmentEndAlongTrackMeters = resolveCurrentSegmentEndAlongTrackMeters(
                polylineIndex,
                currentSegmentIndex,
                totalLengthMeters
        );
        if (currentSegmentEndAlongTrackMeters <= clampedAlongTrackMeters
                || speedMps < MIN_LIVE_SPEED_METERS_PER_SECOND) {
            return routeModelSeconds;
        }

        double liveSegmentTargetAlongTrackMeters = Math.min(
                clampedTargetAlongTrackMeters,
                currentSegmentEndAlongTrackMeters
        );
        double currentSegmentSeconds = (liveSegmentTargetAlongTrackMeters - clampedAlongTrackMeters) / speedMps;
        if (clampedTargetAlongTrackMeters <= currentSegmentEndAlongTrackMeters) {
            return Math.max(0.0, currentSegmentSeconds);
        }

        Double laterSegmentsSeconds = estimateSecondsBetweenAlongTrack(
                route,
                polylineIndex,
                currentSegmentEndAlongTrackMeters,
                clampedTargetAlongTrackMeters
        );
        if (laterSegmentsSeconds != null) {
            return Math.max(0.0, currentSegmentSeconds) + laterSegmentsSeconds;
        }
        return routeModelSeconds;
    }

    @Nullable
    private static Double estimateSecondsBetweenAlongTrack(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            double startAlongTrackMeters,
            double endAlongTrackMeters
    ) {
        double totalLengthMeters = polylineIndex.totalLengthMeters();
        double clampedStartAlongTrackMeters = clampAlongTrackMeters(startAlongTrackMeters, totalLengthMeters);
        double clampedEndAlongTrackMeters = clampAlongTrackMeters(endAlongTrackMeters, totalLengthMeters);
        if (clampedEndAlongTrackMeters <= clampedStartAlongTrackMeters) {
            return 0.0;
        }

        Double trackTimeSeconds = estimateSecondsFromTrackTimes(
                route,
                polylineIndex,
                clampedStartAlongTrackMeters,
                clampedEndAlongTrackMeters
        );
        if (trackTimeSeconds != null) {
            return trackTimeSeconds;
        }
        return estimateSecondsForDistance(
                route,
                polylineIndex,
                clampedEndAlongTrackMeters - clampedStartAlongTrackMeters
        );
    }

    private static double clampAlongTrackMeters(double alongTrackMeters, double totalLengthMeters) {
        return Math.max(0.0, Math.min(alongTrackMeters, totalLengthMeters));
    }

    private static double resolveCurrentSegmentEndAlongTrackMeters(
            @NonNull PolylineIndex polylineIndex,
            int currentSegmentIndex,
            double totalLengthMeters
    ) {
        if (currentSegmentIndex < 0) {
            return -1.0;
        }
        return clampAlongTrackMeters(
                polylineIndex.distanceAtPointIndex(currentSegmentIndex + 1),
                totalLengthMeters
        );
    }

    @Nullable
    private static Double estimateSecondsForDistance(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            double remainingMeters
    ) {
        double totalLengthMeters = polylineIndex.totalLengthMeters();
        if (route.totalTimeSeconds > 0.0 && totalLengthMeters > 0.0) {
            return route.totalTimeSeconds * (remainingMeters / totalLengthMeters);
        }
        return null;
    }

    @Nullable
    private static Double estimateSecondsFromTrackTimes(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            double startAlongTrackMeters,
            double endAlongTrackMeters
    ) {
        List<Double> timesSeconds = route.timesSeconds;
        if (timesSeconds.size() != route.track.size()) {
            return null;
        }

        double startTimeSeconds = interpolateTimeAtAlongTrack(polylineIndex, timesSeconds, startAlongTrackMeters);
        double endTimeSeconds = interpolateTimeAtAlongTrack(polylineIndex, timesSeconds, endAlongTrackMeters);
        if (!Double.isFinite(startTimeSeconds) || !Double.isFinite(endTimeSeconds)) {
            return null;
        }
        return Math.max(0.0, endTimeSeconds - startTimeSeconds);
    }

    private static double interpolateTimeAtAlongTrack(
            @NonNull PolylineIndex polylineIndex,
            @NonNull List<Double> timesSeconds,
            double alongTrackMeters
    ) {
        if (timesSeconds.isEmpty()) {
            return Double.NaN;
        }
        if (timesSeconds.size() == 1) {
            return timesSeconds.get(0);
        }

        double clampedAlongTrackMeters = Math.max(0.0, Math.min(alongTrackMeters, polylineIndex.totalLengthMeters()));
        return interpolateTimeAcrossSegments(polylineIndex, timesSeconds, clampedAlongTrackMeters);
    }

    private static double interpolateTimeAcrossSegments(
            @NonNull PolylineIndex polylineIndex,
            @NonNull List<Double> timesSeconds,
            double alongTrackMeters
    ) {
        double previousDistanceMeters = 0.0;
        double previousTimeSeconds = timesSeconds.get(0);
        if (!Double.isFinite(previousTimeSeconds)) {
            return Double.NaN;
        }

        for (int i = 1; i < timesSeconds.size(); i++) {
            double nextDistanceMeters = polylineIndex.distanceAtPointIndex(i);
            double nextTimeSeconds = timesSeconds.get(i);
            if (!Double.isFinite(nextTimeSeconds)) {
                return Double.NaN;
            }
            if (alongTrackMeters <= nextDistanceMeters) {
                return interpolateTimeInSegment(
                        alongTrackMeters,
                        previousDistanceMeters,
                        nextDistanceMeters,
                        previousTimeSeconds,
                        nextTimeSeconds
                );
            }
            previousDistanceMeters = nextDistanceMeters;
            previousTimeSeconds = nextTimeSeconds;
        }

        return timesSeconds.get(timesSeconds.size() - 1);
    }

    private static double interpolateTimeInSegment(
            double alongTrackMeters,
            double segmentStartMeters,
            double segmentEndMeters,
            double startTimeSeconds,
            double endTimeSeconds
    ) {
        double segmentDistanceMeters = segmentEndMeters - segmentStartMeters;
        if (segmentDistanceMeters <= 0.0) {
            return endTimeSeconds;
        }
        double ratio = (alongTrackMeters - segmentStartMeters) / segmentDistanceMeters;
        return startTimeSeconds + ratio * (endTimeSeconds - startTimeSeconds);
    }
}

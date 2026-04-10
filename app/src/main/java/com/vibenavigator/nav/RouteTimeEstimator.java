package com.vibenavigator.nav;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vibenavigator.nav.route.GeoJsonRoute;
import com.vibenavigator.nav.route.PolylineIndex;

import java.util.List;

final class RouteTimeEstimator {

    private static final double MIN_LIVE_SPEED_METERS_PER_SECOND = 1.0;

    private RouteTimeEstimator() {
    }

    @Nullable
    static Double estimateSecondsToTrackPoint(
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

        if (isOnCurrentSegment(currentSegmentIndex, trackPointIndex)) {
            return estimateCurrentSegmentSeconds(
                    route,
                    polylineIndex,
                    alongTrackMeters,
                    trackPointIndex,
                    speedMps
            );
        }

        int currentSegmentEndPointIndex = currentSegmentIndex + 1;
        if (currentSegmentEndPointIndex > 0
                && currentSegmentEndPointIndex < route.track.size()
                && speedMps >= MIN_LIVE_SPEED_METERS_PER_SECOND) {
            Double currentSegmentSeconds = estimateLiveSecondsToTrackPoint(
                    polylineIndex,
                    alongTrackMeters,
                    currentSegmentEndPointIndex,
                    speedMps
            );
            Double laterSegmentsSeconds = estimateSecondsBetweenTrackPoints(
                    route,
                    polylineIndex,
                    currentSegmentEndPointIndex,
                    trackPointIndex
            );
            if (currentSegmentSeconds != null && laterSegmentsSeconds != null) {
                return currentSegmentSeconds + laterSegmentsSeconds;
            }
        }
        return estimateSecondsUsingRouteModel(route, polylineIndex, alongTrackMeters, trackPointIndex);
    }

    @Nullable
    static Double estimateSecondsBetweenTrackPoints(
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

        List<Double> timesSeconds = route.timesSeconds;
        if (timesSeconds.size() == route.track.size()) {
            double startTimeSeconds = timesSeconds.get(startTrackPointIndex);
            double endTimeSeconds = timesSeconds.get(endTrackPointIndex);
            if (Double.isFinite(startTimeSeconds) && Double.isFinite(endTimeSeconds)) {
                return Math.max(0.0, endTimeSeconds - startTimeSeconds);
            }
        }

        double startMeters = polylineIndex.distanceAtPointIndex(startTrackPointIndex);
        double endMeters = polylineIndex.distanceAtPointIndex(endTrackPointIndex);
        double remainingMeters = Math.max(0.0, endMeters - startMeters);
        return estimateSecondsForDistance(route, polylineIndex, remainingMeters);
    }

    @Nullable
    private static Double estimateCurrentSegmentSeconds(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            double alongTrackMeters,
            int trackPointIndex,
            float speedMps
    ) {
        if (speedMps >= MIN_LIVE_SPEED_METERS_PER_SECOND) {
            return estimateLiveSecondsToTrackPoint(polylineIndex, alongTrackMeters, trackPointIndex, speedMps);
        }
        return estimateSecondsUsingRouteModel(route, polylineIndex, alongTrackMeters, trackPointIndex);
    }

    @Nullable
    private static Double estimateLiveSecondsToTrackPoint(
            @NonNull PolylineIndex polylineIndex,
            double alongTrackMeters,
            int trackPointIndex,
            float speedMps
    ) {
        double targetAlongTrackMeters = polylineIndex.distanceAtPointIndex(trackPointIndex);
        double remainingMeters = Math.max(0.0, targetAlongTrackMeters - alongTrackMeters);
        return speedMps >= MIN_LIVE_SPEED_METERS_PER_SECOND
                ? remainingMeters / speedMps
                : null;
    }

    @Nullable
    private static Double estimateSecondsUsingRouteModel(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            double alongTrackMeters,
            int targetTrackPointIndex
    ) {
        Double trackTimeSeconds = estimateSecondsFromTrackTimes(
                route,
                polylineIndex,
                alongTrackMeters,
                targetTrackPointIndex
        );
        if (trackTimeSeconds != null) {
            return trackTimeSeconds;
        }

        double targetAlongTrackMeters = polylineIndex.distanceAtPointIndex(targetTrackPointIndex);
        double remainingMeters = Math.max(0.0, targetAlongTrackMeters - alongTrackMeters);
        return estimateSecondsForDistance(route, polylineIndex, remainingMeters);
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

    private static boolean isOnCurrentSegment(int currentSegmentIndex, int trackPointIndex) {
        return currentSegmentIndex >= 0 && trackPointIndex <= currentSegmentIndex + 1;
    }

    @Nullable
    private static Double estimateSecondsFromTrackTimes(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            double alongTrackMeters,
            int targetTrackPointIndex
    ) {
        List<Double> timesSeconds = route.timesSeconds;
        if (timesSeconds.size() != route.track.size()
                || targetTrackPointIndex < 0
                || targetTrackPointIndex >= timesSeconds.size()) {
            return null;
        }

        double currentTimeSeconds = interpolateTimeAtAlongTrack(polylineIndex, timesSeconds, alongTrackMeters);
        double targetTimeSeconds = timesSeconds.get(targetTrackPointIndex);
        if (!Double.isFinite(currentTimeSeconds) || !Double.isFinite(targetTimeSeconds)) {
            return null;
        }
        return Math.max(0.0, targetTimeSeconds - currentTimeSeconds);
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
            if (clampedAlongTrackMeters <= nextDistanceMeters) {
                double segmentDistanceMeters = nextDistanceMeters - previousDistanceMeters;
                if (segmentDistanceMeters <= 0.0) {
                    return nextTimeSeconds;
                }
                double ratio = (clampedAlongTrackMeters - previousDistanceMeters) / segmentDistanceMeters;
                return previousTimeSeconds + ratio * (nextTimeSeconds - previousTimeSeconds);
            }
            previousDistanceMeters = nextDistanceMeters;
            previousTimeSeconds = nextTimeSeconds;
        }

        return timesSeconds.get(timesSeconds.size() - 1);
    }
}

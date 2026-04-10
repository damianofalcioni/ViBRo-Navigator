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

        double targetAlongTrackMeters = polylineIndex.distanceAtPointIndex(trackPointIndex);
        double remainingMeters = Math.max(0.0, targetAlongTrackMeters - alongTrackMeters);
        if (isOnCurrentSegment(currentSegmentIndex, trackPointIndex)) {
            return speedMps >= MIN_LIVE_SPEED_METERS_PER_SECOND
                    ? remainingMeters / speedMps
                    : null;
        }
        return estimateSecondsFromTrackTimes(route, polylineIndex, alongTrackMeters, trackPointIndex);
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

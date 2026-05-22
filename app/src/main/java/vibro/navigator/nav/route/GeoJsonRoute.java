package vibro.navigator.nav.route;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;

import java.util.Collections;
import java.util.List;

public final class GeoJsonRoute {
    @NonNull
    public final List<LatLon> track;
    @NonNull
    public final List<VoiceHint> voiceHints;
    @NonNull
    public final List<Double> timesSeconds;
    @NonNull
    public final List<RouteSpeedLimitSegment> speedLimitSegments;
    public final double totalTimeSeconds;
    public final double trackLengthMeters;

    public GeoJsonRoute(
            @NonNull List<LatLon> track,
            @NonNull List<VoiceHint> voiceHints,
            double totalTimeSeconds,
            double trackLengthMeters
    ) {
        this(track, voiceHints, Collections.emptyList(), totalTimeSeconds, trackLengthMeters);
    }

    public GeoJsonRoute(
            @NonNull List<LatLon> track,
            @NonNull List<VoiceHint> voiceHints,
            @NonNull List<Double> timesSeconds,
            double totalTimeSeconds,
            double trackLengthMeters
    ) {
        this(track, voiceHints, timesSeconds, Collections.emptyList(), totalTimeSeconds, trackLengthMeters);
    }

    public GeoJsonRoute(
            @NonNull List<LatLon> track,
            @NonNull List<VoiceHint> voiceHints,
            @NonNull List<Double> timesSeconds,
            @NonNull List<RouteSpeedLimitSegment> speedLimitSegments,
            double totalTimeSeconds,
            double trackLengthMeters
    ) {
        this.track = track;
        this.voiceHints = voiceHints;
        this.timesSeconds = timesSeconds;
        this.speedLimitSegments = speedLimitSegments;
        this.totalTimeSeconds = totalTimeSeconds;
        this.trackLengthMeters = trackLengthMeters;
    }

    @Nullable
    public RouteSpeedLimit speedLimitAt(double alongTrackMeters) {
        for (RouteSpeedLimitSegment segment : speedLimitSegments) {
            if (segment.contains(alongTrackMeters)) {
                return segment.speedLimit;
            }
        }
        return null;
    }
}

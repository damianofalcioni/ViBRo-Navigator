package vibro.navigator.nav.route;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;

import java.util.ArrayList;
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
        List<LatLon> trackCopy = immutableCopy(track);
        this.track = trackCopy;
        this.voiceHints = immutableCopy(validVoiceHints(voiceHints, trackCopy.size()));
        this.timesSeconds = immutableCopy(timesSeconds);
        this.speedLimitSegments = immutableCopy(speedLimitSegments);
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

    @NonNull
    private static <T> List<T> immutableCopy(@NonNull List<T> values) {
        if (values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    @NonNull
    private static List<VoiceHint> validVoiceHints(@NonNull List<VoiceHint> voiceHints, int trackSize) {
        if (voiceHints.isEmpty() || trackSize <= 0) {
            return Collections.emptyList();
        }
        List<VoiceHint> valid = new ArrayList<>(voiceHints.size());
        for (VoiceHint hint : voiceHints) {
            if (isValidVoiceHint(hint, trackSize)) {
                valid.add(hint);
            }
        }
        return valid;
    }

    private static boolean isValidVoiceHint(@NonNull VoiceHint hint, int trackSize) {
        return hint.indexInTrack >= 0
                && hint.indexInTrack < trackSize
                && Double.isFinite(hint.distanceToNextMeters)
                && hint.distanceToNextMeters >= 0.0;
    }
}

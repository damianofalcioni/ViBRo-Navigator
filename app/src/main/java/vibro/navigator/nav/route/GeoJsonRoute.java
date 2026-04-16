package vibro.navigator.nav.route;

import androidx.annotation.NonNull;

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
        this.track = track;
        this.voiceHints = voiceHints;
        this.timesSeconds = timesSeconds;
        this.totalTimeSeconds = totalTimeSeconds;
        this.trackLengthMeters = trackLengthMeters;
    }
}

package com.vibenavigator.nav.route;

import androidx.annotation.NonNull;

import com.vibenavigator.geo.LatLon;

import java.util.List;

public final class GeoJsonRoute {
    @NonNull
    public final List<LatLon> track;
    @NonNull
    public final List<VoiceHint> voiceHints;
    public final double totalTimeSeconds;
    public final double trackLengthMeters;

    public GeoJsonRoute(
            @NonNull List<LatLon> track,
            @NonNull List<VoiceHint> voiceHints,
            double totalTimeSeconds,
            double trackLengthMeters
    ) {
        this.track = track;
        this.voiceHints = voiceHints;
        this.totalTimeSeconds = totalTimeSeconds;
        this.trackLengthMeters = trackLengthMeters;
    }
}

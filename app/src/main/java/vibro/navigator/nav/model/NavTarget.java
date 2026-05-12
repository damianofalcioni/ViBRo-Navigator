package vibro.navigator.nav.model;

import androidx.annotation.NonNull;

public final class NavTarget {
    @NonNull
    public final String label;
    public final double alongTrackMeters;
    public final int trackIndex;

    public NavTarget(@NonNull String label, double alongTrackMeters) {
        this(label, alongTrackMeters, -1);
    }

    public NavTarget(@NonNull String label, double alongTrackMeters, int trackIndex) {
        this.label = label;
        this.alongTrackMeters = alongTrackMeters;
        this.trackIndex = trackIndex;
    }
}


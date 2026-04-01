package com.vibenavigator.nav;

import androidx.annotation.NonNull;

public final class NavTarget {
    @NonNull
    public final String label;
    public final double alongTrackMeters;

    public NavTarget(@NonNull String label, double alongTrackMeters) {
        this.label = label;
        this.alongTrackMeters = alongTrackMeters;
    }
}


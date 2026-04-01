package com.vibenavigator.poi.ui;

import androidx.annotation.NonNull;

import com.vibenavigator.poi.Poi;

public final class PoiSuggestion {
    @NonNull
    public final Poi poi;
    public final boolean deletable;

    public PoiSuggestion(@NonNull Poi poi, boolean deletable) {
        this.poi = poi;
        this.deletable = deletable;
    }
}

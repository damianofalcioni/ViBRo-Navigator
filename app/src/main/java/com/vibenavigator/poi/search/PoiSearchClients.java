package com.vibenavigator.poi.search;

import androidx.annotation.NonNull;

import com.vibenavigator.BuildConfig;

public final class PoiSearchClients {
    private PoiSearchClients() {
    }

    @NonNull
    public static PoiSearchClient createDefault() {
        String key = BuildConfig.GOOGLE_MAPS_API_KEY;
        if (key != null && !key.trim().isEmpty()) {
            return new GoogleGeocodeClient(key.trim());
        }
        return new OsmNominatimClient();
    }
}

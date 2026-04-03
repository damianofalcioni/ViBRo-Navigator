package com.vibenavigator.nav;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vibenavigator.geo.LatLon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NavigationRequest {

    public static final String EXTRA_PROFILE = "com.vibenavigator.extra.PROFILE";
    public static final String EXTRA_DEST_NAME = "com.vibenavigator.extra.DEST_NAME";
    public static final String EXTRA_DEST_LAT = "com.vibenavigator.extra.DEST_LAT";
    public static final String EXTRA_DEST_LON = "com.vibenavigator.extra.DEST_LON";
    public static final String EXTRA_STOPS = "com.vibenavigator.extra.STOPS";

    @Nullable
    public final String profile;
    @Nullable
    public final String destinationName;
    @Nullable
    public final LatLon destination;
    @NonNull
    public final List<LatLon> stops;

    public NavigationRequest(
            @Nullable String profile,
            @Nullable String destinationName,
            @Nullable LatLon destination,
            @NonNull List<LatLon> stops
    ) {
        this.profile = profile;
        this.destinationName = destinationName;
        this.destination = destination;
        this.stops = Collections.unmodifiableList(new ArrayList<>(stops));
    }

    @NonNull
    public static NavigationRequest fromIntent(@Nullable Intent intent) {
        if (intent == null) {
            return new NavigationRequest(null, null, null, Collections.emptyList());
        }

        String profile = intent.getStringExtra(EXTRA_PROFILE);
        String destinationName = intent.getStringExtra(EXTRA_DEST_NAME);
        double lat = intent.getDoubleExtra(EXTRA_DEST_LAT, Double.NaN);
        double lon = intent.getDoubleExtra(EXTRA_DEST_LON, Double.NaN);
        LatLon destination = (!Double.isNaN(lat) && !Double.isNaN(lon)) ? new LatLon(lat, lon) : null;

        ArrayList<String> rawStops = intent.getStringArrayListExtra(EXTRA_STOPS);
        List<LatLon> stops = new ArrayList<>();
        if (rawStops != null) {
            for (String rawStop : rawStops) {
                LatLon stop = parseLatLon(rawStop);
                if (stop != null) {
                    stops.add(stop);
                }
            }
        }

        return new NavigationRequest(profile, destinationName, destination, stops);
    }

    public boolean isComplete() {
        return destination != null && profile != null && !profile.trim().isEmpty();
    }

    public void putInto(@NonNull Intent intent) {
        if (profile != null) {
            intent.putExtra(EXTRA_PROFILE, profile);
        }
        if (destinationName != null) {
            intent.putExtra(EXTRA_DEST_NAME, destinationName);
        }
        if (destination != null) {
            intent.putExtra(EXTRA_DEST_LAT, destination.lat);
            intent.putExtra(EXTRA_DEST_LON, destination.lon);
        }
        if (!stops.isEmpty()) {
            intent.putStringArrayListExtra(EXTRA_STOPS, toStopStrings());
        }
    }

    @NonNull
    public ArrayList<String> toStopStrings() {
        ArrayList<String> serializedStops = new ArrayList<>(stops.size());
        for (LatLon stop : stops) {
            serializedStops.add(stop.lat + "," + stop.lon);
        }
        return serializedStops;
    }

    @NonNull
    public String describe() {
        return "profile=" + safe(profile)
                + ", destName=" + safe(destinationName)
                + ", destination=" + formatLatLon(destination)
                + ", stops=" + stops.size();
    }

    @Nullable
    private static LatLon parseLatLon(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String[] parts = value.split(",");
        if (parts.length < 2) {
            return null;
        }
        try {
            double lat = Double.parseDouble(parts[0].trim());
            double lon = Double.parseDouble(parts[1].trim());
            return new LatLon(lat, lon);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @NonNull
    private static String formatLatLon(@Nullable LatLon value) {
        if (value == null) {
            return "null";
        }
        return value.lat + "," + value.lon;
    }

    @NonNull
    private static String safe(@Nullable String value) {
        return value == null ? "null" : value;
    }
}

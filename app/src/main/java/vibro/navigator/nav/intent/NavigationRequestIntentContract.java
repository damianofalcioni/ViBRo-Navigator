package vibro.navigator.nav.intent;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.model.NavigationRequest;

public final class NavigationRequestIntentContract {

    public static final String EXTRA_PROFILE = "vibro.navigator.extra.PROFILE";
    public static final String EXTRA_DEST_NAME = "vibro.navigator.extra.DEST_NAME";
    public static final String EXTRA_DEST_LAT = "vibro.navigator.extra.DEST_LAT";
    public static final String EXTRA_DEST_LON = "vibro.navigator.extra.DEST_LON";
    public static final String EXTRA_STOPS = "vibro.navigator.extra.STOPS";

    private NavigationRequestIntentContract() {
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

    public static void putInto(@NonNull Intent intent, @NonNull NavigationRequest request) {
        if (request.profile != null) {
            intent.putExtra(EXTRA_PROFILE, request.profile);
        }
        if (request.destinationName != null) {
            intent.putExtra(EXTRA_DEST_NAME, request.destinationName);
        }
        if (request.destination != null) {
            intent.putExtra(EXTRA_DEST_LAT, request.destination.lat);
            intent.putExtra(EXTRA_DEST_LON, request.destination.lon);
        }
        if (!request.stops.isEmpty()) {
            intent.putStringArrayListExtra(EXTRA_STOPS, toStopStrings(request.stops));
        }
    }

    @NonNull
    public static ArrayList<String> toStopStrings(@NonNull List<LatLon> stops) {
        ArrayList<String> serializedStops = new ArrayList<>(stops.size());
        for (LatLon stop : stops) {
            serializedStops.add(stop.lat + "," + stop.lon);
        }
        return serializedStops;
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
}

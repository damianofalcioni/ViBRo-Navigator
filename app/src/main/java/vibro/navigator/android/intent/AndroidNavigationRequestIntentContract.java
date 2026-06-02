package vibro.navigator.android.intent;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.model.NavigationRequest;

public final class AndroidNavigationRequestIntentContract {

    public static final String EXTRA_PROFILE = "vibro.navigator.extra.PROFILE";
    public static final String EXTRA_DEST_NAME = "vibro.navigator.extra.DEST_NAME";
    public static final String EXTRA_DEST_LAT = "vibro.navigator.extra.DEST_LAT";
    public static final String EXTRA_DEST_LON = "vibro.navigator.extra.DEST_LON";
    public static final String EXTRA_STOPS = "vibro.navigator.extra.STOPS";

    private AndroidNavigationRequestIntentContract() {
    }

    @NonNull
    public static NavigationRequest fromIntent(@Nullable Intent intent) {
        if (intent == null) {
            return fromExtras(null);
        }

        return fromExtras(new Extras(
                intent.getStringExtra(EXTRA_PROFILE),
                intent.getStringExtra(EXTRA_DEST_NAME),
                intent.getDoubleExtra(EXTRA_DEST_LAT, Double.NaN),
                intent.getDoubleExtra(EXTRA_DEST_LON, Double.NaN),
                intent.getStringArrayListExtra(EXTRA_STOPS)
        ));
    }

    @NonNull
    static NavigationRequest fromExtras(@Nullable Extras extras) {
        if (extras == null) {
            return new NavigationRequest(null, null, null, Collections.emptyList());
        }

        LatLon destination = (!Double.isNaN(extras.destinationLat) && !Double.isNaN(extras.destinationLon))
                ? new LatLon(extras.destinationLat, extras.destinationLon)
                : null;
        List<LatLon> stops = new ArrayList<>();
        for (String rawStop : extras.stops) {
            LatLon stop = parseLatLon(rawStop);
            if (stop != null) {
                stops.add(stop);
            }
        }

        return new NavigationRequest(extras.profile, extras.destinationName, destination, stops);
    }

    public static void putInto(@NonNull Intent intent, @NonNull NavigationRequest request) {
        Extras extras = toExtras(request);
        if (extras.profile != null) {
            intent.putExtra(EXTRA_PROFILE, extras.profile);
        }
        if (extras.destinationName != null) {
            intent.putExtra(EXTRA_DEST_NAME, extras.destinationName);
        }
        if (!Double.isNaN(extras.destinationLat) && !Double.isNaN(extras.destinationLon)) {
            intent.putExtra(EXTRA_DEST_LAT, extras.destinationLat);
            intent.putExtra(EXTRA_DEST_LON, extras.destinationLon);
        }
        if (!extras.stops.isEmpty()) {
            intent.putStringArrayListExtra(EXTRA_STOPS, extras.stops);
        }
    }

    @NonNull
    static Extras toExtras(@NonNull NavigationRequest request) {
        double destinationLat = request.destination == null ? Double.NaN : request.destination.lat;
        double destinationLon = request.destination == null ? Double.NaN : request.destination.lon;
        return new Extras(
                request.profile,
                request.destinationName,
                destinationLat,
                destinationLon,
                toStopStrings(request.stops)
        );
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

    static final class Extras {
        @Nullable
        final String profile;
        @Nullable
        final String destinationName;
        final double destinationLat;
        final double destinationLon;
        @NonNull
        final ArrayList<String> stops;

        Extras(
                @Nullable String profile,
                @Nullable String destinationName,
                double destinationLat,
                double destinationLon,
                @Nullable ArrayList<String> stops
        ) {
            this.profile = profile;
            this.destinationName = destinationName;
            this.destinationLat = destinationLat;
            this.destinationLon = destinationLon;
            this.stops = stops == null ? new ArrayList<>() : new ArrayList<>(stops);
        }
    }
}

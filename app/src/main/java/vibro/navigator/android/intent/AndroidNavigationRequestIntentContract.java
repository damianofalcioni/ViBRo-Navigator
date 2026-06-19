package vibro.navigator.android.intent;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavigationRoutingMode;

public final class AndroidNavigationRequestIntentContract {

    public static final String EXTRA_PROFILE = "vibro.navigator.extra.PROFILE";
    public static final String EXTRA_PROFILE_PARAMETERS = "vibro.navigator.extra.PROFILE_PARAMETERS";
    public static final String EXTRA_ROUTING_MODE = "vibro.navigator.extra.ROUTING_MODE";
    public static final String EXTRA_DEST_NAME = "vibro.navigator.extra.DEST_NAME";
    public static final String EXTRA_DEST_LAT = "vibro.navigator.extra.DEST_LAT";
    public static final String EXTRA_DEST_LON = "vibro.navigator.extra.DEST_LON";
    public static final String EXTRA_STOPS = "vibro.navigator.extra.STOPS";
    public static final String EXTRA_ROUND_TRIP_DISTANCE_METERS =
            "vibro.navigator.extra.ROUND_TRIP_DISTANCE_METERS";

    private AndroidNavigationRequestIntentContract() {
    }

    @NonNull
    public static NavigationRequest fromIntent(@Nullable Intent intent) {
        if (intent == null) {
            return fromExtras(null);
        }

        return fromExtras(new Extras(
                intent.getStringExtra(EXTRA_ROUTING_MODE),
                intent.getStringExtra(EXTRA_PROFILE),
                intent.getStringExtra(EXTRA_PROFILE_PARAMETERS),
                intent.getStringExtra(EXTRA_DEST_NAME),
                intent.getDoubleExtra(EXTRA_DEST_LAT, Double.NaN),
                intent.getDoubleExtra(EXTRA_DEST_LON, Double.NaN),
                intent.getStringArrayListExtra(EXTRA_STOPS),
                intent.getIntExtra(EXTRA_ROUND_TRIP_DISTANCE_METERS, 0)
        ));
    }

    @NonNull
    static NavigationRequest fromExtras(@Nullable Extras extras) {
        if (extras == null) {
            return new NavigationRequest(null, null, null, Collections.emptyList());
        }

        NavigationRoutingMode routingMode = NavigationRoutingMode.fromSerializedName(extras.routingMode);
        LatLon destination = LatLon.isValidCoordinate(extras.destinationLat, extras.destinationLon)
                ? new LatLon(extras.destinationLat, extras.destinationLon)
                : null;
        List<LatLon> stops = new ArrayList<>();
        for (String rawStop : extras.stops) {
            LatLon stop = parseLatLon(rawStop);
            if (stop != null) {
                stops.add(stop);
            }
        }

        return new NavigationRequest(
                routingMode,
                extras.profile,
                extras.profileParameters,
                extras.destinationName,
                destination,
                stops,
                extras.roundTripDistanceMeters
        );
    }

    public static void putInto(@NonNull Intent intent, @NonNull NavigationRequest request) {
        Extras extras = toExtras(request);
        intent.putExtra(EXTRA_ROUTING_MODE, extras.routingMode);
        if (extras.profile != null) {
            intent.putExtra(EXTRA_PROFILE, extras.profile);
        }
        if (extras.profileParameters != null) {
            intent.putExtra(EXTRA_PROFILE_PARAMETERS, extras.profileParameters);
        }
        if (extras.destinationName != null) {
            intent.putExtra(EXTRA_DEST_NAME, extras.destinationName);
        }
        if (LatLon.isValidCoordinate(extras.destinationLat, extras.destinationLon)) {
            intent.putExtra(EXTRA_DEST_LAT, extras.destinationLat);
            intent.putExtra(EXTRA_DEST_LON, extras.destinationLon);
        }
        if (!extras.stops.isEmpty()) {
            intent.putStringArrayListExtra(EXTRA_STOPS, extras.stops);
        }
        if (extras.roundTripDistanceMeters > 0) {
            intent.putExtra(EXTRA_ROUND_TRIP_DISTANCE_METERS, extras.roundTripDistanceMeters);
        }
    }

    @NonNull
    static Extras toExtras(@NonNull NavigationRequest request) {
        double destinationLat = request.destination == null ? Double.NaN : request.destination.lat;
        double destinationLon = request.destination == null ? Double.NaN : request.destination.lon;
        return new Extras(
                request.routingMode.serializedName(),
                request.profile,
                request.profileParameters,
                request.destinationName,
                destinationLat,
                destinationLon,
                toStopStrings(request.stops),
                request.roundTripDistanceMeters
        );
    }

    @NonNull
    static ArrayList<String> toStopStrings(@NonNull List<LatLon> stops) {
        ArrayList<String> serializedStops = new ArrayList<>(stops.size());
        for (LatLon stop : stops) {
            if (stop.isValid()) {
                serializedStops.add(stop.lat + "," + stop.lon);
            }
        }
        return serializedStops;
    }

    @Nullable
    private static LatLon parseLatLon(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String[] parts = value.split(",");
        if (parts.length != 2) {
            return null;
        }
        try {
            double lat = Double.parseDouble(parts[0].trim());
            double lon = Double.parseDouble(parts[1].trim());
            return LatLon.isValidCoordinate(lat, lon) ? new LatLon(lat, lon) : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static final class Extras {
        @NonNull
        final String routingMode;
        @Nullable
        final String profile;
        @Nullable
        final String profileParameters;
        @Nullable
        final String destinationName;
        final double destinationLat;
        final double destinationLon;
        @NonNull
        final ArrayList<String> stops;
        final int roundTripDistanceMeters;

        Extras(
                @Nullable String profile,
                @Nullable String destinationName,
                double destinationLat,
                double destinationLon,
                @Nullable ArrayList<String> stops
        ) {
            this(null, profile, null, destinationName, destinationLat, destinationLon, stops, 0);
        }

        Extras(
                @Nullable String routingMode,
                @Nullable String profile,
                @Nullable String profileParameters,
                @Nullable String destinationName,
                double destinationLat,
                double destinationLon,
                @Nullable ArrayList<String> stops
        ) {
            this(routingMode, profile, profileParameters, destinationName, destinationLat, destinationLon, stops, 0);
        }

        Extras(
                @Nullable String routingMode,
                @Nullable String profile,
                @Nullable String profileParameters,
                @Nullable String destinationName,
                double destinationLat,
                double destinationLon,
                @Nullable ArrayList<String> stops,
                int roundTripDistanceMeters
        ) {
            this.routingMode = NavigationRoutingMode.fromSerializedName(routingMode).serializedName();
            this.profile = profile;
            this.profileParameters = clean(profileParameters);
            this.destinationName = destinationName;
            this.destinationLat = destinationLat;
            this.destinationLon = destinationLon;
            this.stops = stops == null ? new ArrayList<>() : new ArrayList<>(stops);
            this.roundTripDistanceMeters = Math.max(0, roundTripDistanceMeters);
        }

        @Nullable
        private static String clean(@Nullable String value) {
            if (value == null) {
                return null;
            }
            String trimmed = value.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
    }
}

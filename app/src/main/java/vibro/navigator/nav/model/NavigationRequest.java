package vibro.navigator.nav.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NavigationRequest {
    @NonNull
    public final NavigationRoutingMode routingMode;
    @Nullable
    public final String profile;
    @Nullable
    public final String profileParameters;
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
        this(NavigationRoutingMode.BROUTER, profile, null, destinationName, destination, stops);
    }

    public NavigationRequest(
            @NonNull NavigationRoutingMode routingMode,
            @Nullable String profile,
            @Nullable String destinationName,
            @Nullable LatLon destination,
            @NonNull List<LatLon> stops
    ) {
        this(routingMode, profile, null, destinationName, destination, stops);
    }

    public NavigationRequest(
            @NonNull NavigationRoutingMode routingMode,
            @Nullable String profile,
            @Nullable String profileParameters,
            @Nullable String destinationName,
            @Nullable LatLon destination,
            @NonNull List<LatLon> stops
    ) {
        this.routingMode = routingMode;
        this.profile = profile;
        this.profileParameters = clean(profileParameters);
        this.destinationName = destinationName;
        this.destination = destination;
        this.stops = Collections.unmodifiableList(new ArrayList<>(stops));
    }

    public boolean isComplete() {
        return destination != null && (isStraightLine() || (profile != null && !profile.trim().isEmpty()));
    }

    public boolean isStraightLine() {
        return routingMode == NavigationRoutingMode.STRAIGHT_LINE;
    }

    @NonNull
    public String describe() {
        return "mode=" + routingMode.serializedName()
                + ", profile=" + safe(profile)
                + ", profileParams=" + (profileParameters == null ? 0 : profileParameters.length())
                + ", destName=" + safe(destinationName)
                + ", destination=" + formatLatLon(destination)
                + ", stops=" + stops.size();
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

    @Nullable
    private static String clean(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

package vibro.navigator.nav.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NavigationRequest {
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

    public boolean isComplete() {
        return destination != null && profile != null && !profile.trim().isEmpty();
    }

    @NonNull
    public String describe() {
        return "profile=" + safe(profile)
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
}

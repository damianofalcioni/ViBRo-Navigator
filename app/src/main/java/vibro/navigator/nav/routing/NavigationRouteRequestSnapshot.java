package vibro.navigator.nav.routing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.brouter.NogoPoint;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.model.NavigationRoutingMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NavigationRouteRequestSnapshot {
    public final int requestNumber;
    public final int requestToken;
    @NonNull
    public final NavigationRoutingMode routingMode;
    @NonNull
    public final LatLon start;
    @NonNull
    public final List<LatLon> intermediates;
    @Nullable
    public final LatLon destination;
    @Nullable
    public final String profile;
    @Nullable
    public final String profileParameters;
    @NonNull
    public final List<NogoPoint> blocked;
    public final int roundTripDistanceMeters;

    public NavigationRouteRequestSnapshot(
            int requestNumber,
            int requestToken,
            @NonNull LatLon start,
            @NonNull List<LatLon> intermediates,
            @Nullable LatLon destination,
            @Nullable String profile,
            @Nullable String profileParameters,
            @NonNull List<NogoPoint> blocked
    ) {
        this(
                requestNumber,
                requestToken,
                NavigationRoutingMode.BROUTER,
                start,
                intermediates,
                destination,
                profile,
                profileParameters,
                blocked,
                0
        );
    }

    public NavigationRouteRequestSnapshot(
            int requestNumber,
            int requestToken,
            @NonNull NavigationRoutingMode routingMode,
            @NonNull LatLon start,
            @NonNull List<LatLon> intermediates,
            @Nullable LatLon destination,
            @Nullable String profile,
            @Nullable String profileParameters,
            @NonNull List<NogoPoint> blocked,
            int roundTripDistanceMeters
    ) {
        this.requestNumber = requestNumber;
        this.requestToken = requestToken;
        this.routingMode = routingMode;
        this.start = start;
        this.intermediates = immutableCopy(intermediates);
        this.destination = destination;
        this.profile = profile;
        this.profileParameters = clean(profileParameters);
        this.blocked = immutableCopy(blocked);
        this.roundTripDistanceMeters = Math.max(0, roundTripDistanceMeters);
    }

    public boolean isRoundTrip() {
        return routingMode == NavigationRoutingMode.ROUND_TRIP;
    }

    @NonNull
    private static <T> List<T> immutableCopy(@NonNull List<T> values) {
        if (values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
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

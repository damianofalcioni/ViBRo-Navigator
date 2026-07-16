package vibro.navigator.nav.routing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.brouter.NogoPoint;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.model.NavigationRoutingMode;
import vibro.navigator.nav.model.RoundTripDirection;

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
    public final boolean customProfile;
    @Nullable
    public final String profileParameters;
    @NonNull
    public final List<NogoPoint> blocked;
    public final int roundTripDistanceMeters;
    public final int roundTripDirectionDegrees;

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
                false,
                profileParameters,
                blocked,
                0,
                RoundTripDirection.DEFAULT_DIRECTION_DEGREES
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
        this(
                requestNumber,
                requestToken,
                routingMode,
                start,
                intermediates,
                destination,
                profile,
                false,
                profileParameters,
                blocked,
                roundTripDistanceMeters,
                RoundTripDirection.DEFAULT_DIRECTION_DEGREES
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
            boolean customProfile,
            @Nullable String profileParameters,
            @NonNull List<NogoPoint> blocked,
            int roundTripDistanceMeters
    ) {
        this(
                requestNumber,
                requestToken,
                routingMode,
                start,
                intermediates,
                destination,
                profile,
                customProfile,
                profileParameters,
                blocked,
                roundTripDistanceMeters,
                RoundTripDirection.DEFAULT_DIRECTION_DEGREES
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
            int roundTripDistanceMeters,
            int roundTripDirectionDegrees
    ) {
        this(
                requestNumber,
                requestToken,
                routingMode,
                start,
                intermediates,
                destination,
                profile,
                false,
                profileParameters,
                blocked,
                roundTripDistanceMeters,
                roundTripDirectionDegrees
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
            boolean customProfile,
            @Nullable String profileParameters,
            @NonNull List<NogoPoint> blocked,
            int roundTripDistanceMeters,
            int roundTripDirectionDegrees
    ) {
        this.requestNumber = requestNumber;
        this.requestToken = requestToken;
        this.routingMode = routingMode;
        this.start = start;
        this.intermediates = immutableCopy(intermediates);
        this.destination = destination;
        this.profile = profile;
        this.customProfile = customProfile;
        this.profileParameters = clean(profileParameters);
        this.blocked = immutableCopy(blocked);
        this.roundTripDistanceMeters = Math.max(0, roundTripDistanceMeters);
        this.roundTripDirectionDegrees = RoundTripDirection.sanitizeDirectionDegrees(roundTripDirectionDegrees);
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

package vibro.navigator.brouter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.model.RoundTripDirection;

public final class BRouterRouteRequest {
    @NonNull
    public final LatLon start;
    @NonNull
    public final List<LatLon> intermediates;
    @Nullable
    public final LatLon destination;
    @NonNull
    public final String profile;
    public final boolean customProfile;
    @Nullable
    public final String profileParameters;
    @NonNull
    public final List<NogoPoint> blockedWaypoints;
    public final int roundTripDistanceMeters;
    public final int roundTripDirectionDegrees;

    public BRouterRouteRequest(
            @NonNull LatLon start,
            @Nullable List<LatLon> intermediates,
            @NonNull LatLon destination,
            @NonNull String profile,
            @Nullable List<NogoPoint> blockedWaypoints
    ) {
        this(start, intermediates, destination, profile, null, blockedWaypoints);
    }

    public BRouterRouteRequest(
            @NonNull LatLon start,
            @Nullable List<LatLon> intermediates,
            @NonNull LatLon destination,
            @NonNull String profile,
            @Nullable String profileParameters,
            @Nullable List<NogoPoint> blockedWaypoints
    ) {
        this(start, intermediates, destination, profile, false, profileParameters, blockedWaypoints);
    }

    public BRouterRouteRequest(
            @NonNull LatLon start,
            @Nullable List<LatLon> intermediates,
            @NonNull LatLon destination,
            @NonNull String profile,
            boolean customProfile,
            @Nullable String profileParameters,
            @Nullable List<NogoPoint> blockedWaypoints
    ) {
        this(start, intermediates, destination, profile, customProfile, profileParameters, blockedWaypoints, 0);
    }

    private BRouterRouteRequest(
            @NonNull LatLon start,
            @Nullable List<LatLon> intermediates,
            @Nullable LatLon destination,
            @NonNull String profile,
            boolean customProfile,
            @Nullable String profileParameters,
            @Nullable List<NogoPoint> blockedWaypoints,
            int roundTripDistanceMeters
    ) {
        this(
                start,
                intermediates,
                destination,
                profile,
                customProfile,
                profileParameters,
                blockedWaypoints,
                roundTripDistanceMeters,
                RoundTripDirection.DEFAULT_DIRECTION_DEGREES
        );
    }

    private BRouterRouteRequest(
            @NonNull LatLon start,
            @Nullable List<LatLon> intermediates,
            @Nullable LatLon destination,
            @NonNull String profile,
            boolean customProfile,
            @Nullable String profileParameters,
            @Nullable List<NogoPoint> blockedWaypoints,
            int roundTripDistanceMeters,
            int roundTripDirectionDegrees
    ) {
        this.start = start;
        this.intermediates = immutableCopy(intermediates);
        this.destination = destination;
        this.profile = profile;
        this.customProfile = customProfile;
        this.profileParameters = clean(profileParameters);
        this.blockedWaypoints = immutableCopy(blockedWaypoints);
        this.roundTripDistanceMeters = Math.max(0, roundTripDistanceMeters);
        this.roundTripDirectionDegrees = RoundTripDirection.sanitizeDirectionDegrees(roundTripDirectionDegrees);
    }

    @NonNull
    public static BRouterRouteRequest roundTrip(
            @NonNull LatLon start,
            @NonNull String profile,
            @Nullable String profileParameters,
            @Nullable List<NogoPoint> blockedWaypoints,
            int roundTripDistanceMeters
    ) {
        return roundTrip(
                start,
                profile,
                false,
                profileParameters,
                blockedWaypoints,
                roundTripDistanceMeters,
                RoundTripDirection.DEFAULT_DIRECTION_DEGREES
        );
    }

    @NonNull
    public static BRouterRouteRequest roundTrip(
            @NonNull LatLon start,
            @NonNull String profile,
            @Nullable String profileParameters,
            @Nullable List<NogoPoint> blockedWaypoints,
            int roundTripDistanceMeters,
            int roundTripDirectionDegrees
    ) {
        return roundTrip(
                start,
                profile,
                false,
                profileParameters,
                blockedWaypoints,
                roundTripDistanceMeters,
                roundTripDirectionDegrees
        );
    }

    @NonNull
    public static BRouterRouteRequest roundTrip(
            @NonNull LatLon start,
            @NonNull String profile,
            boolean customProfile,
            @Nullable String profileParameters,
            @Nullable List<NogoPoint> blockedWaypoints,
            int roundTripDistanceMeters,
            int roundTripDirectionDegrees
    ) {
        return new BRouterRouteRequest(
                start,
                Collections.emptyList(),
                null,
                profile,
                customProfile,
                profileParameters,
                blockedWaypoints,
                roundTripDistanceMeters,
                roundTripDirectionDegrees
        );
    }

    public boolean isRoundTrip() {
        return roundTripDistanceMeters > 0;
    }

    @NonNull
    private static <T> List<T> immutableCopy(@Nullable List<T> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(items));
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

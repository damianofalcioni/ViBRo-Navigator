package vibro.navigator.brouter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.GeoJsonRouteParser;
import vibro.navigator.logging.AppLogger;

import java.util.ArrayList;
import java.util.List;

public final class BRouterRouter {

    private static final String TAG = "BRouterRouter";

    @NonNull
    public GeoJsonRoute routeGeoJson(
            @NonNull BRouterRouteClient client,
            @NonNull LatLon start,
            @NonNull List<LatLon> intermediates,
            @NonNull LatLon end,
            @NonNull String profile,
            @Nullable List<NogoPoint> blockedWaypoints
    ) throws Exception {
        return routeGeoJson(client, start, intermediates, end, profile, blockedWaypoints, null);
    }

    @NonNull
    public GeoJsonRoute routeGeoJson(
            @NonNull BRouterRouteClient client,
            @NonNull LatLon start,
            @NonNull List<LatLon> intermediates,
            @NonNull LatLon end,
            @NonNull String profile,
            @Nullable List<NogoPoint> blockedWaypoints,
            @Nullable String profileParameters
    ) throws Exception {
        return routeGeoJson(
                client,
                start,
                intermediates,
                end,
                profile,
                false,
                blockedWaypoints,
                profileParameters
        );
    }

    @NonNull
    public GeoJsonRoute routeGeoJson(
            @NonNull BRouterRouteClient client,
            @NonNull LatLon start,
            @NonNull List<LatLon> intermediates,
            @NonNull LatLon end,
            @NonNull String profile,
            boolean customProfile,
            @Nullable List<NogoPoint> blockedWaypoints,
            @Nullable String profileParameters
    ) throws Exception {
        List<LatLon> stops = intermediates != null ? intermediates : new ArrayList<>();
        AppLogger.i(TAG, "Building route request start=" + start.lat + "," + start.lon
                + " destination=" + end.lat + "," + end.lon
                + " profile=" + profile
                + " profileSource=" + (customProfile ? "custom" : "brouter")
                + " profileParams=" + (profileParameters == null ? 0 : profileParameters.length())
                + " intermediates=" + stops.size()
                + " blocked=" + (blockedWaypoints == null ? 0 : blockedWaypoints.size()));
        String decoded = requestRoutePayload(
                client,
                start,
                stops,
                end,
                profile,
                customProfile,
                blockedWaypoints,
                profileParameters
        );
        return parseRoutePayload(decoded, stops, end);
    }

    @NonNull
    public GeoJsonRoute roundTripGeoJson(
            @NonNull BRouterRouteClient client,
            @NonNull LatLon start,
            @NonNull String profile,
            @Nullable List<NogoPoint> blockedWaypoints,
            int roundTripDistanceMeters,
            int roundTripDirectionDegrees,
            @Nullable String profileParameters
    ) throws Exception {
        return roundTripGeoJson(
                client,
                start,
                profile,
                false,
                blockedWaypoints,
                roundTripDistanceMeters,
                roundTripDirectionDegrees,
                profileParameters
        );
    }

    @NonNull
    public GeoJsonRoute roundTripGeoJson(
            @NonNull BRouterRouteClient client,
            @NonNull LatLon start,
            @NonNull String profile,
            boolean customProfile,
            @Nullable List<NogoPoint> blockedWaypoints,
            int roundTripDistanceMeters,
            int roundTripDirectionDegrees,
            @Nullable String profileParameters
    ) throws Exception {
        AppLogger.i(TAG, "Building round trip request start=" + start.lat + "," + start.lon
                + " profile=" + profile
                + " profileSource=" + (customProfile ? "custom" : "brouter")
                + " profileParams=" + (profileParameters == null ? 0 : profileParameters.length())
                + " radiusMeters=" + roundTripDistanceMeters
                + " directionDegrees=" + roundTripDirectionDegrees
                + " blocked=" + (blockedWaypoints == null ? 0 : blockedWaypoints.size()));
        String decoded = client.requestRoutePayload(BRouterRouteRequest.roundTrip(
                start,
                profile,
                customProfile,
                profileParameters,
                blockedWaypoints,
                roundTripDistanceMeters,
                roundTripDirectionDegrees
        ));
        if (decoded == null) {
            AppLogger.w(TAG, "BRouter returned null round trip payload");
            throw BRouterRouteException.serviceUnavailable("BRouter service not available");
        }
        AppLogger.dMultiline(TAG, "Full BRouter response=", decoded);
        return parseRoutePayload(decoded);
    }

    @NonNull
    private String requestRoutePayload(
            @NonNull BRouterRouteClient client,
            @NonNull LatLon start,
            @NonNull List<LatLon> stops,
            @NonNull LatLon end,
            @NonNull String profile,
            boolean customProfile,
            @Nullable List<NogoPoint> blockedWaypoints,
            @Nullable String profileParameters
    ) throws Exception {
        String decoded = client.requestRoutePayload(new BRouterRouteRequest(
                start,
                stops,
                end,
                profile,
                customProfile,
                profileParameters,
                blockedWaypoints
        ));
        if (decoded == null) {
            AppLogger.w(TAG, "BRouter returned null route payload");
            throw BRouterRouteException.serviceUnavailable("BRouter service not available");
        }
        AppLogger.dMultiline(TAG, "Full BRouter response=", decoded);
        return decoded;
    }

    @NonNull
    private GeoJsonRoute parseRoutePayload(@NonNull String decoded) throws Exception {
        return parseRoutePayload(decoded, null, null);
    }

    @NonNull
    private GeoJsonRoute parseRoutePayload(
            @NonNull String decoded,
            @Nullable List<LatLon> intermediates,
            @Nullable LatLon destination
    ) throws Exception {
        String sanitized = decoded == null ? "" : decoded.trim();
        if (!sanitized.startsWith("{")) {
            AppLogger.w(TAG, "BRouter returned non-GeoJSON payload prefix="
                    + sanitized.substring(0, Math.min(120, sanitized.length())));
            throw BRouterRouteException.fromTextResponse(sanitized);
        }
        GeoJsonRoute route = GeoJsonRouteParser.parse(decoded);
        if (destination != null) {
            route = BRouterRouteBeelineAppender.appendDestinationBeelines(
                    route,
                    intermediates != null ? intermediates : new ArrayList<LatLon>(),
                    destination
            );
        }
        AppLogger.i(TAG, "Parsed route trackPoints=" + route.track.size()
                + " voiceHints=" + route.voiceHints.size()
                + " lengthMeters=" + route.trackLengthMeters
                + " totalTimeSeconds=" + route.totalTimeSeconds);
        return route;
    }
}

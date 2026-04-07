package com.vibenavigator.brouter;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vibenavigator.geo.LatLon;
import com.vibenavigator.nav.route.GeoJsonRoute;
import com.vibenavigator.nav.route.GeoJsonRouteParser;
import com.vibenavigator.util.AppLogger;

import java.util.ArrayList;
import java.util.List;

public final class BRouterRouter {

    private static final String TAG = "BRouterRouter";

    @NonNull
    public GeoJsonRoute routeGeoJson(
            @NonNull Context context,
            @NonNull LatLon start,
            @NonNull List<LatLon> intermediates,
            @NonNull LatLon end,
            @NonNull String profile,
            @Nullable List<NogoPoint> blockedWaypoints
    ) throws Exception {
        try (BRouterClient client = new BRouterClient(context)) {
            return routeGeoJson(client, start, intermediates, end, profile, blockedWaypoints);
        }
    }

    @NonNull
    public GeoJsonRoute routeGeoJson(
            @NonNull BRouterClient client,
            @NonNull LatLon start,
            @NonNull List<LatLon> intermediates,
            @NonNull LatLon end,
            @NonNull String profile,
            @Nullable List<NogoPoint> blockedWaypoints
    ) throws Exception {
        List<LatLon> stops = intermediates != null ? intermediates : new ArrayList<>();
        AppLogger.i(TAG, "Building route request start=" + start.lat + "," + start.lon
                + " destination=" + end.lat + "," + end.lon
                + " profile=" + profile
                + " intermediates=" + stops.size()
                + " blocked=" + (blockedWaypoints == null ? 0 : blockedWaypoints.size()));
        Bundle params = BRouterParams.buildRouteParams(
                start,
                stops,
                end,
                profile,
                blockedWaypoints
        );

        String raw = client.getTrackFromParams(params);
        if (raw == null) {
            AppLogger.w(TAG, "BRouter returned null route payload");
            throw BRouterRouteException.serviceUnavailable("BRouter service not available");
        }
        String decoded = BRouterClient.decodeResult(raw);
        AppLogger.dMultiline(TAG, "Full BRouter response=", decoded);
        String sanitized = decoded == null ? "" : decoded.trim();
        if (!sanitized.startsWith("{")) {
            AppLogger.w(TAG, "BRouter returned non-GeoJSON payload prefix="
                    + sanitized.substring(0, Math.min(120, sanitized.length())));
            throw BRouterRouteException.fromTextResponse(sanitized);
        }
        GeoJsonRoute route = GeoJsonRouteParser.parse(decoded);
        AppLogger.i(TAG, "Parsed route trackPoints=" + route.track.size()
                + " voiceHints=" + route.voiceHints.size()
                + " lengthMeters=" + route.trackLengthMeters
                + " totalTimeSeconds=" + route.totalTimeSeconds);
        return route;
    }
}

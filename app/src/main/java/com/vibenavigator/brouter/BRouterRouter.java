package com.vibenavigator.brouter;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vibenavigator.geo.LatLon;
import com.vibenavigator.nav.route.GeoJsonRoute;
import com.vibenavigator.nav.route.GeoJsonRouteParser;

import java.util.ArrayList;
import java.util.List;

public final class BRouterRouter {

    @NonNull
    public GeoJsonRoute routeGeoJson(
            @NonNull Context context,
            @NonNull LatLon start,
            @NonNull List<LatLon> intermediates,
            @NonNull LatLon end,
            @NonNull String profile,
            @Nullable List<LatLon> blockedWaypoints
    ) throws Exception {
        List<LatLon> stops = intermediates != null ? intermediates : new ArrayList<>();
        Bundle params = BRouterParams.buildRouteParams(
                start,
                stops,
                end,
                profile,
                BRouterParams.VehicleMode.MOTORCAR,
                false,
                blockedWaypoints
        );

        try (BRouterClient client = new BRouterClient(context)) {
            String raw = client.getTrackFromParams(params);
            if (raw == null) {
                throw new IllegalStateException("BRouter service not available");
            }
            String decoded = BRouterClient.decodeResult(raw);
            if (!decoded.trim().startsWith("{")) {
                throw new IllegalStateException(decoded.trim());
            }
            return GeoJsonRouteParser.parse(decoded);
        }
    }
}

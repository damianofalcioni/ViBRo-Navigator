package vibro.navigator.nav.routing;

import androidx.annotation.NonNull;

import vibro.navigator.brouter.BRouterRouteClient;
import vibro.navigator.brouter.BRouterRouter;
import vibro.navigator.brouter.NogoPoint;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;

import java.util.List;

public final class NavigationBRouterRouteCalculator implements NavigationRouteExecutor.RouteCalculator, AutoCloseable {

    private final BRouterRouter router = new BRouterRouter();
    @NonNull
    private final BRouterRouteClient client;

    public NavigationBRouterRouteCalculator(@NonNull BRouterRouteClient client) {
        this.client = client;
    }

    @NonNull
    @Override
    public synchronized GeoJsonRoute routeGeoJson(
            @NonNull LatLon start,
            @NonNull List<LatLon> intermediates,
            @NonNull LatLon destination,
            @NonNull String profile,
            @NonNull List<NogoPoint> blocked
    ) throws Exception {
        return routeGeoJson(start, intermediates, destination, profile, blocked, "");
    }

    @NonNull
    @Override
    public synchronized GeoJsonRoute routeGeoJson(
            @NonNull LatLon start,
            @NonNull List<LatLon> intermediates,
            @NonNull LatLon destination,
            @NonNull String profile,
            @NonNull List<NogoPoint> blocked,
            @NonNull String profileParameters
    ) throws Exception {
        return router.routeGeoJson(client, start, intermediates, destination, profile, blocked, profileParameters);
    }

    @NonNull
    @Override
    public synchronized GeoJsonRoute routeGeoJson(
            @NonNull LatLon start,
            @NonNull List<LatLon> intermediates,
            @NonNull LatLon destination,
            @NonNull String profile,
            boolean customProfile,
            @NonNull List<NogoPoint> blocked,
            @NonNull String profileParameters
    ) throws Exception {
        return router.routeGeoJson(
                client,
                start,
                intermediates,
                destination,
                profile,
                customProfile,
                blocked,
                profileParameters
        );
    }

    @NonNull
    @Override
    public synchronized GeoJsonRoute roundTripGeoJson(
            @NonNull LatLon start,
            @NonNull String profile,
            @NonNull List<NogoPoint> blocked,
            int roundTripDistanceMeters,
            int roundTripDirectionDegrees,
            @NonNull String profileParameters
    ) throws Exception {
        return router.roundTripGeoJson(
                client,
                start,
                profile,
                blocked,
                roundTripDistanceMeters,
                roundTripDirectionDegrees,
                profileParameters
        );
    }

    @NonNull
    @Override
    public synchronized GeoJsonRoute roundTripGeoJson(
            @NonNull LatLon start,
            @NonNull String profile,
            boolean customProfile,
            @NonNull List<NogoPoint> blocked,
            int roundTripDistanceMeters,
            int roundTripDirectionDegrees,
            @NonNull String profileParameters
    ) throws Exception {
        return router.roundTripGeoJson(
                client,
                start,
                profile,
                customProfile,
                blocked,
                roundTripDistanceMeters,
                roundTripDirectionDegrees,
                profileParameters
        );
    }

    @Override
    public synchronized void close() {
        try {
            client.close();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to close BRouter route client", e);
        }
    }
}

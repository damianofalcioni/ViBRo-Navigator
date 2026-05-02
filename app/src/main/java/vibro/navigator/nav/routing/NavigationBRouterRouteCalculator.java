package vibro.navigator.nav.routing;

import android.content.Context;

import androidx.annotation.NonNull;

import vibro.navigator.brouter.BRouterClient;
import vibro.navigator.brouter.BRouterRouter;
import vibro.navigator.brouter.NogoPoint;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;

import java.util.List;

public final class NavigationBRouterRouteCalculator implements NavigationRouteExecutor.RouteCalculator, AutoCloseable {

    private final BRouterRouter router = new BRouterRouter();
    private BRouterClient client;

    @NonNull
    @Override
    public synchronized GeoJsonRoute routeGeoJson(
            @NonNull Context context,
            @NonNull LatLon start,
            @NonNull List<LatLon> intermediates,
            @NonNull LatLon destination,
            @NonNull String profile,
            @NonNull List<NogoPoint> blocked
    ) throws Exception {
        if (client == null) {
            client = new BRouterClient(context.getApplicationContext());
        }
        return router.routeGeoJson(client, start, intermediates, destination, profile, blocked);
    }

    @Override
    public synchronized void close() {
        if (client != null) {
            client.close();
            client = null;
        }
    }
}

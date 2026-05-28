package vibro.navigator.nav.session;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.export.NavigationRouteGpxExporter;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.route.GeoJsonRoute;

final class NavigationSessionRouteExporter {
    private NavigationSessionRouteExporter() {
    }

    @Nullable
    static String export(
            @NonNull Context context,
            @NonNull NavigationSessionRouteState routeState,
            @NonNull NavigationRequest request
    ) {
        GeoJsonRoute route = routeState.currentRoute();
        if (route == null || route.track.isEmpty()) {
            return null;
        }
        return NavigationRouteGpxExporter.export(
                context,
                route,
                routeState.remainingIntermediateStops(request.stops)
        );
    }
}

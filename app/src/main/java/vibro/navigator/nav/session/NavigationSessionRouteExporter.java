package vibro.navigator.nav.session;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.format.AndroidNavigationTextResources;
import vibro.navigator.nav.export.NavigationRouteGpxExporter;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.route.GeoJsonRoute;

final class NavigationSessionRouteExporter {
    private NavigationSessionRouteExporter() {
    }

    @Nullable
    static String export(
            @NonNull Context context,
            @NonNull NavigationSessionRouteState routeState,
            @NonNull StraightLineNavigationState straightLineState,
            @Nullable NavigationLocation currentLocation,
            @NonNull NavigationRequest request
    ) {
        if (request.isStraightLine()) {
            return exportStraightLine(context, straightLineState, currentLocation, request);
        }
        return exportRoute(context, routeState, request);
    }

    @Nullable
    private static String exportRoute(
            @NonNull Context context,
            @NonNull NavigationSessionRouteState routeState,
            @NonNull NavigationRequest request
    ) {
        GeoJsonRoute route = routeState.currentRoute();
        if (route == null || route.track.isEmpty()) {
            return null;
        }
        return NavigationRouteGpxExporter.export(
                new AndroidNavigationTextResources(context),
                route,
                routeState.remainingIntermediateStops(request.stops)
        );
    }

    @Nullable
    private static String exportStraightLine(
            @NonNull Context context,
            @NonNull StraightLineNavigationState straightLineState,
            @Nullable NavigationLocation currentLocation,
            @NonNull NavigationRequest request
    ) {
        if (request.destination == null) {
            return null;
        }
        GeoJsonRoute route = straightLineState.buildExportRoute(request, currentLocation);
        if (route == null || route.track.isEmpty()) {
            return null;
        }
        return NavigationRouteGpxExporter.exportStraightLine(
                new AndroidNavigationTextResources(context),
                route,
                request.stops,
                request.destination
        );
    }
}

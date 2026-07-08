package vibro.navigator.nav.session;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.format.AndroidNavigationTextResources;
import vibro.navigator.nav.export.NavigationRouteGpxExporter;
import vibro.navigator.nav.export.NavigationRouteGpxExportHistory;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.route.GeoJsonRoute;

import java.util.Collections;
import java.util.List;

final class NavigationSessionRouteExporter {
    private NavigationSessionRouteExporter() {
    }

    @Nullable
    static String export(
            @NonNull Context context,
            @NonNull NavigationSessionRouteState routeState,
            @NonNull StraightLineNavigationState straightLineState,
            @Nullable NavigationLocation currentLocation,
            @NonNull List<NavigationLocation> acceptedFixes,
            @NonNull NavigationRequest request
    ) {
        if (request.isStraightLine()) {
            return exportStraightLine(context, straightLineState, currentLocation, acceptedFixes, request);
        }
        return exportRoute(context, routeState, acceptedFixes, request);
    }

    @Nullable
    private static String exportRoute(
            @NonNull Context context,
            @NonNull NavigationSessionRouteState routeState,
            @NonNull List<NavigationLocation> acceptedFixes,
            @NonNull NavigationRequest request
    ) {
        GeoJsonRoute route = routeState.currentRoute();
        if (route == null || route.track.isEmpty()) {
            return null;
        }
        return NavigationRouteGpxExporter.export(
                new AndroidNavigationTextResources(context),
                route,
                routeState.remainingIntermediateStops(request.stops),
                new NavigationRouteGpxExportHistory(
                        routeState.passedRoutesForExport(),
                        routeState.recalculationBridgeSegmentsForExport(),
                        acceptedFixes
                )
        );
    }

    @Nullable
    private static String exportStraightLine(
            @NonNull Context context,
            @NonNull StraightLineNavigationState straightLineState,
            @Nullable NavigationLocation currentLocation,
            @NonNull List<NavigationLocation> acceptedFixes,
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
                request.destination,
                new NavigationRouteGpxExportHistory(Collections.emptyList(), acceptedFixes)
        );
    }
}

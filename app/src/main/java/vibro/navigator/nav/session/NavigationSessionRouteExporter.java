package vibro.navigator.nav.session;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.format.AndroidNavigationTextResources;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.export.NavigationRouteGpxExporter;
import vibro.navigator.nav.export.NavigationRouteGpxExportHistory;
import vibro.navigator.nav.export.NavigationRouteGpxSource;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.route.GeoJsonRoute;

import java.util.Collections;
import java.util.List;

public final class NavigationSessionRouteExporter {
    private NavigationSessionRouteExporter() {
    }

    /** Call on the session's main thread; build the returned source on the export worker. */
    @Nullable
    public static NavigationRouteGpxSource capture(@NonNull NavigationSession session,
            @NonNull NavigationTextResources textResources) {
        return capture(textResources, session.components.routeState, session.components.straightLineState,
                session.components.locationState.getLastFilteredLocation(),
                session.components.acceptedFixHistory.snapshotForExport(), session.currentRequest);
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
        return export(
                new AndroidNavigationTextResources(context),
                routeState,
                straightLineState,
                currentLocation,
                acceptedFixes,
                request
        );
    }

    @Nullable
    static String export(
            @NonNull NavigationTextResources textResources,
            @NonNull NavigationSessionRouteState routeState,
            @NonNull StraightLineNavigationState straightLineState,
            @Nullable NavigationLocation currentLocation,
            @NonNull List<NavigationLocation> acceptedFixes,
            @NonNull NavigationRequest request
    ) {
        NavigationRouteGpxSource source = capture(textResources, routeState, straightLineState,
                currentLocation, acceptedFixes, request);
        return source == null ? null : source.build();
    }

    @Nullable
    static NavigationRouteGpxSource capture(NavigationTextResources textResources,
            NavigationSessionRouteState routeState, StraightLineNavigationState straightLineState,
            NavigationLocation currentLocation, List<NavigationLocation> acceptedFixes,
            NavigationRequest request) {
        if (request.isStraightLine()) {
            GeoJsonRoute route = straightLineState.buildExportRoute(request, currentLocation);
            if (request.destination == null || route == null || route.track.isEmpty()) {
                return null;
            }
            return () -> NavigationRouteGpxExporter.exportStraightLine(textResources, route,
                    request.stops, request.destination,
                    new NavigationRouteGpxExportHistory(Collections.emptyList(), acceptedFixes));
        }
        NavigationRouteHistoryExportSnapshot history = routeState.historyForExport().snapshotForExport();
        return () -> exportRoute(textResources, history, acceptedFixes, request);
    }

    @Nullable
    private static String exportRoute(
            @NonNull NavigationTextResources textResources,
            @NonNull NavigationRouteHistoryExportSnapshot history,
            @NonNull List<NavigationLocation> acceptedFixes,
            @NonNull NavigationRequest request
    ) {
        GeoJsonRoute route = history.remainingRoute();
        if (route == null || route.track.isEmpty()) {
            return null;
        }
        return NavigationRouteGpxExporter.export(
                textResources,
                route,
                request.stops,
                history.history(acceptedFixes)
        );
    }
}

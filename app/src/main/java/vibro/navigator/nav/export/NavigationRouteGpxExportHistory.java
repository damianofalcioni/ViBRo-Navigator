package vibro.navigator.nav.export;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.route.GeoJsonRoute;

public final class NavigationRouteGpxExportHistory {
    @NonNull
    public final List<PassedRoute> passedRoutes;
    @NonNull
    public final List<List<LatLon>> recalculationBridgeSegments;
    @NonNull
    public final List<NavigationLocation> acceptedFixes;

    public NavigationRouteGpxExportHistory(
            @NonNull List<PassedRoute> passedRoutes,
            @NonNull List<NavigationLocation> acceptedFixes
    ) {
        this(passedRoutes, Collections.emptyList(), acceptedFixes);
    }

    public NavigationRouteGpxExportHistory(
            @NonNull List<PassedRoute> passedRoutes,
            @NonNull List<List<LatLon>> recalculationBridgeSegments,
            @NonNull List<NavigationLocation> acceptedFixes
    ) {
        this.passedRoutes = immutablePassedRoutes(passedRoutes);
        this.recalculationBridgeSegments = immutableNestedPoints(recalculationBridgeSegments);
        this.acceptedFixes = immutableLocationCopies(acceptedFixes);
    }

    @NonNull
    public static NavigationRouteGpxExportHistory empty() {
        return new NavigationRouteGpxExportHistory(Collections.emptyList(), Collections.emptyList());
    }

    public static final class PassedRoute {
        @NonNull
        public final GeoJsonRoute route;
        @NonNull
        public final List<LatLon> segment;
        public final int maxPassedTrackIndex;
        public final boolean includeInstructionWaypoints;

        public PassedRoute(
                @NonNull GeoJsonRoute route,
                @NonNull List<LatLon> segment,
                int maxPassedTrackIndex
        ) {
            this(route, segment, maxPassedTrackIndex, true);
        }

        public PassedRoute(
                @NonNull GeoJsonRoute route,
                @NonNull List<LatLon> segment,
                int maxPassedTrackIndex,
                boolean includeInstructionWaypoints
        ) {
            this.route = route;
            this.segment = immutablePoints(segment);
            this.maxPassedTrackIndex = maxPassedTrackIndex;
            this.includeInstructionWaypoints = includeInstructionWaypoints;
        }
    }

    @NonNull
    private static List<PassedRoute> immutablePassedRoutes(@NonNull List<PassedRoute> values) {
        return values.isEmpty()
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(values));
    }

    @NonNull
    private static List<NavigationLocation> immutableLocationCopies(@NonNull List<NavigationLocation> values) {
        if (values.isEmpty()) {
            return Collections.emptyList();
        }
        List<NavigationLocation> copy = new ArrayList<>(values.size());
        for (NavigationLocation value : values) {
            copy.add(new NavigationLocation(value));
        }
        return Collections.unmodifiableList(copy);
    }

    @NonNull
    private static List<LatLon> immutablePoints(@NonNull List<LatLon> values) {
        if (values.isEmpty()) {
            return Collections.emptyList();
        }
        List<LatLon> copy = new ArrayList<>(values.size());
        for (LatLon value : values) {
            copy.add(new LatLon(value.lat, value.lon));
        }
        return Collections.unmodifiableList(copy);
    }

    @NonNull
    private static List<List<LatLon>> immutableNestedPoints(@NonNull List<List<LatLon>> values) {
        if (values.isEmpty()) {
            return Collections.emptyList();
        }
        List<List<LatLon>> copy = new ArrayList<>(values.size());
        for (List<LatLon> value : values) {
            List<LatLon> segment = immutablePoints(value);
            if (!segment.isEmpty()) {
                copy.add(segment);
            }
        }
        return copy.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(copy);
    }
}

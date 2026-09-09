package vibro.navigator.nav.session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.export.NavigationRouteGpxExportHistory.PassedRoute;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;

/** Retains existing route instructions at a reached departure boundary; never invents maneuvers. */
final class NavigationTravelInstructions {
    private static final double MAX_BOUNDARY_UNCERTAINTY_METERS = 25;
    private final List<PassedRoute> reached = new ArrayList<>();
    private final Set<Integer> activeIndices = new HashSet<>();
    private GeoJsonRoute route;
    private PolylineIndex index;

    void reset() {
        reached.clear();
        activeIndices.clear();
        route = null;
        index = null;
    }

    void onRouteApplied(GeoJsonRoute value, PolylineIndex polylineIndex) {
        route = value;
        index = polylineIndex;
        activeIndices.clear();
    }

    void rememberBoundary(NavigationLocation location, PolylineIndex.Match progress, double entryMeters) {
        if (route == null || progress == null || !location.hasAccuracy()) {
            return;
        }
        double radius = Math.min(MAX_BOUNDARY_UNCERTAINTY_METERS, Math.max(0, location.getAccuracy()) + 8);
        for (VoiceHint hint : route.voiceHints) {
            double along = index.distanceAtPointIndex(hint.indexInTrack);
            if (along >= entryMeters && Math.abs(along - progress.alongTrackMeters) <= radius) {
                rememberIfReached(hint, location, radius);
            }
        }
    }

    List<PassedRoute> snapshot() {
        return new ArrayList<>(reached);
    }

    private void rememberIfReached(VoiceHint hint, NavigationLocation location, double radius) {
        LatLon point = route.track.get(hint.indexInTrack);
        if (GeoMath.distanceMeters(point.lat, point.lon, location.getLatitude(), location.getLongitude()) > radius
                || !activeIndices.add(hint.indexInTrack)) {
            return;
        }
        VoiceHint preserved = new VoiceHint(0, hint.command, hint.exitNumber,
                hint.distanceToNextMeters, hint.angleDegrees);
        GeoJsonRoute instruction = new GeoJsonRoute(Collections.singletonList(point),
                Collections.singletonList(preserved), Double.NaN, 0);
        reached.add(new PassedRoute(instruction, Collections.emptyList(), 0));
    }
}

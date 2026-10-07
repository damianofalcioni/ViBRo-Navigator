package vibro.navigator.nav.orientation;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;

/** Smooths small lateral route offsets for display without changing guidance geometry. */
public final class NavigationRouteDisplayHeadingResolver {
    // A street-scale window suppresses short roadside connectors without spanning long bends.
    private static final double BEHIND_METERS = 15.0;
    private static final double AHEAD_METERS = 35.0;
    private static final double MIN_WINDOW_METERS = 20.0;
    private static final double EDGE_BASELINE_METERS = 3.0;
    private static final double MAX_EDGE_ANGLE_DEGREES = 20.0;
    private static final double MAX_LATERAL_OFFSET_METERS = 6.0;

    private final GeoJsonRoute route;
    private final PolylineIndex index;
    private final double[] maneuverDistances;
    private double lastAlongMeters = Double.NaN;
    private int lastSegmentIndex = -1;
    private double cachedHeading;

    public NavigationRouteDisplayHeadingResolver(GeoJsonRoute route, PolylineIndex index) {
        this.route = route;
        this.index = index;
        maneuverDistances = buildManeuverDistances();
    }

    private double[] buildManeuverDistances() {
        SortedSet<Double> distances = new TreeSet<>();
        for (VoiceHint hint : route.voiceHints) {
            if (hint.command != 1) {
                distances.add(index.distanceAtPointIndex(hint.indexInTrack));
            }
        }
        double[] result = new double[distances.size()];
        int position = 0;
        for (double distance : distances) {
            result[position++] = distance;
        }
        return result;
    }

    public double resolve(PolylineIndex.Match match) {
        if (match.alongTrackMeters != lastAlongMeters || match.segmentIndex != lastSegmentIndex) {
            cachedHeading = resolveUncached(match);
            lastAlongMeters = match.alongTrackMeters;
            lastSegmentIndex = match.segmentIndex;
        }
        return cachedHeading;
    }

    private double resolveUncached(PolylineIndex.Match match) {
        double fallback = NavigationExpectedBearingResolver.resolve(index, match);
        if (isNearManeuver(match.alongTrackMeters)) {
            return fallback;
        }
        double[] window = new double[] {Math.max(0.0, match.alongTrackMeters - BEHIND_METERS),
                Math.min(index.totalLengthMeters(), match.alongTrackMeters + AHEAD_METERS)};
        if (window[1] - window[0] < MIN_WINDOW_METERS) {
            return fallback;
        }
        LatLon start = index.pointAtDistance(window[0]);
        LatLon end = index.pointAtDistance(window[1]);
        double heading = bearing(start, end);
        if (!hasParallelEdges(window, start, end, heading)
                || !staysWithinCorridor(match, window, start, heading)) {
            return fallback;
        }
        return heading;
    }

    private boolean isNearManeuver(double alongMeters) {
        int boundary = Arrays.binarySearch(maneuverDistances, alongMeters);
        if (boundary >= 0) {
            return true;
        }
        int next = -boundary - 1;
        return (next > 0 && alongMeters - maneuverDistances[next - 1] <= BEHIND_METERS)
                || (next < maneuverDistances.length && maneuverDistances[next] - alongMeters <= AHEAD_METERS);
    }

    private boolean hasParallelEdges(double[] window, LatLon start, LatLon end, double heading) {
        LatLon afterStart = index.pointAtDistance(window[0] + EDGE_BASELINE_METERS);
        LatLon beforeEnd = index.pointAtDistance(window[1] - EDGE_BASELINE_METERS);
        return GeoMath.angularDiffDegrees(bearing(start, afterStart), heading) <= MAX_EDGE_ANGLE_DEGREES
                && GeoMath.angularDiffDegrees(bearing(beforeEnd, end), heading) <= MAX_EDGE_ANGLE_DEGREES;
    }

    private boolean staysWithinCorridor(PolylineIndex.Match match, double[] window, LatLon start, double heading) {
        int first = match.segmentIndex;
        while (first > 0 && index.distanceAtPointIndex(first) > window[0]) {
            first--;
        }
        for (int point = first + 1; point < route.track.size() && index.distanceAtPointIndex(point) < window[1]; point++) {
            if (lateralOffset(start, route.track.get(point), heading) > MAX_LATERAL_OFFSET_METERS) {
                return false;
            }
        }
        return true;
    }

    private static double lateralOffset(LatLon start, LatLon point, double heading) {
        double distance = GeoMath.distanceMeters(start.lat, start.lon, point.lat, point.lon);
        return distance * Math.abs(Math.sin(Math.toRadians(bearing(start, point) - heading)));
    }

    private static double bearing(LatLon start, LatLon end) {
        return GeoMath.bearingDegrees(start.lat, start.lon, end.lat, end.lon);
    }
}

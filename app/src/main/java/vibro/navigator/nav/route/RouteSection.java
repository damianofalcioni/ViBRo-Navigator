package vibro.navigator.nav.route;

import java.util.ArrayList;
import java.util.List;

import vibro.navigator.geo.LatLon;

/** A geometry slice for history/export; navigation continues to use the original indices. */
public final class RouteSection {
    private RouteSection() {
    }

    public static GeoJsonRoute between(GeoJsonRoute route, double from, double to) {
        PolylineIndex index = new PolylineIndex(route.track);
        double start = Math.max(0, Math.min(from, index.totalLengthMeters()));
        double end = Math.max(start, Math.min(to, index.totalLengthMeters()));
        List<LatLon> points = new ArrayList<>();
        List<VoiceHint> hints = new ArrayList<>();
        List<Double> times = new ArrayList<>();
        double startTime = RouteSectionTimes.at(route, index, start);
        if (route.track.isEmpty()) {
            return new GeoJsonRoute(points, hints, 0, 0);
        }
        appendSample(points, times, index.pointAtDistance(start), Double.isFinite(startTime) ? 0 : Double.NaN);
        for (int i = 0; i < route.track.size(); i++) {
            double distance = index.distanceAtPointIndex(i);
            if (distance < start || distance > end) {
                continue;
            }
            double time = Double.isFinite(startTime) ? route.timesSeconds.get(i) - startTime : Double.NaN;
            appendSample(points, times, route.track.get(i), time);
            appendHints(hints, route.voiceHints, i, points.size() - 1);
        }
        double duration = RouteSectionTimes.at(route, index, end) - startTime;
        appendSample(points, times, index.pointAtDistance(end), duration);
        return new GeoJsonRoute(points, hints, times, duration, end - start);
    }

    private static void appendHints(List<VoiceHint> target, List<VoiceHint> source, int originalIndex, int newIndex) {
        for (VoiceHint hint : source) {
            if (hint.indexInTrack == originalIndex) {
                target.add(new VoiceHint(newIndex, hint.command, hint.exitNumber,
                        hint.distanceToNextMeters, hint.angleDegrees));
            }
        }
    }

    private static void appendSample(List<LatLon> points, List<Double> times, LatLon point, double time) {
        int count = points.size();
        appendDistinct(points, point);
        if (points.size() > count && Double.isFinite(time)) {
            times.add(time);
        }
    }

    public static void appendDistinct(List<LatLon> points, LatLon point) {
        if (point == null) {
            return;
        }
        if (!points.isEmpty()) {
            LatLon last = points.get(points.size() - 1);
            if (Math.abs(last.lat - point.lat) < 0.0000001
                    && Math.abs(last.lon - point.lon) < 0.0000001) {
                return;
            }
        }
        points.add(new LatLon(point.lat, point.lon));
    }
}

package vibro.navigator.nav.compass;

import java.util.ArrayList;
import java.util.List;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassRouteGeometry.SamplePoint;

/** Clips display samples while retaining original navigation distances and indices. */
final class CompassRouteHistoryClipper {
    private CompassRouteHistoryClipper() {
    }

    static List<SamplePoint> pointsFrom(List<SamplePoint> source, double entryMeters) {
        List<SamplePoint> result = new ArrayList<>();
        for (int i = 0; i < source.size(); i++) {
            SamplePoint point = source.get(i);
            if (point.alongTrackMeters >= entryMeters) {
                appendEntryPoint(result, source, i, entryMeters);
                result.add(point);
            }
        }
        return result;
    }

    private static void appendEntryPoint(List<SamplePoint> result, List<SamplePoint> source,
            int index, double entryMeters) {
        if (!result.isEmpty() || index == 0) {
            return;
        }
        SamplePoint before = source.get(index - 1);
        SamplePoint after = source.get(index);
        double span = after.alongTrackMeters - before.alongTrackMeters;
        if (span <= 0 || after.alongTrackMeters == entryMeters) {
            return;
        }
        double fraction = (entryMeters - before.alongTrackMeters) / span;
        result.add(new SamplePoint(new LatLon(
                before.point.lat + fraction * (after.point.lat - before.point.lat),
                before.point.lon + fraction * (after.point.lon - before.point.lon)),
                entryMeters, before.trackIndex));
    }
}

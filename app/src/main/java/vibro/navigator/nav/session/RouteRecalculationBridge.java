package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;

final class RouteRecalculationBridge {
    private static final double DUPLICATE_POINT_TOLERANCE_DEGREES = 0.0000001;

    private RouteRecalculationBridge() {
    }

    @NonNull
    static List<LatLon> segment(@Nullable LatLon from, @Nullable LatLon to) {
        if (from == null || to == null || samePoint(from, to)) {
            return Collections.emptyList();
        }
        List<LatLon> segment = new ArrayList<>(2);
        segment.add(copy(from));
        segment.add(copy(to));
        return segment;
    }

    @NonNull
    static List<List<LatLon>> copiedSegments(@NonNull List<List<LatLon>> segments) {
        if (segments.isEmpty()) {
            return Collections.emptyList();
        }
        List<List<LatLon>> copy = new ArrayList<>(segments.size());
        for (List<LatLon> segment : segments) {
            copy.add(copiedPoints(segment));
        }
        return Collections.unmodifiableList(copy);
    }

    @NonNull
    static List<LatLon> copiedPoints(@NonNull List<LatLon> points) {
        if (points.isEmpty()) {
            return Collections.emptyList();
        }
        List<LatLon> copy = new ArrayList<>(points.size());
        for (LatLon point : points) {
            copy.add(copy(point));
        }
        return copy;
    }

    @Nullable
    static LatLon firstRoutePoint(@NonNull GeoJsonRoute route) {
        return route.track.isEmpty() ? null : copy(route.track.get(0));
    }

    @NonNull
    static LatLon lastPoint(@NonNull List<LatLon> points) {
        return copy(points.get(points.size() - 1));
    }

    private static boolean samePoint(@NonNull LatLon first, @NonNull LatLon second) {
        return Math.abs(first.lat - second.lat) <= DUPLICATE_POINT_TOLERANCE_DEGREES
                && Math.abs(first.lon - second.lon) <= DUPLICATE_POINT_TOLERANCE_DEGREES;
    }

    @NonNull
    private static LatLon copy(@NonNull LatLon point) {
        return new LatLon(point.lat, point.lon);
    }
}

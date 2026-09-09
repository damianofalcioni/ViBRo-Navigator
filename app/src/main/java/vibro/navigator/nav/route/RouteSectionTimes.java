package vibro.navigator.nav.route;

/** Interpolates slice endpoints without discarding BRouter's per-point timing model. */
final class RouteSectionTimes {
    private RouteSectionTimes() {
    }

    static double at(GeoJsonRoute route, PolylineIndex index, double distance) {
        if (route.track.isEmpty() || route.timesSeconds.size() != route.track.size()) {
            return Double.NaN;
        }
        for (int i = 1; i < route.track.size(); i++) {
            double end = index.distanceAtPointIndex(i);
            if (end >= distance) {
                double start = index.distanceAtPointIndex(i - 1);
                double fraction = end > start ? Math.max(0, (distance - start) / (end - start)) : 0;
                double before = route.timesSeconds.get(i - 1);
                return before + fraction * (route.timesSeconds.get(i) - before);
            }
        }
        return route.timesSeconds.get(route.timesSeconds.size() - 1);
    }
}

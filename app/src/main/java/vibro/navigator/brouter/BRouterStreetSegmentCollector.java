package vibro.navigator.brouter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassStreetSegment;
import vibro.navigator.nav.compass.CompassStreetType;

/** Keeps a bounded street sample spread across the requested area, independent of rd5 cell order. */
final class BRouterStreetSegmentCollector implements BRouterStreetGeometrySink {
    private static final int GRID_SIZE = 4;
    private static final Comparator<Candidate> FARTHEST_FIRST =
            (left, right) -> Double.compare(right.distanceSquared, left.distanceSquared);

    private final BRouterSegmentBounds bounds;
    private final int limit;
    private final double centerLat;
    private final double centerLon;
    private final double lonScale;
    private final List<PriorityQueue<Candidate>> buckets = new ArrayList<>();
    private int size;

    BRouterStreetSegmentCollector(BRouterSegmentBounds bounds, int limit) {
        this.bounds = bounds;
        this.limit = Math.max(0, limit);
        centerLat = (bounds.minLat + bounds.maxLat) * 0.5d;
        centerLon = (bounds.minLon + bounds.maxLon) * 0.5d;
        lonScale = Math.cos(Math.toRadians(centerLat));
        for (int i = 0; i < GRID_SIZE * GRID_SIZE; i++) {
            buckets.add(new PriorityQueue<>(11, FARTHEST_FIRST));
        }
    }

    @Override
    public void offer(List<LatLon> points, CompassStreetType type) {
        if (limit == 0 || points.size() < 2 || !bounds.intersects(points)) {
            return;
        }
        LatLon anchor = nearestPoint(points);
        double x = (anchor.lon - bounds.minLon) / (bounds.maxLon - bounds.minLon);
        double y = (anchor.lat - bounds.minLat) / (bounds.maxLat - bounds.minLat);
        double distanceSquared = (x - 0.5d) * (x - 0.5d) + (y - 0.5d) * (y - 0.5d);
        buckets.get(gridIndex(y) * GRID_SIZE + gridIndex(x)).add(
                new Candidate(new CompassStreetSegment(points, type), distanceSquared)
        );
        size++;
        if (size > limit) {
            mostPopulatedBucket().poll();
            size--;
        }
    }

    void appendTo(List<CompassStreetSegment> out) {
        List<Candidate> selected = new ArrayList<>(size);
        for (PriorityQueue<Candidate> bucket : buckets) {
            selected.addAll(bucket);
        }
        Collections.sort(selected, Collections.reverseOrder(FARTHEST_FIRST));
        for (Candidate candidate : selected) {
            out.add(candidate.segment);
        }
    }

    private PriorityQueue<Candidate> mostPopulatedBucket() {
        PriorityQueue<Candidate> largest = buckets.get(0);
        for (PriorityQueue<Candidate> bucket : buckets) {
            if (shouldTrimBefore(bucket, largest)) {
                largest = bucket;
            }
        }
        return largest;
    }

    private static boolean shouldTrimBefore(
            PriorityQueue<Candidate> bucket,
            PriorityQueue<Candidate> other
    ) {
        if (bucket.size() != other.size()) {
            return bucket.size() > other.size();
        }
        return !bucket.isEmpty() && FARTHEST_FIRST.compare(bucket.peek(), other.peek()) < 0;
    }

    private LatLon nearestPoint(List<LatLon> points) {
        LatLon nearest = points.get(0);
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (LatLon point : points) {
            double x = (point.lon - centerLon) * lonScale;
            double y = point.lat - centerLat;
            double distance = x * x + y * y;
            if (distance < nearestDistance) {
                nearest = point;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static int gridIndex(double fraction) {
        return Math.max(0, Math.min(GRID_SIZE - 1, (int) (fraction * GRID_SIZE)));
    }

    private static final class Candidate {
        final CompassStreetSegment segment;
        final double distanceSquared;

        Candidate(CompassStreetSegment segment, double distanceSquared) {
            this.segment = segment;
            this.distanceSquared = distanceSquared;
        }
    }
}

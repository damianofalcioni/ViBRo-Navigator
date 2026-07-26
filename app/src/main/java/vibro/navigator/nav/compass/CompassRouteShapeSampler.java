package vibro.navigator.nav.compass;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.PolylineIndex;

import java.util.ArrayList;
import java.util.List;

final class CompassRouteShapeSampler {
    private static final double REQUIRED_BEND_ERROR_METERS = 2.0;

    private CompassRouteShapeSampler() {
    }

    @NonNull
    static List<CompassRouteGeometry.SamplePoint> sample(
            @NonNull List<LatLon> track,
            @NonNull PolylineIndex index,
            int maximumPointCount
    ) {
        if (track.isEmpty()) {
            return new ArrayList<>();
        }
        int pointLimit = Math.max(2, maximumPointCount);
        if (track.size() <= pointLimit) {
            return copyTrackPoints(track, index);
        }
        boolean[] selected = selectShapePoints(track, pointLimit);
        fillLargestDistanceGaps(selected, index, pointLimit);
        return copySelectedPoints(track, index, selected);
    }

    @NonNull
    private static boolean[] selectShapePoints(@NonNull List<LatLon> track, int pointLimit) {
        boolean[] selected = new boolean[track.size()];
        selected[0] = true;
        selected[track.size() - 1] = true;
        int selectedCount = 2;
        List<ShapeCandidate> candidates = new ArrayList<>();
        addCandidate(candidates, track, 0, track.size() - 1);
        ShapeCandidate candidate = removeLargestRequiredBend(candidates);
        while (selectedCount < pointLimit && candidate != null) {
            selected[candidate.splitIndex] = true;
            selectedCount++;
            addCandidate(candidates, track, candidate.startIndex, candidate.splitIndex);
            addCandidate(candidates, track, candidate.splitIndex, candidate.endIndex);
            candidate = removeLargestRequiredBend(candidates);
        }
        return selected;
    }

    @Nullable
    private static ShapeCandidate removeLargestRequiredBend(@NonNull List<ShapeCandidate> candidates) {
        int largestIndex = -1;
        double largestErrorMeters = -1.0;
        for (int i = 0; i < candidates.size(); i++) {
            if (candidates.get(i).errorMeters > largestErrorMeters) {
                largestIndex = i;
                largestErrorMeters = candidates.get(i).errorMeters;
            }
        }
        return largestIndex >= 0 && largestErrorMeters >= REQUIRED_BEND_ERROR_METERS
                ? candidates.remove(largestIndex)
                : null;
    }

    private static void addCandidate(
            @NonNull List<ShapeCandidate> candidates,
            @NonNull List<LatLon> track,
            int startIndex,
            int endIndex
    ) {
        ShapeCandidate candidate = findShapeCandidate(track, startIndex, endIndex);
        if (candidate != null) {
            candidates.add(candidate);
        }
    }

    @Nullable
    private static ShapeCandidate findShapeCandidate(
            @NonNull List<LatLon> track,
            int startIndex,
            int endIndex
    ) {
        if (endIndex - startIndex < 2) {
            return null;
        }
        LatLon start = track.get(startIndex);
        LatLon end = track.get(endIndex);
        double largestErrorMeters = -1.0;
        int splitIndex = -1;
        for (int i = startIndex + 1; i < endIndex; i++) {
            double errorMeters = distanceToSegmentMeters(track.get(i), start, end);
            if (errorMeters > largestErrorMeters) {
                largestErrorMeters = errorMeters;
                splitIndex = i;
            }
        }
        return splitIndex < 0
                ? null
                : new ShapeCandidate(startIndex, endIndex, splitIndex, largestErrorMeters);
    }

    private static double distanceToSegmentMeters(
            @NonNull LatLon point,
            @NonNull LatLon start,
            @NonNull LatLon end
    ) {
        double endEast = GeoMath.eastMeters(start.lat, start.lon, end.lat, end.lon);
        double endNorth = GeoMath.northMeters(start.lat, end.lat);
        double pointEast = GeoMath.eastMeters(start.lat, start.lon, point.lat, point.lon);
        double pointNorth = GeoMath.northMeters(start.lat, point.lat);
        double segmentLengthSquared = endEast * endEast + endNorth * endNorth;
        if (segmentLengthSquared <= 0.0) {
            return Math.hypot(pointEast, pointNorth);
        }
        double projection = (pointEast * endEast + pointNorth * endNorth) / segmentLengthSquared;
        double boundedProjection = Math.max(0.0, Math.min(1.0, projection));
        return Math.hypot(
                pointEast - boundedProjection * endEast,
                pointNorth - boundedProjection * endNorth
        );
    }

    private static void fillLargestDistanceGaps(
            @NonNull boolean[] selected,
            @NonNull PolylineIndex index,
            int pointLimit
    ) {
        int selectedCount = countSelected(selected);
        while (selectedCount < pointLimit) {
            IndexGap gap = findLargestDistanceGap(selected, index);
            if (gap == null) {
                return;
            }
            selected[gap.midpointIndex(index)] = true;
            selectedCount++;
        }
    }

    private static int countSelected(@NonNull boolean[] selected) {
        int count = 0;
        for (boolean value : selected) {
            if (value) {
                count++;
            }
        }
        return count;
    }

    @Nullable
    private static IndexGap findLargestDistanceGap(
            @NonNull boolean[] selected,
            @NonNull PolylineIndex index
    ) {
        IndexGap largest = null;
        int previousIndex = 0;
        for (int i = 1; i < selected.length; i++) {
            if (!selected[i]) {
                continue;
            }
            if (i - previousIndex > 1) {
                IndexGap candidate = new IndexGap(previousIndex, i, index);
                if (largest == null || candidate.lengthMeters > largest.lengthMeters) {
                    largest = candidate;
                }
            }
            previousIndex = i;
        }
        return largest;
    }

    @NonNull
    private static List<CompassRouteGeometry.SamplePoint> copyTrackPoints(
            @NonNull List<LatLon> track,
            @NonNull PolylineIndex index
    ) {
        boolean[] selected = new boolean[track.size()];
        for (int i = 0; i < selected.length; i++) {
            selected[i] = true;
        }
        return copySelectedPoints(track, index, selected);
    }

    @NonNull
    private static List<CompassRouteGeometry.SamplePoint> copySelectedPoints(
            @NonNull List<LatLon> track,
            @NonNull PolylineIndex index,
            @NonNull boolean[] selected
    ) {
        List<CompassRouteGeometry.SamplePoint> result = new ArrayList<>();
        for (int i = 0; i < selected.length; i++) {
            if (selected[i]) {
                result.add(new CompassRouteGeometry.SamplePoint(
                        track.get(i),
                        index.distanceAtPointIndex(i),
                        i
                ));
            }
        }
        return result;
    }

    private static final class ShapeCandidate {
        final int startIndex;
        final int endIndex;
        final int splitIndex;
        final double errorMeters;

        ShapeCandidate(int startIndex, int endIndex, int splitIndex, double errorMeters) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.splitIndex = splitIndex;
            this.errorMeters = errorMeters;
        }
    }

    private static final class IndexGap {
        final int startIndex;
        final int endIndex;
        final double lengthMeters;

        IndexGap(int startIndex, int endIndex, @NonNull PolylineIndex index) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.lengthMeters = index.distanceAtPointIndex(endIndex) - index.distanceAtPointIndex(startIndex);
        }

        int midpointIndex(@NonNull PolylineIndex index) {
            double targetMeters = index.distanceAtPointIndex(startIndex) + lengthMeters / 2.0;
            int closestIndex = startIndex + 1;
            double closestDistance = Double.MAX_VALUE;
            for (int i = startIndex + 1; i < endIndex; i++) {
                double distance = Math.abs(index.distanceAtPointIndex(i) - targetMeters);
                if (distance < closestDistance) {
                    closestIndex = i;
                    closestDistance = distance;
                }
            }
            return closestIndex;
        }
    }
}

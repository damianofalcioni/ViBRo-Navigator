package vibro.navigator.nav.compass;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class CompassRouteVisibleRanges {
    static final CompassRouteVisibleRanges EMPTY = new CompassRouteVisibleRanges(Collections.emptyList());

    @NonNull
    private final List<Range> ranges;

    private CompassRouteVisibleRanges(@NonNull List<Range> ranges) {
        this.ranges = ranges.isEmpty()
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(ranges));
    }

    @NonNull
    static CompassRouteVisibleRanges select(
            @NonNull CompassRouteGeometry geometry,
            double currentLatitude,
            double currentLongitude,
            float visibleRadiusMeters,
            float drawPaddingMeters
    ) {
        int pointCount = geometry.fullRoutePointCount();
        if (pointCount < 2) {
            return EMPTY;
        }
        double boundsMeters = Math.max(0.0, visibleRadiusMeters) + Math.max(0.0, drawPaddingMeters);
        CompassRouteSpatialIndex spatialIndex = geometry.fullRouteSpatialIndex();
        ViewportBounds viewport = ViewportBounds.around(currentLatitude, currentLongitude, boundsMeters);
        RangeAccumulator accumulator = new RangeAccumulator(
                geometry,
                currentLatitude,
                currentLongitude,
                boundsMeters
        );
        for (int blockIndex = 0; blockIndex < spatialIndex.blockCount(); blockIndex++) {
            int blockStartSegmentIndex = spatialIndex.startSegmentIndexAt(blockIndex);
            if (viewport.intersects(spatialIndex, blockIndex)) {
                accumulator.addBlock(
                        blockStartSegmentIndex,
                        spatialIndex.endSegmentIndexAt(blockIndex)
                );
            } else {
                accumulator.skipBlock(blockStartSegmentIndex);
            }
        }
        return accumulator.finish(pointCount);
    }

    int rangeCount() {
        return ranges.size();
    }

    int startIndexAt(int rangeIndex) {
        return ranges.get(rangeIndex).startIndex;
    }

    int endIndexAt(int rangeIndex) {
        return ranges.get(rangeIndex).endIndex;
    }

    private static boolean segmentIntersectsBounds(
            @Nullable LatLon first,
            @Nullable LatLon second,
            double currentLatitude,
            double currentLongitude,
            double boundsMeters
    ) {
        if (first == null || second == null) {
            return false;
        }
        double firstEast = GeoMath.eastMeters(
                currentLatitude,
                currentLongitude,
                first.lat,
                first.lon
        );
        double secondEast = GeoMath.eastMeters(
                currentLatitude,
                currentLongitude,
                second.lat,
                second.lon
        );
        double firstNorth = GeoMath.northMeters(currentLatitude, first.lat);
        double secondNorth = GeoMath.northMeters(currentLatitude, second.lat);
        return intervalIntersectsBounds(firstEast, secondEast, boundsMeters)
                && intervalIntersectsBounds(firstNorth, secondNorth, boundsMeters);
    }

    private static boolean intervalIntersectsBounds(double first, double second, double boundsMeters) {
        return Math.min(first, second) <= boundsMeters && Math.max(first, second) >= -boundsMeters;
    }

    private static final class Range {
        final int startIndex;
        final int endIndex;

        Range(int startIndex, int endIndex) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
    }

    private static final class ViewportBounds {
        final double minLatitude;
        final double maxLatitude;
        final double minLongitude;
        final double maxLongitude;

        private ViewportBounds(
                double minLatitude,
                double maxLatitude,
                double minLongitude,
                double maxLongitude
        ) {
            this.minLatitude = minLatitude;
            this.maxLatitude = maxLatitude;
            this.minLongitude = minLongitude;
            this.maxLongitude = maxLongitude;
        }

        @NonNull
        static ViewportBounds around(double latitude, double longitude, double radiusMeters) {
            double latitudeDelta = radiusMeters / 111_320.0;
            double longitudeScale = 111_320.0 * Math.max(0.01, Math.abs(Math.cos(Math.toRadians(latitude))));
            double longitudeDelta = radiusMeters / longitudeScale;
            return new ViewportBounds(
                    latitude - latitudeDelta,
                    latitude + latitudeDelta,
                    longitude - longitudeDelta,
                    longitude + longitudeDelta
            );
        }

        boolean intersects(@NonNull CompassRouteSpatialIndex spatialIndex, int blockIndex) {
            return spatialIndex.intersects(
                    blockIndex,
                    minLatitude,
                    maxLatitude,
                    minLongitude,
                    maxLongitude
            );
        }
    }

    private static final class RangeAccumulator {
        @NonNull
        private final CompassRouteGeometry geometry;
        private final double currentLatitude;
        private final double currentLongitude;
        private final double boundsMeters;
        @NonNull
        private final List<Range> ranges = new ArrayList<>();
        private int activeRangeStart = -1;

        RangeAccumulator(
                @NonNull CompassRouteGeometry geometry,
                double currentLatitude,
                double currentLongitude,
                double boundsMeters
        ) {
            this.geometry = geometry;
            this.currentLatitude = currentLatitude;
            this.currentLongitude = currentLongitude;
            this.boundsMeters = boundsMeters;
        }

        void addBlock(int startSegmentIndex, int endSegmentIndex) {
            for (int segmentIndex = startSegmentIndex;
                 segmentIndex < endSegmentIndex;
                 segmentIndex++) {
                acceptSegment(segmentIndex, segmentIsVisible(segmentIndex));
            }
        }

        void skipBlock(int startSegmentIndex) {
            closeActiveRange(startSegmentIndex + 1);
        }

        @NonNull
        CompassRouteVisibleRanges finish(int pointCount) {
            closeActiveRange(pointCount);
            return ranges.isEmpty() ? EMPTY : new CompassRouteVisibleRanges(ranges);
        }

        private boolean segmentIsVisible(int segmentIndex) {
            return segmentIntersectsBounds(
                    geometry.fullRoutePointAt(segmentIndex),
                    geometry.fullRoutePointAt(segmentIndex + 1),
                    currentLatitude,
                    currentLongitude,
                    boundsMeters
            );
        }

        private void acceptSegment(int segmentIndex, boolean visible) {
            if (visible) {
                if (activeRangeStart < 0) {
                    activeRangeStart = segmentIndex;
                }
                return;
            }
            closeActiveRange(segmentIndex + 1);
        }

        private void closeActiveRange(int endPointIndex) {
            if (activeRangeStart < 0) {
                return;
            }
            ranges.add(new Range(activeRangeStart, endPointIndex));
            activeRangeStart = -1;
        }
    }
}

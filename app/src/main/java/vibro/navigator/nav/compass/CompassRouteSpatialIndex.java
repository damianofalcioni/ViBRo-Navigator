package vibro.navigator.nav.compass;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class CompassRouteSpatialIndex {
    private static final int SEGMENTS_PER_BLOCK = 32;

    @NonNull
    private final List<Block> blocks;

    CompassRouteSpatialIndex(@NonNull List<CompassRouteGeometry.SamplePoint> points) {
        if (points.size() < 2) {
            blocks = Collections.emptyList();
            return;
        }
        List<Block> built = new ArrayList<>();
        int segmentCount = points.size() - 1;
        for (int startSegmentIndex = 0;
             startSegmentIndex < segmentCount;
             startSegmentIndex += SEGMENTS_PER_BLOCK) {
            int endSegmentIndex = Math.min(segmentCount, startSegmentIndex + SEGMENTS_PER_BLOCK);
            built.add(buildBlock(points, startSegmentIndex, endSegmentIndex));
        }
        blocks = Collections.unmodifiableList(built);
    }

    int blockCount() {
        return blocks.size();
    }

    int startSegmentIndexAt(int blockIndex) {
        return blocks.get(blockIndex).startSegmentIndex;
    }

    int endSegmentIndexAt(int blockIndex) {
        return blocks.get(blockIndex).endSegmentIndex;
    }

    boolean intersects(
            int blockIndex,
            double minLatitude,
            double maxLatitude,
            double minLongitude,
            double maxLongitude
    ) {
        Block block = blocks.get(blockIndex);
        return block.minLatitude <= maxLatitude
                && block.maxLatitude >= minLatitude
                && block.minLongitude <= maxLongitude
                && block.maxLongitude >= minLongitude;
    }

    @NonNull
    private static Block buildBlock(
            @NonNull List<CompassRouteGeometry.SamplePoint> points,
            int startSegmentIndex,
            int endSegmentIndex
    ) {
        double minLatitude = Double.POSITIVE_INFINITY;
        double maxLatitude = Double.NEGATIVE_INFINITY;
        double minLongitude = Double.POSITIVE_INFINITY;
        double maxLongitude = Double.NEGATIVE_INFINITY;
        for (int pointIndex = startSegmentIndex; pointIndex <= endSegmentIndex; pointIndex++) {
            CompassRouteGeometry.SamplePoint sample = points.get(pointIndex);
            minLatitude = Math.min(minLatitude, sample.point.lat);
            maxLatitude = Math.max(maxLatitude, sample.point.lat);
            minLongitude = Math.min(minLongitude, sample.point.lon);
            maxLongitude = Math.max(maxLongitude, sample.point.lon);
        }
        return new Block(
                startSegmentIndex,
                endSegmentIndex,
                minLatitude,
                maxLatitude,
                minLongitude,
                maxLongitude
        );
    }

    private static final class Block {
        final int startSegmentIndex;
        final int endSegmentIndex;
        final double minLatitude;
        final double maxLatitude;
        final double minLongitude;
        final double maxLongitude;

        Block(
                int startSegmentIndex,
                int endSegmentIndex,
                double minLatitude,
                double maxLatitude,
                double minLongitude,
                double maxLongitude
        ) {
            this.startSegmentIndex = startSegmentIndex;
            this.endSegmentIndex = endSegmentIndex;
            this.minLatitude = minLatitude;
            this.maxLatitude = maxLatitude;
            this.minLongitude = minLongitude;
            this.maxLongitude = maxLongitude;
        }
    }
}

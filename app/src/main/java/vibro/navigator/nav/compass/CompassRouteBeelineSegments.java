package vibro.navigator.nav.compass;

import androidx.annotation.NonNull;

import java.util.List;

public final class CompassRouteBeelineSegments {
    @NonNull
    private final List<CompassRouteGeometry.SamplePoint> sampledPoints;
    @NonNull
    private final List<CompassRouteGeometry.SamplePoint> fullRoutePoints;
    @NonNull
    private final boolean[] beelineTrackSegments;

    CompassRouteBeelineSegments(
            @NonNull List<CompassRouteGeometry.SamplePoint> sampledPoints,
            @NonNull List<CompassRouteGeometry.SamplePoint> fullRoutePoints,
            @NonNull boolean[] beelineTrackSegments
    ) {
        this.sampledPoints = sampledPoints;
        this.fullRoutePoints = fullRoutePoints;
        this.beelineTrackSegments = beelineTrackSegments.clone();
    }

    public boolean isSampledSegment(int startPointIndex) {
        return isBeelineSegment(sampledPoints, startPointIndex);
    }

    public boolean isFullRouteSegment(int startPointIndex) {
        return isBeelineSegment(fullRoutePoints, startPointIndex);
    }

    private boolean isBeelineSegment(
            @NonNull List<CompassRouteGeometry.SamplePoint> points,
            int startPointIndex
    ) {
        if (startPointIndex < 0 || startPointIndex + 1 >= points.size()) {
            return false;
        }
        int startTrackIndex = points.get(startPointIndex).trackIndex;
        int endTrackIndex = points.get(startPointIndex + 1).trackIndex;
        return startTrackIndex >= 0
                && endTrackIndex == startTrackIndex + 1
                && startTrackIndex < beelineTrackSegments.length
                && beelineTrackSegments[startTrackIndex];
    }
}

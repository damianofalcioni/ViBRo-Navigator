package vibro.navigator.nav.compass;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;

public final class CompassFullRouteView {
    @NonNull
    static final CompassFullRouteView EMPTY = new CompassFullRouteView(
            null,
            CompassRouteVisibleRanges.EMPTY,
            0,
            0
    );

    @Nullable
    private final CompassRouteGeometry geometry;
    @NonNull
    private final CompassRouteVisibleRanges visibleRanges;
    private final int passedPointCount;
    private final int remainingStartPointIndex;

    private CompassFullRouteView(
            @Nullable CompassRouteGeometry geometry,
            @NonNull CompassRouteVisibleRanges visibleRanges,
            int passedPointCount,
            int remainingStartPointIndex
    ) {
        this.geometry = geometry;
        this.visibleRanges = visibleRanges;
        this.passedPointCount = passedPointCount;
        this.remainingStartPointIndex = remainingStartPointIndex;
    }

    @NonNull
    static CompassFullRouteView from(@NonNull NavCompassRouteGeometryInput input) {
        if (!input.displayMetrics.movingScaleActive) {
            return EMPTY;
        }
        float drawPaddingMeters = Math.max(
                24f,
                Math.max(input.radiusMetrics.accuracyRadiusMeters, input.radiusMetrics.routeThresholdMeters)
        );
        CompassRouteVisibleRanges ranges = CompassRouteVisibleRanges.select(
                input.routeGeometry,
                input.currentLatitude,
                input.currentLongitude,
                input.radiusMetrics.visibleRadiusMeters,
                drawPaddingMeters
        );
        int passed = input.routeGeometry.passedFullRoutePointCount(input.alongTrackMeters);
        int remainingStart = input.routeGeometry.fullRoutePointCount() == 0
                ? 0
                : Math.max(0, passed - 1);
        return new CompassFullRouteView(input.routeGeometry, ranges, passed, remainingStart);
    }

    public boolean isActive() {
        return geometry != null;
    }

    public int pointCount() {
        return geometry == null ? 0 : geometry.fullRoutePointCount();
    }

    public int passedPointCount() {
        return passedPointCount;
    }

    public int remainingStartPointIndex() {
        return remainingStartPointIndex;
    }

    public int rangeCount() {
        return visibleRanges.rangeCount();
    }

    public int rangeStartIndexAt(int rangeIndex) {
        return visibleRanges.startIndexAt(rangeIndex);
    }

    public int rangeEndIndexAt(int rangeIndex) {
        return visibleRanges.endIndexAt(rangeIndex);
    }

    @Nullable
    public LatLon pointAt(int index) {
        return geometry == null ? null : geometry.fullRoutePointAt(index);
    }
}

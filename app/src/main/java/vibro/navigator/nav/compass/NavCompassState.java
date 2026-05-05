package vibro.navigator.nav.compass;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;

public final class NavCompassState {
    @NonNull
    public final CompassDisplayMode displayMode;
    @NonNull
    public final CompassRadiusState radiusState;
    @NonNull
    public final CompassProgressLabels progressLabels;

    @NonNull
    public final List<CompassRoutePoint> passedRoutePoints;
    @NonNull
    public final List<CompassRoutePoint> routePoints;
    @NonNull
    public final List<CompassRoutePoint> hintPoints;
    @Nullable
    private final CompassRouteGeometry routeGeometry;
    private final double currentLatitude;
    private final double currentLongitude;
    private final int passedRouteSamplePointCount;
    private final int remainingRouteStartSamplePointIndex;

    @NonNull
    public static NavCompassState fromProjectedPoints(
            float headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            float referenceSpeedMps,
            float visibleRadiusMeters,
            float accuracyRadiusMeters,
            @NonNull List<CompassRoutePoint> passedRoutePoints,
            @NonNull List<CompassRoutePoint> routePoints,
            @NonNull List<CompassRoutePoint> hintPoints,
            float destinationEastMeters,
            float destinationNorthMeters,
            boolean destinationWithinRadius
    ) {
        return new NavCompassState(
                headingDegrees,
                headingAccuracyDegrees,
                referenceSpeedMps,
                referenceSpeedMps,
                referenceSpeedMps,
                visibleRadiusMeters,
                visibleRadiusMeters,
                visibleRadiusMeters,
                accuracyRadiusMeters,
                false,
                0f,
                passedRoutePoints,
                routePoints,
                hintPoints,
                destinationEastMeters,
                destinationNorthMeters,
                destinationWithinRadius
        );
    }

    @NonNull
    public static NavCompassState fromProjectedPoints(
            float headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            float referenceSpeedMps,
            float visibleRadiusMeters,
            float accuracyRadiusMeters,
            boolean movingScaleActive,
            float routeThresholdMeters,
            @NonNull List<CompassRoutePoint> passedRoutePoints,
            @NonNull List<CompassRoutePoint> routePoints,
            @NonNull List<CompassRoutePoint> hintPoints,
            float destinationEastMeters,
            float destinationNorthMeters,
            boolean destinationWithinRadius
    ) {
        return new NavCompassState(
                headingDegrees,
                headingAccuracyDegrees,
                referenceSpeedMps,
                referenceSpeedMps,
                referenceSpeedMps,
                visibleRadiusMeters,
                visibleRadiusMeters,
                visibleRadiusMeters,
                accuracyRadiusMeters,
                movingScaleActive,
                routeThresholdMeters,
                passedRoutePoints,
                routePoints,
                hintPoints,
                destinationEastMeters,
                destinationNorthMeters,
                destinationWithinRadius
        );
    }

    @NonNull
    public static NavCompassState fromRouteGeometry(
            float headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            float referenceSpeedMps,
            float fullRouteReferenceSpeedMps,
            float sixtySecondReferenceSpeedMps,
            float visibleRadiusMeters,
            float fullRouteVisibleRadiusMeters,
            float sixtySecondVisibleRadiusMeters,
            float accuracyRadiusMeters,
            boolean movingScaleActive,
            float routeThresholdMeters,
            @NonNull CompassRouteGeometry routeGeometry,
            double currentLatitude,
            double currentLongitude,
            int passedRouteSamplePointCount,
            float destinationEastMeters,
            float destinationNorthMeters,
            boolean destinationWithinRadius
    ) {
        return new NavCompassState(
                headingDegrees,
                headingAccuracyDegrees,
                referenceSpeedMps,
                fullRouteReferenceSpeedMps,
                sixtySecondReferenceSpeedMps,
                visibleRadiusMeters,
                fullRouteVisibleRadiusMeters,
                sixtySecondVisibleRadiusMeters,
                accuracyRadiusMeters,
                movingScaleActive,
                routeThresholdMeters,
                routeGeometry,
                currentLatitude,
                currentLongitude,
                passedRouteSamplePointCount,
                destinationEastMeters,
                destinationNorthMeters,
                destinationWithinRadius
        );
    }

    private NavCompassState(
            float headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            float referenceSpeedMps,
            float visibleRadiusMeters,
            float accuracyRadiusMeters,
            @NonNull List<CompassRoutePoint> passedRoutePoints,
            @NonNull List<CompassRoutePoint> routePoints,
            @NonNull List<CompassRoutePoint> hintPoints,
            float destinationEastMeters,
            float destinationNorthMeters,
            boolean destinationWithinRadius
    ) {
        this(
                headingDegrees,
                headingAccuracyDegrees,
                referenceSpeedMps,
                visibleRadiusMeters,
                accuracyRadiusMeters,
                false,
                0f,
                passedRoutePoints,
                routePoints,
                hintPoints,
                destinationEastMeters,
                destinationNorthMeters,
                destinationWithinRadius
        );
    }

    private NavCompassState(
            float headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            float referenceSpeedMps,
            float visibleRadiusMeters,
            float accuracyRadiusMeters,
            boolean movingScaleActive,
            float routeThresholdMeters,
            @NonNull List<CompassRoutePoint> passedRoutePoints,
            @NonNull List<CompassRoutePoint> routePoints,
            @NonNull List<CompassRoutePoint> hintPoints,
            float destinationEastMeters,
            float destinationNorthMeters,
            boolean destinationWithinRadius
    ) {
        this(
                headingDegrees,
                headingAccuracyDegrees,
                referenceSpeedMps,
                referenceSpeedMps,
                referenceSpeedMps,
                visibleRadiusMeters,
                visibleRadiusMeters,
                visibleRadiusMeters,
                accuracyRadiusMeters,
                movingScaleActive,
                routeThresholdMeters,
                passedRoutePoints,
                routePoints,
                hintPoints,
                destinationEastMeters,
                destinationNorthMeters,
                destinationWithinRadius
        );
    }

    private NavCompassState(
            float headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            float referenceSpeedMps,
            float fullRouteReferenceSpeedMps,
            float sixtySecondReferenceSpeedMps,
            float visibleRadiusMeters,
            float fullRouteVisibleRadiusMeters,
            float sixtySecondVisibleRadiusMeters,
            float accuracyRadiusMeters,
            boolean movingScaleActive,
            float routeThresholdMeters,
            @NonNull List<CompassRoutePoint> passedRoutePoints,
            @NonNull List<CompassRoutePoint> routePoints,
            @NonNull List<CompassRoutePoint> hintPoints,
            float destinationEastMeters,
            float destinationNorthMeters,
            boolean destinationWithinRadius
    ) {
        this.displayMode = new CompassDisplayMode(
                headingDegrees,
                headingAccuracyDegrees,
                referenceSpeedMps,
                fullRouteReferenceSpeedMps,
                sixtySecondReferenceSpeedMps,
                movingScaleActive
        );
        this.radiusState = new CompassRadiusState(
                visibleRadiusMeters,
                fullRouteVisibleRadiusMeters,
                sixtySecondVisibleRadiusMeters,
                accuracyRadiusMeters,
                routeThresholdMeters
        );
        this.progressLabels = new CompassProgressLabels(
                destinationEastMeters,
                destinationNorthMeters,
                destinationWithinRadius
        );
        this.passedRoutePoints = Collections.unmodifiableList(passedRoutePoints);
        this.routePoints = Collections.unmodifiableList(routePoints);
        this.hintPoints = Collections.unmodifiableList(hintPoints);
        this.routeGeometry = null;
        this.currentLatitude = Double.NaN;
        this.currentLongitude = Double.NaN;
        this.passedRouteSamplePointCount = passedRoutePoints.size();
        this.remainingRouteStartSamplePointIndex = routePoints.isEmpty() ? 0 : 0;
    }

    private NavCompassState(
            float headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            float referenceSpeedMps,
            float fullRouteReferenceSpeedMps,
            float sixtySecondReferenceSpeedMps,
            float visibleRadiusMeters,
            float fullRouteVisibleRadiusMeters,
            float sixtySecondVisibleRadiusMeters,
            float accuracyRadiusMeters,
            boolean movingScaleActive,
            float routeThresholdMeters,
            @NonNull CompassRouteGeometry routeGeometry,
            double currentLatitude,
            double currentLongitude,
            int passedRouteSamplePointCount,
            float destinationEastMeters,
            float destinationNorthMeters,
            boolean destinationWithinRadius
    ) {
        this.displayMode = new CompassDisplayMode(
                headingDegrees,
                headingAccuracyDegrees,
                referenceSpeedMps,
                fullRouteReferenceSpeedMps,
                sixtySecondReferenceSpeedMps,
                movingScaleActive
        );
        this.radiusState = new CompassRadiusState(
                visibleRadiusMeters,
                fullRouteVisibleRadiusMeters,
                sixtySecondVisibleRadiusMeters,
                accuracyRadiusMeters,
                routeThresholdMeters
        );
        this.progressLabels = new CompassProgressLabels(
                destinationEastMeters,
                destinationNorthMeters,
                destinationWithinRadius
        );
        this.routeGeometry = routeGeometry;
        this.currentLatitude = currentLatitude;
        this.currentLongitude = currentLongitude;
        this.passedRouteSamplePointCount = Math.max(
                0,
                Math.min(passedRouteSamplePointCount, routeGeometry.routeSamplePointCount())
        );
        this.remainingRouteStartSamplePointIndex = routeGeometry.routeSamplePointCount() == 0
                ? 0
                : Math.max(0, this.passedRouteSamplePointCount - 1);
        this.passedRoutePoints = new ProjectedRoutePointList(
                routeGeometry,
                this.currentLatitude,
                this.currentLongitude,
                0,
                this.passedRouteSamplePointCount,
                false
        );
        this.routePoints = new ProjectedRoutePointList(
                routeGeometry,
                this.currentLatitude,
                this.currentLongitude,
                this.remainingRouteStartSamplePointIndex,
                routeGeometry.routeSamplePointCount(),
                false
        );
        this.hintPoints = new ProjectedRoutePointList(
                routeGeometry,
                this.currentLatitude,
                this.currentLongitude,
                0,
                routeGeometry.hintSamplePointCount(),
                true
        );
    }

    @NonNull
    public NavCompassState withDisplayMode(boolean sixtySecondView) {
        float targetVisibleRadiusMeters = sixtySecondView
                ? radiusState.sixtySecondVisibleRadiusMeters
                : radiusState.fullRouteVisibleRadiusMeters;
        return withDisplayMode(sixtySecondView, targetVisibleRadiusMeters);
    }

    @NonNull
    public NavCompassState withDisplayMode(boolean sixtySecondView, float visibleRadiusMeters) {
        float targetVisibleRadiusMeters = sanitizeVisibleRadiusMeters(
                visibleRadiusMeters,
                radiusState.targetVisibleRadiusMeters(sixtySecondView)
        );
        if (displayMode.movingScaleActive == sixtySecondView
                && Math.abs(radiusState.visibleRadiusMeters - targetVisibleRadiusMeters) <= 0.01f) {
            return this;
        }
        float targetReferenceSpeedMps = sixtySecondView
                ? resolveMovingLegendReferenceSpeedMps(targetVisibleRadiusMeters)
                : displayMode.fullRouteReferenceSpeedMps;
        boolean targetDestinationWithinRadius = Math.hypot(
                progressLabels.destinationEastMeters,
                progressLabels.destinationNorthMeters
        )
                <= targetVisibleRadiusMeters;
        if (routeGeometry != null) {
            return new NavCompassState(
                    displayMode.headingDegrees,
                    displayMode.headingAccuracyDegrees,
                    targetReferenceSpeedMps,
                    displayMode.fullRouteReferenceSpeedMps,
                    displayMode.sixtySecondReferenceSpeedMps,
                    targetVisibleRadiusMeters,
                    radiusState.fullRouteVisibleRadiusMeters,
                    radiusState.sixtySecondVisibleRadiusMeters,
                    radiusState.accuracyRadiusMeters,
                    sixtySecondView,
                    radiusState.routeThresholdMeters,
                    routeGeometry,
                    currentLatitude,
                    currentLongitude,
                    passedRouteSamplePointCount,
                    progressLabels.destinationEastMeters,
                    progressLabels.destinationNorthMeters,
                    targetDestinationWithinRadius
            );
        }
        return new NavCompassState(
                displayMode.headingDegrees,
                displayMode.headingAccuracyDegrees,
                targetReferenceSpeedMps,
                displayMode.fullRouteReferenceSpeedMps,
                displayMode.sixtySecondReferenceSpeedMps,
                targetVisibleRadiusMeters,
                radiusState.fullRouteVisibleRadiusMeters,
                radiusState.sixtySecondVisibleRadiusMeters,
                radiusState.accuracyRadiusMeters,
                sixtySecondView,
                radiusState.routeThresholdMeters,
                passedRoutePoints,
                routePoints,
                hintPoints,
                progressLabels.destinationEastMeters,
                progressLabels.destinationNorthMeters,
                targetDestinationWithinRadius
        );
    }

    private static float sanitizeVisibleRadiusMeters(float visibleRadiusMeters, float fallbackVisibleRadiusMeters) {
        return Float.isFinite(visibleRadiusMeters) && visibleRadiusMeters > 0f
                ? visibleRadiusMeters
                : fallbackVisibleRadiusMeters;
    }

    private static float resolveMovingLegendReferenceSpeedMps(float visibleRadiusMeters) {
        return CompassRadiusResolver.movingLegendReferenceSpeedMps(visibleRadiusMeters, 0f);
    }

    public boolean hasRouteGeometry() {
        return routeGeometry != null;
    }

    public int routeSamplePointCount() {
        return routeGeometry == null ? 0 : routeGeometry.routeSamplePointCount();
    }

    public int passedRouteSamplePointCount() {
        return routeGeometry == null ? passedRoutePoints.size() : passedRouteSamplePointCount;
    }

    public int remainingRouteStartSamplePointIndex() {
        return routeGeometry == null ? 0 : remainingRouteStartSamplePointIndex;
    }

    @Nullable
    public LatLon routeSamplePointAt(int index) {
        return routeGeometry == null ? null : routeGeometry.routeSamplePointAt(index);
    }

    public int hintSamplePointCount() {
        return routeGeometry == null ? 0 : routeGeometry.hintSamplePointCount();
    }

    @Nullable
    public LatLon hintSamplePointAt(int index) {
        return routeGeometry == null ? null : routeGeometry.hintSamplePointAt(index);
    }

    public double currentLatitude() {
        return currentLatitude;
    }

    public double currentLongitude() {
        return currentLongitude;
    }

    private static final class ProjectedRoutePointList extends AbstractList<CompassRoutePoint> {
        @NonNull
        private final CompassRouteGeometry routeGeometry;
        private final double currentLatitude;
        private final double currentLongitude;
        private final int startIndex;
        private final int endIndex;
        private final boolean hintPoints;

        private ProjectedRoutePointList(
                @NonNull CompassRouteGeometry routeGeometry,
                double currentLatitude,
                double currentLongitude,
                int startIndex,
                int endIndex,
                boolean hintPoints
        ) {
            this.routeGeometry = routeGeometry;
            this.currentLatitude = currentLatitude;
            this.currentLongitude = currentLongitude;
            this.startIndex = startIndex;
            this.endIndex = Math.max(startIndex, endIndex);
            this.hintPoints = hintPoints;
        }

        @NonNull
        @Override
        public CompassRoutePoint get(int index) {
            int absoluteIndex = startIndex + index;
            LatLon point = hintPoints
                    ? routeGeometry.hintSamplePointAt(absoluteIndex)
                    : routeGeometry.routeSamplePointAt(absoluteIndex);
            if (point == null) {
                throw new IndexOutOfBoundsException("index=" + index);
            }
            return new CompassRoutePoint(
                    (float) GeoMath.eastMeters(currentLatitude, currentLongitude, point.lat, point.lon),
                    (float) GeoMath.northMeters(currentLatitude, point.lat)
            );
        }

        @Override
        public int size() {
            return endIndex - startIndex;
        }
    }
}

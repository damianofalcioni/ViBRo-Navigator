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
                new CompassDisplayMetrics(
                        headingDegrees,
                        headingAccuracyDegrees,
                        referenceSpeedMps,
                        referenceSpeedMps,
                        referenceSpeedMps,
                        false
                ),
                new CompassRadiusMetrics(
                        visibleRadiusMeters,
                        visibleRadiusMeters,
                        visibleRadiusMeters,
                        accuracyRadiusMeters,
                        0f
                ),
                passedRoutePoints,
                routePoints,
                hintPoints,
                new CompassDestinationProjection(
                        destinationEastMeters,
                        destinationNorthMeters,
                        destinationWithinRadius
                )
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
                new CompassDisplayMetrics(
                        headingDegrees,
                        headingAccuracyDegrees,
                        referenceSpeedMps,
                        referenceSpeedMps,
                        referenceSpeedMps,
                        movingScaleActive
                ),
                new CompassRadiusMetrics(
                        visibleRadiusMeters,
                        visibleRadiusMeters,
                        visibleRadiusMeters,
                        accuracyRadiusMeters,
                        routeThresholdMeters
                ),
                passedRoutePoints,
                routePoints,
                hintPoints,
                new CompassDestinationProjection(
                        destinationEastMeters,
                        destinationNorthMeters,
                        destinationWithinRadius
                )
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
                new CompassDisplayMetrics(
                        headingDegrees,
                        headingAccuracyDegrees,
                        referenceSpeedMps,
                        fullRouteReferenceSpeedMps,
                        sixtySecondReferenceSpeedMps,
                        movingScaleActive
                ),
                new CompassRadiusMetrics(
                        visibleRadiusMeters,
                        fullRouteVisibleRadiusMeters,
                        sixtySecondVisibleRadiusMeters,
                        accuracyRadiusMeters,
                        routeThresholdMeters
                ),
                routeGeometry,
                currentLatitude,
                currentLongitude,
                passedRouteSamplePointCount,
                new CompassDestinationProjection(
                        destinationEastMeters,
                        destinationNorthMeters,
                        destinationWithinRadius
                )
        );
    }

    private NavCompassState(
            @NonNull CompassDisplayMetrics displayMetrics,
            @NonNull CompassRadiusMetrics radiusMetrics,
            @NonNull List<CompassRoutePoint> passedRoutePoints,
            @NonNull List<CompassRoutePoint> routePoints,
            @NonNull List<CompassRoutePoint> hintPoints,
            @NonNull CompassDestinationProjection destinationProjection
    ) {
        this.displayMode = new CompassDisplayMode(
                displayMetrics.headingDegrees,
                displayMetrics.headingAccuracyDegrees,
                displayMetrics.referenceSpeedMps,
                displayMetrics.fullRouteReferenceSpeedMps,
                displayMetrics.sixtySecondReferenceSpeedMps,
                displayMetrics.movingScaleActive
        );
        this.radiusState = new CompassRadiusState(
                radiusMetrics.visibleRadiusMeters,
                radiusMetrics.fullRouteVisibleRadiusMeters,
                radiusMetrics.sixtySecondVisibleRadiusMeters,
                radiusMetrics.accuracyRadiusMeters,
                radiusMetrics.routeThresholdMeters
        );
        this.progressLabels = new CompassProgressLabels(
                destinationProjection.eastMeters,
                destinationProjection.northMeters,
                destinationProjection.withinRadius
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
            @NonNull CompassDisplayMetrics displayMetrics,
            @NonNull CompassRadiusMetrics radiusMetrics,
            @NonNull CompassRouteGeometry routeGeometry,
            double currentLatitude,
            double currentLongitude,
            int passedRouteSamplePointCount,
            @NonNull CompassDestinationProjection destinationProjection
    ) {
        this.displayMode = new CompassDisplayMode(
                displayMetrics.headingDegrees,
                displayMetrics.headingAccuracyDegrees,
                displayMetrics.referenceSpeedMps,
                displayMetrics.fullRouteReferenceSpeedMps,
                displayMetrics.sixtySecondReferenceSpeedMps,
                displayMetrics.movingScaleActive
        );
        this.radiusState = new CompassRadiusState(
                radiusMetrics.visibleRadiusMeters,
                radiusMetrics.fullRouteVisibleRadiusMeters,
                radiusMetrics.sixtySecondVisibleRadiusMeters,
                radiusMetrics.accuracyRadiusMeters,
                radiusMetrics.routeThresholdMeters
        );
        this.progressLabels = new CompassProgressLabels(
                destinationProjection.eastMeters,
                destinationProjection.northMeters,
                destinationProjection.withinRadius
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
                    new CompassDisplayMetrics(
                            displayMode.headingDegrees,
                            displayMode.headingAccuracyDegrees,
                            targetReferenceSpeedMps,
                            displayMode.fullRouteReferenceSpeedMps,
                            displayMode.sixtySecondReferenceSpeedMps,
                            sixtySecondView
                    ),
                    new CompassRadiusMetrics(
                            targetVisibleRadiusMeters,
                            radiusState.fullRouteVisibleRadiusMeters,
                            radiusState.sixtySecondVisibleRadiusMeters,
                            radiusState.accuracyRadiusMeters,
                            radiusState.routeThresholdMeters
                    ),
                    routeGeometry,
                    currentLatitude,
                    currentLongitude,
                    passedRouteSamplePointCount,
                    new CompassDestinationProjection(
                            progressLabels.destinationEastMeters,
                            progressLabels.destinationNorthMeters,
                            targetDestinationWithinRadius
                    )
            );
        }
        return new NavCompassState(
                new CompassDisplayMetrics(
                        displayMode.headingDegrees,
                        displayMode.headingAccuracyDegrees,
                        targetReferenceSpeedMps,
                        displayMode.fullRouteReferenceSpeedMps,
                        displayMode.sixtySecondReferenceSpeedMps,
                        sixtySecondView
                ),
                new CompassRadiusMetrics(
                        targetVisibleRadiusMeters,
                        radiusState.fullRouteVisibleRadiusMeters,
                        radiusState.sixtySecondVisibleRadiusMeters,
                        radiusState.accuracyRadiusMeters,
                        radiusState.routeThresholdMeters
                ),
                passedRoutePoints,
                routePoints,
                hintPoints,
                new CompassDestinationProjection(
                        progressLabels.destinationEastMeters,
                        progressLabels.destinationNorthMeters,
                        targetDestinationWithinRadius
                )
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

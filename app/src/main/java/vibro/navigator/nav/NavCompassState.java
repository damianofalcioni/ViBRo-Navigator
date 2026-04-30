package vibro.navigator.nav;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;

public final class NavCompassState {

    public static final class RoutePoint {
        public final float eastMeters;
        public final float northMeters;

        public RoutePoint(float eastMeters, float northMeters) {
            this.eastMeters = eastMeters;
            this.northMeters = northMeters;
        }
    }

    public final float headingDegrees;
    @Nullable
    public final Float headingAccuracyDegrees;
    public final float referenceSpeedMps;
    public final float fullRouteReferenceSpeedMps;
    public final float sixtySecondReferenceSpeedMps;
    public final float visibleRadiusMeters;
    public final float fullRouteVisibleRadiusMeters;
    public final float sixtySecondVisibleRadiusMeters;
    public final float accuracyRadiusMeters;
    public final boolean movingScaleActive;
    public final float routeThresholdMeters;
    @NonNull
    public final List<RoutePoint> passedRoutePoints;
    @NonNull
    public final List<RoutePoint> routePoints;
    @NonNull
    public final List<RoutePoint> hintPoints;
    public final float destinationEastMeters;
    public final float destinationNorthMeters;
    public final boolean destinationWithinRadius;
    @Nullable
    private final CompassRouteGeometry routeGeometry;
    private final double currentLatitude;
    private final double currentLongitude;
    private final int passedRouteSamplePointCount;
    private final int remainingRouteStartSamplePointIndex;

    public NavCompassState(
            float headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            float referenceSpeedMps,
            float visibleRadiusMeters,
            float accuracyRadiusMeters,
            @NonNull List<RoutePoint> passedRoutePoints,
            @NonNull List<RoutePoint> routePoints,
            @NonNull List<RoutePoint> hintPoints,
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

    public NavCompassState(
            float headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            float referenceSpeedMps,
            float visibleRadiusMeters,
            float accuracyRadiusMeters,
            boolean movingScaleActive,
            float routeThresholdMeters,
            @NonNull List<RoutePoint> passedRoutePoints,
            @NonNull List<RoutePoint> routePoints,
            @NonNull List<RoutePoint> hintPoints,
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

    public NavCompassState(
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
            @NonNull List<RoutePoint> passedRoutePoints,
            @NonNull List<RoutePoint> routePoints,
            @NonNull List<RoutePoint> hintPoints,
            float destinationEastMeters,
            float destinationNorthMeters,
            boolean destinationWithinRadius
    ) {
        this.headingDegrees = headingDegrees;
        this.headingAccuracyDegrees = headingAccuracyDegrees;
        this.referenceSpeedMps = referenceSpeedMps;
        this.fullRouteReferenceSpeedMps = fullRouteReferenceSpeedMps;
        this.sixtySecondReferenceSpeedMps = sixtySecondReferenceSpeedMps;
        this.visibleRadiusMeters = visibleRadiusMeters;
        this.fullRouteVisibleRadiusMeters = fullRouteVisibleRadiusMeters;
        this.sixtySecondVisibleRadiusMeters = sixtySecondVisibleRadiusMeters;
        this.accuracyRadiusMeters = accuracyRadiusMeters;
        this.movingScaleActive = movingScaleActive;
        this.routeThresholdMeters = routeThresholdMeters;
        this.passedRoutePoints = Collections.unmodifiableList(passedRoutePoints);
        this.routePoints = Collections.unmodifiableList(routePoints);
        this.hintPoints = Collections.unmodifiableList(hintPoints);
        this.destinationEastMeters = destinationEastMeters;
        this.destinationNorthMeters = destinationNorthMeters;
        this.destinationWithinRadius = destinationWithinRadius;
        this.routeGeometry = null;
        this.currentLatitude = Double.NaN;
        this.currentLongitude = Double.NaN;
        this.passedRouteSamplePointCount = passedRoutePoints.size();
        this.remainingRouteStartSamplePointIndex = routePoints.isEmpty() ? 0 : 0;
    }

    NavCompassState(
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
        this.headingDegrees = headingDegrees;
        this.headingAccuracyDegrees = headingAccuracyDegrees;
        this.referenceSpeedMps = referenceSpeedMps;
        this.fullRouteReferenceSpeedMps = fullRouteReferenceSpeedMps;
        this.sixtySecondReferenceSpeedMps = sixtySecondReferenceSpeedMps;
        this.visibleRadiusMeters = visibleRadiusMeters;
        this.fullRouteVisibleRadiusMeters = fullRouteVisibleRadiusMeters;
        this.sixtySecondVisibleRadiusMeters = sixtySecondVisibleRadiusMeters;
        this.accuracyRadiusMeters = accuracyRadiusMeters;
        this.movingScaleActive = movingScaleActive;
        this.routeThresholdMeters = routeThresholdMeters;
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
        this.destinationEastMeters = destinationEastMeters;
        this.destinationNorthMeters = destinationNorthMeters;
        this.destinationWithinRadius = destinationWithinRadius;
    }

    @NonNull
    public NavCompassState withDisplayMode(boolean sixtySecondView) {
        float targetVisibleRadiusMeters = sixtySecondView
                ? sixtySecondVisibleRadiusMeters
                : fullRouteVisibleRadiusMeters;
        return withDisplayMode(sixtySecondView, targetVisibleRadiusMeters);
    }

    @NonNull
    NavCompassState withDisplayMode(boolean sixtySecondView, float visibleRadiusMeters) {
        float targetVisibleRadiusMeters = sanitizeVisibleRadiusMeters(
                visibleRadiusMeters,
                sixtySecondView ? sixtySecondVisibleRadiusMeters : fullRouteVisibleRadiusMeters
        );
        if (movingScaleActive == sixtySecondView
                && Math.abs(this.visibleRadiusMeters - targetVisibleRadiusMeters) <= 0.01f) {
            return this;
        }
        float targetReferenceSpeedMps = sixtySecondView
                ? resolveMovingLegendReferenceSpeedMps(targetVisibleRadiusMeters)
                : fullRouteReferenceSpeedMps;
        boolean targetDestinationWithinRadius = Math.hypot(destinationEastMeters, destinationNorthMeters)
                <= targetVisibleRadiusMeters;
        if (routeGeometry != null) {
            return new NavCompassState(
                    headingDegrees,
                    headingAccuracyDegrees,
                    targetReferenceSpeedMps,
                    fullRouteReferenceSpeedMps,
                    sixtySecondReferenceSpeedMps,
                    targetVisibleRadiusMeters,
                    fullRouteVisibleRadiusMeters,
                    sixtySecondVisibleRadiusMeters,
                    accuracyRadiusMeters,
                    sixtySecondView,
                    routeThresholdMeters,
                    routeGeometry,
                    currentLatitude,
                    currentLongitude,
                    passedRouteSamplePointCount,
                    destinationEastMeters,
                    destinationNorthMeters,
                    targetDestinationWithinRadius
            );
        }
        return new NavCompassState(
                headingDegrees,
                headingAccuracyDegrees,
                targetReferenceSpeedMps,
                fullRouteReferenceSpeedMps,
                sixtySecondReferenceSpeedMps,
                targetVisibleRadiusMeters,
                fullRouteVisibleRadiusMeters,
                sixtySecondVisibleRadiusMeters,
                accuracyRadiusMeters,
                sixtySecondView,
                routeThresholdMeters,
                passedRoutePoints,
                routePoints,
                hintPoints,
                destinationEastMeters,
                destinationNorthMeters,
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

    private static final class ProjectedRoutePointList extends AbstractList<RoutePoint> {
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
        public RoutePoint get(int index) {
            int absoluteIndex = startIndex + index;
            LatLon point = hintPoints
                    ? routeGeometry.hintSamplePointAt(absoluteIndex)
                    : routeGeometry.routeSamplePointAt(absoluteIndex);
            if (point == null) {
                throw new IndexOutOfBoundsException("index=" + index);
            }
            return new RoutePoint(
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

package vibro.navigator.nav.compass;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.NavigationRouteGeometryState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NavCompassState {
    @NonNull
    public final CompassDisplayMode displayMode;
    @NonNull
    public final CompassRadiusState radiusState;
    @NonNull
    public final CompassProgressLabels progressLabels;
    @Nullable
    public final CompassDestinationProjection routeStartApproachProjection;

    @NonNull
    public final List<CompassRoutePoint> passedRoutePoints;
    @NonNull
    public final List<CompassRoutePoint> routePoints;
    @NonNull
    public final List<CompassRoutePoint> hintPoints;
    @Nullable
    public final CompassOrientationCue orientationCue;
    @Nullable
    private final CompassRouteGeometry routeGeometry;
    private final double currentLatitude;
    private final double currentLongitude;
    private final int passedRouteSamplePointCount;
    private final int remainingRouteStartSamplePointIndex;

    @NonNull
    static NavCompassState fromProjectedPoints(@NonNull NavCompassProjectedPointsInput input) {
        return new NavCompassState(input);
    }

    @NonNull
    static NavCompassState fromRouteGeometry(@NonNull NavCompassRouteGeometryInput input) {
        return new NavCompassState(input);
    }

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
        return fromProjectedPoints(new NavCompassProjectedPointsInput(
                displayMetrics(headingDegrees, headingAccuracyDegrees, referenceSpeedMps, false),
                radiusMetrics(visibleRadiusMeters, accuracyRadiusMeters, 0f),
                passedRoutePoints,
                routePoints,
                hintPoints,
                new CompassDestinationProjection(
                        destinationEastMeters,
                        destinationNorthMeters,
                        (float) NavigationRouteGeometryState.resolveDestinationReachedRadiusMeters(accuracyRadiusMeters),
                        destinationWithinRadius
                )
        ));
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
        return fromProjectedPoints(new NavCompassProjectedPointsInput(
                displayMetrics(headingDegrees, headingAccuracyDegrees, referenceSpeedMps, movingScaleActive),
                radiusMetrics(visibleRadiusMeters, accuracyRadiusMeters, routeThresholdMeters),
                passedRoutePoints,
                routePoints,
                hintPoints,
                new CompassDestinationProjection(
                        destinationEastMeters,
                        destinationNorthMeters,
                        Math.max(
                                routeThresholdMeters,
                                (float) NavigationRouteGeometryState.resolveDestinationReachedRadiusMeters(
                                        accuracyRadiusMeters
                                )
                        ),
                        destinationWithinRadius
                )
        ));
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
            float destinationReachedRadiusMeters,
            boolean destinationWithinRadius
    ) {
        return fromRouteGeometry(new NavCompassRouteGeometryInput(
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
                        destinationReachedRadiusMeters,
                        destinationWithinRadius
                )
        ));
    }

    private NavCompassState(@NonNull NavCompassProjectedPointsInput input) {
        this.displayMode = new CompassDisplayMode(
                input.displayMetrics.headingDegrees,
                input.displayMetrics.headingAccuracyDegrees,
                input.displayMetrics.referenceSpeedMps,
                input.displayMetrics.fullRouteReferenceSpeedMps,
                input.displayMetrics.sixtySecondReferenceSpeedMps,
                input.displayMetrics.movingScaleActive
        );
        this.radiusState = new CompassRadiusState(
                input.radiusMetrics.visibleRadiusMeters,
                input.radiusMetrics.fullRouteVisibleRadiusMeters,
                input.radiusMetrics.sixtySecondVisibleRadiusMeters,
                input.radiusMetrics.accuracyRadiusMeters,
                input.radiusMetrics.routeThresholdMeters
        );
        this.progressLabels = new CompassProgressLabels(
                input.destinationProjection.eastMeters,
                input.destinationProjection.northMeters,
                input.destinationProjection.reachedRadiusMeters,
                input.destinationProjection.withinRadius
        );
        this.routeStartApproachProjection = input.routeStartApproachProjection;
        this.passedRoutePoints = Collections.unmodifiableList(new ArrayList<>(input.passedRoutePoints));
        this.routePoints = Collections.unmodifiableList(new ArrayList<>(input.routePoints));
        this.hintPoints = Collections.unmodifiableList(new ArrayList<>(input.hintPoints));
        this.orientationCue = input.orientationCue;
        this.routeGeometry = null;
        this.currentLatitude = Double.NaN;
        this.currentLongitude = Double.NaN;
        this.passedRouteSamplePointCount = input.passedRoutePoints.size();
        this.remainingRouteStartSamplePointIndex = input.routePoints.isEmpty() ? 0 : 0;
    }

    private NavCompassState(@NonNull NavCompassRouteGeometryInput input) {
        this.displayMode = new CompassDisplayMode(
                input.displayMetrics.headingDegrees,
                input.displayMetrics.headingAccuracyDegrees,
                input.displayMetrics.referenceSpeedMps,
                input.displayMetrics.fullRouteReferenceSpeedMps,
                input.displayMetrics.sixtySecondReferenceSpeedMps,
                input.displayMetrics.movingScaleActive
        );
        this.radiusState = new CompassRadiusState(
                input.radiusMetrics.visibleRadiusMeters,
                input.radiusMetrics.fullRouteVisibleRadiusMeters,
                input.radiusMetrics.sixtySecondVisibleRadiusMeters,
                input.radiusMetrics.accuracyRadiusMeters,
                input.radiusMetrics.routeThresholdMeters
        );
        this.progressLabels = new CompassProgressLabels(
                input.destinationProjection.eastMeters,
                input.destinationProjection.northMeters,
                input.destinationProjection.reachedRadiusMeters,
                input.destinationProjection.withinRadius
        );
        this.routeStartApproachProjection = input.routeStartApproachProjection;
        this.routeGeometry = input.routeGeometry;
        this.currentLatitude = input.currentLatitude;
        this.currentLongitude = input.currentLongitude;
        this.passedRouteSamplePointCount = Math.max(
                0,
                Math.min(input.passedRouteSamplePointCount, input.routeGeometry.routeSamplePointCount())
        );
        this.remainingRouteStartSamplePointIndex = input.routeGeometry.routeSamplePointCount() == 0
                ? 0
                : Math.max(0, this.passedRouteSamplePointCount - 1);
        this.passedRoutePoints = new ProjectedCompassRoutePointList(
                input.routeGeometry,
                this.currentLatitude,
                this.currentLongitude,
                0,
                this.passedRouteSamplePointCount,
                false
        );
        this.routePoints = new ProjectedCompassRoutePointList(
                input.routeGeometry,
                this.currentLatitude,
                this.currentLongitude,
                this.remainingRouteStartSamplePointIndex,
                input.routeGeometry.routeSamplePointCount(),
                false
        );
        this.hintPoints = new ProjectedCompassRoutePointList(
                input.routeGeometry,
                this.currentLatitude,
                this.currentLongitude,
                0,
                input.routeGeometry.hintSamplePointCount(),
                true
        );
        this.orientationCue = input.orientationCue;
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
            return fromRouteGeometry(new NavCompassRouteGeometryInput(
                    displayMetrics(
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
                            progressLabels.destinationReachedRadiusMeters,
                            targetDestinationWithinRadius
                    ),
                    routeStartApproachProjection,
                    orientationCue
            ));
        }
        return fromProjectedPoints(new NavCompassProjectedPointsInput(
                displayMetrics(
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
                        progressLabels.destinationReachedRadiusMeters,
                        targetDestinationWithinRadius
                ),
                routeStartApproachProjection,
                orientationCue
        ));
    }

    @NonNull
    private static CompassDisplayMetrics displayMetrics(
            float headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            float referenceSpeedMps,
            boolean movingScaleActive
    ) {
        return displayMetrics(
                headingDegrees,
                headingAccuracyDegrees,
                referenceSpeedMps,
                referenceSpeedMps,
                referenceSpeedMps,
                movingScaleActive
        );
    }

    @NonNull
    private static CompassDisplayMetrics displayMetrics(
            float headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            float referenceSpeedMps,
            float fullRouteReferenceSpeedMps,
            float sixtySecondReferenceSpeedMps,
            boolean movingScaleActive
    ) {
        return new CompassDisplayMetrics(
                headingDegrees,
                headingAccuracyDegrees,
                referenceSpeedMps,
                fullRouteReferenceSpeedMps,
                sixtySecondReferenceSpeedMps,
                movingScaleActive
        );
    }

    @NonNull
    private static CompassRadiusMetrics radiusMetrics(
            float visibleRadiusMeters,
            float accuracyRadiusMeters,
            float routeThresholdMeters
    ) {
        return new CompassRadiusMetrics(
                visibleRadiusMeters,
                visibleRadiusMeters,
                visibleRadiusMeters,
                accuracyRadiusMeters,
                routeThresholdMeters
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

    @Nullable
    public CompassRouteGeometry routeGeometry() {
        return routeGeometry;
    }

    public double currentLatitude() {
        return currentLatitude;
    }

    public double currentLongitude() {
        return currentLongitude;
    }
}

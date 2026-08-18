package vibro.navigator.nav.compass;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;

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
    @NonNull
    public final List<CompassBlockedArea> blockedAreas;
    @NonNull
    public final CompassStreetOverlay streetOverlay;
    @Nullable
    public final CompassOrientationCue orientationCue;
    @NonNull
    public final CompassFullRouteView fullRouteView;
    @Nullable
    private final CompassRouteGeometry routeGeometry;
    private final double currentLatitude;
    private final double currentLongitude;
    private final int passedRouteSamplePointCount;
    private final int remainingRouteStartSamplePointIndex;
    private final double alongTrackMeters;

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
                        CompassRadiusMetrics.destinationReachedRadiusMeters(accuracyRadiusMeters),
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
                        Math.max(
                                routeThresholdMeters,
                                CompassRadiusMetrics.destinationReachedRadiusMeters(accuracyRadiusMeters)
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
            float movingScaleReferenceSpeedMps,
            float visibleRadiusMeters,
            float fullRouteVisibleRadiusMeters,
            float movingScaleVisibleRadiusMeters,
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
                        movingScaleReferenceSpeedMps,
                        movingScaleActive
                ),
                new CompassRadiusMetrics(
                        visibleRadiusMeters,
                        fullRouteVisibleRadiusMeters,
                        movingScaleVisibleRadiusMeters,
                        accuracyRadiusMeters,
                        routeThresholdMeters
                ),
                routeGeometry,
                currentLatitude,
                currentLongitude,
                routeGeometry.alongTrackMetersForSampleCount(passedRouteSamplePointCount),
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
                input.displayMetrics.movingScaleReferenceSpeedMps,
                input.displayMetrics.movingScaleHorizonSeconds,
                input.displayMetrics.movingScaleSpeedBucket,
                input.displayMetrics.movingScaleActive,
                input.displayMetrics.straightLineMode
        );
        this.radiusState = new CompassRadiusState(
                input.radiusMetrics.visibleRadiusMeters,
                input.radiusMetrics.fullRouteVisibleRadiusMeters,
                input.radiusMetrics.movingScaleVisibleRadiusMeters,
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
        this.blockedAreas = Collections.unmodifiableList(new ArrayList<>(input.blockedAreas));
        this.streetOverlay = CompassStreetOverlay.EMPTY;
        this.orientationCue = input.orientationCue;
        this.routeGeometry = null;
        this.currentLatitude = Double.NaN;
        this.currentLongitude = Double.NaN;
        this.passedRouteSamplePointCount = input.passedRoutePoints.size();
        this.remainingRouteStartSamplePointIndex = 0;
        this.fullRouteView = CompassFullRouteView.EMPTY;
        this.alongTrackMeters = 0.0;
    }

    private NavCompassState(@NonNull NavCompassRouteGeometryInput input) {
        this.displayMode = new CompassDisplayMode(
                input.displayMetrics.headingDegrees,
                input.displayMetrics.headingAccuracyDegrees,
                input.displayMetrics.referenceSpeedMps,
                input.displayMetrics.fullRouteReferenceSpeedMps,
                input.displayMetrics.movingScaleReferenceSpeedMps,
                input.displayMetrics.movingScaleHorizonSeconds,
                input.displayMetrics.movingScaleSpeedBucket,
                input.displayMetrics.movingScaleActive,
                input.displayMetrics.straightLineMode
        );
        this.radiusState = new CompassRadiusState(
                input.radiusMetrics.visibleRadiusMeters,
                input.radiusMetrics.fullRouteVisibleRadiusMeters,
                input.radiusMetrics.movingScaleVisibleRadiusMeters,
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
        this.alongTrackMeters = input.alongTrackMeters;
        this.passedRouteSamplePointCount = input.routeGeometry.passedRoutePointCount(input.alongTrackMeters);
        this.remainingRouteStartSamplePointIndex = input.routeGeometry.routeSamplePointCount() == 0
                ? 0
                : Math.max(0, this.passedRouteSamplePointCount - 1);
        this.fullRouteView = CompassFullRouteView.from(input);
        this.passedRoutePoints = new ProjectedCompassPassedRoutePointList(
                input.routeGeometry,
                this.currentLatitude,
                this.currentLongitude,
                this.passedRouteSamplePointCount
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
        this.blockedAreas = Collections.unmodifiableList(new ArrayList<>(input.blockedAreas));
        this.streetOverlay = CompassStreetOverlay.EMPTY;
        this.orientationCue = input.orientationCue;
    }

    private NavCompassState(
            @NonNull NavCompassState source,
            @NonNull CompassStreetOverlay streetOverlay
    ) {
        this(source, source.displayMode, streetOverlay);
    }

    NavCompassState(
            @NonNull NavCompassState source,
            @NonNull CompassDisplayMode displayMode,
            @NonNull CompassStreetOverlay streetOverlay
    ) {
        this.displayMode = displayMode;
        radiusState = source.radiusState;
        progressLabels = source.progressLabels;
        routeStartApproachProjection = source.routeStartApproachProjection;
        passedRoutePoints = source.passedRoutePoints;
        routePoints = source.routePoints;
        hintPoints = source.hintPoints;
        blockedAreas = source.blockedAreas;
        this.streetOverlay = streetOverlay;
        orientationCue = source.orientationCue;
        routeGeometry = source.routeGeometry;
        currentLatitude = source.currentLatitude;
        currentLongitude = source.currentLongitude;
        passedRouteSamplePointCount = source.passedRouteSamplePointCount;
        remainingRouteStartSamplePointIndex = source.remainingRouteStartSamplePointIndex;
        fullRouteView = source.fullRouteView;
        alongTrackMeters = source.alongTrackMeters;
    }

    @NonNull
    public NavCompassState withDisplayMode(boolean movingScaleView) {
        float targetVisibleRadiusMeters = movingScaleView
                ? radiusState.movingScaleVisibleRadiusMeters
                : radiusState.fullRouteVisibleRadiusMeters;
        return withDisplayMode(movingScaleView, targetVisibleRadiusMeters);
    }

    @NonNull
    public NavCompassState withDisplayMode(boolean movingScaleView, float visibleRadiusMeters) {
        float targetVisibleRadiusMeters = sanitizeVisibleRadiusMeters(
                visibleRadiusMeters,
                radiusState.targetVisibleRadiusMeters(movingScaleView)
        );
        if (displayMode.movingScaleActive == movingScaleView
                && Math.abs(radiusState.visibleRadiusMeters - targetVisibleRadiusMeters) <= 0.01f) {
            return this;
        }
        float targetReferenceSpeedMps = movingScaleView
                ? displayMode.movingScaleReferenceSpeedMps
                : displayMode.fullRouteReferenceSpeedMps;
        boolean targetDestinationWithinRadius = Math.hypot(
                progressLabels.destinationEastMeters,
                progressLabels.destinationNorthMeters
        )
                <= targetVisibleRadiusMeters;
        if (routeGeometry != null) {
            return fromRouteGeometry(new NavCompassRouteGeometryInput(
                    CompassDisplayMetrics.forDisplayMode(
                            displayMode,
                            targetReferenceSpeedMps,
                            movingScaleView
                    ),
                    new CompassRadiusMetrics(
                            targetVisibleRadiusMeters,
                            radiusState.fullRouteVisibleRadiusMeters,
                            radiusState.movingScaleVisibleRadiusMeters,
                            radiusState.accuracyRadiusMeters,
                            radiusState.routeThresholdMeters
                    ),
                    routeGeometry,
                    currentLatitude,
                    currentLongitude,
                    alongTrackMeters,
                    new CompassDestinationProjection(
                            progressLabels.destinationEastMeters,
                            progressLabels.destinationNorthMeters,
                            progressLabels.destinationReachedRadiusMeters,
                            targetDestinationWithinRadius
                    ),
                    blockedAreas,
                    routeStartApproachProjection,
                    orientationCue
            )).withStreetOverlay(streetOverlay);
        }
        return fromProjectedPoints(new NavCompassProjectedPointsInput(
                CompassDisplayMetrics.forDisplayMode(
                        displayMode,
                        targetReferenceSpeedMps,
                        movingScaleView
                ),
                new CompassRadiusMetrics(
                        targetVisibleRadiusMeters,
                        radiusState.fullRouteVisibleRadiusMeters,
                        radiusState.movingScaleVisibleRadiusMeters,
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
                blockedAreas,
                routeStartApproachProjection,
                orientationCue
        )).withStreetOverlay(streetOverlay);
    }

    @NonNull
    public NavCompassState withStreetOverlay(@NonNull CompassStreetOverlay streetOverlay) {
        return new NavCompassState(this, streetOverlay);
    }

    private static float sanitizeVisibleRadiusMeters(float visibleRadiusMeters, float fallbackVisibleRadiusMeters) {
        return Float.isFinite(visibleRadiusMeters) && visibleRadiusMeters > 0f
                ? visibleRadiusMeters
                : fallbackVisibleRadiusMeters;
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

    @NonNull
    public CompassPassedRouteSegments archivedPassedRouteSegments() {
        return routeGeometry == null
                ? CompassPassedRouteSegments.EMPTY
                : routeGeometry.archivedPassedRouteSegments();
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

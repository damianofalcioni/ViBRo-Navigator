package vibro.navigator.nav.compass;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

final class NavCompassProjectedPointsInput {
    @NonNull
    public final CompassDisplayMetrics displayMetrics;
    @NonNull
    public final CompassRadiusMetrics radiusMetrics;
    @NonNull
    public final List<CompassRoutePoint> passedRoutePoints;
    @NonNull
    public final List<CompassRoutePoint> routePoints;
    @NonNull
    public final List<CompassRoutePoint> hintPoints;
    @NonNull
    public final List<CompassBlockedArea> blockedAreas;
    @NonNull
    public final CompassDestinationProjection destinationProjection;
    @Nullable
    public final CompassDestinationProjection routeStartApproachProjection;
    @Nullable
    public final CompassOrientationCue orientationCue;

    NavCompassProjectedPointsInput(
            @NonNull CompassDisplayMetrics displayMetrics,
            @NonNull CompassRadiusMetrics radiusMetrics,
            @NonNull List<CompassRoutePoint> passedRoutePoints,
            @NonNull List<CompassRoutePoint> routePoints,
            @NonNull List<CompassRoutePoint> hintPoints,
            @NonNull CompassDestinationProjection destinationProjection
    ) {
        this(displayMetrics, radiusMetrics, passedRoutePoints, routePoints, hintPoints, destinationProjection, null);
    }

    NavCompassProjectedPointsInput(
            @NonNull CompassDisplayMetrics displayMetrics,
            @NonNull CompassRadiusMetrics radiusMetrics,
            @NonNull List<CompassRoutePoint> passedRoutePoints,
            @NonNull List<CompassRoutePoint> routePoints,
            @NonNull List<CompassRoutePoint> hintPoints,
            @NonNull CompassDestinationProjection destinationProjection,
            @Nullable CompassOrientationCue orientationCue
    ) {
        this(
                displayMetrics,
                radiusMetrics,
                passedRoutePoints,
                routePoints,
                hintPoints,
                destinationProjection,
                Collections.emptyList(),
                null,
                orientationCue
        );
    }

    NavCompassProjectedPointsInput(
            @NonNull CompassDisplayMetrics displayMetrics,
            @NonNull CompassRadiusMetrics radiusMetrics,
            @NonNull List<CompassRoutePoint> passedRoutePoints,
            @NonNull List<CompassRoutePoint> routePoints,
            @NonNull List<CompassRoutePoint> hintPoints,
            @NonNull CompassDestinationProjection destinationProjection,
            @Nullable CompassDestinationProjection routeStartApproachProjection,
            @Nullable CompassOrientationCue orientationCue
    ) {
        this(
                displayMetrics,
                radiusMetrics,
                passedRoutePoints,
                routePoints,
                hintPoints,
                destinationProjection,
                Collections.emptyList(),
                routeStartApproachProjection,
                orientationCue
        );
    }

    NavCompassProjectedPointsInput(
            @NonNull CompassDisplayMetrics displayMetrics,
            @NonNull CompassRadiusMetrics radiusMetrics,
            @NonNull List<CompassRoutePoint> passedRoutePoints,
            @NonNull List<CompassRoutePoint> routePoints,
            @NonNull List<CompassRoutePoint> hintPoints,
            @NonNull CompassDestinationProjection destinationProjection,
            @NonNull List<CompassBlockedArea> blockedAreas,
            @Nullable CompassDestinationProjection routeStartApproachProjection,
            @Nullable CompassOrientationCue orientationCue
    ) {
        this.displayMetrics = displayMetrics;
        this.radiusMetrics = radiusMetrics;
        this.passedRoutePoints = passedRoutePoints;
        this.routePoints = routePoints;
        this.hintPoints = hintPoints;
        this.blockedAreas = blockedAreas;
        this.destinationProjection = destinationProjection;
        this.routeStartApproachProjection = routeStartApproachProjection;
        this.orientationCue = orientationCue;
    }
}

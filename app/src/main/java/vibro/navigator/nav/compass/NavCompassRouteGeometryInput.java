package vibro.navigator.nav.compass;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

final class NavCompassRouteGeometryInput {
    @NonNull
    public final CompassDisplayMetrics displayMetrics;
    @NonNull
    public final CompassRadiusMetrics radiusMetrics;
    @NonNull
    public final CompassRouteGeometry routeGeometry;
    public final double currentLatitude;
    public final double currentLongitude;
    public final double alongTrackMeters;
    @NonNull
    public final CompassDestinationProjection destinationProjection;
    @NonNull
    public final List<CompassBlockedArea> blockedAreas;
    @Nullable
    public final CompassDestinationProjection routeStartApproachProjection;
    @Nullable
    public final CompassOrientationCue orientationCue;

    NavCompassRouteGeometryInput(
            @NonNull CompassDisplayMetrics displayMetrics,
            @NonNull CompassRadiusMetrics radiusMetrics,
            @NonNull CompassRouteGeometry routeGeometry,
            double currentLatitude,
            double currentLongitude,
            double alongTrackMeters,
            @NonNull CompassDestinationProjection destinationProjection
    ) {
        this(
                displayMetrics,
                radiusMetrics,
                routeGeometry,
                currentLatitude,
                currentLongitude,
                alongTrackMeters,
                destinationProjection,
                Collections.emptyList(),
                null,
                null
        );
    }

    NavCompassRouteGeometryInput(
            @NonNull CompassDisplayMetrics displayMetrics,
            @NonNull CompassRadiusMetrics radiusMetrics,
            @NonNull CompassRouteGeometry routeGeometry,
            double currentLatitude,
            double currentLongitude,
            double alongTrackMeters,
            @NonNull CompassDestinationProjection destinationProjection,
            @Nullable CompassOrientationCue orientationCue
    ) {
        this(
                displayMetrics,
                radiusMetrics,
                routeGeometry,
                currentLatitude,
                currentLongitude,
                alongTrackMeters,
                destinationProjection,
                Collections.emptyList(),
                null,
                orientationCue
        );
    }

    NavCompassRouteGeometryInput(
            @NonNull CompassDisplayMetrics displayMetrics,
            @NonNull CompassRadiusMetrics radiusMetrics,
            @NonNull CompassRouteGeometry routeGeometry,
            double currentLatitude,
            double currentLongitude,
            double alongTrackMeters,
            @NonNull CompassDestinationProjection destinationProjection,
            @Nullable CompassDestinationProjection routeStartApproachProjection,
            @Nullable CompassOrientationCue orientationCue
    ) {
        this(
                displayMetrics,
                radiusMetrics,
                routeGeometry,
                currentLatitude,
                currentLongitude,
                alongTrackMeters,
                destinationProjection,
                Collections.emptyList(),
                routeStartApproachProjection,
                orientationCue
        );
    }

    NavCompassRouteGeometryInput(
            @NonNull CompassDisplayMetrics displayMetrics,
            @NonNull CompassRadiusMetrics radiusMetrics,
            @NonNull CompassRouteGeometry routeGeometry,
            double currentLatitude,
            double currentLongitude,
            double alongTrackMeters,
            @NonNull CompassDestinationProjection destinationProjection,
            @NonNull List<CompassBlockedArea> blockedAreas,
            @Nullable CompassDestinationProjection routeStartApproachProjection,
            @Nullable CompassOrientationCue orientationCue
    ) {
        this.displayMetrics = displayMetrics;
        this.radiusMetrics = radiusMetrics;
        this.routeGeometry = routeGeometry;
        this.currentLatitude = currentLatitude;
        this.currentLongitude = currentLongitude;
        this.alongTrackMeters = alongTrackMeters;
        this.destinationProjection = destinationProjection;
        this.blockedAreas = blockedAreas;
        this.routeStartApproachProjection = routeStartApproachProjection;
        this.orientationCue = orientationCue;
    }
}

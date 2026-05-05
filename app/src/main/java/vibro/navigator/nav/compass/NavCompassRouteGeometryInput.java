package vibro.navigator.nav.compass;

import androidx.annotation.NonNull;

final class NavCompassRouteGeometryInput {
    @NonNull
    public final CompassDisplayMetrics displayMetrics;
    @NonNull
    public final CompassRadiusMetrics radiusMetrics;
    @NonNull
    public final CompassRouteGeometry routeGeometry;
    public final double currentLatitude;
    public final double currentLongitude;
    public final int passedRouteSamplePointCount;
    @NonNull
    public final CompassDestinationProjection destinationProjection;

    NavCompassRouteGeometryInput(
            @NonNull CompassDisplayMetrics displayMetrics,
            @NonNull CompassRadiusMetrics radiusMetrics,
            @NonNull CompassRouteGeometry routeGeometry,
            double currentLatitude,
            double currentLongitude,
            int passedRouteSamplePointCount,
            @NonNull CompassDestinationProjection destinationProjection
    ) {
        this.displayMetrics = displayMetrics;
        this.radiusMetrics = radiusMetrics;
        this.routeGeometry = routeGeometry;
        this.currentLatitude = currentLatitude;
        this.currentLongitude = currentLongitude;
        this.passedRouteSamplePointCount = passedRouteSamplePointCount;
        this.destinationProjection = destinationProjection;
    }
}

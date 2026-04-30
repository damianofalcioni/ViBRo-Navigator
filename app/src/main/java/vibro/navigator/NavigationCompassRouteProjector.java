package vibro.navigator;

import androidx.annotation.NonNull;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.NavCompassState;

final class NavigationCompassRouteProjector {

    private NavigationCompassRouteProjector() {
    }

    static void projectHeadingUp(
            float eastMeters,
            float northMeters,
            float headingDegrees,
            @NonNull NavigationRoutePathRenderer.PlotPoint out
    ) {
        double radians = Math.toRadians(headingDegrees);
        float rotatedEast = (float) (eastMeters * Math.cos(radians) - northMeters * Math.sin(radians));
        float rotatedNorth = (float) (eastMeters * Math.sin(radians) + northMeters * Math.cos(radians));
        out.set(rotatedEast, rotatedNorth);
    }

    static void projectRoutePoint(
            @NonNull NavCompassState state,
            @NonNull LatLon point,
            float headingDegrees,
            @NonNull NavigationRoutePathRenderer.PlotPoint out
    ) {
        float eastMeters = (float) GeoMath.eastMeters(
                state.currentLatitude(),
                state.currentLongitude(),
                point.lat,
                point.lon
        );
        float northMeters = (float) GeoMath.northMeters(state.currentLatitude(), point.lat);
        projectHeadingUp(eastMeters, northMeters, headingDegrees, out);
    }
}

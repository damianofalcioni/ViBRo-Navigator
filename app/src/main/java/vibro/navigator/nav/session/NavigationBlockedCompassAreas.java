package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.brouter.NogoPoint;
import vibro.navigator.geo.GeoMath;
import vibro.navigator.nav.compass.CompassBlockedArea;
import vibro.navigator.nav.location.NavigationLocation;

final class NavigationBlockedCompassAreas {
    private NavigationBlockedCompassAreas() {
    }

    @NonNull
    static List<CompassBlockedArea> project(
            @Nullable NavigationLocation currentLocation,
            @NonNull List<NogoPoint> blockedPoints
    ) {
        if (currentLocation == null || blockedPoints.isEmpty()) {
            return Collections.emptyList();
        }
        List<CompassBlockedArea> areas = new ArrayList<>(blockedPoints.size());
        double currentLat = currentLocation.getLatitude();
        double currentLon = currentLocation.getLongitude();
        for (NogoPoint point : blockedPoints) {
            CompassBlockedArea area = project(currentLat, currentLon, point);
            if (area != null) {
                areas.add(area);
            }
        }
        return areas.isEmpty() ? Collections.emptyList() : areas;
    }

    @Nullable
    private static CompassBlockedArea project(double currentLat, double currentLon, @NonNull NogoPoint point) {
        if (!Double.isFinite(point.lat)
                || !Double.isFinite(point.lon)
                || !Double.isFinite(point.radiusMeters)
                || point.radiusMeters <= 0.0) {
            return null;
        }
        return new CompassBlockedArea(
                (float) GeoMath.eastMeters(currentLat, currentLon, point.lat, point.lon),
                (float) GeoMath.northMeters(currentLat, point.lat),
                (float) point.radiusMeters
        );
    }
}

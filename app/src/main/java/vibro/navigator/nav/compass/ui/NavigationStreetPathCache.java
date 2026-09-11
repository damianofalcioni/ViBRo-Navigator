package vibro.navigator.nav.compass.ui;

import android.graphics.Path;

import vibro.navigator.nav.compass.CompassStreetOverlay;
import vibro.navigator.nav.compass.CompassStreetSegment;

final class NavigationStreetPathCache {
    static final float DRAW_PADDING_METERS = 24f;
    private Path path = new Path();
    private CompassStreetOverlay overlay;
    private double latitude;
    private double longitude;
    private float radius;
    private float scale;

    Path pathFor(CompassStreetOverlay streets, double lat, double lon, float visibleRadius, float pixelsPerMeter) {
        if (matches(streets, lat, lon, visibleRadius, pixelsPerMeter)) {
            return path;
        }
        overlay = streets;
        latitude = lat;
        longitude = lon;
        radius = visibleRadius;
        scale = pixelsPerMeter;
        path = new Path();
        NavigationStreetPathBuilder builder = new NavigationStreetPathBuilder(
                path, lat, lon, visibleRadius, DRAW_PADDING_METERS, pixelsPerMeter
        );
        for (CompassStreetSegment segment : streets.segments) {
            builder.append(segment);
        }
        return path;
    }

    void clear() {
        if (overlay != null) {
            overlay = null;
            path = new Path();
        }
    }

    private boolean matches(CompassStreetOverlay streets, double lat, double lon, float visibleRadius, float pixelsPerMeter) {
        return overlay == streets && latitude == lat && longitude == lon
                && radius == visibleRadius && scale == pixelsPerMeter;
    }
}

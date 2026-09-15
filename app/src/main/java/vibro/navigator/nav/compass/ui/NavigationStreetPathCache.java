package vibro.navigator.nav.compass.ui;

import android.graphics.Path;

import vibro.navigator.nav.compass.CompassStreetCategory;
import vibro.navigator.nav.compass.CompassStreetOverlay;
import vibro.navigator.nav.compass.CompassStreetSegment;

final class NavigationStreetPathCache {
    static final float DRAW_PADDING_METERS = 24f;
    private NavigationStreetPaths paths = new NavigationStreetPaths();
    private CompassStreetOverlay overlay;
    private double latitude;
    private double longitude;
    private float radius;
    private float scale;

    NavigationStreetPaths pathsFor(
            CompassStreetOverlay streets,
            double lat,
            double lon,
            float visibleRadius,
            float pixelsPerMeter
    ) {
        if (matches(streets, lat, lon, visibleRadius, pixelsPerMeter)) {
            return paths;
        }
        overlay = streets;
        latitude = lat;
        longitude = lon;
        radius = visibleRadius;
        scale = pixelsPerMeter;
        paths = new NavigationStreetPaths();
        NavigationStreetPathBuilder[] builders = buildersFor(paths, lat, lon, visibleRadius, pixelsPerMeter);
        for (CompassStreetSegment segment : streets.segments) {
            builders[segment.type.category().ordinal()].append(segment);
        }
        return paths;
    }

    void clear() {
        if (overlay != null) {
            overlay = null;
            paths = new NavigationStreetPaths();
        }
    }

    private static NavigationStreetPathBuilder[] buildersFor(
            NavigationStreetPaths paths,
            double lat,
            double lon,
            float visibleRadius,
            float pixelsPerMeter
    ) {
        CompassStreetCategory[] categories = CompassStreetCategory.values();
        NavigationStreetPathBuilder[] builders = new NavigationStreetPathBuilder[categories.length];
        for (CompassStreetCategory category : categories) {
            Path path = paths.pathFor(category);
            builders[category.ordinal()] = new NavigationStreetPathBuilder(
                    path,
                    lat,
                    lon,
                    visibleRadius,
                    DRAW_PADDING_METERS,
                    pixelsPerMeter
            );
        }
        return builders;
    }

    private boolean matches(
            CompassStreetOverlay streets,
            double lat,
            double lon,
            float visibleRadius,
            float pixelsPerMeter
    ) {
        return overlay == streets && latitude == lat && longitude == lon
                && radius == visibleRadius && scale == pixelsPerMeter;
    }
}

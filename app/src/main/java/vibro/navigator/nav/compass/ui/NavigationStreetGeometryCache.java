package vibro.navigator.nav.compass.ui;

import vibro.navigator.nav.compass.CompassStreetCategory;
import vibro.navigator.nav.compass.CompassStreetOverlay;
import vibro.navigator.nav.compass.CompassStreetSegment;

final class NavigationStreetGeometryCache {
    static final float DRAW_PADDING_METERS = 24f;
    private NavigationStreetBatches batches = new NavigationStreetBatches();
    private CompassStreetOverlay overlay;
    private double latitude;
    private double longitude;
    private float radius;
    private float scale;

    NavigationStreetBatches batchesFor(
            CompassStreetOverlay streets,
            double lat,
            double lon,
            float visibleRadius,
            float pixelsPerMeter
    ) {
        if (matches(streets, lat, lon, visibleRadius, pixelsPerMeter)) {
            return batches;
        }
        overlay = streets;
        latitude = lat;
        longitude = lon;
        radius = visibleRadius;
        scale = pixelsPerMeter;
        batches = new NavigationStreetBatches();
        NavigationStreetGeometryBuilder[] builders = buildersFor(batches, lat, lon, visibleRadius, pixelsPerMeter);
        for (CompassStreetSegment segment : streets.segments) {
            builders[segment.type.category().ordinal()].append(segment);
        }
        return batches;
    }

    void clear() {
        if (overlay != null) {
            overlay = null;
            batches = new NavigationStreetBatches();
        }
    }

    private static NavigationStreetGeometryBuilder[] buildersFor(
            NavigationStreetBatches batches,
            double lat,
            double lon,
            float visibleRadius,
            float pixelsPerMeter
    ) {
        CompassStreetCategory[] categories = CompassStreetCategory.values();
        NavigationStreetGeometryBuilder[] builders = new NavigationStreetGeometryBuilder[categories.length];
        for (CompassStreetCategory category : categories) {
            builders[category.ordinal()] = new NavigationStreetGeometryBuilder(
                    batches.geometryFor(category),
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

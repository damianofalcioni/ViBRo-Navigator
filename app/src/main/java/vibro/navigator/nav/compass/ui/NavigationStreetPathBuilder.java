package vibro.navigator.nav.compass.ui;

import android.graphics.Path;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassStreetSegment;

/** Builds north-up pixel geometry once; heading changes are handled by the canvas transform. */
final class NavigationStreetPathBuilder {
    private static final float MIN_RENDERED_POINT_DISTANCE_PIXELS = 0.75f;
    private static final float MIN_RENDERED_POINT_DISTANCE_SQUARED =
            MIN_RENDERED_POINT_DISTANCE_PIXELS * MIN_RENDERED_POINT_DISTANCE_PIXELS;
    private final Path path;
    private final double latitude;
    private final double longitude;
    private final double eastMetersPerDegree;
    private final float radius;
    private final float padding;
    private final float scale;
    private final RouteDrawingMath.ClippedSegment clipped = new RouteDrawingMath.ClippedSegment();
    private boolean active;
    private boolean hasPendingPoint;
    private float lastX;
    private float lastY;
    private float currentX;
    private float currentY;

    NavigationStreetPathBuilder(Path path, double latitude, double longitude, float radius, float padding, float scale) {
        this.path = path;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
        this.padding = padding;
        this.scale = scale;
        eastMetersPerDegree = 111_320d * Math.cos(Math.toRadians(latitude));
    }

    void append(CompassStreetSegment segment) {
        flushPendingPoint();
        active = false;
        hasPendingPoint = false;
        if (segment.points.size() < 2) {
            return;
        }
        LatLon first = segment.points.get(0);
        float previousX = east(first);
        float previousY = north(first);
        for (int i = 1; i < segment.points.size(); i++) {
            LatLon point = segment.points.get(i);
            float x = east(point);
            float y = north(point);
            appendLine(previousX, previousY, x, y);
            previousX = x;
            previousY = y;
        }
        flushPendingPoint();
    }

    private void appendLine(float startX, float startY, float endX, float endY) {
        if (!RouteDrawingMath.isRouteSegmentNearVisibleArea(startX, startY, endX, endY, radius, padding)
                || !RouteDrawingMath.clipSegmentToBounds(
                        startX, startY, endX, endY, (radius + padding) * 1.414214f, clipped
                )) {
            flushPendingPoint();
            active = false;
            hasPendingPoint = false;
            return;
        }
        // This larger north-up box contains the drawing box at every heading.
        // The renderer applies the exact screen-aligned clip before rotating.
        float x = clipped.startX * scale;
        float y = -clipped.startY * scale;
        if (!active || x != currentX || y != currentY) {
            flushPendingPoint();
            path.moveTo(x, y);
            lastX = x;
            lastY = y;
            active = true;
        }
        currentX = clipped.endX * scale;
        currentY = -clipped.endY * scale;
        if (distanceSquared(currentX, currentY, lastX, lastY) >= MIN_RENDERED_POINT_DISTANCE_SQUARED) {
            path.lineTo(currentX, currentY);
            lastX = currentX;
            lastY = currentY;
            hasPendingPoint = false;
        } else {
            hasPendingPoint = true;
        }
    }

    private void flushPendingPoint() {
        if (!active || !hasPendingPoint) {
            return;
        }
        if (distanceSquared(currentX, currentY, lastX, lastY) > 0f) {
            path.lineTo(currentX, currentY);
            lastX = currentX;
            lastY = currentY;
        }
        hasPendingPoint = false;
    }

    private static float distanceSquared(float firstX, float firstY, float secondX, float secondY) {
        float dx = firstX - secondX;
        float dy = firstY - secondY;
        return dx * dx + dy * dy;
    }

    private float east(LatLon point) {
        return (float) ((point.lon - longitude) * eastMetersPerDegree);
    }

    private float north(LatLon point) {
        return (float) ((point.lat - latitude) * 111_320d);
    }
}

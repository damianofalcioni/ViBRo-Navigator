package vibro.navigator;

final class RouteDrawingMath {
    private RouteDrawingMath() {
    }

    static boolean isRouteSegmentNearVisibleArea(
            float startX,
            float startY,
            float endX,
            float endY,
            float visibleRadiusMeters,
            float paddingMeters
    ) {
        if (!Float.isFinite(startX)
                || !Float.isFinite(startY)
                || !Float.isFinite(endX)
                || !Float.isFinite(endY)
                || !Float.isFinite(visibleRadiusMeters)
                || visibleRadiusMeters <= 0f) {
            return false;
        }
        float drawRadiusMeters = visibleRadiusMeters + Math.max(0f, paddingMeters);
        if (Math.hypot(startX, startY) <= drawRadiusMeters
                || Math.hypot(endX, endY) <= drawRadiusMeters) {
            return true;
        }
        float dx = endX - startX;
        float dy = endY - startY;
        float lengthSquared = dx * dx + dy * dy;
        if (lengthSquared <= 0f || !Float.isFinite(lengthSquared)) {
            return false;
        }
        float t = -((startX * dx) + (startY * dy)) / lengthSquared;
        t = Math.max(0f, Math.min(1f, t));
        float closestX = startX + dx * t;
        float closestY = startY + dy * t;
        return Math.hypot(closestX, closestY) <= drawRadiusMeters;
    }

    static float clampRouteCoordinate(float coordinateMeters, float drawBoundsMeters) {
        if (!Float.isFinite(coordinateMeters)) {
            return 0f;
        }
        if (!Float.isFinite(drawBoundsMeters) || drawBoundsMeters <= 0f) {
            return coordinateMeters;
        }
        return Math.max(-drawBoundsMeters, Math.min(drawBoundsMeters, coordinateMeters));
    }
}

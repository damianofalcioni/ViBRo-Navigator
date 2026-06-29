package vibro.navigator.nav.compass.ui;

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
        if (!hasFiniteSegmentEndpoints(startX, startY, endX, endY)
                || !hasUsableVisibleRadius(visibleRadiusMeters)) {
            return false;
        }
        float drawRadiusMeters = visibleRadiusMeters + Math.max(0f, paddingMeters);
        if (isPointWithinRadius(startX, startY, drawRadiusMeters)
                || isPointWithinRadius(endX, endY, drawRadiusMeters)) {
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

    private static boolean hasFiniteSegmentEndpoints(float startX, float startY, float endX, float endY) {
        return Float.isFinite(startX)
                && Float.isFinite(startY)
                && Float.isFinite(endX)
                && Float.isFinite(endY);
    }

    private static boolean hasUsableVisibleRadius(float visibleRadiusMeters) {
        return Float.isFinite(visibleRadiusMeters) && visibleRadiusMeters > 0f;
    }

    private static boolean isPointWithinRadius(float x, float y, float radiusMeters) {
        return Math.hypot(x, y) <= radiusMeters;
    }

    static boolean clipSegmentToBounds(
            float startX,
            float startY,
            float endX,
            float endY,
            float drawBoundsMeters,
            ClippedSegment out
    ) {
        if (!hasFiniteSegmentEndpoints(startX, startY, endX, endY)) {
            return false;
        }
        if (!Float.isFinite(drawBoundsMeters) || drawBoundsMeters <= 0f) {
            out.set(startX, startY, endX, endY);
            return true;
        }
        SegmentClip clip = new SegmentClip(startX, startY, endX, endY, drawBoundsMeters);
        if (!clip.apply()) {
            return false;
        }
        out.set(
                startX + clip.dx * clip.startFraction,
                startY + clip.dy * clip.startFraction,
                startX + clip.dx * clip.endFraction,
                startY + clip.dy * clip.endFraction
        );
        return true;
    }

    static final class ClippedSegment {
        float startX;
        float startY;
        float endX;
        float endY;

        void set(float startX, float startY, float endX, float endY) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
        }
    }

    private static final class SegmentClip {
        private final float startX;
        private final float startY;
        private final float bounds;
        private final float dx;
        private final float dy;
        private float startFraction;
        private float endFraction;

        SegmentClip(float startX, float startY, float endX, float endY, float bounds) {
            this.startX = startX;
            this.startY = startY;
            this.bounds = bounds;
            dx = endX - startX;
            dy = endY - startY;
            startFraction = 0f;
            endFraction = 1f;
        }

        boolean apply() {
            return clip(-dx, startX + bounds)
                    && clip(dx, bounds - startX)
                    && clip(-dy, startY + bounds)
                    && clip(dy, bounds - startY);
        }

        private boolean clip(float direction, float distanceToEdge) {
            if (direction == 0f) {
                return distanceToEdge >= 0f;
            }
            float fraction = distanceToEdge / direction;
            if (direction < 0f) {
                if (fraction > endFraction) {
                    return false;
                }
                startFraction = Math.max(startFraction, fraction);
                return true;
            }
            if (fraction < startFraction) {
                return false;
            }
            endFraction = Math.min(endFraction, fraction);
            return true;
        }
    }
}

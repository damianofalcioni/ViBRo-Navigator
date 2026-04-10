package com.vibenavigator.nav;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vibenavigator.brouter.NogoPoint;
import com.vibenavigator.geo.GeoMath;
import com.vibenavigator.geo.LatLon;
import com.vibenavigator.nav.route.PolylineIndex;

import java.util.ArrayList;
import java.util.List;

final class NavigationBlockedRouteState {

    private static final double BLOCKED_ROUTE_FIRST_POINT_OFFSET_METERS = 20.0;
    private static final double BLOCKED_ROUTE_POINT_STEP_METERS = 18.0;
    private static final double BLOCKED_RADIUS_BASE_METERS = 12.0;
    private static final double BLOCKED_RADIUS_STEP_METERS = 6.0;
    private static final double BLOCKED_RADIUS_MAX_METERS = 30.0;
    private static final int BLOCKED_POINT_COUNT_MAX = 3;
    private static final double BLOCKED_SAME_AREA_METERS = 35.0;
    private static final double BLOCKED_QUICK_REPEAT_NEARBY_METERS = 75.0;
    private static final long BLOCKED_QUICK_REPEAT_WINDOW_MS = 15_000L;

    private final List<NogoPoint> blocked = new ArrayList<>();

    @Nullable
    private LatLon lastBlockedAreaCenter;
    private long lastBlockedAreaAtMs;
    private int lastBlockedAreaLevel;

    void reset() {
        blocked.clear();
        lastBlockedAreaCenter = null;
        lastBlockedAreaAtMs = 0L;
        lastBlockedAreaLevel = 0;
    }

    @NonNull
    List<NogoPoint> copyBlockedPoints() {
        return new ArrayList<>(blocked);
    }

    @NonNull
    List<NogoPoint> addBlockedPointsAhead(
            @NonNull PolylineIndex polylineIndex,
            double alongTrackMeters,
            long nowMs
    ) {
        List<NogoPoint> added = new ArrayList<>();
        LatLon anchor = polylineIndex.pointAtDistance(alongTrackMeters + BLOCKED_ROUTE_FIRST_POINT_OFFSET_METERS);
        if (anchor == null) {
            return added;
        }

        int level = nextBlockedAreaLevel(anchor, nowMs);
        double radiusMeters = blockedRadiusForLevel(level);
        int pointCount = blockedPointCountForLevel(level);
        replaceNearbyBlockedPoints(anchor);

        for (int i = 0; i < pointCount; i++) {
            double distance = alongTrackMeters
                    + BLOCKED_ROUTE_FIRST_POINT_OFFSET_METERS
                    + (i * BLOCKED_ROUTE_POINT_STEP_METERS);
            LatLon point = polylineIndex.pointAtDistance(distance);
            if (point == null) {
                continue;
            }
            NogoPoint nogo = new NogoPoint(point.lat, point.lon, radiusMeters);
            blocked.add(nogo);
            added.add(nogo);
        }

        lastBlockedAreaCenter = anchor;
        lastBlockedAreaAtMs = nowMs;
        lastBlockedAreaLevel = level;
        return added;
    }

    private int nextBlockedAreaLevel(@NonNull LatLon anchor, long nowMs) {
        if (lastBlockedAreaCenter == null || lastBlockedAreaLevel <= 0) {
            return 1;
        }
        double distanceMeters = GeoMath.distanceMeters(
                lastBlockedAreaCenter.lat,
                lastBlockedAreaCenter.lon,
                anchor.lat,
                anchor.lon
        );
        boolean sameArea = distanceMeters <= BLOCKED_SAME_AREA_METERS;
        boolean quickNearbyRepeat = nowMs - lastBlockedAreaAtMs <= BLOCKED_QUICK_REPEAT_WINDOW_MS
                && distanceMeters <= BLOCKED_QUICK_REPEAT_NEARBY_METERS;
        if (sameArea || quickNearbyRepeat) {
            return Math.min(BLOCKED_POINT_COUNT_MAX, lastBlockedAreaLevel + 1);
        }
        return 1;
    }

    private void replaceNearbyBlockedPoints(@NonNull LatLon anchor) {
        for (int i = blocked.size() - 1; i >= 0; i--) {
            NogoPoint existing = blocked.get(i);
            if (GeoMath.distanceMeters(existing.lat, existing.lon, anchor.lat, anchor.lon)
                    <= BLOCKED_QUICK_REPEAT_NEARBY_METERS) {
                blocked.remove(i);
            }
        }
    }

    private int blockedPointCountForLevel(int level) {
        return Math.max(1, Math.min(BLOCKED_POINT_COUNT_MAX, level));
    }

    private double blockedRadiusForLevel(int level) {
        return Math.min(
                BLOCKED_RADIUS_MAX_METERS,
                BLOCKED_RADIUS_BASE_METERS + ((Math.max(1, level) - 1) * BLOCKED_RADIUS_STEP_METERS)
        );
    }
}

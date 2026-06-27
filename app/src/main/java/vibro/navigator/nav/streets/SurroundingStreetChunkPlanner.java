package vibro.navigator.nav.streets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.location.NavigationLocation;

final class SurroundingStreetChunkPlanner {
    private static final double ROUTE_SAMPLE_STEP_METERS = 420.0d;
    private static final float PREFETCH_VISIBLE_RADIUS_MULTIPLIER = 10f;
    private static final float MAX_PREFETCH_DISTANCE_METERS = 20_000f;
    private static final int MAX_DISPLAY_CHUNKS = 32;
    private static final int MAX_PREFETCH_CHUNKS = 64;

    @NonNull
    private final SurroundingStreetViewportPolicy viewportPolicy = new SurroundingStreetViewportPolicy();

    boolean shouldShow(@Nullable NavCompassState compassState) {
        return viewportPolicy.shouldShow(compassState);
    }

    @NonNull
    SurroundingStreetChunkSelection select(
            @NonNull NavCompassState compassState,
            @Nullable NavigationLocation location
    ) {
        LatLon current = currentPoint(location);
        if (current == null) {
            return SurroundingStreetChunkSelection.EMPTY;
        }
        List<SurroundingStreetChunkKey> displayKeys = selectDisplayKeys(compassState, current);
        List<SurroundingStreetChunkKey> prefetchKeys = selectPrefetchKeys(compassState, current);
        return new SurroundingStreetChunkSelection(displayKeys, prefetchKeys);
    }

    @NonNull
    private List<SurroundingStreetChunkKey> selectDisplayKeys(
            @NonNull NavCompassState compassState,
            @NonNull LatLon current
    ) {
        Set<SurroundingStreetChunkKey> keys = new LinkedHashSet<>();
        keys.add(SurroundingStreetChunkKey.from(current));
        addRouteKeys(compassState, current, compassState.radiusState.visibleRadiusMeters, keys, MAX_DISPLAY_CHUNKS);
        return new ArrayList<>(keys);
    }

    @NonNull
    private List<SurroundingStreetChunkKey> selectPrefetchKeys(
            @NonNull NavCompassState compassState,
            @NonNull LatLon current
    ) {
        Set<SurroundingStreetChunkKey> keys = new LinkedHashSet<>();
        keys.add(SurroundingStreetChunkKey.from(current));
        addRouteKeys(compassState, current, prefetchDistanceMeters(compassState), keys, MAX_PREFETCH_CHUNKS);
        return new ArrayList<>(keys);
    }

    private void addRouteKeys(
            @NonNull NavCompassState compassState,
            @NonNull LatLon current,
            double horizonMeters,
            @NonNull Set<SurroundingStreetChunkKey> keys,
            int maxKeys
    ) {
        if (!compassState.hasRouteGeometry() || horizonMeters <= 0.0d) {
            return;
        }
        addRouteSegments(compassState, current, horizonMeters, keys, maxKeys);
    }

    private void addRouteSegments(
            @NonNull NavCompassState compassState,
            @NonNull LatLon current,
            double horizonMeters,
            @NonNull Set<SurroundingStreetChunkKey> keys,
            int maxKeys
    ) {
        LatLon previous = current;
        double remainingMeters = horizonMeters;
        int startIndex = routeStartIndex(compassState);
        for (int i = startIndex; i < compassState.routeSamplePointCount() && remainingMeters > 0.0d; i++) {
            LatLon point = compassState.routeSamplePointAt(i);
            if (point == null) {
                continue;
            }
            remainingMeters -= addSegmentKeys(previous, point, remainingMeters, keys, maxKeys);
            previous = point;
            if (keys.size() >= maxKeys) {
                return;
            }
        }
    }

    private double addSegmentKeys(
            @NonNull LatLon start,
            @NonNull LatLon end,
            double remainingMeters,
            @NonNull Set<SurroundingStreetChunkKey> keys,
            int maxKeys
    ) {
        double segmentMeters = GeoMath.distanceMeters(start.lat, start.lon, end.lat, end.lon);
        if (segmentMeters <= 0.0d) {
            keys.add(SurroundingStreetChunkKey.from(end));
            return 0.0d;
        }
        double usedMeters = Math.min(segmentMeters, remainingMeters);
        addInterpolatedSegmentKeys(start, end, segmentMeters, usedMeters, keys, maxKeys);
        return usedMeters;
    }

    private void addInterpolatedSegmentKeys(
            @NonNull LatLon start,
            @NonNull LatLon end,
            double segmentMeters,
            double usedMeters,
            @NonNull Set<SurroundingStreetChunkKey> keys,
            int maxKeys
    ) {
        int steps = Math.max(1, (int) Math.ceil(usedMeters / ROUTE_SAMPLE_STEP_METERS));
        for (int step = 1; step <= steps && keys.size() < maxKeys; step++) {
            double distanceMeters = Math.min(usedMeters, step * ROUTE_SAMPLE_STEP_METERS);
            keys.add(SurroundingStreetChunkKey.from(interpolate(start, end, distanceMeters / segmentMeters)));
        }
    }

    private float prefetchDistanceMeters(@NonNull NavCompassState compassState) {
        float visibleRadiusMeters = compassState.radiusState.sixtySecondVisibleRadiusMeters;
        if (!Float.isFinite(visibleRadiusMeters) || visibleRadiusMeters <= 0f) {
            visibleRadiusMeters = compassState.radiusState.visibleRadiusMeters;
        }
        return Math.min(MAX_PREFETCH_DISTANCE_METERS, visibleRadiusMeters * PREFETCH_VISIBLE_RADIUS_MULTIPLIER);
    }

    private static int routeStartIndex(@NonNull NavCompassState compassState) {
        return Math.max(0, Math.min(
                compassState.passedRouteSamplePointCount(),
                Math.max(0, compassState.routeSamplePointCount() - 1)
        ));
    }

    @NonNull
    private static LatLon interpolate(@NonNull LatLon start, @NonNull LatLon end, double ratio) {
        double safeRatio = Math.max(0.0d, Math.min(1.0d, ratio));
        return new LatLon(
                start.lat + (end.lat - start.lat) * safeRatio,
                start.lon + (end.lon - start.lon) * safeRatio
        );
    }

    @Nullable
    private static LatLon currentPoint(@Nullable NavigationLocation location) {
        if (location == null || !LatLon.isValidCoordinate(location.getLatitude(), location.getLongitude())) {
            return null;
        }
        return new LatLon(location.getLatitude(), location.getLongitude());
    }
}

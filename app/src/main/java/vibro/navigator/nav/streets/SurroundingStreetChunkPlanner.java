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
    private static final float CACHE_RADIUS_MULTIPLIER = 2f;
    private static final float MAX_EXTRACTION_VISIBLE_RADIUS_METERS = 2_500f;
    private static final float PREFETCH_VISIBLE_RADIUS_MULTIPLIER = 10f;
    private static final float MAX_PREFETCH_DISTANCE_METERS = 20_000f;
    private static final int MAX_DISPLAY_CHUNKS = 64;
    private static final int MAX_PREFETCH_CHUNKS = 128;

    @NonNull
    private final SurroundingStreetViewportPolicy viewportPolicy = new SurroundingStreetViewportPolicy();
    @NonNull
    private final SurroundingStreetAreaChunkSelector areaSelector = new SurroundingStreetAreaChunkSelector();

    boolean shouldShow(@Nullable NavCompassState compassState) {
        return viewportPolicy.shouldShow(compassState);
    }

    boolean hasSameSelectionInputs(
            @Nullable NavCompassState previous,
            @NonNull NavCompassState current
    ) {
        return previous != null
                && previous.routeGeometry() == current.routeGeometry()
                && previous.passedRouteSamplePointCount() == current.passedRouteSamplePointCount()
                && sameFloat(
                        previous.radiusState.visibleRadiusMeters,
                        current.radiusState.visibleRadiusMeters
                )
                && sameFloat(
                        previous.radiusState.movingScaleVisibleRadiusMeters,
                        current.radiusState.movingScaleVisibleRadiusMeters
                )
                && sameFloat(
                        previous.displayMode.referenceSpeedMps,
                        current.displayMode.referenceSpeedMps
                );
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
        areaSelector.addAreaKeys(current, cacheRadiusMeters(compassState), keys, MAX_DISPLAY_CHUNKS);
        return new ArrayList<>(keys);
    }

    @NonNull
    private List<SurroundingStreetChunkKey> selectPrefetchKeys(
            @NonNull NavCompassState compassState,
            @NonNull LatLon current
    ) {
        Set<SurroundingStreetChunkKey> keys = new LinkedHashSet<>();
        keys.add(SurroundingStreetChunkKey.from(current));
        areaSelector.addAreaKeys(current, cacheRadiusMeters(compassState), keys, MAX_PREFETCH_CHUNKS);
        addRouteKeys(
                compassState,
                current,
                prefetchDistanceMeters(compassState),
                cacheRadiusMeters(compassState),
                keys,
                MAX_PREFETCH_CHUNKS
        );
        return new ArrayList<>(keys);
    }

    private void addRouteKeys(
            @NonNull NavCompassState compassState,
            @NonNull LatLon current,
            double horizonMeters,
            float cacheRadiusMeters,
            @NonNull Set<SurroundingStreetChunkKey> keys,
            int maxKeys
    ) {
        if (!compassState.hasRouteGeometry() || horizonMeters <= 0.0d) {
            return;
        }
        addRouteSegments(compassState, current, horizonMeters, cacheRadiusMeters, keys, maxKeys);
    }

    private void addRouteSegments(
            @NonNull NavCompassState compassState,
            @NonNull LatLon current,
            double horizonMeters,
            float cacheRadiusMeters,
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
            remainingMeters -= addSegmentKeys(previous, point, remainingMeters, cacheRadiusMeters, keys, maxKeys);
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
            float cacheRadiusMeters,
            @NonNull Set<SurroundingStreetChunkKey> keys,
            int maxKeys
    ) {
        double segmentMeters = GeoMath.distanceMeters(start.lat, start.lon, end.lat, end.lon);
        if (segmentMeters <= 0.0d) {
            areaSelector.addAreaKeys(end, cacheRadiusMeters, keys, maxKeys);
            return 0.0d;
        }
        double usedMeters = Math.min(segmentMeters, remainingMeters);
        addInterpolatedSegmentKeys(start, end, segmentMeters, usedMeters, cacheRadiusMeters, keys, maxKeys);
        return usedMeters;
    }

    private void addInterpolatedSegmentKeys(
            @NonNull LatLon start,
            @NonNull LatLon end,
            double segmentMeters,
            double usedMeters,
            float cacheRadiusMeters,
            @NonNull Set<SurroundingStreetChunkKey> keys,
            int maxKeys
    ) {
        int steps = Math.max(1, (int) Math.ceil(usedMeters / ROUTE_SAMPLE_STEP_METERS));
        for (int step = 1; step <= steps && keys.size() < maxKeys; step++) {
            double distanceMeters = Math.min(usedMeters, step * ROUTE_SAMPLE_STEP_METERS);
            areaSelector.addAreaKeys(
                    interpolate(start, end, distanceMeters / segmentMeters),
                    cacheRadiusMeters,
                    keys,
                    maxKeys
            );
        }
    }

    private float cacheRadiusMeters(@NonNull NavCompassState compassState) {
        float visibleRadiusMeters = compassState.radiusState.visibleRadiusMeters;
        return isPositiveFinite(visibleRadiusMeters)
                ? Math.min(visibleRadiusMeters, MAX_EXTRACTION_VISIBLE_RADIUS_METERS) * CACHE_RADIUS_MULTIPLIER
                : visibleRadiusMeters;
    }

    private float prefetchDistanceMeters(@NonNull NavCompassState compassState) {
        float visibleRadiusMeters = compassState.radiusState.movingScaleVisibleRadiusMeters;
        if (!isPositiveFinite(visibleRadiusMeters)) {
            visibleRadiusMeters = compassState.radiusState.visibleRadiusMeters;
        }
        return Math.min(MAX_PREFETCH_DISTANCE_METERS, visibleRadiusMeters * PREFETCH_VISIBLE_RADIUS_MULTIPLIER);
    }

    private static boolean isPositiveFinite(float value) {
        return Float.isFinite(value) && value > 0f;
    }

    private static boolean sameFloat(float first, float second) {
        return Float.floatToIntBits(first) == Float.floatToIntBits(second);
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

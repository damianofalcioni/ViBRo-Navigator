package vibro.navigator.nav.streets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassStreetOverlay;
import vibro.navigator.nav.compass.CompassStreetSegment;

final class SurroundingStreetOverlayCache {
    private static final int MAX_CHUNKS = 240;
    private static final int MAX_CACHED_SEGMENTS = 40_000;
    private static final int MAX_CACHED_POINTS = 160_000;
    private static final double FINGERPRINT_SCALE = 1_000_000.0d;
    private static final String KEY_SEPARATOR = ":";

    @NonNull
    private final SurroundingStreetTypeFilter typeFilter = new SurroundingStreetTypeFilter();
    @NonNull
    private final SurroundingStreetSpeedBucketResolver speedBucketResolver =
            new SurroundingStreetSpeedBucketResolver();
    @NonNull
    private final LinkedHashMap<SurroundingStreetChunkKey, Entry> entries =
            new LinkedHashMap<>(16, 0.75f, true);
    private int cachedSegments;
    private int cachedPoints;
    private SurroundingStreetSpeedBucket activeSpeedBucket;

    void clear() {
        entries.clear();
        cachedSegments = 0;
        cachedPoints = 0;
        resetSpeedBucket();
    }

    void resetSpeedBucket() {
        activeSpeedBucket = null;
    }

    boolean contains(@NonNull SurroundingStreetChunkKey key) {
        return entries.containsKey(key);
    }

    @NonNull
    List<SurroundingStreetChunkKey> missing(
            @NonNull Collection<SurroundingStreetChunkKey> keys,
            int limit
    ) {
        List<SurroundingStreetChunkKey> missing = new ArrayList<>();
        for (SurroundingStreetChunkKey key : keys) {
            if (!contains(key)) {
                missing.add(key);
            }
            if (missing.size() >= limit) {
                return missing;
            }
        }
        return missing;
    }

    void put(@NonNull SurroundingStreetChunkKey key, @NonNull CompassStreetOverlay overlay) {
        removeExisting(key);
        Entry entry = Entry.from(overlay);
        entries.put(key, entry);
        cachedSegments += entry.segmentCount;
        cachedPoints += entry.pointCount;
        trimToBudget();
    }

    @NonNull
    CompassStreetOverlay overlayFor(
            @NonNull Collection<SurroundingStreetChunkKey> keys,
            int maxSegments
    ) {
        return overlayFor(keys, maxSegments, SurroundingStreetSpeedBucket.LOW);
    }

    @NonNull
    CompassStreetOverlay overlayFor(
            @NonNull Collection<SurroundingStreetChunkKey> keys,
            int maxSegments,
            float referenceSpeedMps
    ) {
        activeSpeedBucket = speedBucketResolver.resolve(referenceSpeedMps, activeSpeedBucket);
        return overlayFor(keys, maxSegments, activeSpeedBucket);
    }

    @NonNull
    CompassStreetOverlay overlayFor(
            @NonNull Collection<SurroundingStreetChunkKey> keys,
            int maxSegments,
            @NonNull SurroundingStreetSpeedBucket speedBucket
    ) {
        List<CompassStreetSegment> segments = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (SurroundingStreetChunkKey key : keys) {
            addSegments(entries.get(key), segments, seen, maxSegments, speedBucket);
            if (segments.size() >= maxSegments) {
                break;
            }
        }
        return segments.isEmpty() ? CompassStreetOverlay.EMPTY : new CompassStreetOverlay(segments);
    }

    private void addSegments(
            @Nullable Entry entry,
            @NonNull List<CompassStreetSegment> segments,
            @NonNull Set<String> seen,
            int maxSegments,
            @NonNull SurroundingStreetSpeedBucket speedBucket
    ) {
        if (entry == null || entry.overlay.isEmpty()) {
            return;
        }
        for (CompassStreetSegment segment : entry.overlay.segments) {
            if (!typeFilter.isVisible(segment, speedBucket)) {
                continue;
            }
            if (seen.add(fingerprint(segment))) {
                segments.add(segment);
            }
            if (segments.size() >= maxSegments) {
                return;
            }
        }
    }

    private void removeExisting(@NonNull SurroundingStreetChunkKey key) {
        Entry previous = entries.remove(key);
        if (previous == null) {
            return;
        }
        cachedSegments -= previous.segmentCount;
        cachedPoints -= previous.pointCount;
    }

    private void trimToBudget() {
        while (isOverBudget()) {
            Map.Entry<SurroundingStreetChunkKey, Entry> eldest = entries.entrySet().iterator().next();
            removeExisting(eldest.getKey());
        }
    }

    private boolean isOverBudget() {
        return entries.size() > MAX_CHUNKS
                || cachedSegments > MAX_CACHED_SEGMENTS
                || cachedPoints > MAX_CACHED_POINTS;
    }

    @NonNull
    private static String fingerprint(@NonNull CompassStreetSegment segment) {
        if (segment.points.isEmpty()) {
            return "";
        }
        LatLon first = segment.points.get(0);
        LatLon last = segment.points.get(segment.points.size() - 1);
        String forward = pointKey(first) + KEY_SEPARATOR + pointKey(last);
        String reverse = pointKey(last) + KEY_SEPARATOR + pointKey(first);
        String endpoints = forward.compareTo(reverse) <= 0 ? forward : reverse;
        return segment.points.size() + KEY_SEPARATOR + endpoints;
    }

    @NonNull
    private static String pointKey(@NonNull LatLon point) {
        return Math.round(point.lat * FINGERPRINT_SCALE)
                + KEY_SEPARATOR
                + Math.round(point.lon * FINGERPRINT_SCALE);
    }

    private static final class Entry {
        @NonNull
        final CompassStreetOverlay overlay;
        final int segmentCount;
        final int pointCount;

        private Entry(@NonNull CompassStreetOverlay overlay, int segmentCount, int pointCount) {
            this.overlay = overlay;
            this.segmentCount = segmentCount;
            this.pointCount = pointCount;
        }

        @NonNull
        static Entry from(@NonNull CompassStreetOverlay overlay) {
            return new Entry(overlay, overlay.segments.size(), countPoints(overlay));
        }

        private static int countPoints(@NonNull CompassStreetOverlay overlay) {
            int count = 0;
            for (CompassStreetSegment segment : overlay.segments) {
                count += segment.points.size();
            }
            return count;
        }
    }
}

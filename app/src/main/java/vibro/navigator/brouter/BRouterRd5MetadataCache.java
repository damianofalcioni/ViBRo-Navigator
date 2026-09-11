package vibro.navigator.brouter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.LinkedHashMap;

/**
 * Keeps the small, immutable parts of an rd5 index warm between neighboring chunk reads.
 * Cell payloads remain in {@link BRouterDecodedStreetCache}; this cache only avoids rereading
 * the per-degree microcache position table for an unchanged source revision.
 */
final class BRouterRd5MetadataCache {
    private static final int MAX_CACHED_FILES = 4;
    private static final int SUB_TILE_COUNT = 25;

    @NonNull
    private final LinkedHashMap<String, Entry> entries = new LinkedHashMap<>(8, 0.75f, true);

    @Nullable
    synchronized Entry get(@NonNull String revisionKey) {
        return entries.get(revisionKey);
    }

    synchronized void put(@NonNull String revisionKey, @NonNull Entry entry) {
        entries.put(revisionKey, entry);
        while (entries.size() > MAX_CACHED_FILES) {
            String eldest = entries.keySet().iterator().next();
            entries.remove(eldest);
        }
    }

    static final class Entry {
        @NonNull
        final long[] topIndex;
        final int divisor;
        @NonNull
        final String cacheRevision;
        @NonNull
        private final int[][] microCachePositions = new int[SUB_TILE_COUNT][];

        Entry(
                @NonNull long[] topIndex,
                int divisor,
                @NonNull String cacheRevision
        ) {
            this.topIndex = topIndex;
            this.divisor = divisor;
            this.cacheRevision = cacheRevision;
        }

        @Nullable
        synchronized int[] positionsFor(int tileIndex) {
            return microCachePositions[tileIndex];
        }

        synchronized void storePositions(int tileIndex, @NonNull int[] positions) {
            microCachePositions[tileIndex] = positions;
        }
    }
}

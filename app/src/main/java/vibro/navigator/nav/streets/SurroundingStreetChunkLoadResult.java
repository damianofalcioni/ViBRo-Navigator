package vibro.navigator.nav.streets;

import androidx.annotation.NonNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import vibro.navigator.nav.compass.CompassStreetOverlay;

final class SurroundingStreetChunkLoadResult {
    @NonNull
    private final Map<SurroundingStreetChunkKey, CompassStreetOverlay> overlays = new LinkedHashMap<>();
    private final Set<SurroundingStreetChunkKey> retryable = new HashSet<>();

    void put(@NonNull SurroundingStreetChunkKey key, @NonNull CompassStreetOverlay overlay) {
        overlays.put(key, overlay);
    }

    void retry(SurroundingStreetChunkKey key) {
        retryable.add(key);
    }

    void putInto(@NonNull SurroundingStreetOverlayCache cache, long retryAt) {
        for (Map.Entry<SurroundingStreetChunkKey, CompassStreetOverlay> entry : overlays.entrySet()) {
            cache.put(entry.getKey(), entry.getValue());
        }
        for (SurroundingStreetChunkKey key : retryable) {
            cache.retryAfter(key, retryAt);
        }
    }
}

package vibro.navigator.nav.streets;

import androidx.annotation.NonNull;

import java.util.LinkedHashMap;
import java.util.Map;

import vibro.navigator.nav.compass.CompassStreetOverlay;

final class SurroundingStreetChunkLoadResult {
    @NonNull
    private final Map<SurroundingStreetChunkKey, CompassStreetOverlay> overlays = new LinkedHashMap<>();

    void put(@NonNull SurroundingStreetChunkKey key, @NonNull CompassStreetOverlay overlay) {
        overlays.put(key, overlay);
    }

    void putInto(@NonNull SurroundingStreetOverlayCache cache) {
        for (Map.Entry<SurroundingStreetChunkKey, CompassStreetOverlay> entry : overlays.entrySet()) {
            cache.put(entry.getKey(), entry.getValue());
        }
    }
}

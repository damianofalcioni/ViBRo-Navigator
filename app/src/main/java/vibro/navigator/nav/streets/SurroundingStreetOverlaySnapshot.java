package vibro.navigator.nav.streets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import vibro.navigator.nav.compass.CompassStreetOverlay;
import vibro.navigator.nav.policy.NavigationSpeedBucket;

/** Reuses the immutable display overlay until its chunks, filter, or limit change. */
final class SurroundingStreetOverlaySnapshot {
    private List<SurroundingStreetChunkKey> keys;
    private NavigationSpeedBucket bucket;
    private int limit;
    private CompassStreetOverlay overlay;

    CompassStreetOverlay find(Collection<SurroundingStreetChunkKey> requested, int maxSegments, NavigationSpeedBucket speed) {
        return keys != null && keys.equals(requested) && limit == maxSegments && bucket == speed ? overlay : null;
    }

    CompassStreetOverlay save(
            Collection<SurroundingStreetChunkKey> requested, int maxSegments,
            NavigationSpeedBucket speed, CompassStreetOverlay result
    ) {
        keys = new ArrayList<>(requested);
        limit = maxSegments;
        bucket = speed;
        overlay = result;
        return result;
    }

    void clear() {
        keys = null;
        overlay = null;
    }
}

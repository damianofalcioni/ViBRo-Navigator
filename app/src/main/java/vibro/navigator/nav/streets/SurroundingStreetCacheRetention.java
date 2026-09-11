package vibro.navigator.nav.streets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Protects nearby display chunks and avoids retrying chunks that cannot fit the current viewport. */
final class SurroundingStreetCacheRetention {
    private static final int MAX_REMEMBERED_EVICTIONS = 256;
    private List<SurroundingStreetChunkKey> displayKeys = Collections.emptyList();
    private final Set<SurroundingStreetChunkKey> budgetEvictions = new LinkedHashSet<>();

    void clear() {
        displayKeys = Collections.emptyList();
        budgetEvictions.clear();
    }

    void setDisplayKeys(List<SurroundingStreetChunkKey> keys) {
        if (!displayKeys.equals(keys)) {
            displayKeys = new ArrayList<>(keys);
            budgetEvictions.clear();
        }
    }

    boolean canLoad(SurroundingStreetChunkKey key) {
        return !budgetEvictions.contains(key);
    }

    void recordEviction(SurroundingStreetChunkKey key) {
        budgetEvictions.add(key);
        if (budgetEvictions.size() > MAX_REMEMBERED_EVICTIONS) {
            budgetEvictions.remove(budgetEvictions.iterator().next());
        }
    }

    SurroundingStreetChunkKey evictionKey(Set<SurroundingStreetChunkKey> cachedKeys) {
        for (SurroundingStreetChunkKey key : cachedKeys) {
            if (!displayKeys.contains(key)) {
                return key;
            }
        }
        return farthestDisplayKey(cachedKeys);
    }

    private SurroundingStreetChunkKey farthestDisplayKey(Set<SurroundingStreetChunkKey> cachedKeys) {
        for (int i = displayKeys.size() - 1; i >= 0; i--) {
            SurroundingStreetChunkKey key = displayKeys.get(i);
            if (cachedKeys.contains(key)) {
                return key;
            }
        }
        throw new IllegalStateException("Cannot evict from an empty street cache");
    }
}

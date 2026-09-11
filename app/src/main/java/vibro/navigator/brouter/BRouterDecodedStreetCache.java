package vibro.navigator.brouter;

import java.util.LinkedHashMap;

/** A repository-local LRU; decoded coordinates are packed rather than retained as point objects. */
final class BRouterDecodedStreetCache {
    static final int MAX_BYTES = 4 * 1024 * 1024;
    private static final int MAX_CELLS = 8;
    private final LinkedHashMap<String, BRouterPackedStreetCell> cells = new LinkedHashMap<>(16, 0.75f, true);
    private final BRouterRd5MetadataCache metadataCache = new BRouterRd5MetadataCache();
    private final int maxBytes;
    private int bytes;

    BRouterDecodedStreetCache() {
        this(MAX_BYTES);
    }

    BRouterDecodedStreetCache(int maxBytes) {
        this.maxBytes = Math.max(0, maxBytes);
    }

    int maxCellBytes() {
        return maxBytes;
    }

    BRouterRd5MetadataCache metadataCache() {
        return metadataCache;
    }

    synchronized BRouterPackedStreetCell get(String key) {
        return cells.get(key);
    }

    synchronized void put(String key, BRouterPackedStreetCell cell) {
        if (cell == null || cell.byteSize() > maxBytes) {
            return;
        }
        remove(key);
        cells.put(key, cell);
        bytes += cell.byteSize();
        while (bytes > maxBytes || cells.size() > MAX_CELLS) {
            remove(cells.keySet().iterator().next());
        }
    }

    private void remove(String key) {
        BRouterPackedStreetCell removed = cells.remove(key);
        if (removed != null) {
            bytes -= removed.byteSize();
        }
    }
}

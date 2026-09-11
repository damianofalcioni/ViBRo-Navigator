package vibro.navigator.brouter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import java.util.Arrays;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassStreetType;

public class BRouterDecodedStreetCacheTest {
    @Test
    public void byteBudgetEvictsLeastRecentlyUsedCell() {
        BRouterPackedStreetCell cell = cell();
        assertEquals(40, cell.byteSize());
        BRouterDecodedStreetCache cache = new BRouterDecodedStreetCache(80);
        cache.put("first", cell);
        cache.put("second", cell);
        assertSame(cell, cache.get("first"));

        cache.put("third", cell);

        assertNull(cache.get("second"));
        assertSame(cell, cache.get("first"));
        assertSame(cell, cache.get("third"));
    }

    @Test
    public void entryBudgetAlsoBoundsSmallCells() {
        BRouterDecodedStreetCache cache = new BRouterDecodedStreetCache();
        BRouterPackedStreetCell cell = cell();
        for (int i = 0; i < 9; i++) {
            cache.put(Integer.toString(i), cell);
        }
        assertNull(cache.get("0"));
        assertSame(cell, cache.get("1"));
        assertSame(cell, cache.get("8"));
    }

    private static BRouterPackedStreetCell cell() {
        BRouterPackedStreetCell.Builder builder = new BRouterPackedStreetCell.Builder(1000);
        builder.offer(Arrays.asList(new LatLon(48d, 16d), new LatLon(48.001d, 16.001d)), CompassStreetType.RESIDENTIAL);
        return builder.build();
    }
}

package vibro.navigator.map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MapPoiCacheTest {
    private static final MapPickerBounds BOUNDS = MapPickerBounds.of(48.0d, 16.0d, 49.0d, 17.0d, 14);

    @Test
    public void rememberAll_marksDiscoveredEmptyCategoriesAsCovered() {
        MapPoiCache cache = new MapPoiCache();
        List<MapPoiCategory> categories = MapPoiCategoryFilter.fromNames(Arrays.asList(
                "Fuel",
                "Hospital"
        ));
        MapPoiCategory fuel = categories.get(0);
        List<MapPoiMarker> markers = Collections.singletonList(
                new MapPoiMarker("Fuel Station", 48.5d, 16.5d, fuel)
        );

        cache.rememberAll(BOUNDS, categories, markers);

        assertTrue(cache.missingRequests(BOUNDS, categories).isEmpty());
        assertEquals(1, cache.visibleMarkers(BOUNDS, categories).size());
    }
}

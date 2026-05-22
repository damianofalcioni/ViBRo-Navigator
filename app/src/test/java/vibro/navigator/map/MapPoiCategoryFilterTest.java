package vibro.navigator.map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MapPoiCategoryFilterTest {

    @Test
    public void fromNames_buildsConfiguredCategoriesInOrder() {
        List<MapPoiCategory> categories = MapPoiCategoryFilter.fromNames(Arrays.asList(
                "Fuel",
                "Restaurant"
        ));

        assertEquals(2, categories.size());
        assertEquals("Fuel", categories.get(0).label);
        assertEquals("Restaurant", categories.get(1).label);
        assertEquals("name=fuel", categories.get(0).id);
    }

    @Test
    public void fromNames_resolvesPlainNamesAcrossCommonPoiKeys() throws Exception {
        MapPoiCategory category = MapPoiCategoryFilter.fromNames(Collections.singletonList("Fuel")).get(0);

        assertTrue(category.overpassSelectors().contains("[\"amenity\"=\"fuel\"]"));
        assertTrue(category.overpassSelectors().contains("[\"shop\"=\"fuel\"]"));
        assertTrue(category.matches(new JSONObject("{\"amenity\":\"fuel\"}")));
        assertTrue(category.matches(new JSONObject("{\"shop\":\"fuel\"}")));
        assertFalse(category.matches(new JSONObject("{\"amenity\":\"restaurant\"}")));
    }

    @Test
    public void fromNames_resolvesShopAndPublicTransportDisplayNames() throws Exception {
        List<MapPoiCategory> categories = MapPoiCategoryFilter.fromNames(Arrays.asList(
                "Convenience Shop",
                "Public Transport Platform",
                "Bus Stop"
        ));

        assertTrue(categories.get(0).overpassSelectors().contains("[\"shop\"=\"convenience\"]"));
        assertTrue(categories.get(1).overpassSelectors().contains("[\"public_transport\"=\"platform\"]"));
        assertTrue(categories.get(2).overpassSelectors().contains("[\"highway\"=\"bus_stop\"]"));
        assertTrue(categories.get(2).matches(new JSONObject("{\"highway\":\"bus_stop\"}")));
    }
}

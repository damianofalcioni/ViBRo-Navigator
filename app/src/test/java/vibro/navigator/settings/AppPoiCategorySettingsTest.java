package vibro.navigator.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;

import org.junit.Test;

import java.util.Arrays;

import vibro.navigator.testutil.InMemorySharedPreferences;

public class AppPoiCategorySettingsTest {
    private static final String CATEGORY_FUEL = "Fuel";
    private static final String CATEGORY_RESTAURANT = "Restaurant";

    private final SharedPreferences preferences = new InMemorySharedPreferences();

    @Test
    public void defaults_prefillCommonDrivingWalkingAndCyclingCategories() {
        assertTrue(AppPoiCategoryPreferences.isMapPoiCategoryFilterEnabled(preferences));
        assertEquals(
                Arrays.asList(
                        "Bicycle Repair Station",
                        "Drinking Water",
                        CATEGORY_FUEL,
                        "Hospital",
                        "Parking",
                        "Pharmacy",
                        "Police",
                        "Public Transport Stop Position",
                        "Supermarket Shop",
                        "Taxi",
                        "Toilets"
                ),
                AppPoiCategoryPreferences.getMapPoiCategoryNames(preferences)
        );
        assertEquals(
                AppPoiCategoryPreferences.getMapPoiCategoryNames(preferences),
                AppPoiCategoryPreferences.getEnabledMapPoiCategoryNames(preferences)
        );
    }

    @Test
    public void enabledNames_omitDisabledRows() {
        AppPoiCategoryPreferences.setMapPoiCategorySettings(preferences, Arrays.asList(
                new AppPoiCategorySetting(CATEGORY_FUEL, true),
                new AppPoiCategorySetting(CATEGORY_RESTAURANT, false)
        ));

        assertEquals(Arrays.asList(CATEGORY_FUEL, CATEGORY_RESTAURANT),
                AppPoiCategoryPreferences.getMapPoiCategoryNames(preferences));
        assertEquals(Arrays.asList(CATEGORY_FUEL),
                AppPoiCategoryPreferences.getEnabledMapPoiCategoryNames(preferences));
    }
}

package vibro.navigator.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
public class AppPoiCategorySettingsTest {
    private static final String CATEGORY_FUEL = "Fuel";
    private static final String CATEGORY_RESTAURANT = "Restaurant";

    private Context context;

    @Before
    public void setUp() {
        Application application = ApplicationProvider.getApplicationContext();
        context = application;
        context.getSharedPreferences("vibro.navigator.settings", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    @Test
    public void defaults_prefillCommonDrivingWalkingAndCyclingCategories() {
        assertTrue(AppSettings.isMapPoiCategoryFilterEnabled(context));
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
                AppSettings.getMapPoiCategoryNames(context)
        );
        assertEquals(
                AppSettings.getMapPoiCategoryNames(context),
                AppSettings.getEnabledMapPoiCategoryNames(context)
        );
    }

    @Test
    public void enabledNames_omitDisabledRows() {
        AppSettings.setMapPoiCategorySettings(context, Arrays.asList(
                new AppPoiCategorySetting(CATEGORY_FUEL, true),
                new AppPoiCategorySetting(CATEGORY_RESTAURANT, false)
        ));

        assertEquals(Arrays.asList(CATEGORY_FUEL, CATEGORY_RESTAURANT), AppSettings.getMapPoiCategoryNames(context));
        assertEquals(Arrays.asList(CATEGORY_FUEL), AppSettings.getEnabledMapPoiCategoryNames(context));
    }
}

package vibro.navigator.settings;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;

import vibro.navigator.nav.compass.CompassStreetCategory;
import vibro.navigator.nav.compass.CompassStreetType;
import vibro.navigator.nav.compass.CompassStreetVisibility;

@RunWith(RobolectricTestRunner.class)
public class AppCompassStreetTypeSettingsTest {
    @Test
    public void freshInstallEnablesEveryGroupAndType() {
        SharedPreferences preferences = preferences();
        preferences.edit().clear().commit();
        CompassStreetVisibility visibility = AppCompassStreetTypeSettings.get(preferences);
        for (CompassStreetCategory category : CompassStreetCategory.values()) {
            assertTrue(visibility.isCategoryEnabled(category));
        }
        for (CompassStreetType type : CompassStreetType.values()) {
            assertTrue(visibility.isVisible(type));
        }
    }

    @Test
    public void groupMasterPreservesIndividualChoicesAndUnknownNamesAreIgnored() {
        SharedPreferences preferences = preferences();
        preferences.edit().clear().commit();
        AppCompassStreetTypeSettings.set(preferences, new CompassStreetVisibility(
                EnumSet.of(CompassStreetCategory.SPECIAL_ROUTING),
                EnumSet.of(CompassStreetType.RAILWAY)
        ));
        CompassStreetVisibility stored = AppCompassStreetTypeSettings.get(preferences);
        assertFalse(stored.isVisible(CompassStreetType.RAILWAY));
        assertFalse(stored.isVisible(CompassStreetType.WATERWAY));
        assertTrue(stored.isTypeEnabled(CompassStreetType.WATERWAY));
        AppCompassStreetTypeSettings.set(preferences, new CompassStreetVisibility(
                EnumSet.noneOf(CompassStreetCategory.class), stored.disabledTypes()
        ));
        assertFalse(AppCompassStreetTypeSettings.get(preferences).isVisible(CompassStreetType.RAILWAY));
        assertTrue(AppCompassStreetTypeSettings.get(preferences).isVisible(CompassStreetType.WATERWAY));

        preferences.edit().putStringSet("compass_surrounding_street_disabled_types",
                new HashSet<>(Arrays.asList("RAILWAY", "FUTURE_TYPE"))).commit();
        assertFalse(AppCompassStreetTypeSettings.get(preferences).isTypeEnabled(CompassStreetType.RAILWAY));
    }

    private SharedPreferences preferences() {
        return RuntimeEnvironment.getApplication().getSharedPreferences(AppSettings.PREFS, 0);
    }
}

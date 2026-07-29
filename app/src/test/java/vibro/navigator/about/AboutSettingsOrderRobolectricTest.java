package vibro.navigator.about;

import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.LinearLayout;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import vibro.navigator.R;

@RunWith(RobolectricTestRunner.class)
public class AboutSettingsOrderRobolectricTest {
    @Test
    public void aboutPageSettingsRowsFollowCategorizedOrder() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        LinearLayout section = activity.findViewById(R.id.aboutSettingsSection);

        assertDirectChildOrder(
                section,
                R.id.aboutSettingsTitle,
                R.id.aboutSettingsDisplayTitle,
                R.id.aboutLightThemeRow,
                R.id.aboutImperialUnitsRow,
                R.id.aboutSettingsGuidanceTitle,
                R.id.aboutNavigationNotificationsRow,
                R.id.aboutManeuverVoiceRow,
                R.id.aboutDynamicGpsFixIntervalRow,
                R.id.aboutSingleInstructionModeRow,
                R.id.aboutNavigationCustomButtonRow,
                R.id.aboutAutoSaveGpxRow,
                R.id.aboutAndroidAutoRow,
                R.id.aboutSettingsCompassTitle,
                R.id.aboutCompassSurroundingStreetsRow,
                R.id.aboutCompassInstantZoomRow,
                R.id.aboutCompassStationaryFullRouteZoomRow,
                R.id.aboutCompassFullscreenRouteRow,
                R.id.aboutSettingsSearchInputTitle,
                R.id.aboutPoiCategoriesRow,
                R.id.aboutSpeechRecognitionRow,
                R.id.aboutGooglePoiApiKeyContainer,
                R.id.aboutSettingsAdvancedTitle,
                R.id.aboutFusedLocationRow,
                R.id.aboutLogEnabledRow,
                R.id.aboutExportDatabaseRow,
                R.id.aboutImportDatabaseRow
        );
    }

    private static void assertDirectChildOrder(LinearLayout parent, int... childIds) {
        int previousIndex = -1;
        for (int childId : childIds) {
            View child = parent.findViewById(childId);
            int currentIndex = parent.indexOfChild(child);

            assertTrue("Missing direct settings child " + childId, currentIndex >= 0);
            assertTrue(
                    "Settings child " + childId + " is out of order",
                    currentIndex > previousIndex
            );
            previousIndex = currentIndex;
        }
    }
}

package vibro.navigator.about;

import static org.junit.Assert.assertEquals;
import static org.robolectric.Shadows.shadowOf;
import static vibro.navigator.about.AboutDialogButtonStyleAssertions.assertBorderlessProfileInfoBackground;

import android.app.AlertDialog;
import android.app.Application;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowAlertDialog;

import java.util.concurrent.TimeUnit;

import vibro.navigator.R;
import vibro.navigator.distribution.DistributionServices;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppAndroidAutoSettings;
import vibro.navigator.settings.AppSettings;

@RunWith(RobolectricTestRunner.class)
public class AboutSettingInfoButtonsRobolectricTest {
    @Before
    public void setUp() {
        Application context = ApplicationProvider.getApplicationContext();
        AppLogger.init(context);
        AppLogger.setLoggingEnabled(context, false);
        AppSettings.setGooglePoiApiKey(context, "");
        AppAndroidAutoSettings.setIntegrationEnabled(context, true);
    }

    @Test
    public void aboutPageConfiguresInfoButtonsForEverySettingEntry() {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();

        assertInfoButton(activity, R.id.aboutLogEnabledInfoButton, R.string.label_log_enabled);
        assertInfoButton(activity, R.id.aboutAutoSaveGpxInfoButton, R.string.label_auto_save_gpx_enabled);
        assertInfoButton(activity, R.id.aboutFusedLocationInfoButton, R.string.label_fused_location_enabled);
        assertInfoButton(
                activity,
                R.id.aboutDynamicGpsFixIntervalInfoButton,
                R.string.label_dynamic_gps_fix_interval_enabled
        );
        assertInfoButton(activity, R.id.aboutImperialUnitsInfoButton, R.string.label_imperial_units_enabled);
        assertInfoButton(activity, R.id.aboutLightThemeInfoButton, R.string.label_light_theme_enabled);
        assertInfoButton(
                activity,
                R.id.aboutCompassSurroundingStreetsInfoButton,
                R.string.label_compass_surrounding_streets_enabled
        );
        assertInfoButton(
                activity,
                R.id.aboutNavigationNotificationsInfoButton,
                R.string.label_navigation_notifications_enabled
        );
        assertInfoButton(activity, R.id.aboutAndroidAutoInfoButton, R.string.label_android_auto_integration_enabled);
        assertInfoButton(activity, R.id.aboutPoiCategoriesInfoButton, R.string.label_poi_categories);
        assertInfoButton(activity, R.id.aboutManeuverVoiceInfoButton, R.string.label_maneuver_voice);
        assertInfoButton(activity, R.id.aboutGooglePoiApiKeyInfoButton, R.string.label_google_poi_api_key);
        assertInfoButton(activity, R.id.aboutExportDatabaseInfoButton, R.string.action_export_database);
        assertInfoButton(activity, R.id.aboutImportDatabaseInfoButton, R.string.action_import_database);
        assertConditionalSettingRows(activity);
    }

    @Test
    public void clickingSettingInfoButtonShowsExplanationDialog() {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        ImageButton infoButton = activity.findViewById(R.id.aboutLogEnabledInfoButton);

        infoButton.performClick();
        shadowOf(Looper.getMainLooper()).idleFor(
                AboutDeferredDialogAction.OPEN_DELAY_MS + 50,
                TimeUnit.MILLISECONDS
        );

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        TextView message = dialog.findViewById(android.R.id.message);

        assertEquals(activity.getString(R.string.about_setting_log_info), message.getText().toString());
    }

    private static void assertInfoButton(AboutActivity activity, int buttonId, int labelResId) {
        ImageButton button = activity.findViewById(buttonId);
        String label = activity.getString(labelResId);

        assertEquals(
                activity.getString(R.string.format_about_setting_info_content_description, label),
                button.getContentDescription().toString()
        );
        assertBorderlessProfileInfoBackground(activity, button);
    }

    private static void assertConditionalSettingRows(AboutActivity activity) {
        assertEquals(
                DistributionServices.supportsAndroidAutoIntegration() ? View.VISIBLE : View.GONE,
                activity.findViewById(R.id.aboutAndroidAutoRow).getVisibility()
        );
        assertEquals(
                DistributionServices.supportsUserGooglePoiApiKey() ? View.VISIBLE : View.GONE,
                activity.findViewById(R.id.aboutGooglePoiApiKeyContainer).getVisibility()
        );
    }
}

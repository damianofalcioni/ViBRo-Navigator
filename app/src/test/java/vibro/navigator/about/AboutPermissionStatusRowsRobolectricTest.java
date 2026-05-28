package vibro.navigator.about;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowPackageManager;

import vibro.navigator.R;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.startup.NavigationPreflight;

@RunWith(RobolectricTestRunner.class)
public class AboutPermissionStatusRowsRobolectricTest {

    @Before
    public void setUp() {
        Application context = ApplicationProvider.getApplicationContext();
        AppLogger.init(context);
    }

    @Test
    public void aboutPageShowsPermissionRowsBeforeSensorStatus() {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        LinearLayout content = activity.findViewById(R.id.aboutContent);
        View permissionList = activity.findViewById(R.id.aboutPermissionStatusList);
        View sensorStatusSubtitle = activity.findViewById(R.id.aboutSensorStatusSubtitle);

        assertTrue(content.indexOfChild(permissionList) < content.indexOfChild(sensorStatusSubtitle));
        assertPermissionRow(
                activity,
                R.id.aboutPermissionLocationLabel,
                R.id.aboutPermissionLocationStatus,
                R.string.label_permission_location
        );
        assertPermissionRow(
                activity,
                R.id.aboutPermissionLocationServicesLabel,
                R.id.aboutPermissionLocationServicesStatus,
                R.string.label_permission_location_services
        );
        assertPermissionRow(
                activity,
                R.id.aboutPermissionNotificationsLabel,
                R.id.aboutPermissionNotificationsStatus,
                R.string.label_permission_notifications
        );
        assertPermissionRow(
                activity,
                R.id.aboutPermissionBatteryLabel,
                R.id.aboutPermissionBatteryStatus,
                R.string.label_permission_battery
        );
    }

    @Test
    public void aboutPagePermissionRowsOpenMatchingSettingsPages() {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        registerResolvableIntent(activity, NavigationPreflight.newAppDetailsSettingsIntent(activity));
        registerResolvableIntent(activity, NavigationPreflight.newLocationSettingsIntent());
        registerResolvableIntent(activity, NavigationPreflight.newNotificationSettingsIntent(activity));
        registerResolvableIntent(activity, NavigationPreflight.newBatteryOptimizationIntent(activity));

        Intent permissionIntent = clickAndReadIntent(activity, R.id.aboutPermissionLocationRow);
        Intent locationIntent = clickAndReadIntent(activity, R.id.aboutPermissionLocationServicesRow);
        Intent notificationIntent = clickAndReadIntent(activity, R.id.aboutPermissionNotificationsRow);
        Intent batteryIntent = clickAndReadIntent(activity, R.id.aboutPermissionBatteryRow);

        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, permissionIntent.getAction());
        assertEquals("package:" + activity.getPackageName(), String.valueOf(permissionIntent.getData()));
        assertEquals(Settings.ACTION_LOCATION_SOURCE_SETTINGS, locationIntent.getAction());
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, notificationIntent.getAction());
        assertEquals(NavigationPreflight.newBatteryOptimizationIntent(activity).getAction(), batteryIntent.getAction());
    }

    private static void assertPermissionRow(
            AboutActivity activity,
            int labelId,
            int statusId,
            int expectedLabelId
    ) {
        TextView label = activity.findViewById(labelId);
        TextView status = activity.findViewById(statusId);

        assertEquals(activity.getString(expectedLabelId), label.getText().toString());
        assertTrue(status.length() > 0);
    }

    private static Intent clickAndReadIntent(AboutActivity activity, int rowId) {
        View row = activity.findViewById(rowId);
        row.performClick();
        return shadowOf(activity).getNextStartedActivity();
    }

    private static void registerResolvableIntent(AboutActivity activity, Intent intent) {
        ShadowPackageManager shadowPackageManager = shadowOf(activity.getPackageManager());
        ComponentName component = new ComponentName("com.android.settings", "com.android.settings.SettingsActivity");
        shadowPackageManager.addActivityIfNotPresent(component);
        shadowPackageManager.addIntentFilterForActivity(component, intentFilterFor(intent));
    }

    private static IntentFilter intentFilterFor(Intent intent) {
        IntentFilter filter = new IntentFilter(intent.getAction());
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        if (intent.getData() != null && intent.getData().getScheme() != null) {
            filter.addDataScheme(intent.getData().getScheme());
        }
        return filter;
    }
}

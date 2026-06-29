package vibro.navigator.nav.startup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPackageManager;

import vibro.navigator.android.startup.AndroidNavigationPreflight;
import vibro.navigator.android.startup.AndroidNavigationSettingsLauncher;

@RunWith(RobolectricTestRunner.class)
public class NavigationSettingsLauncherTest {

    @Test
    public void launch_usesPrimaryIntentWhenResolvable() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        Intent primaryIntent = AndroidNavigationPreflight.newLocationSettingsIntent();
        registerResolvableIntent(activity, primaryIntent);

        assertTrue(AndroidNavigationSettingsLauncher.launch(activity, primaryIntent));

        Intent startedIntent = shadowOf(activity).getNextStartedActivity();
        assertEquals(Settings.ACTION_LOCATION_SOURCE_SETTINGS, startedIntent.getAction());
    }

    @Test
    public void launch_fallsBackToAppDetailsWhenPrimaryIntentIsUnresolvable() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        Intent primaryIntent = new Intent("vibro.navigator.TEST_UNRESOLVABLE");
        Intent fallbackIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", activity.getPackageName(), null));
        registerResolvableIntent(activity, fallbackIntent);

        assertTrue(AndroidNavigationSettingsLauncher.launch(activity, primaryIntent));

        Intent startedIntent = shadowOf(activity).getNextStartedActivity();
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, startedIntent.getAction());
        assertEquals("package:" + activity.getPackageName(), String.valueOf(startedIntent.getData()));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.N_MR1)
    public void newNotificationSettingsIntent_usesLegacyExtrasBeforeOreo() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();

        Intent intent = AndroidNavigationPreflight.newNotificationSettingsIntent(activity);

        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, intent.getAction());
        assertEquals(activity.getPackageName(), intent.getStringExtra("app_package"));
        assertEquals(activity.getApplicationInfo().uid, intent.getIntExtra("app_uid", -1));
        assertFalse(intent.hasExtra(Settings.EXTRA_APP_PACKAGE));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.O)
    public void newNotificationSettingsIntent_usesPackageExtraOnOreoAndAbove() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();

        Intent intent = AndroidNavigationPreflight.newNotificationSettingsIntent(activity);

        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, intent.getAction());
        assertEquals(activity.getPackageName(), intent.getStringExtra(Settings.EXTRA_APP_PACKAGE));
        assertFalse(intent.hasExtra("app_package"));
        assertFalse(intent.hasExtra("app_uid"));
    }

    @Test
    public void newBatteryOptimizationRequestIntent_requestsPackageSpecificExemption() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();

        Intent intent = AndroidNavigationPreflight.newBatteryOptimizationRequestIntent(activity);

        assertEquals(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, intent.getAction());
        assertEquals("package:" + activity.getPackageName(), String.valueOf(intent.getData()));
    }

    @Test
    public void newBatteryOptimizationSettingsIntent_opensGenericOptimizationSettings() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();

        Intent intent = AndroidNavigationPreflight.newBatteryOptimizationSettingsIntent(activity);

        assertEquals(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS, intent.getAction());
        assertNull(intent.getData());
    }

    private static void registerResolvableIntent(Activity activity, Intent intent) {
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

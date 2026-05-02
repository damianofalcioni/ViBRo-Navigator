package vibro.navigator.nav.startup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(RobolectricTestRunner.class)
public class NavigationSettingsLauncherTest {

    @Test
    public void launch_usesPrimaryIntentWhenResolvable() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        Intent primaryIntent = NavigationPreflight.newLocationSettingsIntent();
        registerResolvableIntent(activity, primaryIntent);

        assertTrue(NavigationSettingsLauncher.launch(activity, primaryIntent));

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

        assertTrue(NavigationSettingsLauncher.launch(activity, primaryIntent));

        Intent startedIntent = shadowOf(activity).getNextStartedActivity();
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, startedIntent.getAction());
        assertEquals("package:" + activity.getPackageName(), String.valueOf(startedIntent.getData()));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.N_MR1)
    public void newNotificationSettingsIntent_usesLegacyExtrasBeforeOreo() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();

        Intent intent = NavigationPreflight.newNotificationSettingsIntent(activity);

        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, intent.getAction());
        assertEquals(activity.getPackageName(), intent.getStringExtra("app_package"));
        assertEquals(activity.getApplicationInfo().uid, intent.getIntExtra("app_uid", -1));
        assertFalse(intent.hasExtra(Settings.EXTRA_APP_PACKAGE));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.O)
    public void newNotificationSettingsIntent_usesPackageExtraOnOreoAndAbove() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();

        Intent intent = NavigationPreflight.newNotificationSettingsIntent(activity);

        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, intent.getAction());
        assertEquals(activity.getPackageName(), intent.getStringExtra(Settings.EXTRA_APP_PACKAGE));
        assertFalse(intent.hasExtra("app_package"));
        assertFalse(intent.hasExtra("app_uid"));
    }

    private static void registerResolvableIntent(Activity activity, Intent intent) {
        ShadowPackageManager shadowPackageManager = shadowOf(activity.getPackageManager());
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = new ActivityInfo();
        resolveInfo.activityInfo.packageName = "com.android.settings";
        resolveInfo.activityInfo.name = "com.android.settings.SettingsActivity";
        shadowPackageManager.addResolveInfoForIntent(intent, resolveInfo);
    }
}

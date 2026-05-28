package vibro.navigator.brouter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(RobolectricTestRunner.class)
public class BRouterInstallLauncherTest {

    @Test
    public void launchPlayStore_usesMarketUriWhenResolvable() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        Intent marketIntent = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=" + BRouterProfilesRepository.BROUTER_PACKAGE_NAME)
        );
        registerResolvableIntent(activity, marketIntent, "com.android.vending");

        assertTrue(BRouterInstallLauncher.launchPlayStore(activity));

        Intent startedIntent = shadowOf(activity).getNextStartedActivity();
        assertEquals("market", startedIntent.getData().getScheme());
        assertEquals(BRouterProfilesRepository.BROUTER_PACKAGE_NAME,
                startedIntent.getData().getQueryParameter("id"));
    }

    @Test
    public void launchPlayStore_fallsBackToWebWhenMarketUriIsUnresolvable() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        Intent webIntent = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id="
                        + BRouterProfilesRepository.BROUTER_PACKAGE_NAME)
        );
        registerResolvableIntent(activity, webIntent, "com.android.chrome");

        assertTrue(BRouterInstallLauncher.launchPlayStore(activity));

        Intent startedIntent = shadowOf(activity).getNextStartedActivity();
        assertEquals("https", startedIntent.getData().getScheme());
        assertEquals("play.google.com", startedIntent.getData().getHost());
        assertEquals(BRouterProfilesRepository.BROUTER_PACKAGE_NAME,
                startedIntent.getData().getQueryParameter("id"));
    }

    @Test
    public void launchFdroid_opensFdroidPackagePage() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        Intent fdroidIntent = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://f-droid.org/packages/" + BRouterProfilesRepository.BROUTER_PACKAGE_NAME + "/")
        );
        registerResolvableIntent(activity, fdroidIntent, "org.mozilla.firefox");

        assertTrue(BRouterInstallLauncher.launchFdroid(activity));

        Intent startedIntent = shadowOf(activity).getNextStartedActivity();
        assertEquals("https", startedIntent.getData().getScheme());
        assertEquals("f-droid.org", startedIntent.getData().getHost());
        assertEquals("/packages/" + BRouterProfilesRepository.BROUTER_PACKAGE_NAME + "/",
                startedIntent.getData().getPath());
    }

    @Test
    public void launchPlayStore_returnsFalseWhenNoStorePageCanBeOpened() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();

        assertFalse(BRouterInstallLauncher.launchPlayStore(activity));

        assertEquals(null, shadowOf(activity).getNextStartedActivity());
    }

    private static void registerResolvableIntent(Activity activity, Intent intent, String packageName) {
        ShadowPackageManager shadowPackageManager = shadowOf(activity.getPackageManager());
        ComponentName component = new ComponentName(packageName, packageName + ".StoreActivity");
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

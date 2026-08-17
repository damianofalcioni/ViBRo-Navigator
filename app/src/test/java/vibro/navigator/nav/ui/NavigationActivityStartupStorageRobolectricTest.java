package vibro.navigator.nav.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowPackageManager;

import vibro.navigator.R;
import vibro.navigator.android.intent.AndroidNavigationRequestIntentContract;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.settings.AppCompassSettings;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.R)
public class NavigationActivityStartupStorageRobolectricTest {
    @Before
    public void setUp() {
        Application context = ApplicationProvider.getApplicationContext();
        AppCompassSettings.setSurroundingStreetsEnabled(context, true);
        context.getSharedPreferences("vibenavigator_brouter", Context.MODE_PRIVATE).edit().clear().commit();
        installBRouterPackage(context);
    }

    @Test
    public void startupRequestsSegmentsTreeBeforeNavigationWhenModernStorageGrantIsMissing() {
        TestNavigationActivity activity = Robolectric.buildActivity(
                TestNavigationActivity.class,
                navigationIntent()
        ).setup().get();

        continueFromBRouterPrompt(activity);

        ShadowActivity.IntentForResult started = shadowOf(activity).getNextStartedActivityForResult();
        assertNotNull(started);
        assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE, started.intent.getAction());
    }

    private static void continueFromBRouterPrompt(TestNavigationActivity activity) {
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertEquals(
                activity.getString(R.string.action_continue),
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString()
        );
        assertTrue(String.valueOf(shadowOf(dialog).getMessage()).contains("segments4"));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(android.os.Looper.getMainLooper()).idle();
    }

    private static Intent navigationIntent() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), TestNavigationActivity.class);
        intent.putExtra(AndroidNavigationRequestIntentContract.EXTRA_PROFILE, "shortest");
        intent.putExtra(AndroidNavigationRequestIntentContract.EXTRA_DEST_LAT, 48.18418593077528d);
        intent.putExtra(AndroidNavigationRequestIntentContract.EXTRA_DEST_LON, 16.374241434964404d);
        return intent;
    }

    private static void installBRouterPackage(Context context) {
        ShadowPackageManager shadowPackageManager = shadowOf(context.getPackageManager());
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = BRouterProfilesRepository.BROUTER_PACKAGE_NAME;
        shadowPackageManager.installPackage(packageInfo);
    }

    public static class TestNavigationActivity extends NavigationActivity {
        @Override
        public boolean bindService(Intent service, ServiceConnection conn, int flags) {
            return false;
        }
    }
}

package vibro.navigator.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Looper;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowPackageManager;

import vibro.navigator.R;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.logging.AppLogger;

@RunWith(RobolectricTestRunner.class)
public class MainActivityProfilesTreeStartupRobolectricTest {
    private static final String PREFS_BROUTER = "vibenavigator_brouter";

    @Before
    public void setUp() {
        Application context = ApplicationProvider.getApplicationContext();
        AppLogger.init(context);
        context.getSharedPreferences(PREFS_BROUTER, Context.MODE_PRIVATE).edit().clear().commit();
        installBRouterPackage(context);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.R)
    public void onCreate_whenProfilesTreeGrantMissing_showsInstructionPromptBeforeFolderPicker() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertEquals(
                activity.getString(R.string.action_continue),
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString()
        );
        assertTrue(String.valueOf(shadowOf(dialog).getMessage()).contains("profiles2"));

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        ShadowActivity.IntentForResult started = shadowOf(activity).getNextStartedActivityForResult();
        assertNotNull(started);
        assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE, started.intent.getAction());
    }

    private static void installBRouterPackage(Context context) {
        ShadowPackageManager shadowPackageManager = shadowOf(context.getPackageManager());
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = BRouterProfilesRepository.BROUTER_PACKAGE_NAME;
        shadowPackageManager.installPackage(packageInfo);
    }
}

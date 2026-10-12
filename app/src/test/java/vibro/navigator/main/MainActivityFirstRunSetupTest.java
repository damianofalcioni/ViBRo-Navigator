package vibro.navigator.main;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ComponentName;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Looper;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDialog;

import java.util.concurrent.TimeUnit;

import vibro.navigator.R;
import vibro.navigator.android.startup.AndroidFirstRunPermissions;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.settings.AppMainUiSettings;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.TIRAMISU)
public class MainActivityFirstRunSetupTest {
    @Test
    public void cancellingInstallStartsPermissionsAndRecreationDoesNotDuplicateRequest() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            assertNull(shadowOf(controller.get()).getLastRequestedPermission());
            continueWelcome();
            assertNull(shadowOf(controller.get()).getLastRequestedPermission());
            cancelPrompt();
            assertEquals(AndroidFirstRunPermissions.REQUEST_RUNTIME,
                    shadowOf(controller.get()).getLastRequestedPermission().requestCode);
            assertTrue(AppMainUiSettings.isFirstRunSetupPending(controller.get()));
            controller.recreate();
            assertNull(shadowOf(controller.get()).getLastRequestedPermission());
            finishPermissions(controller.get());
            assertFalse(AppMainUiSettings.isFirstRunSetupPending(controller.get()));
        }
        try (ActivityController<MainActivity> reopened = Robolectric.buildActivity(MainActivity.class).setup()) {
            assertNull(shadowOf(reopened.get()).getLastRequestedPermission());
        }
    }

    @Test
    public void storeReturnStartsSetupAndDetectsNewlyInstalledBRouter() {
        Application app = ApplicationProvider.getApplicationContext();
        ComponentName store = new ComponentName("com.android.vending", "StoreActivity");
        IntentFilter filter = new IntentFilter(Intent.ACTION_VIEW);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        filter.addDataScheme("market");
        shadowOf(app.getPackageManager()).addActivityIfNotPresent(store);
        shadowOf(app.getPackageManager()).addIntentFilterForActivity(store, filter);
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            continueWelcome();
            ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            shadowOf(Looper.getMainLooper()).idle();
            ShadowActivity.IntentForResult storeRequest = shadowOf(controller.get()).getNextStartedActivityForResult();
            assertNotNull(storeRequest);
            assertNull(shadowOf(controller.get()).getLastRequestedPermission());
            controller.get().onActivityResult(storeRequest.requestCode, Activity.RESULT_CANCELED, null);
            assertNull(shadowOf(controller.get()).getLastRequestedPermission());
            controller.pause().stop();
            installBRouter(app);
            controller.recreate();
            idle();
            assertTrue(String.valueOf(shadowOf(ShadowAlertDialog.getLatestAlertDialog()).getMessage()).contains("profiles2"));
        }
    }

    @Test
    public void cancellingFolderPromptsAdvancesThroughProfilesSegmentsAndRuntimePermissions() {
        installBRouter(ApplicationProvider.getApplicationContext());
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            continueWelcome();
            idle();
            assertTrue(String.valueOf(shadowOf(ShadowAlertDialog.getLatestAlertDialog()).getMessage()).contains("profiles2"));
            assertNull(shadowOf(controller.get()).getLastRequestedPermission());
            AlertDialog original = ShadowAlertDialog.getLatestAlertDialog();
            controller.recreate();
            idle();
            assertFalse(original.isShowing());
            assertTrue(String.valueOf(shadowOf(ShadowAlertDialog.getLatestAlertDialog()).getMessage()).contains("profiles2"));
            cancelPrompt();
            idle();
            assertTrue(String.valueOf(shadowOf(ShadowAlertDialog.getLatestAlertDialog()).getMessage()).contains("segments4"));
            cancelPrompt();
            assertNotNull(shadowOf(controller.get()).getLastRequestedPermission());
            finishPermissions(controller.get());
        }
    }

    private static void continueWelcome() {
        ShadowDialog.getLatestDialog().findViewById(R.id.welcomeContinueButton).performClick();
        shadowOf(Looper.getMainLooper()).idleFor(150, TimeUnit.MILLISECONDS);
    }

    private static void idle() {
        shadowOf(Looper.getMainLooper()).idleFor(150, TimeUnit.MILLISECONDS);
    }

    private static void cancelPrompt() {
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
    }

    private static void finishPermissions(MainActivity activity) {
        activity.onRequestPermissionsResult(AndroidFirstRunPermissions.REQUEST_RUNTIME, new String[0], new int[0]);
        for (int attempts = 0; attempts < 3 && AppMainUiSettings.isFirstRunSetupPending(activity); attempts++) {
            cancelPrompt();
        }
        assertFalse(AppMainUiSettings.isFirstRunSetupPending(activity));
    }

    private static void installBRouter(Application app) {
        PackageInfo brouter = new PackageInfo();
        brouter.packageName = BRouterProfilesRepository.BROUTER_PACKAGE_NAME;
        shadowOf(app.getPackageManager()).installPackage(brouter);
    }
}

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
import android.provider.DocumentsContract;
import android.widget.ListAdapter;
import android.widget.Spinner;

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
import org.robolectric.shadows.ShadowToast;

import java.util.concurrent.TimeUnit;

import vibro.navigator.R;
import vibro.navigator.android.brouter.TestBRouterStorage;
import vibro.navigator.android.startup.AndroidFirstRunPermissions;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.nav.model.NavigationRoutingMode;
import vibro.navigator.settings.AppMainUiSettings;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.TIRAMISU)
public class MainActivityFirstRunSetupTest {
    private static final String PROFILES_FOLDER = "profiles2";
    private static final String SEGMENTS_FOLDER = "segments4";
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
            assertMissingFolderCanBeSkipped(controller.get(), PROFILES_FOLDER);
            assertMissingFolderCanBeSkipped(controller.get(), SEGMENTS_FOLDER);
            assertEquals(AndroidFirstRunPermissions.REQUEST_RUNTIME,
                    shadowOf(controller.get()).getLastRequestedPermission().requestCode);
        }
    }

    @Test
    public void foregroundEnablesRoutingAndOffersFoldersAsTheyBecomeAvailable() {
        Application app = ApplicationProvider.getApplicationContext();
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            continueWelcome();
            cancelPrompt();
            finishPermissions(activity);

            Spinner modes = activity.findViewById(R.id.routeModeSpinner);
            int route = MainActivityRouteModeOption.positionOf(NavigationRoutingMode.BROUTER);
            int roundTrip = MainActivityRouteModeOption.positionOf(NavigationRoutingMode.ROUND_TRIP);
            int straightLine = MainActivityRouteModeOption.positionOf(NavigationRoutingMode.STRAIGHT_LINE);
            assertFalse(((ListAdapter) modes.getAdapter()).isEnabled(route));
            assertFalse(((ListAdapter) modes.getAdapter()).isEnabled(roundTrip));
            assertEquals(straightLine, modes.getSelectedItemPosition());

            controller.pause().stop();
            installBRouter(app);
            controller.start().resume();
            idle();
            assertTrue(((ListAdapter) modes.getAdapter()).isEnabled(route));
            assertTrue(((ListAdapter) modes.getAdapter()).isEnabled(roundTrip));
            assertEquals(straightLine, modes.getSelectedItemPosition());
            assertFalse(ShadowAlertDialog.getLatestAlertDialog().isShowing());

            controller.pause().stop();
            TestBRouterStorage.createPrimaryFolder(PROFILES_FOLDER);
            controller.start().resume();
            idle();
            AlertDialog profiles = ShadowAlertDialog.getLatestAlertDialog();
            assertTrue(profiles.isShowing());
            assertTrue(String.valueOf(shadowOf(profiles).getMessage()).contains(PROFILES_FOLDER));
            cancelPrompt();

            controller.pause().stop();
            TestBRouterStorage.createPrimaryFolder(SEGMENTS_FOLDER);
            controller.start().resume();
            idle();
            AlertDialog segments = ShadowAlertDialog.getLatestAlertDialog();
            assertTrue(segments.isShowing());
            assertTrue(String.valueOf(shadowOf(segments).getMessage()).contains(SEGMENTS_FOLDER));
            Intent grant = folderSelection("primary:Android/media/btools.routingapp/brouter/segments4");
            chooseFolder(activity, grant);
            assertEquals(grant.getData(), activity.getContentResolver().getPersistedUriPermissions().get(0).getUri());

            modes.setSelection(roundTrip);
            controller.pause().stop();
            activity.onNewIntent(new Intent(Intent.ACTION_MAIN));
            controller.start().resume();
            idle();
            assertEquals(roundTrip, modes.getSelectedItemPosition());
            assertFalse(ShadowAlertDialog.getLatestAlertDialog().isShowing());
        }
    }

    @Test
    public void cancellingFolderPromptsAdvancesThroughProfilesSegmentsAndRuntimePermissions() {
        installBRouterWithStorage();
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            continueWelcome();
            idle();
            assertTrue(String.valueOf(shadowOf(ShadowAlertDialog.getLatestAlertDialog()).getMessage()).contains(PROFILES_FOLDER));
            assertNull(shadowOf(controller.get()).getLastRequestedPermission());
            AlertDialog original = ShadowAlertDialog.getLatestAlertDialog();
            controller.recreate();
            idle();
            assertFalse(original.isShowing());
            assertTrue(String.valueOf(shadowOf(ShadowAlertDialog.getLatestAlertDialog()).getMessage()).contains(PROFILES_FOLDER));
            cancelPrompt();
            idle();
            assertTrue(String.valueOf(shadowOf(ShadowAlertDialog.getLatestAlertDialog()).getMessage()).contains(SEGMENTS_FOLDER));
            cancelPrompt();
            assertNotNull(shadowOf(controller.get()).getLastRequestedPermission());
            finishPermissions(controller.get());
        }
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    public void rejectedFoldersExplainMissingAccessAndCanBeRetriedOrSkipped() {
        installBRouterWithStorage();
        Intent downloads = folderSelection("primary:Download");
        Intent profiles = folderSelection("primary:Android/media/btools.routingapp/brouter/profiles2");
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            continueWelcome();
            idle();
            int profileRequest = chooseFolder(controller.get(), downloads);
            assertEquals(controller.get().getString(R.string.msg_brouter_profiles_storage_permission_required),
                    ShadowToast.getTextOfLatestToast());
            assertTrue(controller.get().getContentResolver().getPersistedUriPermissions().isEmpty());

            controller.recreate();
            idle();
            assertEquals(profileRequest, chooseFolder(controller.get(), profiles));
            assertEquals(profiles.getData(),
                    controller.get().getContentResolver().getPersistedUriPermissions().get(0).getUri());

            int segmentRequest = chooseFolder(controller.get(), downloads);
            assertNotEquals(profileRequest, segmentRequest);
            assertEquals(controller.get().getString(R.string.msg_compass_surrounding_streets_storage_permission_required),
                    ShadowToast.getTextOfLatestToast());
            assertEquals(1, controller.get().getContentResolver().getPersistedUriPermissions().size());
            assertNull(shadowOf(controller.get()).getLastRequestedPermission());
            cancelPrompt();
            assertEquals(AndroidFirstRunPermissions.REQUEST_RUNTIME,
                    shadowOf(controller.get()).getLastRequestedPermission().requestCode);
            finishPermissions(controller.get());
        }
    }

    private static Intent folderSelection(String documentId) {
        return new Intent()
                .setData(DocumentsContract.buildTreeDocumentUri("com.android.externalstorage.documents", documentId))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    private static int chooseFolder(MainActivity activity, Intent selection) {
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        ShadowActivity.IntentForResult request = shadowOf(activity).getNextStartedActivityForResult();
        assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE, request.intent.getAction());
        activity.onActivityResult(request.requestCode, Activity.RESULT_OK, selection);
        idle();
        return request.requestCode;
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

    private static void installBRouterWithStorage() {
        installBRouter(ApplicationProvider.getApplicationContext());
        TestBRouterStorage.createPrimaryFolder(PROFILES_FOLDER);
        TestBRouterStorage.createPrimaryFolder(SEGMENTS_FOLDER);
    }

    private static void assertMissingFolderCanBeSkipped(MainActivity activity, String folderName) {
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertEquals(activity.getString(R.string.msg_brouter_storage_folder_missing, folderName),
                String.valueOf(shadowOf(dialog).getMessage()));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        idle();
        ShadowActivity.IntentForResult next = shadowOf(activity).getNextStartedActivityForResult();
        assertTrue(next == null || !Intent.ACTION_OPEN_DOCUMENT_TREE.equals(next.intent.getAction()));
    }
}

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
import android.os.Bundle;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.widget.EditText;
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

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import vibro.navigator.R;
import vibro.navigator.android.brouter.TestBRouterStorage;
import vibro.navigator.android.startup.AndroidFirstRunPermissions;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.nav.model.NavigationRoutingMode;
import vibro.navigator.nav.ui.NavigationActivity;
import vibro.navigator.settings.AppCompassSettings;
import vibro.navigator.settings.AppMainUiSettings;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.TIRAMISU)
public class MainActivityFirstRunSetupTest {
    private static final String BROUTER_FOLDER = "brouter";
    private static final String BROUTER_FOLDER_ID = "primary:Android/media/btools.routingapp/brouter";
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
            controller.start().resume();
            idle();
            MainActivity activity = controller.get();
            assertMissingFolderCanBeSkipped(activity, BROUTER_FOLDER);
            ShadowActivity.IntentForResult manualPicker = openFolderPicker(activity);
            assertFalse(manualPicker.intent.hasExtra(DocumentsContract.EXTRA_INITIAL_URI));
            activity.onActivityResult(manualPicker.requestCode, Activity.RESULT_CANCELED, null);
            assertEquals(AndroidFirstRunPermissions.REQUEST_RUNTIME,
                    shadowOf(activity).getLastRequestedPermission().requestCode);
            finishPermissions(activity);

            controller.pause().stop();
            TestBRouterStorage.createPrimaryFolder(SEGMENTS_FOLDER);
            controller.start().resume();
            assertTrue(String.valueOf(shadowOf(ShadowAlertDialog.getLatestAlertDialog()).getMessage())
                    .contains(BROUTER_FOLDER));
        }
    }

    @Test
    public void foregroundEnablesRoutingAndOffersFolderWhenItBecomesAvailable() {
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
            TestBRouterStorage.requireGrantToQueryPrimaryFolder(app);
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
            assertTrue(String.valueOf(shadowOf(profiles).getMessage()).contains(BROUTER_FOLDER));
            TestBRouterStorage.createPrimaryFolder(SEGMENTS_FOLDER);
            Intent grant = folderSelection(BROUTER_FOLDER_ID);
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
    public void skippingUnverifiedFolderDuringSetupAllowsOfferAfterDiscovery() {
        Application app = ApplicationProvider.getApplicationContext();
        installBRouter(app);
        TestBRouterStorage.requireGrantToQueryPrimaryFolder(app);

        try (var controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            continueWelcome();
            idle();
            ShadowActivity.IntentForResult request = openFolderPicker(controller.get());
            assertTrue(request.intent.hasExtra(DocumentsContract.EXTRA_INITIAL_URI));
            controller.get().onActivityResult(request.requestCode, Activity.RESULT_CANCELED, null);
            idle();
            finishPermissions(controller.get());
            assertFalse(ShadowAlertDialog.getLatestAlertDialog().isShowing());

            controller.pause().stop();
            TestBRouterStorage.createPrimaryFolder(PROFILES_FOLDER);
            controller.start().resume();
            AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
            assertTrue(dialog.isShowing());
            assertTrue(String.valueOf(shadowOf(dialog).getMessage()).contains(BROUTER_FOLDER));
        }
    }

    @Test
    public void navigationLaunchCarriesDismissedSegmentsPromptAcrossRecreation() {
        Application app = ApplicationProvider.getApplicationContext();
        AppMainUiSettings.completeWelcome(app);
        AppMainUiSettings.setRoutingMode(app, NavigationRoutingMode.STRAIGHT_LINE);
        installBRouter(app);
        TestBRouterStorage.createPrimaryFolder(SEGMENTS_FOLDER);

        try (var controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            AlertDialog segments = ShadowAlertDialog.getLatestAlertDialog();
            assertTrue(String.valueOf(shadowOf(segments).getMessage()).contains(BROUTER_FOLDER));
            cancelPrompt();
            controller.recreate();

            MainActivity activity = controller.get();
            EditText destination = activity.findViewById(R.id.destinationEdit);
            destination.setText("48.18, 16.37");
            activity.findViewById(R.id.startNavButton).performClick();

            Intent trip = shadowOf(activity).getNextStartedActivity();
            assertNotNull(trip);
            assertEquals(NavigationActivity.class.getName(), trip.getComponent().getClassName());
            assertEquals(Collections.singletonList(DocumentsContract.buildDocumentUri(
                            "com.android.externalstorage.documents", BROUTER_FOLDER_ID)
                            .toString()),
                    trip.getStringArrayListExtra(NavigationActivity.EXTRA_SKIPPED_SEGMENTS_FOLDERS));
            assertTrue(AppCompassSettings.isSurroundingStreetsEnabled(activity));
        }
        try (var reopened = Robolectric.buildActivity(MainActivity.class).setup()) {
            AlertDialog segments = ShadowAlertDialog.getLatestAlertDialog();
            assertTrue(segments.isShowing());
            assertTrue(String.valueOf(shadowOf(segments).getMessage()).contains(BROUTER_FOLDER));
            assertTrue(AppCompassSettings.isSurroundingStreetsEnabled(reopened.get()));
        }
    }

    @Test
    public void cancellingFolderPromptAdvancesToRuntimePermissions() {
        installBRouterWithStorage();
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            continueWelcome();
            idle();
            assertTrue(String.valueOf(shadowOf(ShadowAlertDialog.getLatestAlertDialog()).getMessage()).contains(BROUTER_FOLDER));
            assertNull(shadowOf(controller.get()).getLastRequestedPermission());
            AlertDialog original = ShadowAlertDialog.getLatestAlertDialog();
            controller.recreate();
            idle();
            assertFalse(original.isShowing());
            assertTrue(String.valueOf(shadowOf(ShadowAlertDialog.getLatestAlertDialog()).getMessage()).contains(BROUTER_FOLDER));
            cancelPrompt();
            idle();
            assertNotNull(shadowOf(controller.get()).getLastRequestedPermission());
            finishPermissions(controller.get());
        }
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    public void rejectedFoldersExplainMissingAccessAndCanBeRetriedOrSkipped() {
        installBRouterWithStorage();
        Intent downloads = folderSelection("primary:Download");
        Intent profiles = folderSelection(BROUTER_FOLDER_ID);
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            continueWelcome();
            idle();
            int profileRequest = chooseFolder(controller.get(), downloads);
            assertEquals(controller.get().getString(R.string.msg_brouter_folder_required),
                    ShadowToast.getTextOfLatestToast());
            assertTrue(controller.get().getContentResolver().getPersistedUriPermissions().isEmpty());

            controller.recreate();
            idle();
            assertEquals(profileRequest, chooseFolder(controller.get(), profiles));
            assertEquals(profiles.getData(),
                    controller.get().getContentResolver().getPersistedUriPermissions().get(0).getUri());

            assertEquals(1, controller.get().getContentResolver().getPersistedUriPermissions().size());
            assertEquals(AndroidFirstRunPermissions.REQUEST_RUNTIME,
                    shadowOf(controller.get()).getLastRequestedPermission().requestCode);
            finishPermissions(controller.get());
        }
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.Q,
            shadows = vibro.navigator.android.brouter.AndroidBRouterSegmentsTreeAccessPromptTest.DelayedDiscovery.class)
    public void resolvedFirstRunGrantWaitsForTheMainScreenToResume() {
        installBRouterWithStorage();
        try (var controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            continueWelcome();
            shadowOf(Looper.getMainLooper()).idleFor(800, TimeUnit.MILLISECONDS);
            int request = openFolderPicker(activity).requestCode;
            controller.pause().stop().start();
            activity.onActivityResult(request, Activity.RESULT_OK, folderSelection(BROUTER_FOLDER_ID));
            controller.resume().pause().stop();
            shadowOf(Looper.getMainLooper()).idleFor(800, TimeUnit.MILLISECONDS);
            assertNull(shadowOf(activity).getLastRequestedPermission());

            controller.restart().resume();

            assertEquals(AndroidFirstRunPermissions.REQUEST_RUNTIME,
                    shadowOf(activity).getLastRequestedPermission().requestCode);
        }
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    public void rejectedForegroundGrantCanBeRetriedAfterReturningFromPicker() {
        Application app = ApplicationProvider.getApplicationContext();
        AppMainUiSettings.completeWelcome(app);
        installBRouter(app);
        TestBRouterStorage.createPrimaryFolder(SEGMENTS_FOLDER);

        try (var controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            ShadowActivity.IntentForResult request = openFolderPicker(activity);
            controller.pause().stop().start();
            activity.onActivityResult(request.requestCode, Activity.RESULT_OK, folderSelection("primary:Download"));
            controller.resume();
            idle();

            assertEquals(activity.getString(R.string.msg_brouter_folder_required),
                    ShadowToast.getTextOfLatestToast());
            assertTrue(activity.getContentResolver().getPersistedUriPermissions().isEmpty());
            assertTrue(ShadowAlertDialog.getLatestAlertDialog().isShowing());

            Intent segments = folderSelection(BROUTER_FOLDER_ID);
            TestBRouterStorage.createPrimaryFolder(PROFILES_FOLDER);
            assertEquals(request.requestCode, chooseFolder(activity, segments));
            assertEquals(segments.getData(), activity.getContentResolver().getPersistedUriPermissions().get(0).getUri());
            assertFalse(ShadowAlertDialog.getLatestAlertDialog().isShowing());
        }
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    public void restoresRetryAfterRejectedFirstRunResultWasAlreadyDelivered() {
        installBRouterWithStorage();
        Bundle state = new Bundle();
        try (var controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            continueWelcome();
            idle();
            int request = openFolderPicker(controller.get()).requestCode;
            controller.pause().stop().saveInstanceState(state).start();
            controller.get().onActivityResult(request, Activity.RESULT_OK, folderSelection("primary:Download"));
        }

        try (var restored = Robolectric.buildActivity(MainActivity.class).setup(state)) {
            idle();
            assertTrue(AppMainUiSettings.isFirstRunSetupPending(restored.get()));
            AlertDialog retry = ShadowAlertDialog.getLatestAlertDialog();
            assertTrue(retry.isShowing());
            assertTrue(String.valueOf(shadowOf(retry).getMessage()).contains(BROUTER_FOLDER));
        }
    }

    private static Intent folderSelection(String documentId) {
        return new Intent()
                .setData(DocumentsContract.buildTreeDocumentUri("com.android.externalstorage.documents", documentId))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    private static int chooseFolder(MainActivity activity, Intent selection) {
        ShadowActivity.IntentForResult request = openFolderPicker(activity);
        activity.onActivityResult(request.requestCode, Activity.RESULT_OK, selection);
        idle();
        return request.requestCode;
    }

    private static ShadowActivity.IntentForResult openFolderPicker(MainActivity activity) {
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        ShadowActivity.IntentForResult request = shadowOf(activity).getNextStartedActivityForResult();
        assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE, request.intent.getAction());
        return request;
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
        assertEquals(activity.getString(R.string.msg_brouter_storage_folder_missing, folderName)
                        + "\n\n" + activity.getString(R.string.msg_brouter_direct_tiles_hint),
                String.valueOf(shadowOf(dialog).getMessage()));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        idle();
        ShadowActivity.IntentForResult next = shadowOf(activity).getNextStartedActivityForResult();
        assertTrue(next == null || !Intent.ACTION_OPEN_DOCUMENT_TREE.equals(next.intent.getAction()));
    }
}

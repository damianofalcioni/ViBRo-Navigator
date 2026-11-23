package vibro.navigator.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowToast;

import java.io.File;

import vibro.navigator.R;
import vibro.navigator.android.brouter.AndroidBRouterProfilesRepositoryFactory;
import vibro.navigator.android.brouter.AndroidBRouterSegmentsRepositoryFactory;
import vibro.navigator.android.brouter.TestBRouterStorage;
import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.android.storage.AndroidStorageVolumes;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.settings.AppMainUiSettings;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.Q)
public class MainActivityBRouterRecoveryTest {
    private static final String PROFILES_FOLDER = "profiles2";
    private static final String MEDIA_BASE = "Android/media/btools.routingapp/brouter";
    private static final String SEGMENTS_FOLDER = "segments4";
    private Application app;

    @Before
    public void completeSetup() {
        app = ApplicationProvider.getApplicationContext();
        AppMainUiSettings.completeWelcome(app);
        AppMainUiSettings.completeFirstRunSetup(app);
        PackageInfo info = new PackageInfo();
        info.packageName = BRouterProfilesRepository.BROUTER_PACKAGE_NAME;
        shadowOf(app.getPackageManager()).installPackage(info);
    }

    @Test
    public void manualActionOpensPickerWhenDiscoveryCannotConfirmProfiles() {
        TestBRouterStorage.requireGrantToQueryPrimaryFolder(app);
        try (var controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            assertNull(ShadowAlertDialog.getLatestAlertDialog());
            View action = activity.findViewById(R.id.brouterStorageAccessButton);
            assertEquals(View.VISIBLE, action.getVisibility());

            action.performClick();
            assertTrue(ShadowAlertDialog.getLatestAlertDialog().isShowing());
            int request = openPicker(activity);
            TestBRouterStorage.createPrimaryFolder(PROFILES_FOLDER);
            TestBRouterStorage.createPrimaryFolder(SEGMENTS_FOLDER);
            activity.onActivityResult(request, Activity.RESULT_OK, folderGrant());
            idle();

            assertEquals(View.GONE, action.getVisibility());
        }
    }

    @Test
    public void manualBrowseSurvivesRecreationAndARejectedSelection() {
        TestBRouterStorage.requireGrantToQueryPrimaryFolder(app);
        try (var controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            controller.get().findViewById(R.id.brouterStorageAccessButton).performClick();
            assertTrue(ShadowAlertDialog.getLatestAlertDialog().isShowing());

            controller.recreate();
            MainActivity activity = controller.get();
            assertTrue(ShadowAlertDialog.getLatestAlertDialog().isShowing());
            int request = openPicker(activity);
            Uri invalid = AndroidDocumentAccess.buildExternalStorageTreeUri("primary:Download");
            activity.onActivityResult(request, Activity.RESULT_OK,
                    new Intent().setData(invalid).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
            idle();

            assertTrue(ShadowAlertDialog.getLatestAlertDialog().isShowing());
            assertEquals(request, openPicker(activity));
        }
    }

    @Test
    public void offersTheSameFolderAgainAfterAnAcceptedGrantIsRevoked() {
        TestBRouterStorage.createPrimaryFolder(PROFILES_FOLDER);
        TestBRouterStorage.createPrimaryFolder(SEGMENTS_FOLDER);
        try (var controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            int request = openPicker(activity);
            Intent grant = folderGrant();
            activity.onActivityResult(request, Activity.RESULT_OK, grant);
            idle();
            assertFalse(ShadowAlertDialog.getLatestAlertDialog().isShowing());
            controller.pause().stop();
            app.getContentResolver().releasePersistableUriPermission(grant.getData(), Intent.FLAG_GRANT_READ_URI_PERMISSION);

            controller.restart().resume();
            idle();

            assertPromptContains(MEDIA_BASE);
        }
    }

    @Test
    public void resultHandledAfterSnapshotSatisfiesBothFeaturesOnRestore() {
        TestBRouterStorage.createPrimaryFolder(PROFILES_FOLDER);
        TestBRouterStorage.createPrimaryFolder(SEGMENTS_FOLDER);
        Bundle snapshot = new Bundle();
        try (var controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            int code = openPicker(activity);
            controller.pause().stop().saveInstanceState(snapshot);
            activity.onActivityResult(code, Activity.RESULT_OK, folderGrant());
        }
        try (var restored = Robolectric.buildActivity(MainActivity.class).setup(snapshot)) {
            assertTrue(AndroidBRouterProfilesRepositoryFactory.create().hasPersistedProfilesTreeAccess(restored.get()));
            assertTrue(AndroidBRouterSegmentsRepositoryFactory.create().hasPersistedSegmentsTreeAccess(restored.get()));
            assertFalse(ShadowAlertDialog.getLatestAlertDialog().isShowing());
        }
    }

    @Test
    public void declinedFolderDoesNotSuppressReplacementFolder() {
        saveProfileGrant();
        TestBRouterStorage.createPrimaryFolder(SEGMENTS_FOLDER);
        try (var controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            assertPromptContains(MEDIA_BASE);
            ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
            idle();
            controller.pause().stop();

            File root = AndroidStorageVolumes.storageRoot(app, "primary");
            assertTrue(new File(root, "brouter/segments4").mkdirs());
            controller.start().resume();

            AlertDialog replacement = ShadowAlertDialog.getLatestAlertDialog();
            assertTrue(replacement.isShowing());
            assertTrue(String.valueOf(shadowOf(replacement).getMessage()).contains("brouter"));
            assertFalse(String.valueOf(shadowOf(replacement).getMessage()).contains(MEDIA_BASE));
            replacement.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
            idle();
            controller.pause().stop().start().resume();
            assertFalse(ShadowAlertDialog.getLatestAlertDialog().isShowing());
        }
    }

    @Test
    public void unmountedGrantAllowsSelectingAnAvailableReplacement() {
        saveProfileGrant();
        TestBRouterStorage.createPrimaryFolder(SEGMENTS_FOLDER);
        Uri stale = AndroidDocumentAccess.buildExternalStorageTreeUri("ABCD-1234:" + MEDIA_BASE);
        app.getContentResolver().takePersistableUriPermission(stale, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        AndroidBRouterSegmentsRepositoryFactory.create().saveSegmentsTreeUri(app, stale);

        try (var controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            assertPromptContains(MEDIA_BASE);
            assertEquals(View.VISIBLE, controller.get().findViewById(R.id.brouterStorageAccessButton).getVisibility());
        }
    }

    @Test
    public void pickerLaunchFailureKeepsManualRetryAvailable() {
        saveProfileGrant();
        TestBRouterStorage.createPrimaryFolder(SEGMENTS_FOLDER);
        try (var controller = Robolectric.buildActivity(NoPickerMainActivity.class).setup()) {
            MainActivity activity = controller.get();
            ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            idle();
            assertEquals(activity.getString(R.string.msg_output_folder_picker_unavailable),
                    ShadowToast.getTextOfLatestToast());
            assertFalse(ShadowAlertDialog.getLatestAlertDialog().isShowing());
            activity.findViewById(R.id.brouterStorageAccessButton).performClick();
            assertPromptContains(MEDIA_BASE);
        }
    }

    @Test
    public void profilesOnlyGrantDoesNotOfferAnotherProfilesOnlyFolderForStreetAccess() {
        TestBRouterStorage.createPrimaryFolder(PROFILES_FOLDER);
        TestBRouterStorage.createPrimaryDirectory("brouter/profiles2");
        try (var controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            assertPromptContains(MEDIA_BASE);
            int request = openPicker(activity);
            activity.onActivityResult(request, Activity.RESULT_OK, folderGrant());
            idle();

            assertFalse(ShadowAlertDialog.getLatestAlertDialog().isShowing());
            assertEquals(View.VISIBLE, activity.findViewById(R.id.brouterStorageAccessButton).getVisibility());
        }
    }

    private void saveProfileGrant() {
        Uri profiles = AndroidDocumentAccess.buildExternalStorageTreeUri("primary:" + MEDIA_BASE + "/profiles2");
        app.getContentResolver().takePersistableUriPermission(profiles, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        AndroidBRouterProfilesRepositoryFactory.create().saveProfilesTreeUri(app, profiles);
    }

    private static Intent folderGrant() {
        return new Intent().setData(AndroidDocumentAccess.buildExternalStorageTreeUri("primary:" + MEDIA_BASE))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    private static void assertPromptContains(String path) {
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertTrue(dialog.isShowing());
        assertTrue(String.valueOf(shadowOf(dialog).getMessage()).contains(path));
    }

    private static int openPicker(MainActivity activity) {
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        idle();
        return shadowOf(activity).getNextStartedActivityForResult().requestCode;
    }

    private static void idle() {
        shadowOf(Looper.getMainLooper()).idle();
    }

    public static class NoPickerMainActivity extends MainActivity {
        @Override
        public void startActivityForResult(Intent intent, int requestCode) {
            if (Intent.ACTION_OPEN_DOCUMENT_TREE.equals(intent.getAction())) {
                throw new ActivityNotFoundException("No system folder picker");
            }
            super.startActivityForResult(intent, requestCode);
        }
    }
}

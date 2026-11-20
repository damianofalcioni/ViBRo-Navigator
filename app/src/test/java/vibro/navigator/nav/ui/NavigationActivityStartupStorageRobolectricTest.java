package vibro.navigator.nav.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.core.content.IntentCompat;

import java.util.ArrayList;
import java.util.Collections;

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
import vibro.navigator.android.brouter.AndroidBRouterSegmentsRepositoryFactory;
import vibro.navigator.android.brouter.TestBRouterStorage;
import vibro.navigator.android.intent.AndroidNavigationRequestIntentContract;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.nav.startup.NavigationStartupCoordinator;
import vibro.navigator.settings.AppCompassSettings;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.R)
public class NavigationActivityStartupStorageRobolectricTest {
    private static final String SEGMENTS_FOLDER = "segments4";
    private static final String STORAGE_AUTHORITY = "com.android.externalstorage.documents";
    private static final String PRIMARY_PARENT_ID = "primary:Android/media/btools.routingapp/brouter";

    @Before
    public void setUp() {
        Application context = ApplicationProvider.getApplicationContext();
        AppCompassSettings.setSurroundingStreetsEnabled(context, true);
        context.getSharedPreferences("vibenavigator_brouter", Context.MODE_PRIVATE).edit().clear().commit();
        installBRouterPackage(context);
    }

    @Test
    public void startupRequestsSegmentsTreeBeforeNavigationWhenModernStorageGrantIsMissing() {
        TestBRouterStorage.createPrimaryFolder(SEGMENTS_FOLDER);
        TestNavigationActivity activity = Robolectric.buildActivity(
                TestNavigationActivity.class,
                navigationIntent()
        ).setup().get();

        continueFromBRouterPrompt(activity);

        ShadowActivity.IntentForResult started = shadowOf(activity).getNextStartedActivityForResult();
        assertNotNull(started);
        assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE, started.intent.getAction());
    }

    @Test
    public void unverifiedSegmentsFolderKeepsSurroundingStreetsEnabledAndContinuesStartup() {
        TestBRouterStorage.requireGrantToQueryPrimaryFolder(ApplicationProvider.getApplicationContext());
        TestNavigationActivity activity = Robolectric.buildActivity(TestNavigationActivity.class, navigationIntent())
                .setup().get();

        assertNull(ShadowAlertDialog.getLatestAlertDialog());
        assertTrue(AppCompassSettings.isSurroundingStreetsEnabled(activity));
        assertEquals(NavigationStartupCoordinator.REQUEST_PERMISSIONS,
                shadowOf(activity).getNextStartedActivityForResult().requestCode);
    }

    @Test
    public void missingSegmentsFolderDefersPreflightUntilNavigationResumes() {
        try (var controller = Robolectric.buildActivity(TestNavigationActivity.class, navigationIntent()).create().start()) {
            TestNavigationActivity activity = controller.get();

            assertNull(shadowOf(activity).getLastRequestedPermission());
            assertTrue(AppCompassSettings.isSurroundingStreetsEnabled(activity));

            controller.resume();

            assertEquals(NavigationStartupCoordinator.REQUEST_PERMISSIONS,
                    shadowOf(activity).getLastRequestedPermission().requestCode);
        }
    }

    @Test
    public void segmentsPickerResultDefersPreflightUntilNavigationResumes() {
        TestBRouterStorage.createPrimaryFolder(SEGMENTS_FOLDER);
        try (var controller = Robolectric.buildActivity(TestNavigationActivity.class, navigationIntent()).setup()) {
            TestNavigationActivity activity = controller.get();
            continueFromBRouterPrompt(activity);
            int requestCode = shadowOf(activity).getNextStartedActivityForResult().requestCode;
            controller.pause().stop();

            activity.onActivityResult(requestCode, Activity.RESULT_CANCELED, null);

            assertFalse(AppCompassSettings.isSurroundingStreetsEnabled(activity));
            assertNull(shadowOf(activity).getLastRequestedPermission());

            controller.restart().resume();

            assertEquals(NavigationStartupCoordinator.REQUEST_PERMISSIONS,
                    shadowOf(activity).getLastRequestedPermission().requestCode);
        }
    }

    @Test
    public void deferredPickerResultContinuesStartupAfterRecreation() {
        TestBRouterStorage.createPrimaryFolder(SEGMENTS_FOLDER);
        Bundle stateBeforeResult = new Bundle();
        Bundle state = new Bundle();
        try (var controller = Robolectric.buildActivity(TestNavigationActivity.class, navigationIntent()).setup()) {
            TestNavigationActivity activity = controller.get();
            continueFromBRouterPrompt(activity);
            int requestCode = shadowOf(activity).getNextStartedActivityForResult().requestCode;
            controller.pause().stop().saveInstanceState(stateBeforeResult);
            activity.onActivityResult(requestCode, Activity.RESULT_CANCELED, null);
            assertNull(shadowOf(activity).getLastRequestedPermission());
            controller.saveInstanceState(state);
        }

        assertRestoredStartupRequestsPermissions(state);
        assertRestoredStartupRequestsPermissions(stateBeforeResult);
    }

    @Test
    public void recreationWhilePickerIsOpenWaitsForItsResult() {
        TestBRouterStorage.createPrimaryFolder(SEGMENTS_FOLDER);
        Bundle state = new Bundle();
        int requestCode;
        try (var controller = Robolectric.buildActivity(TestNavigationActivity.class, navigationIntent()).setup()) {
            TestNavigationActivity activity = controller.get();
            continueFromBRouterPrompt(activity);
            requestCode = shadowOf(activity).getNextStartedActivityForResult().requestCode;
            controller.pause().stop().saveInstanceState(state);
        }

        try (var recreated = Robolectric.buildActivity(TestNavigationActivity.class, navigationIntent()).setup(state)) {
            TestNavigationActivity activity = recreated.get();
            assertFalse(ShadowAlertDialog.getLatestAlertDialog().isShowing());
            assertNull(shadowOf(activity).getNextStartedActivityForResult());

            Intent grant = new Intent().setData(DocumentsContract.buildTreeDocumentUri(
                    STORAGE_AUTHORITY, PRIMARY_PARENT_ID))
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.onActivityResult(requestCode, Activity.RESULT_OK, grant);

            assertTrue(AndroidBRouterSegmentsRepositoryFactory.create().hasPersistedSegmentsTreeAccess(activity));
            assertEquals(NavigationStartupCoordinator.REQUEST_PERMISSIONS,
                    shadowOf(activity).getLastRequestedPermission().requestCode);
        }
    }

    @Test
    public void mainScreenDismissalSkipsPromptAndKeepsSurroundingStreetsEnabled() {
        TestBRouterStorage.createPrimaryFolder(SEGMENTS_FOLDER);
        Intent intent = navigationIntentSkipping(PRIMARY_PARENT_ID);
        try (var controller = Robolectric.buildActivity(TestNavigationActivity.class, intent).create().start()) {
            TestNavigationActivity activity = controller.get();
            assertNull(shadowOf(activity).getLastRequestedPermission());

            controller.resume();

            assertNull(ShadowAlertDialog.getLatestAlertDialog());
            assertTrue(AppCompassSettings.isSurroundingStreetsEnabled(activity));
            assertEquals(NavigationStartupCoordinator.REQUEST_PERMISSIONS,
                    shadowOf(activity).getLastRequestedPermission().requestCode);
        }
    }

    private static void assertRestoredStartupRequestsPermissions(Bundle state) {
        try (var recreated = Robolectric.buildActivity(TestNavigationActivity.class, navigationIntent()).setup(state)) {
            assertEquals(NavigationStartupCoordinator.REQUEST_PERMISSIONS,
                    shadowOf(recreated.get()).getLastRequestedPermission().requestCode);
        }
    }

    @Test
    public void dismissalOfDifferentFolderDoesNotSuppressNavigationPrompt() {
        TestBRouterStorage.createPrimaryFolder(SEGMENTS_FOLDER);
        Intent intent = navigationIntentSkipping("primary:brouter");

        try (var controller = Robolectric.buildActivity(TestNavigationActivity.class, intent).setup()) {
            assertTrue(ShadowAlertDialog.getLatestAlertDialog().isShowing());
            assertNull(shadowOf(controller.get()).getLastRequestedPermission());
        }
    }

    @Test
    public void decliningAnAvailableSegmentsFolderDisablesSurroundingStreets() {
        TestBRouterStorage.createPrimaryFolder(SEGMENTS_FOLDER);
        TestNavigationActivity activity = Robolectric.buildActivity(TestNavigationActivity.class, navigationIntent())
                .setup().get();
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        assertFalse(AppCompassSettings.isSurroundingStreetsEnabled(activity));
        assertEquals(NavigationStartupCoordinator.REQUEST_PERMISSIONS,
                shadowOf(activity).getLastRequestedPermission().requestCode);
    }

    @Test
    public void dismissingOpenPromptKeepsStreetsEnabledWhenGrantArrives() {
        TestBRouterStorage.createPrimaryFolder(SEGMENTS_FOLDER);
        try (var controller = Robolectric.buildActivity(TestNavigationActivity.class, navigationIntent()).setup()) {
            var activity = controller.get();
            var prompt = ShadowAlertDialog.getLatestAlertDialog();
            assertTrue(prompt.isShowing());
            savePrimaryParent(activity);

            prompt.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
            shadowOf(Looper.getMainLooper()).idle();

            assertTrue(AppCompassSettings.isSurroundingStreetsEnabled(activity));
            assertEquals(NavigationStartupCoordinator.REQUEST_PERMISSIONS,
                    shadowOf(activity).getLastRequestedPermission().requestCode);
        }
    }

    @Test
    public void knownProfilesOnlyParentDoesNotPromptAtStartup() {
        TestBRouterStorage.createPrimaryFolder("profiles2");
        savePrimaryParent(ApplicationProvider.getApplicationContext());
        try (var controller = Robolectric.buildActivity(TestNavigationActivity.class, navigationIntent()).setup()) {
            var activity = controller.get();
            assertNull(ShadowAlertDialog.getLatestAlertDialog());
            assertTrue(AppCompassSettings.isSurroundingStreetsEnabled(activity));
            assertEquals(NavigationStartupCoordinator.REQUEST_PERMISSIONS,
                    shadowOf(activity).getLastRequestedPermission().requestCode);
        }
    }

    private static void savePrimaryParent(Context context) {
        Uri parent = DocumentsContract.buildTreeDocumentUri(STORAGE_AUTHORITY, PRIMARY_PARENT_ID);
        context.getContentResolver().takePersistableUriPermission(parent, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Uri profiles = DocumentsContract.buildDocumentUriUsingTree(parent,
                DocumentsContract.getTreeDocumentId(parent) + "/profiles2");
        Uri segments = DocumentsContract.buildDocumentUriUsingTree(parent,
                DocumentsContract.getTreeDocumentId(parent) + "/" + SEGMENTS_FOLDER);
        boolean hasStreets = vibro.navigator.android.storage.AndroidDocumentAccess.canReadExternalDirectory(
                context, DocumentsContract.getDocumentId(segments));
        vibro.navigator.brouter.BRouterFolderGrant.save(context, parent, profiles, hasStreets ? segments : null);
    }

    @Test
    public void invalidFolderOffersRetryOrSkipBeforeNavigationPreflight() {
        TestBRouterStorage.createPrimaryFolder(SEGMENTS_FOLDER);
        try (var controller = Robolectric.buildActivity(TestNavigationActivity.class, navigationIntent()).setup()) {
            var activity = controller.get();
            continueFromBRouterPrompt(activity);
            int request = shadowOf(activity).getNextStartedActivityForResult().requestCode;
            Intent invalid = new Intent().setData(DocumentsContract.buildTreeDocumentUri(
                    STORAGE_AUTHORITY, "primary:Download"))
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            controller.pause();
            activity.onActivityResult(request, Activity.RESULT_OK, invalid);
            assertNull(shadowOf(activity).getLastRequestedPermission());
            controller.resume();

            var retry = ShadowAlertDialog.getLatestAlertDialog();
            assertTrue(retry.isShowing());
            retry.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
            shadowOf(Looper.getMainLooper()).idle();
            assertTrue(AppCompassSettings.isSurroundingStreetsEnabled(activity));
            assertEquals(NavigationStartupCoordinator.REQUEST_PERMISSIONS,
                    shadowOf(activity).getLastRequestedPermission().requestCode);
        }
    }

    @Test
    public void restoredPickerRetryKeepsTheOriginalFolder() {
        TestBRouterStorage.createPrimaryFolder(SEGMENTS_FOLDER);
        try (var controller = Robolectric.buildActivity(TestNavigationActivity.class, navigationIntent()).setup()) {
            var activity = controller.get();
            continueFromBRouterPrompt(activity);
            var initial = shadowOf(activity).getNextStartedActivityForResult();
            Uri folder = IntentCompat.getParcelableExtra(initial.intent, DocumentsContract.EXTRA_INITIAL_URI, Uri.class);
            assertNotNull(folder);
            Intent invalid = new Intent().setData(DocumentsContract.buildTreeDocumentUri(
                    STORAGE_AUTHORITY, "primary:Download"))
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.onActivityResult(initial.requestCode, Activity.RESULT_OK, invalid);

            controller.recreate();
            var retry = ShadowAlertDialog.getLatestAlertDialog();
            assertTrue(retry.isShowing());
            retry.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            shadowOf(Looper.getMainLooper()).idle();

            Intent picker = shadowOf(controller.get()).getNextStartedActivityForResult().intent;
            assertEquals(folder, IntentCompat.getParcelableExtra(picker, DocumentsContract.EXTRA_INITIAL_URI, Uri.class));
        }
    }

    @Test
    public void retryIsSkippedIfSegmentsWereGrantedWhilePaused() {
        TestBRouterStorage.createPrimaryFolder(SEGMENTS_FOLDER);
        try (var controller = Robolectric.buildActivity(TestNavigationActivity.class, navigationIntent()).setup()) {
            TestNavigationActivity activity = controller.get();
            int request = openSegmentsPicker(activity);
            controller.pause();
            rejectFolder(activity, request);
            Uri parent = DocumentsContract.buildTreeDocumentUri(
                    STORAGE_AUTHORITY, PRIMARY_PARENT_ID);
            var granted = new java.util.concurrent.atomic.AtomicBoolean();
            vibro.navigator.android.brouter.AndroidBRouterFolderAccess.saveSelection(activity, treeResult(parent),
                    granted::set);
            assertTrue(granted.get());

            controller.resume();

            assertFalse(ShadowAlertDialog.getLatestAlertDialog().isShowing());
            assertEquals(NavigationStartupCoordinator.REQUEST_PERMISSIONS,
                    shadowOf(activity).getLastRequestedPermission().requestCode);
        }
    }

    @Test
    public void returningFromTheRetryPickerKeepsStreetSettingEnabled() {
        TestBRouterStorage.createPrimaryFolder(SEGMENTS_FOLDER);
        try (var controller = Robolectric.buildActivity(TestNavigationActivity.class, navigationIntent()).setup()) {
            TestNavigationActivity activity = controller.get();
            rejectFolder(activity, openSegmentsPicker(activity));
            ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            shadowOf(Looper.getMainLooper()).idle();
            int retry = shadowOf(activity).getNextStartedActivityForResult().requestCode;

            activity.onActivityResult(retry, Activity.RESULT_CANCELED, null);

            assertTrue(AppCompassSettings.isSurroundingStreetsEnabled(activity));
            assertNotNull(shadowOf(activity).getLastRequestedPermission());
        }
    }

    private static int openSegmentsPicker(TestNavigationActivity activity) {
        continueFromBRouterPrompt(activity);
        return shadowOf(activity).getNextStartedActivityForResult().requestCode;
    }

    private static void rejectFolder(TestNavigationActivity activity, int request) {
        Uri invalid = DocumentsContract.buildTreeDocumentUri(STORAGE_AUTHORITY, "primary:Download");
        activity.onActivityResult(request, Activity.RESULT_OK, treeResult(invalid));
    }

    private static Intent treeResult(Uri tree) {
        return new Intent().setData(tree).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    private static void continueFromBRouterPrompt(TestNavigationActivity activity) {
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertEquals(
                activity.getString(R.string.action_continue),
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString()
        );
        assertTrue(String.valueOf(shadowOf(dialog).getMessage()).contains("brouter"));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
    }

    private static Intent navigationIntent() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), TestNavigationActivity.class);
        intent.putExtra(AndroidNavigationRequestIntentContract.EXTRA_PROFILE, "shortest");
        intent.putExtra(AndroidNavigationRequestIntentContract.EXTRA_DEST_LAT, 48.18418593077528d);
        intent.putExtra(AndroidNavigationRequestIntentContract.EXTRA_DEST_LON, 16.374241434964404d);
        return intent;
    }

    private static Intent navigationIntentSkipping(String documentId) {
        String folder = DocumentsContract.buildDocumentUri(STORAGE_AUTHORITY, documentId).toString();
        return navigationIntent().putStringArrayListExtra(NavigationActivity.EXTRA_SKIPPED_SEGMENTS_FOLDERS,
                new ArrayList<>(Collections.singletonList(folder)));
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

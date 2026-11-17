package vibro.navigator.android.brouter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.view.ViewConfiguration;

import androidx.core.content.IntentCompat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowAlertDialog;

import java.io.File;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import vibro.navigator.android.storage.AndroidStorageVolumes;
import vibro.navigator.brouter.BRouterSegmentsRepository;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.Q)
public class AndroidBRouterSegmentsTreeAccessPromptTest {
    private static final String AUTHORITY = "com.android.externalstorage.documents";

    @Test
    public void opensPickerAtFixedLegacySharedSegmentsFolder() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        String folder = "brouter";
        assertTrue(new File(AndroidStorageVolumes.storageRoot(activity, "primary"), folder).mkdirs());

        Intent picker = openPicker(activity);
        Uri initial = IntentCompat.getParcelableExtra(picker, DocumentsContract.EXTRA_INITIAL_URI, Uri.class);
        assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE, picker.getAction());
        assertEquals(DocumentsContract.buildDocumentUri(AUTHORITY, "primary:" + folder), initial);
    }

    @Test
    public void opensManualPickerWhenNoKnownSegmentsFolderExists() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();

        Intent picker = openPicker(activity);

        assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE, picker.getAction());
        assertFalse(picker.hasExtra(DocumentsContract.EXTRA_INITIAL_URI));
    }

    @Test
    public void navigationOffersSegmentsAheadOfAnEarlierProfilesOnlyLocation() {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            TestBRouterStorage.createPrimaryFolder("profiles2");
            TestBRouterStorage.createPrimaryDirectory("brouter/segments4");

            assertEquals(DocumentsContract.buildDocumentUri(AUTHORITY, "primary:brouter"),
                    findAvailableFolder(activity));
        }
    }

    @Test
    public void offersSegmentsFolderVisibleOnlyThroughDocumentProvider() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        String id = "primary:Android/media/btools.routingapp/brouter";
        TestBRouterStorage.exposeDirectoryThroughProvider(activity, id);

        Uri available = findAvailableFolder(activity);
        assertNotNull(available);
        AndroidBRouterSegmentsTreeAccessPrompt.showNow(activity, available, 42, "StorageTest",
                () -> { }, () -> { }, () -> { });
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        Intent picker = shadowOf(activity).getNextStartedActivityForResult().intent;
        assertEquals(DocumentsContract.buildDocumentUri(AUTHORITY, id),
                IntentCompat.getParcelableExtra(picker, DocumentsContract.EXTRA_INITIAL_URI, Uri.class));
    }

    @Test
    public void keepsManualSegmentsBrowsingAvailableWhenProviderRequiresGrant() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        String folder = "Android/media/btools.routingapp/brouter";
        assertFalse(new File(AndroidStorageVolumes.storageRoot(activity, "primary"), folder).exists());
        TestBRouterStorage.requireGrantToQueryPrimaryFolder(activity);

        assertNull(findAvailableFolder(activity));

        Intent picker = openPicker(activity);
        assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE, picker.getAction());
        assertEquals(DocumentsContract.buildDocumentUri(AUTHORITY, "primary:" + folder),
                IntentCompat.getParcelableExtra(picker, DocumentsContract.EXTRA_INITIAL_URI, Uri.class));
    }

    @Test
    public void rejectsChildFoldersAndAcceptsCustomBRouterParent() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        Uri profiles = DocumentsContract.buildTreeDocumentUri(AUTHORITY, "primary:custom/brouter/profiles2");
        Intent data = new Intent().setData(profiles).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        assertFalse(TestBRouterStorage.saveSelection(activity, data));

        BRouterSegmentsRepository repository = AndroidBRouterSegmentsRepositoryFactory.create();
        repository.saveSegmentsTreeUri(activity, profiles);
        activity.getContentResolver().takePersistableUriPermission(profiles, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        assertFalse(repository.hasPersistedSegmentsTreeAccess(activity));

        Uri alternate = DocumentsContract.buildTreeDocumentUri(AUTHORITY, "primary:offline-routing/brouter");
        TestBRouterStorage.createPrimaryDirectory("offline-routing/brouter/segments4");
        data.setData(alternate);
        assertTrue(TestBRouterStorage.saveSelection(activity, data));
    }

    @Test
    public void rejectsNewDownloadsSelectionAndKeepsExistingGrant() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        BRouterSegmentsRepository repository = AndroidBRouterSegmentsRepositoryFactory.create();
        Uri downloads = DocumentsContract.buildTreeDocumentUri(AUTHORITY, "primary:Download");
        Intent data = new Intent().setData(downloads).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        assertFalse(TestBRouterStorage.saveSelection(activity, data));

        repository.saveSegmentsTreeUri(activity, downloads);
        activity.getContentResolver().takePersistableUriPermission(downloads, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        assertTrue(repository.hasPersistedSegmentsTreeAccess(activity));
    }

    @Test
    public void rejectsOppositeFolderFromProviderWithOpaqueIds() {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            Uri tree = DocumentsContract.buildTreeDocumentUri("test.documents", "42");
            TestBRouterStorage.exposeNamedFolder(tree, "profiles2");
            Intent data = new Intent().setData(tree).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            assertFalse(TestBRouterStorage.saveSelection(activity, data));
            assertTrue(activity.getContentResolver().getPersistedUriPermissions().isEmpty());
        }
    }

    @Test
    @Config(shadows = DelayedDiscovery.class)
    public void pendingLookupCoalescesTapsAndWaitsForResume() {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            TestBRouterStorage.createPrimaryFolder("segments4");
            Runnable request = () -> AndroidBRouterSegmentsTreeAccessPrompt.show(
                    activity, 42, "StorageTest", () -> { }, () -> { });
            request.run();
            request.run();
            controller.pause().stop();
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(800));
            assertNull(ShadowAlertDialog.getLatestAlertDialog());

            controller.start().resume();
            var dialog = ShadowAlertDialog.getLatestAlertDialog();
            assertTrue(dialog.isShowing());
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            shadowOf(Looper.getMainLooper()).idle();
            assertNotNull(shadowOf(activity).getNextStartedActivityForResult());
            assertFalse(dialog.isShowing());
            assertNull(shadowOf(activity).getNextStartedActivityForResult());
        }
    }

    @Implements(value = AndroidBRouterFolderDiscovery.class, isInAndroidSdk = false)
    public static class DelayedDiscovery {
        @Implementation
        protected static void execute(Runnable work) {
            new Handler(Looper.getMainLooper()).postDelayed(work, 600);
        }
    }

    private static Uri findAvailableFolder(Activity activity) {
        java.util.concurrent.atomic.AtomicReference<Uri> uri = new java.util.concurrent.atomic.AtomicReference<>();
        AndroidBRouterSegmentsTreeAccessPrompt.findAvailableFolder(activity, java.util.Collections.emptyList(), uri::set);
        shadowOf(Looper.getMainLooper()).idle();
        return uri.get();
    }

    private static Intent openPicker(Activity activity) {
        AndroidBRouterSegmentsTreeAccessPrompt.show(activity, 42, "StorageTest", () -> { }, () -> { });
        shadowOf(Looper.getMainLooper()).idleFor(ViewConfiguration.getPressedStateDuration(), TimeUnit.MILLISECONDS);
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        ShadowActivity.IntentForResult request = shadowOf(activity).getNextStartedActivityForResult();
        assertNotNull(request);
        return request.intent;
    }
}

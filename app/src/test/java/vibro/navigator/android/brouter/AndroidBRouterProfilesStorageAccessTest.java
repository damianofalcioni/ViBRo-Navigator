package vibro.navigator.android.brouter;

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
import android.content.ContextWrapper;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.Process;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;
import android.view.ViewConfiguration;

import androidx.test.core.app.ApplicationProvider;
import androidx.core.content.IntentCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.StorageVolumeBuilder;

import java.io.File;
import java.util.concurrent.TimeUnit;

import vibro.navigator.R;
import vibro.navigator.android.storage.AndroidStorageVolumes;
import vibro.navigator.android.storage.TestOutputStorageCard;
import vibro.navigator.brouter.BRouterProfilesRepository;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.Q, Build.VERSION_CODES.R})
public class AndroidBRouterProfilesStorageAccessTest {
    private static final String PROFILES_FOLDER = "profiles2";
    private static final String AUTHORITY = "com.android.externalstorage.documents";
    private static final String PRIMARY_ROOT_ID = "primary:";
    private static final String LEGACY_FOLDER = "Android/data/btools.routingapp/files/brouter/profiles2";
    private static final String MEDIA_FOLDER = "Android/media/btools.routingapp/brouter/profiles2";

    @Before
    public void resetStorageSettings() {
        Application context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("vibenavigator_brouter", Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test
    public void modernStorageNeedsFolderGrantEvenIfLegacyPermissionIsGranted() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        BRouterProfilesRepository repository = AndroidBRouterProfilesRepositoryFactory.create();
        shadowOf((Application) activity.getApplicationContext()).grantPermissions("android.permission.READ_EXTERNAL_STORAGE");
        assertFalse(AndroidBRouterProfilesStorageAccess.hasAccess(activity, repository));
    }

    @Test
    public void modernStorageRecognizesPersistedFolderReadPermission() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        BRouterProfilesRepository repository = AndroidBRouterProfilesRepositoryFactory.create();
        Uri tree = DocumentsContract.buildTreeDocumentUri(AUTHORITY, "primary:profiles2");
        repository.saveProfilesTreeUri(activity, tree);
        activity.getContentResolver().takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION);

        assertTrue(AndroidBRouterProfilesStorageAccess.hasAccess(activity, repository));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.M)
    public void folderInstructionsSupportTreeAndDocumentUrisOnMinimumAndroidVersion() {
        Uri tree = DocumentsContract.buildTreeDocumentUri(AUTHORITY, PRIMARY_ROOT_ID + LEGACY_FOLDER);
        Uri document = DocumentsContract.buildDocumentUri(AUTHORITY, PRIMARY_ROOT_ID + LEGACY_FOLDER);
        assertEquals(LEGACY_FOLDER, AndroidBRouterStorageInstructions.folderPath(tree, PROFILES_FOLDER));
        assertEquals(LEGACY_FOLDER, AndroidBRouterStorageInstructions.folderPath(document, PROFILES_FOLDER));
    }

    @Test
    public void missingFoldersStillAllowManualBrowsing() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        new TestOutputStorageCard(activity);
        BRouterProfilesRepository repository = AndroidBRouterProfilesRepositoryFactory.create();
        Uri downloads = DocumentsContract.buildTreeDocumentUri("com.android.providers.downloads.documents",
                "raw:/storage/emulated/0/Download");
        repository.saveProfilesTreeUri(activity, downloads);
        activity.getContentResolver().takePersistableUriPermission(downloads, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        assertFalse(AndroidBRouterProfilesStorageAccess.hasAccess(activity, repository));

        Uri custom = DocumentsContract.buildDocumentUri(AUTHORITY, "primary:Documents/custom.brf");
        repository.saveCustomProfile(activity, custom, "custom");
        assertManualBrowseAvailable(activity, repository);
    }

    @Test
    public void onlyAndroid10OffersProviderVisibleLegacyProfilesForNewGrants() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        BRouterProfilesRepository repository = AndroidBRouterProfilesRepositoryFactory.create();
        TestBRouterStorage.exposeDirectoryThroughProvider(activity, PRIMARY_ROOT_ID + LEGACY_FOLDER);

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            assertFolderPermissionStartsAt(activity, repository, LEGACY_FOLDER);
        } else {
            assertManualBrowseAvailable(activity, repository);
        }
    }

    @Test
    public void offersFixedLegacySharedProfilesFolder() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        String folder = "brouter/profiles2";
        assertTrue(new File(AndroidStorageVolumes.storageRoot(activity, "primary"), folder).mkdirs());

        assertFolderPermissionStartsAt(activity, AndroidBRouterProfilesRepositoryFactory.create(), folder);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.R)
    public void detectedSdCardProfilesTakePrecedenceOverSavedLegacyProfile() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        File cardRoot = new File(activity.getCacheDir(), TestOutputStorageCard.ID);
        assertTrue(new File(cardRoot, MEDIA_FOLDER).mkdirs());
        Context context = contextWithCard(activity, cardRoot);
        String documentId = TestOutputStorageCard.ID + ":" + MEDIA_FOLDER;
        BRouterProfilesRepository repository = AndroidBRouterProfilesRepositoryFactory.create();
        Uri custom = DocumentsContract.buildDocumentUri(AUTHORITY, PRIMARY_ROOT_ID + LEGACY_FOLDER + "/custom.brf");
        assertEquals(DocumentsContract.buildDocumentUri(AUTHORITY, documentId),
                AndroidBRouterPickerLocation.resolve(context,
                        repository.getProfilesTreePickerHint(context, custom), PROFILES_FOLDER).initialUri);
    }

    @Test
    public void providerVisibleSdCardProfilesTakePrecedenceOverPrimaryProfiles() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        TestBRouterStorage.createPrimaryFolder(PROFILES_FOLDER);
        File cardRoot = new File(activity.getCacheDir(), TestOutputStorageCard.ID);
        Context context = contextWithCard(activity, cardRoot);
        String documentId = TestOutputStorageCard.ID + ":" + MEDIA_FOLDER;
        TestBRouterStorage.exposeDirectoryThroughProvider(context, documentId);

        BRouterProfilesRepository repository = AndroidBRouterProfilesRepositoryFactory.create();
        assertEquals(DocumentsContract.buildDocumentUri(AUTHORITY, documentId),
                AndroidBRouterPickerLocation.resolve(context,
                        repository.getProfilesTreePickerHint(context, null), PROFILES_FOLDER).confirmedUri());
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    public void findsLegacyFolderOnSdCardWhenBothMediaLocationsAreMissing() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        File cardRoot = new File(activity.getCacheDir(), TestOutputStorageCard.ID);
        assertTrue(new File(cardRoot, LEGACY_FOLDER).mkdirs());
        Context context = contextWithCard(activity, cardRoot);

        Uri expected = DocumentsContract.buildDocumentUri(AUTHORITY, TestOutputStorageCard.ID + ":" + LEGACY_FOLDER);
        assertEquals(expected, AndroidBRouterPickerLocation.resolve(context, null, PROFILES_FOLDER).confirmedUri());
    }

    private static Context contextWithCard(Activity activity, File cardRoot) {
        shadowOf(activity.getSystemService(StorageManager.class)).addStorageVolume(
                new StorageVolumeBuilder(TestOutputStorageCard.ID, cardRoot, "SD card",
                        Process.myUserHandle(), Environment.MEDIA_MOUNTED)
                        .setIsRemovable(true).setFsUuid(TestOutputStorageCard.ID).build());
        return new ContextWrapper(activity) {
            @Override
            public File[] getExternalFilesDirs(String type) {
                File cardAppFiles = new File(cardRoot, "Android/data/" + getPackageName() + "/files");
                return new File[]{getExternalFilesDir(type), cardAppFiles};
            }
        };
    }

    @Test
    public void savedProfileInCustomBaseProvidesFolderHintWhenDiscoveryIsDenied() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        TestBRouterStorage.requireGrantToQueryPrimaryFolder(activity, PROFILES_FOLDER);
        BRouterProfilesRepository repository = AndroidBRouterProfilesRepositoryFactory.create();
        String folder = "CustomBase/brouter/profiles2";
        repository.saveCustomProfile(activity, DocumentsContract.buildDocumentUri(AUTHORITY,
                PRIMARY_ROOT_ID + folder + "/custom.brf"), "custom");

        assertFolderPermissionStartsAt(activity, repository, folder);
    }

    @Test
    public void finishingActivityDoesNotReceiveDeferredFolderPrompt() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        AndroidBRouterProfilesStorageAccess.request(activity, AndroidBRouterProfilesRepositoryFactory.create(),
                42, "StorageTest", () -> { }, () -> { });
        activity.finish();
        idlePrompt();
        assertNull(ShadowAlertDialog.getLatestAlertDialog());
    }

    private static void assertFolderPermissionStartsAt(
            Activity activity,
            BRouterProfilesRepository repository,
            String expectedFolder
    ) {
        AndroidBRouterProfilesStorageAccess.request(activity, repository, 42, "StorageTest", () -> { }, () -> { });
        idlePrompt();

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertTrue(String.valueOf(shadowOf(dialog).getMessage()).contains(expectedFolder));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        Intent picker = shadowOf(activity).getNextStartedActivityForResult().intent;
        Uri initial = IntentCompat.getParcelableExtra(picker, DocumentsContract.EXTRA_INITIAL_URI, Uri.class);
        assertEquals(PRIMARY_ROOT_ID + expectedFolder, DocumentsContract.getDocumentId(initial));
        assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE, picker.getAction());
        assertNull(shadowOf(activity).getLastRequestedPermission());
    }

    private static void assertManualBrowseAvailable(Activity activity, BRouterProfilesRepository repository) {
        AndroidBRouterProfilesStorageAccess.request(activity, repository, 42, "StorageTest", () -> { }, () -> { });
        idlePrompt();
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertEquals(activity.getString(R.string.msg_brouter_storage_folder_missing, PROFILES_FOLDER),
                String.valueOf(shadowOf(dialog).getMessage()));
        assertEquals(activity.getString(R.string.action_browse), dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText());
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        Intent picker = shadowOf(activity).getNextStartedActivityForResult().intent;
        assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE, picker.getAction());
        assertFalse(picker.hasExtra(DocumentsContract.EXTRA_INITIAL_URI));
    }

    private static void idlePrompt() {
        shadowOf(Looper.getMainLooper()).idleFor(ViewConfiguration.getPressedStateDuration(), TimeUnit.MILLISECONDS);
    }
}

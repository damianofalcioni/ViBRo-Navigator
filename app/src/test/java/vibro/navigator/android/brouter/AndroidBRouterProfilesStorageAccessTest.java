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
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
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

import java.util.concurrent.TimeUnit;

import vibro.navigator.brouter.BRouterProfilesRepository;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.Q, Build.VERSION_CODES.R})
public class AndroidBRouterProfilesStorageAccessTest {
    private static final String AUTHORITY = "com.android.externalstorage.documents";
    private static final String PRIMARY_ROOT_ID = "primary:";
    private static final String LEGACY_FOLDER = "Android/data/btools.routingapp/files/brouter/profiles2";

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
        assertEquals(LEGACY_FOLDER, AndroidBRouterStorageInstructions.folderPath(tree, "profiles2"));
        assertEquals(LEGACY_FOLDER, AndroidBRouterStorageInstructions.folderPath(document, "profiles2"));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    public void legacyFolderInstructionsAndPickerUseSameLocation() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        BRouterProfilesRepository repository = AndroidBRouterProfilesRepositoryFactory.create();
        Uri custom = DocumentsContract.buildDocumentUri(AUTHORITY, PRIMARY_ROOT_ID + LEGACY_FOLDER + "/custom.brf");
        repository.saveCustomProfile(activity, custom, "custom");
        AndroidBRouterProfilesStorageAccess.request(activity, repository, 42, "StorageTest", () -> { }, () -> { });
        idlePrompt();

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertTrue(String.valueOf(shadowOf(dialog).getMessage()).contains(LEGACY_FOLDER));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        Intent picker = shadowOf(activity).getNextStartedActivityForResult().intent;
        Uri initial = IntentCompat.getParcelableExtra(picker, DocumentsContract.EXTRA_INITIAL_URI, Uri.class);
        assertEquals(PRIMARY_ROOT_ID + LEGACY_FOLDER, DocumentsContract.getDocumentId(initial));
        assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE, picker.getAction());
        assertNull(shadowOf(activity).getLastRequestedPermission());
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

    private static void idlePrompt() {
        shadowOf(Looper.getMainLooper()).idleFor(ViewConfiguration.getPressedStateDuration(), TimeUnit.MILLISECONDS);
    }
}

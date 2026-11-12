package vibro.navigator.android.brouter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.view.ViewConfiguration;

import androidx.core.content.IntentCompat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowAlertDialog;

import java.io.File;
import java.util.concurrent.TimeUnit;

import vibro.navigator.android.storage.AndroidStorageVolumes;
import vibro.navigator.brouter.BRouterSegmentsRepository;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.Q)
public class AndroidBRouterSegmentsTreeAccessPromptTest {
    private static final String AUTHORITY = "com.android.externalstorage.documents";

    @Test
    public void reopensGrantedSegmentsAsDocumentUriOnAndroid10() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        BRouterSegmentsRepository repository = AndroidBRouterSegmentsRepositoryFactory.create();
        String id = "ABCD-1234:Android/media/btools.routingapp/brouter/segments4";
        Uri tree = DocumentsContract.buildTreeDocumentUri(AUTHORITY, id);
        repository.saveSegmentsTreeUri(activity, tree);
        activity.getContentResolver().takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent picker = openPicker(activity, repository);
        Uri initial = IntentCompat.getParcelableExtra(picker, DocumentsContract.EXTRA_INITIAL_URI, Uri.class);
        assertEquals(DocumentsContract.buildDocumentUriUsingTree(tree, id), initial);
    }

    @Test
    public void opensPickerAtFixedLegacySharedSegmentsFolder() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        String folder = "brouter/segments4";
        assertTrue(new File(AndroidStorageVolumes.storageRoot(activity, "primary"), folder).mkdirs());
        BRouterSegmentsRepository repository = AndroidBRouterSegmentsRepositoryFactory.create();

        Intent picker = openPicker(activity, repository);
        Uri initial = IntentCompat.getParcelableExtra(picker, DocumentsContract.EXTRA_INITIAL_URI, Uri.class);
        assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE, picker.getAction());
        assertEquals(DocumentsContract.buildDocumentUri(AUTHORITY, "primary:" + folder), initial);
    }

    @Test
    public void opensManualPickerWhenNoKnownSegmentsFolderExists() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();

        Intent picker = openPicker(activity, AndroidBRouterSegmentsRepositoryFactory.create());

        assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE, picker.getAction());
        assertFalse(picker.hasExtra(DocumentsContract.EXTRA_INITIAL_URI));
    }

    @Test
    public void offersSegmentsFolderVisibleOnlyThroughDocumentProvider() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        String id = "primary:Android/media/btools.routingapp/brouter/segments4";
        TestBRouterStorage.exposeDirectoryThroughProvider(activity, id);

        BRouterSegmentsRepository repository = AndroidBRouterSegmentsRepositoryFactory.create();
        assertNotNull(AndroidBRouterSegmentsTreeAccessPrompt.showIfAvailable(
                activity, repository, 42, "StorageTest", () -> { }, () -> { }));
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        Intent picker = shadowOf(activity).getNextStartedActivityForResult().intent;
        assertEquals(DocumentsContract.buildDocumentUri(AUTHORITY, id),
                IntentCompat.getParcelableExtra(picker, DocumentsContract.EXTRA_INITIAL_URI, Uri.class));
    }

    @Test
    public void rejectsDownloadsAsSegmentsAccess() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        BRouterSegmentsRepository repository = AndroidBRouterSegmentsRepositoryFactory.create();
        Uri downloads = DocumentsContract.buildTreeDocumentUri(AUTHORITY, "primary:Download");
        Intent data = new Intent().setData(downloads).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        assertFalse(AndroidBRouterFolderAccess.persistReadPermission(activity, data, downloads));

        repository.saveSegmentsTreeUri(activity, downloads);
        activity.getContentResolver().takePersistableUriPermission(downloads, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        assertFalse(repository.hasPersistedSegmentsTreeAccess(activity));
    }

    private static Intent openPicker(Activity activity, BRouterSegmentsRepository repository) {
        AndroidBRouterSegmentsTreeAccessPrompt.show(activity, repository, 42, "StorageTest", () -> { }, () -> { });
        shadowOf(Looper.getMainLooper()).idleFor(ViewConfiguration.getPressedStateDuration(), TimeUnit.MILLISECONDS);
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        ShadowActivity.IntentForResult request = shadowOf(activity).getNextStartedActivityForResult();
        assertNotNull(request);
        return request.intent;
    }
}

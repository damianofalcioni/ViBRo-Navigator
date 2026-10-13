package vibro.navigator.android.storage;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.Application;
import android.content.pm.ActivityInfo;
import android.content.ContentValues;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;

import androidx.core.content.IntentCompat;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity.IntentForResult;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 26, 35}, shadows = TestOutputBrowserActivity.class)
public class AndroidOutputBrowserActivityTest {
    private static final Uri FOLDER = Uri.parse(
            "content://com.android.externalstorage.documents/document/primary%3ADownload%2FViBRo%2Fgpx");

    @Test
    public void separateTaskHostsPickerAtSelectedFolderAndFinishesOnCancellation() throws Exception {
        try (ActivityController<AndroidOutputBrowserActivity> controller = browser().setup()) {
            AndroidOutputBrowserActivity activity = controller.get();
            assertEquals(activity.getPackageName() + ".outputBrowser",
                    activity.getPackageManager().getActivityInfo(activity.getComponentName(), 0).taskAffinity);
            IntentForResult launch = shadowOf(activity).getNextStartedActivityForResult();
            shadowOf(activity).getNextStartedActivity();
            assertEquals(AndroidOutputBrowserActivity.REQUEST_OPEN_FILE, launch.requestCode);
            assertEquals(Intent.ACTION_OPEN_DOCUMENT, launch.intent.getAction());
            if (RuntimeEnvironment.getApiLevel() >= 26) {
                assertEquals(FOLDER, IntentCompat.getParcelableExtra(
                        launch.intent, DocumentsContract.EXTRA_INITIAL_URI, Uri.class));
            }
            activity.onActivityResult(launch.requestCode, Activity.RESULT_CANCELED, null);
            assertTrue(activity.isFinishing());
            assertTrue(((TestOutputBrowserActivity) shadowOf(activity)).taskRemoved);
            assertNull(shadowOf(activity).getNextStartedActivity());
        }
    }

    @Test
    public void recreationDoesNotOpenDuplicatePicker() {
        Bundle saved = new Bundle();
        try (ActivityController<AndroidOutputBrowserActivity> controller = browser().setup()) {
            shadowOf(controller.get()).getNextStartedActivityForResult();
            shadowOf(controller.get()).getNextStartedActivity();
            controller.saveInstanceState(saved);
        }
        try (ActivityController<AndroidOutputBrowserActivity> controller = browser().setup(saved)) {
            assertNull(shadowOf(controller.get()).getNextStartedActivityForResult());
        }
    }

    @Test
    public void selectedFileOpensInViewerWithTemporaryReadAccessAndHostFinishes() {
        TestDownloadsProvider provider = TestDownloadsProvider.install(ApplicationProvider.getApplicationContext());
        ContentValues values = new ContentValues();
        values.put("mime_type", "application/gpx+xml");
        Uri file = provider.insert(Uri.parse("content://media/external/downloads"), values);
        try (ActivityController<AndroidOutputBrowserActivity> controller = browser().setup()) {
            AndroidOutputBrowserActivity activity = controller.get();
            shadowOf(activity).getNextStartedActivity();
            activity.onActivityResult(AndroidOutputBrowserActivity.REQUEST_OPEN_FILE,
                    Activity.RESULT_OK, new Intent().setData(file));
            Intent viewer = shadowOf(activity).getNextStartedActivity();
            assertEquals(Intent.ACTION_VIEW, viewer.getAction());
            assertEquals(file, viewer.getData());
            assertEquals("application/gpx+xml", viewer.getType());
            assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK,
                    viewer.getFlags());
            assertTrue(activity.isFinishing());
            assertTrue(((TestOutputBrowserActivity) shadowOf(activity)).taskRemoved);
        }
    }

    @Test
    public void importReturnsToCallerWithReadAccessAndConsumesResultOnce() {
        Intent launch = importLaunch();
        try (ActivityController<AndroidOutputBrowserActivity> controller =
                     Robolectric.buildActivity(AndroidOutputBrowserActivity.class, launch).setup()) {
            AndroidOutputBrowserActivity activity = controller.get();
            shadowOf(activity).getNextStartedActivity();
            Uri file = Uri.parse("content://backups/document/backup.json");
            ClipData clip = ClipData.newRawUri("backup", file);
            Intent selection = new Intent().setData(file);
            selection.setClipData(clip);
            activity.onActivityResult(AndroidOutputBrowserActivity.REQUEST_OPEN_FILE,
                    Activity.RESULT_OK, selection);
            Intent returned = shadowOf(activity).getNextStartedActivity();
            assertEquals(Activity.class.getName(), returned.getComponent().getClassName());
            assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_GRANT_READ_URI_PERMISSION, returned.getFlags());
            assertEquals(file, returned.getData());
            assertEquals(file, returned.getClipData().getItemAt(0).getUri());
            assertTrue(activity.isFinishing());
            assertTrue(((TestOutputBrowserActivity) shadowOf(activity)).taskRemoved);
            int[] deliveries = {0};
            AndroidOutputStorageBrowser.ActivityResult receiver = (request, result, data) -> {
                assertEquals(4002, request);
                assertEquals(Activity.RESULT_OK, result);
                assertEquals(file, data.getData());
                deliveries[0]++;
            };
            AndroidOutputStorageBrowser.dispatchResult(returned, receiver);
            AndroidOutputStorageBrowser.dispatchResult(returned, receiver);
            assertEquals(1, deliveries[0]);
            assertNull(shadowOf(activity).getNextStartedActivity());
        }
    }

    @Test
    public void profileSelectionTransfersOfferedPersistableReadAccess() {
        try (ActivityController<AndroidOutputBrowserActivity> controller =
                     Robolectric.buildActivity(AndroidOutputBrowserActivity.class, importLaunch()).setup()) {
            AndroidOutputBrowserActivity activity = controller.get();
            shadowOf(activity).getNextStartedActivity();
            Uri file = Uri.parse("content://profiles/document/custom.brf");
            Intent selection = new Intent().setData(file).addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            activity.onActivityResult(AndroidOutputBrowserActivity.REQUEST_OPEN_FILE, Activity.RESULT_OK, selection);
            Intent returned = shadowOf(activity).getNextStartedActivity();
            assertEquals(file, returned.getData());
            assertTrue((returned.getFlags() & Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) != 0);
            assertTrue((returned.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
            assertTrue(((TestOutputBrowserActivity) shadowOf(activity)).taskRemoved);
        }
    }

    @Test
    public void unavailablePickerReturnsCancellationToCallerAndFinishes() {
        Intent launch = importLaunch();
        Application application = ApplicationProvider.getApplicationContext();
        ActivityInfo caller = new ActivityInfo();
        caller.packageName = application.getPackageName();
        caller.name = Activity.class.getName();
        shadowOf(application.getPackageManager()).addOrUpdateActivity(caller);
        shadowOf(application).checkActivities(true);
        try (ActivityController<AndroidOutputBrowserActivity> controller =
                     Robolectric.buildActivity(AndroidOutputBrowserActivity.class, launch).setup()) {
            AndroidOutputBrowserActivity activity = controller.get();
            assertTrue(activity.isFinishing());
            assertTrue(((TestOutputBrowserActivity) shadowOf(activity)).taskRemoved);
            Intent returned = shadowOf(activity).getNextStartedActivity();
            assertNotNull(returned);
            assertTrue(AndroidOutputStorageBrowser.hasResult(returned));
            AndroidOutputStorageBrowser.dispatchResult(returned, (request, result, data) -> {
                assertEquals(4002, request);
                assertEquals(Activity.RESULT_CANCELED, result);
            });
        } finally {
            shadowOf(application).checkActivities(false);
        }
    }

    @Test
    public void recreatedImportHostReturnsCancellationWithoutOpeningAnotherPicker() {
        Intent launch = importLaunch();
        Bundle saved = new Bundle();
        try (ActivityController<AndroidOutputBrowserActivity> controller =
                     Robolectric.buildActivity(AndroidOutputBrowserActivity.class, launch).setup()) {
            shadowOf(controller.get()).getNextStartedActivityForResult();
            shadowOf(controller.get()).getNextStartedActivity();
            controller.saveInstanceState(saved);
        }
        try (ActivityController<AndroidOutputBrowserActivity> controller =
                     Robolectric.buildActivity(AndroidOutputBrowserActivity.class, launch).setup(saved)) {
            AndroidOutputBrowserActivity activity = controller.get();
            assertNull(shadowOf(activity).getNextStartedActivityForResult());
            activity.onActivityResult(AndroidOutputBrowserActivity.REQUEST_OPEN_FILE, Activity.RESULT_CANCELED, null);
            Intent returned = shadowOf(activity).getNextStartedActivity();
            AndroidOutputStorageBrowser.dispatchResult(returned, (request, result, data) -> {
                assertEquals(4002, request);
                assertEquals(Activity.RESULT_CANCELED, result);
                assertNull(data.getData());
            });
            assertTrue(activity.isFinishing());
            assertTrue(((TestOutputBrowserActivity) shadowOf(activity)).taskRemoved);
            assertNull(shadowOf(activity).getNextStartedActivity());
        }
    }

    private static Intent importLaunch() {
        try (ActivityController<Activity> caller = Robolectric.buildActivity(Activity.class).setup()) {
            AndroidOutputStorageBrowser.openForResult(caller.get(),
                    AndroidDocumentAccess.openDocumentIntent(FOLDER), 4002);
            Intent launch = shadowOf(caller.get()).getNextStartedActivityForResult().intent;
            shadowOf(caller.get()).getNextStartedActivity();
            return launch;
        }
    }

    private static ActivityController<AndroidOutputBrowserActivity> browser() {
        return Robolectric.buildActivity(AndroidOutputBrowserActivity.class,
                new Intent().setData(FOLDER));
    }
}

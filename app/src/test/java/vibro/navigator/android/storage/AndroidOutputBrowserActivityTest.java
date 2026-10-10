package vibro.navigator.android.storage;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.ContentValues;
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
@Config(sdk = {23, 26, 35})
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
        }
    }

    private static ActivityController<AndroidOutputBrowserActivity> browser() {
        return Robolectric.buildActivity(AndroidOutputBrowserActivity.class,
                new Intent().setData(FOLDER));
    }
}

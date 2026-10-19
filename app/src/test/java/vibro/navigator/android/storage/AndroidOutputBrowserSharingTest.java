package vibro.navigator.android.storage;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.Application;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;

import androidx.core.content.IntentCompat;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 26, 35}, shadows = TestOutputBrowserActivity.class)
public class AndroidOutputBrowserSharingTest {
    private static final String LOG_MIME = "text/plain";
    @Test
    public void multipleGpxFilesShareAllAttachmentsAndReadGrants() {
        assertMultipleFiles("application/gpx+xml");
    }

    @Test
    public void multipleLogsShareAllAttachmentsAndReadGrants() {
        assertMultipleFiles(LOG_MIME);
    }

    private static void assertMultipleFiles(String mime) {
        TestDownloadsProvider provider = TestDownloadsProvider.install(ApplicationProvider.getApplicationContext());
        Uri first = file(provider, mime);
        Uri second = file(provider, mime);
        Intent selection = selection(first, second);
        // Some pickers return the first URI in both data and ClipData.
        selection.setData(first);
        try (ActivityController<AndroidOutputBrowserActivity> controller = browser().setup()) {
            Intent chooser = deliver(controller.get(), selection);
            assertEquals(Intent.ACTION_CHOOSER, chooser.getAction());
            Intent send = IntentCompat.getParcelableExtra(chooser, Intent.EXTRA_INTENT, Intent.class);
            assertEquals(Intent.ACTION_SEND_MULTIPLE, send.getAction());
            assertEquals(mime, send.getType());
            assertEquals(Arrays.asList(first, second), IntentCompat.getParcelableArrayListExtra(
                    send, Intent.EXTRA_STREAM, Uri.class));
            assertGrants(send, first, second);
            assertGrants(chooser, first, second);
            assertTrue((chooser.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0);
            assertTrue(((TestOutputBrowserActivity) shadowOf(controller.get())).taskRemoved);
        }
    }

    @Test
    public void singleClipOnlyLogOffersViewerAndSharingAppsWithReadAccess() throws Exception {
        TestDownloadsProvider provider = TestDownloadsProvider.install(ApplicationProvider.getApplicationContext());
        Uri log = file(provider, LOG_MIME);
        try (ActivityController<AndroidOutputBrowserActivity> controller = browser().setup()) {
            AndroidOutputBrowserActivity activity = controller.get();
            ComponentName viewer = new ComponentName("com.example.viewer", "Viewer");
            IntentFilter filter = new IntentFilter(Intent.ACTION_VIEW);
            filter.addCategory(Intent.CATEGORY_DEFAULT);
            filter.addDataScheme("content");
            filter.addDataType(LOG_MIME);
            shadowOf(activity.getPackageManager()).addActivityIfNotPresent(viewer);
            shadowOf(activity.getPackageManager()).addIntentFilterForActivity(viewer, filter);
            Intent chooser = deliver(activity, selection(log));
            Intent send = IntentCompat.getParcelableExtra(chooser, Intent.EXTRA_INTENT, Intent.class);
            assertEquals(Intent.ACTION_SEND, send.getAction());
            assertEquals(LOG_MIME, send.getType());
            assertEquals(log, IntentCompat.getParcelableExtra(send, Intent.EXTRA_STREAM, Uri.class));
            Parcelable[] viewers = IntentCompat.getParcelableArrayExtra(chooser, Intent.EXTRA_INITIAL_INTENTS, Intent.class);
            assertNotNull(viewers);
            assertEquals(1, viewers.length);
            Intent open = (Intent) viewers[0];
            assertEquals(Intent.ACTION_VIEW, open.getAction());
            assertEquals(log, open.getData());
            assertEquals(viewer, open.getComponent());
            assertGrants(open, log);
            assertGrants(send, log);
            assertGrants(chooser, log);
            assertFalse(chooser.getBooleanExtra(Intent.EXTRA_AUTO_LAUNCH_SINGLE_CHOICE, false));
        }
    }

    @Test
    public void mixedOrUnknownMimeTypesShareWithoutRestrictingTargets() {
        TestDownloadsProvider provider = TestDownloadsProvider.install(ApplicationProvider.getApplicationContext());
        Uri gpx = file(provider, "application/gpx+xml");
        for (String mime : new String[]{LOG_MIME, null}) {
            try (ActivityController<AndroidOutputBrowserActivity> controller = browser().setup()) {
                Intent chooser = deliver(controller.get(), selection(file(provider, mime), gpx));
                Intent send = IntentCompat.getParcelableExtra(chooser, Intent.EXTRA_INTENT, Intent.class);
                assertEquals("*/*", send.getType());
            }
        }
    }

    @Test
    public void selectionAfterRecreationSharesWithoutLaunchingAnotherPicker() {
        TestDownloadsProvider provider = TestDownloadsProvider.install(ApplicationProvider.getApplicationContext());
        Intent selection = selection(file(provider, LOG_MIME), file(provider, LOG_MIME));
        Bundle saved = new Bundle();
        try (ActivityController<AndroidOutputBrowserActivity> controller = browser().setup()) {
            shadowOf(controller.get()).getNextStartedActivityForResult();
            shadowOf(controller.get()).getNextStartedActivity();
            controller.saveInstanceState(saved);
        }
        try (ActivityController<AndroidOutputBrowserActivity> controller = browser().setup(saved)) {
            assertNull(shadowOf(controller.get()).getNextStartedActivityForResult());
            Intent chooser = deliver(controller.get(), selection);
            assertEquals(Intent.ACTION_CHOOSER, chooser.getAction());
        }
    }

    @Test
    public void emptySelectionFinishesWithoutLaunchingAnAction() {
        try (ActivityController<AndroidOutputBrowserActivity> controller = browser().setup()) {
            Intent selection = new Intent();
            selection.setClipData(ClipData.newPlainText("", "no file"));
            assertNull(deliver(controller.get(), selection));
            assertTrue(controller.get().isFinishing());
        }
    }

    @Test
    public void unavailableChooserFinishesSafely() {
        Application app = ApplicationProvider.getApplicationContext();
        TestDownloadsProvider provider = TestDownloadsProvider.install(app);
        Uri log = file(provider, LOG_MIME);
        try (ActivityController<AndroidOutputBrowserActivity> controller = browser().setup()) {
            shadowOf(app).checkActivities(true);
            try {
                assertNull(deliver(controller.get(), selection(log)));
                assertTrue(((TestOutputBrowserActivity) shadowOf(controller.get())).taskRemoved);
            } finally {
                shadowOf(app).checkActivities(false);
            }
        }
    }

    private static Uri file(TestDownloadsProvider provider, String mime) {
        ContentValues values = new ContentValues();
        values.put("mime_type", mime);
        return provider.insert(Uri.parse("content://media/external/downloads"), values);
    }

    private static Intent selection(Uri... files) {
        Intent selection = new Intent();
        ClipData clip = ClipData.newRawUri("", files[0]);
        for (int i = 1; i < files.length; i++) {
            clip.addItem(new ClipData.Item(files[i]));
        }
        selection.setClipData(clip);
        return selection;
    }

    private static Intent deliver(AndroidOutputBrowserActivity activity, Intent selection) {
        shadowOf(activity).getNextStartedActivity();
        activity.onActivityResult(AndroidOutputBrowserActivity.REQUEST_OPEN_FILE, Activity.RESULT_OK, selection);
        return shadowOf(activity).getNextStartedActivity();
    }

    private static void assertGrants(Intent intent, Uri... files) {
        assertTrue((intent.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
        assertEquals(0, intent.getFlags() & (Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION));
        assertEquals(files.length, intent.getClipData().getItemCount());
        for (int i = 0; i < files.length; i++) {
            assertEquals(files[i], intent.getClipData().getItemAt(i).getUri());
        }
    }

    private static ActivityController<AndroidOutputBrowserActivity> browser() {
        return Robolectric.buildActivity(AndroidOutputBrowserActivity.class, new Intent());
    }
}

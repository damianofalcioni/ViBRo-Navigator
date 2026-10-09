package vibro.navigator.about;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Looper;
import android.widget.ListView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowToast;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import vibro.navigator.R;
import vibro.navigator.android.storage.AndroidAppStorageDirs;
import vibro.navigator.android.storage.TestOutputDocumentsProvider;
import vibro.navigator.nav.export.NavigationRouteGpxExporter;
import vibro.navigator.settings.AppOutputFolderSettings;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 35})
public class AboutOutputFolderBrowserTest {
    @Test
    public void defaultFolderListsFilesAndOpensGpxWithReadAccess() throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try (ActivityController<ViewerActivity> controller = activity()) {
            ViewerActivity activity = controller.get();
            AppOutputFolderSettings.set(activity, Kind.GPX, null);
            File folder = new File(AndroidAppStorageDirs.preferredExternalFilesDir(activity), "gpx");
            assertTrue(folder.isDirectory() || folder.mkdirs());
            File file = new File(folder, "browser-ui.gpx");
            Files.writeString(file.toPath(), "<gpx />");
            try {
                new AboutOutputFolderBrowser(activity, worker).open(Kind.GPX);
                await(worker);
                AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
                ListView list = dialog.getListView();
                assertNotNull(list);
                int index = indexOf(list, file.getName());
                list.performItemClick(list.getChildAt(index), index, list.getAdapter().getItemId(index));
                assertEquals(Intent.ACTION_VIEW, activity.viewed.getAction());
                assertEquals(NavigationRouteGpxExporter.GPX_MIME_TYPE, activity.viewed.getType());
                assertTrue((activity.viewed.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
                assertNull(AppOutputFolderSettings.get(activity, Kind.GPX));
            } finally {
                Files.delete(file.toPath());
            }
        } finally {
            worker.shutdown();
        }
    }

    @Test
    public void missingViewerIsHandledWithoutChangingFolder() throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try (ActivityController<ViewerActivity> controller = activity()) {
            ViewerActivity activity = controller.get();
            TestOutputDocumentsProvider.install(activity);
            String selected = TestOutputDocumentsProvider.TREE.toString();
            AppOutputFolderSettings.set(activity, Kind.LOGS, selected);
            activity.rejectViewer = true;
            new AboutOutputFolderBrowser(activity, worker).open(Kind.LOGS);
            await(worker);
            assertEquals(activity.getString(R.string.msg_output_viewer_unavailable), ShadowToast.getTextOfLatestToast());
            assertEquals(selected, AppOutputFolderSettings.get(activity, Kind.LOGS));
        } finally {
            worker.shutdown();
        }
    }

    @Test
    public void emptyDefaultFolderShowsMessageWithoutCreatingFilesOrSelectingFolder() throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try (ActivityController<ViewerActivity> controller = activity()) {
            ViewerActivity activity = controller.get();
            AppOutputFolderSettings.set(activity, Kind.GPX, null);
            new AboutOutputFolderBrowser(activity, worker).open(Kind.GPX);
            await(worker);
            AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
            android.widget.TextView message = dialog.findViewById(android.R.id.message);
            assertEquals(activity.getString(R.string.msg_output_folder_empty), message.getText().toString());
            assertNull(AppOutputFolderSettings.get(activity, Kind.GPX));
            assertNull(activity.viewed);
        } finally {
            worker.shutdown();
        }
    }

    @Test
    public void completionAfterActivityDestructionDoesNotLaunchViewer() throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        CountDownLatch release = new CountDownLatch(1);
        try (ActivityController<ViewerActivity> controller = activity()) {
            ViewerActivity activity = controller.get();
            TestOutputDocumentsProvider.install(activity);
            AppOutputFolderSettings.set(activity, Kind.GPX, TestOutputDocumentsProvider.TREE.toString());
            worker.submit(() -> {
                try {
                    release.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            new AboutOutputFolderBrowser(activity, worker).open(Kind.GPX);
            controller.pause().stop().destroy();
            release.countDown();
            await(worker);
            assertNull(activity.viewed);
        } finally {
            release.countDown();
            worker.shutdown();
        }
    }

    private static ActivityController<ViewerActivity> activity() {
        ActivityController<ViewerActivity> controller = Robolectric.buildActivity(ViewerActivity.class);
        controller.get().setTheme(R.style.Theme_ViBRoNavigator);
        return controller.setup();
    }

    private static void await(ExecutorService worker) throws Exception {
        worker.submit(() -> {}).get(10, TimeUnit.SECONDS);
        shadowOf(Looper.getMainLooper()).idle();
    }

    private static int indexOf(ListView list, String name) {
        for (int i = 0; i < list.getCount(); i++) {
            if (name.equals(list.getItemAtPosition(i))) {
                return i;
            }
        }
        throw new AssertionError("File was not listed");
    }

    public static class ViewerActivity extends Activity {
        Intent viewed;
        boolean rejectViewer;

        @Override
        public void startActivity(Intent intent) {
            if (rejectViewer) {
                throw new ActivityNotFoundException();
            }
            viewed = intent;
        }
    }
}

package vibro.navigator.about;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowToast;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import vibro.navigator.R;
import vibro.navigator.android.storage.TestOutputDocumentsProvider;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppOutputFolderSettings;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;

@RunWith(RobolectricTestRunner.class)
public class AboutOutputFolderSettingsTest {
    private static final String GPX_FOLDER = "content://example/tree/Trips";
    @Test
    public void eachSwitchHasIndependentFolderDialogAndPicker() {
        try (ActivityController<AboutActivity> controller = activity()) {
            AboutActivity activity = controller.get();
            AppOutputFolderSettings.set(activity, Kind.LOGS, null);
            AppOutputFolderSettings.set(activity, Kind.GPX, GPX_FOLDER);

            AlertDialog logs = open(activity, R.id.aboutLogFolderSettingsButton);
            TextView message = logs.findViewById(android.R.id.message);
            assertTrue(message.getText().toString().contains(activity.getString(R.string.label_output_folder_default)));
            assertNotNull(logs.getButton(AlertDialog.BUTTON_NEUTRAL));
            logs.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            shadowOf(Looper.getMainLooper()).idle();
            assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE,
                    shadowOf(activity).getNextStartedActivityForResult().intent.getAction());

            AlertDialog gpx = open(activity, R.id.aboutGpxFolderSettingsButton);
            message = gpx.findViewById(android.R.id.message);
            assertTrue(message.getText().toString().contains("Trips"));
            gpx.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
            assertEquals(GPX_FOLDER, AppOutputFolderSettings.get(activity, Kind.GPX));
            assertNull(AppOutputFolderSettings.get(activity, Kind.LOGS));
        }
    }

    @Test
    public void cancelledPickerAfterRecreationLeavesSelectionUnchanged() {
        try (ActivityController<AboutActivity> controller = activity()) {
            AboutActivity activity = controller.get();
            AppOutputFolderSettings.set(activity, Kind.GPX, GPX_FOLDER);
            AlertDialog dialog = open(activity, R.id.aboutGpxFolderSettingsButton);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            shadowOf(Looper.getMainLooper()).idle();
            int requestCode = shadowOf(activity).getNextStartedActivityForResult().requestCode;
            controller.recreate();
            controller.get().onActivityResult(requestCode, Activity.RESULT_CANCELED, null);
            assertEquals(GPX_FOLDER, AppOutputFolderSettings.get(controller.get(), Kind.GPX));
        }
    }

    @Test
    public void bothFolderDialogsRequireConfirmationAndCancelKeepsFiles() throws IOException {
        try (ActivityController<AboutActivity> controller = activity()) {
            AboutActivity activity = controller.get();
            TestOutputDocumentsProvider provider = TestOutputDocumentsProvider.install(activity);
            AppOutputFolderSettings.set(activity, Kind.LOGS, TestOutputDocumentsProvider.TREE.toString());
            AppOutputFolderSettings.set(activity, Kind.GPX, TestOutputDocumentsProvider.TREE.toString());
            provider.createDocument("output", "text/plain", "vibro-navigator-log-20261009120000.txt");
            provider.createDocument("output", "application/gpx+xml", "vibro-navigator-route-20261009120000.gpx");
            for (int button : new int[]{R.id.aboutLogFolderSettingsButton, R.id.aboutGpxFolderSettingsButton}) {
                AlertDialog folder = open(activity, button);
                folder.findViewById(R.id.outputFolderClearButton).performClick();
                idleDialogOpen();
                AlertDialog confirmation = ShadowAlertDialog.getLatestAlertDialog();
                TextView message = confirmation.findViewById(android.R.id.message);
                assertTrue(message.getText().toString().contains("Other files and subfolders will be kept"));
                assertEquals(2, provider.names().length);
                confirmation.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
                shadowOf(Looper.getMainLooper()).idle();
                assertEquals(2, provider.names().length);
                assertTrue(folder.isShowing());
                folder.dismiss();
            }
        }
    }

    @Test
    public void confirmedLogClearDeletesFilesAndKeepsSelection() throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try (ActivityController<AboutActivity> controller = activity()) {
            AboutActivity activity = controller.get();
            TestOutputDocumentsProvider provider = TestOutputDocumentsProvider.install(activity);
            AppOutputFolderSettings.set(activity, Kind.LOGS, TestOutputDocumentsProvider.TREE.toString());
            AppLogger.init(activity);
            AppLogger.setLoggingEnabled(activity, false);
            provider.createDocument("output", "text/plain", "vibro-navigator-log-20261009120000.txt");
            AboutOutputFolderSettings settings = new AboutOutputFolderSettings(activity, worker);
            settings.configure();
            AlertDialog folder = open(activity, R.id.aboutLogFolderSettingsButton);
            folder.findViewById(R.id.outputFolderClearButton).performClick();
            idleDialogOpen();
            ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            shadowOf(Looper.getMainLooper()).idle();
            worker.submit(() -> {}).get(10, TimeUnit.SECONDS);
            shadowOf(Looper.getMainLooper()).idle();
            assertEquals(0, provider.names().length);
            assertEquals(TestOutputDocumentsProvider.TREE.toString(), AppOutputFolderSettings.get(activity, Kind.LOGS));
            assertEquals(activity.getString(R.string.msg_output_folder_cleared), ShadowToast.getTextOfLatestToast());
            settings.shutdown();
        } finally {
            worker.shutdown();
        }
    }

    @Test
    public void openButtonsViewEachSelectedFolderWithoutChangingSettings() throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try (ActivityController<AboutActivity> controller = activity()) {
            AboutActivity activity = controller.get();
            TestOutputDocumentsProvider.install(activity);
            String selected = TestOutputDocumentsProvider.TREE.toString();
            AppOutputFolderSettings.set(activity, Kind.LOGS, selected);
            AppOutputFolderSettings.set(activity, Kind.GPX, selected);
            AboutOutputFolderSettings settings = new AboutOutputFolderSettings(activity, worker);
            settings.configure();
            for (int button : new int[]{R.id.aboutLogFolderSettingsButton, R.id.aboutGpxFolderSettingsButton}) {
                AlertDialog folder = open(activity, button);
                folder.findViewById(R.id.outputFolderOpenButton).performClick();
                idleDialogOpen();
                worker.submit(() -> {}).get(10, TimeUnit.SECONDS);
                shadowOf(Looper.getMainLooper()).idle();
                Intent viewed = shadowOf(activity).getNextStartedActivity();
                assertEquals(Intent.ACTION_VIEW, viewed.getAction());
                assertEquals(DocumentsContract.Document.MIME_TYPE_DIR, viewed.getType());
                assertTrue(folder.isShowing());
                folder.dismiss();
            }
            assertEquals(selected, AppOutputFolderSettings.get(activity, Kind.LOGS));
            assertEquals(selected, AppOutputFolderSettings.get(activity, Kind.GPX));
            settings.shutdown();
        } finally {
            worker.shutdown();
        }
    }

    @Test
    public void inaccessibleFolderReportsFailureAndKeepsSelection() throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try (ActivityController<AboutActivity> controller = activity()) {
            AboutActivity activity = controller.get();
            AppOutputFolderSettings.set(activity, Kind.GPX, GPX_FOLDER);
            AboutOutputFolderSettings settings = new AboutOutputFolderSettings(activity, worker);
            settings.configure();
            open(activity, R.id.aboutGpxFolderSettingsButton).findViewById(R.id.outputFolderOpenButton).performClick();
            idleDialogOpen();
            worker.submit(() -> {}).get(10, TimeUnit.SECONDS);
            shadowOf(Looper.getMainLooper()).idle();
            assertEquals(activity.getString(R.string.msg_output_folder_open_failed), ShadowToast.getTextOfLatestToast());
            assertEquals(GPX_FOLDER, AppOutputFolderSettings.get(activity, Kind.GPX));
            assertNull(shadowOf(activity).getNextStartedActivity());
            settings.shutdown();
        } finally {
            worker.shutdown();
        }
    }

    private static ActivityController<AboutActivity> activity() {
        Intent intent = new Intent().putExtra(AboutActivity.EXTRA_SCROLL_TO_SETTINGS, true);
        return Robolectric.buildActivity(AboutActivity.class, intent).setup();
    }

    private static AlertDialog open(AboutActivity activity, int buttonId) {
        activity.findViewById(buttonId).performClick();
        idleDialogOpen();
        return ShadowAlertDialog.getLatestAlertDialog();
    }

    private static void idleDialogOpen() {
        shadowOf(Looper.getMainLooper()).idleFor(AboutDeferredDialogAction.OPEN_DELAY_MS, TimeUnit.MILLISECONDS);
    }
}

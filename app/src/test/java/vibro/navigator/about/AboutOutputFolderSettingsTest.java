package vibro.navigator.about;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Looper;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import vibro.navigator.R;
import vibro.navigator.android.storage.AndroidOutputStorage;
import vibro.navigator.android.storage.TestDownloadsProvider;
import vibro.navigator.settings.AppOutputFolderSettings;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 35})
public class AboutOutputFolderSettingsTest {
    @Test
    public void dialogsShowActualFolderAndCompactOpenDeleteOkActions() {
        try (ActivityController<AboutActivity> controller = activity()) {
            AboutActivity activity = controller.get();
            AppOutputFolderSettings.set(activity, Kind.GPX, "content://old/tree/Trips");
            for (int button : new int[]{R.id.aboutLogFolderSettingsButton, R.id.aboutGpxFolderSettingsButton}) {
                AlertDialog dialog = open(activity, button);
                String message = ((TextView) dialog.findViewById(android.R.id.message)).getText().toString();
                assertTrue(message.contains("Current folder:"));
                assertFalse(message.contains("Trips"));
                assertEquals(activity.getString(android.R.string.ok), dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText());
                assertEquals(activity.getString(R.string.action_open_output_folder),
                        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).getText());
                assertEquals(activity.getString(R.string.action_clear_output_folder),
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).getText());
                assertEquals(ContextCompat.getColor(activity, R.color.danger),
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).getCurrentTextColor());
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                shadowOf(Looper.getMainLooper()).idle();
                assertFalse(dialog.isShowing());
                assertNull(shadowOf(activity).getNextStartedActivityForResult());
            }
        }
    }

    @Test
    public void deleteRequiresConfirmationAndCancelLeavesFolderDialogOpen() {
        try (ActivityController<AboutActivity> controller = activity()) {
            AboutActivity activity = controller.get();
            AlertDialog folder = open(activity, R.id.aboutGpxFolderSettingsButton);
            folder.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
            idleDialogOpen();
            AlertDialog confirmation = ShadowAlertDialog.getLatestAlertDialog();
            String message = ((TextView) confirmation.findViewById(android.R.id.message)).getText().toString();
            assertTrue(message.contains("Other files and subfolders will be kept"));
            confirmation.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
            assertTrue(folder.isShowing());
            folder.dismiss();
        }
    }

    @Test
    public void openLaunchesSystemBrowserWithoutDismissingSettings() throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try (ActivityController<AboutActivity> controller = activity()) {
            AboutActivity activity = controller.get();
            AboutOutputFolderSettings settings = new AboutOutputFolderSettings(activity, worker);
            settings.configure();
            for (int button : new int[]{R.id.aboutLogFolderSettingsButton, R.id.aboutGpxFolderSettingsButton}) {
                assertOpen(activity, worker, button);
            }
            settings.shutdown();
        } finally {
            worker.shutdown();
        }
    }

    @Test
    @Config(sdk = 35)
    public void failedWritePreparationStillLaunchesFilesForBothFolders() throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try (ActivityController<AboutActivity> controller = activity()) {
            AboutActivity activity = controller.get();
            TestDownloadsProvider.install(activity).failWrites = true;
            AndroidOutputStorage.resetFailures();
            AboutOutputFolderSettings settings = new AboutOutputFolderSettings(activity, worker);
            settings.configure();
            assertOpen(activity, worker, R.id.aboutLogFolderSettingsButton);
            assertOpen(activity, worker, R.id.aboutGpxFolderSettingsButton);
            settings.shutdown();
        } finally {
            worker.shutdown();
        }
    }

    private static void assertOpen(AboutActivity activity, ExecutorService worker, int button) throws Exception {
        AlertDialog folder = open(activity, button);
        folder.getButton(AlertDialog.BUTTON_NEUTRAL).performClick();
        idleDialogOpen();
        worker.submit(() -> {}).get(10, TimeUnit.SECONDS);
        shadowOf(Looper.getMainLooper()).idle();
        org.robolectric.shadows.ShadowActivity.IntentForResult launch =
                shadowOf(activity).getNextStartedActivityForResult();
        assertEquals(-1, launch.requestCode);
        Intent intent = launch.intent;
        assertNotNull(intent);
        assertEquals(vibro.navigator.android.storage.AndroidOutputBrowserActivity.class.getName(),
                intent.getComponent().getClassName());
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP, intent.getFlags());
        assertTrue(folder.isShowing());
        assertNull(folder.getListView());
        folder.dismiss();
    }

    private static ActivityController<AboutActivity> activity() {
        return Robolectric.buildActivity(AboutActivity.class,
                new Intent().putExtra(AboutActivity.EXTRA_SCROLL_TO_SETTINGS, true)).setup();
    }

    private static AlertDialog open(AboutActivity activity, int button) {
        activity.findViewById(button).performClick();
        idleDialogOpen();
        return ShadowAlertDialog.getLatestAlertDialog();
    }

    private static void idleDialogOpen() {
        shadowOf(Looper.getMainLooper()).idleFor(AboutDeferredDialogAction.OPEN_DELAY_MS, TimeUnit.MILLISECONDS);
    }
}

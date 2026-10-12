package vibro.navigator.about;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.widget.Button;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowToast;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import vibro.navigator.R;
import vibro.navigator.android.storage.AndroidOutputBrowserActivity;
import vibro.navigator.android.storage.AndroidOutputStorageBrowser;
import vibro.navigator.android.storage.TestDownloadsProvider;
import vibro.navigator.settings.AppOutputStorageSettings;
import vibro.navigator.settings.AppSettings;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class AboutDatabaseBackupActionsTest {
    private final Queue<Runnable> work = new ArrayDeque<>();
    private ActivityController<Activity> controller;
    private Activity activity;
    private AboutDatabaseBackupActions actions;
    private TestDownloadsProvider downloads;
    private Button exportButton;
    private Button importButton;
    private int refreshCount;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(Activity.class).setup();
        activity = controller.get();
        AppOutputStorageSettings.disable(activity);
        downloads = TestDownloadsProvider.install(activity);
        exportButton = new Button(activity);
        importButton = new Button(activity);
        actions = new AboutDatabaseBackupActions(activity,
                () -> AppSettings.setImperialUnitsEnabled(activity, true), () -> refreshCount++, work::add);
        actions.configure(exportButton, importButton);
        ShadowToast.reset();
    }

    @After
    public void tearDown() {
        controller.close();
    }

    @Test
    public void exportWaitsForPressFeedbackThenSavesWithFilenameToastWithoutPicker() throws Exception {
        exportButton.performClick();
        assertTrue(work.isEmpty());
        assertEquals(0, downloads.count());
        idlePress();
        assertEquals(1, work.size());
        assertEquals(0, downloads.count());
        assertTrue(AppSettings.isImperialUnitsEnabled(activity));
        finishWork();
        String name = downloads.row(downloads.lastUri).getAsString("_display_name");
        assertEquals(activity.getString(R.string.msg_database_exported, name), ShadowToast.getTextOfLatestToast());
        assertNull(shadowOf(activity).getNextStartedActivityForResult());
        assertNull(shadowOf(activity).getLastRequestedPermission());
        assertTrue(downloads.read(downloads.lastUri).contains("sharedPreferences"));
    }

    @Test
    public void importOpensBackupFolderAndSelectedJsonRestoresSettings() {
        exportButton.performClick();
        idlePress();
        finishWork();
        Uri backup = downloads.lastUri;
        ContentValues metadata = new ContentValues();
        metadata.put("mime_type", "application/octet-stream");
        downloads.update(backup, metadata, null, null);
        importButton.performClick();
        idlePress();
        finishWork();
        ShadowActivity.IntentForResult launch = shadowOf(activity).getNextStartedActivityForResult();
        shadowOf(activity).getNextStartedActivity();
        assertEquals(-1, launch.requestCode);
        assertEquals(AndroidOutputBrowserActivity.class.getName(), launch.intent.getComponent().getClassName());
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP, launch.intent.getFlags());
        AppSettings.setImperialUnitsEnabled(activity, false);
        try (ActivityController<AndroidOutputBrowserActivity> browser =
                     Robolectric.buildActivity(AndroidOutputBrowserActivity.class, launch.intent).setup()) {
            ShadowActivity.IntentForResult picker = shadowOf(browser.get()).getNextStartedActivityForResult();
            assertEquals(Intent.ACTION_OPEN_DOCUMENT, picker.intent.getAction());
            assertEquals("*/*", picker.intent.getType());
            assertTrue(picker.intent.hasExtra(DocumentsContract.EXTRA_INITIAL_URI));
            shadowOf(browser.get()).getNextStartedActivity();
            shadowOf(browser.get()).receiveResult(picker.intent, Activity.RESULT_OK, new Intent().setData(backup));
            Intent returned = shadowOf(browser.get()).getNextStartedActivity();
            actions.handleLaunchIntent(new Intent(returned), true);
            assertTrue(work.isEmpty());
            actions.handleLaunchIntent(returned, false);
            actions.handleLaunchIntent(returned, false);
            assertEquals(1, work.size());
            assertTrue(browser.get().isFinishing());
        }
        // The settings flush runs before the worker; change it again to prove the file is imported.
        AppSettings.setImperialUnitsEnabled(activity, false);
        assertFalse(AppSettings.isImperialUnitsEnabled(activity));
        finishWork();
        assertTrue(AppSettings.isImperialUnitsEnabled(activity));
        assertEquals(1, refreshCount);
        assertEquals(activity.getString(R.string.msg_database_imported), ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void failedExportShowsFailureWithoutPickerOrPartialFile() {
        downloads.failPublish = true;
        exportButton.performClick();
        idlePress();
        finishWork();
        assertEquals(0, downloads.count());
        assertEquals(activity.getString(R.string.msg_database_export_failed), ShadowToast.getTextOfLatestToast());
        assertNull(shadowOf(activity).getNextStartedActivityForResult());
    }

    @Test
    public void malformedBackupShowsFailureWithoutReplacingSettings() throws Exception {
        ContentValues metadata = new ContentValues();
        metadata.put("mime_type", "text/plain");
        Uri file = downloads.insert(Uri.parse("content://media/external_primary/downloads"), metadata);
        try (OutputStream out = activity.getContentResolver().openOutputStream(file)) {
            out.write("{\"schemaVersion\":999}".getBytes(StandardCharsets.UTF_8));
        }
        assertTrue(actions.handleActivityResult(4002, Activity.RESULT_OK, new Intent().setData(file)));
        finishWork();
        assertTrue(AppSettings.isImperialUnitsEnabled(activity));
        assertEquals(0, refreshCount);
        assertEquals(activity.getString(R.string.msg_database_import_failed), ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void duplicateTapsQueueOnlyOneBackup() {
        exportButton.performClick();
        idlePress();
        exportButton.performClick();
        idlePress();
        assertEquals(1, work.size());
        finishWork();
        assertEquals(1, downloads.count());
    }

    @Test
    public void cancelledImportAndUnknownResultsDoNothing() {
        assertTrue(actions.handleActivityResult(4002, Activity.RESULT_CANCELED, null));
        assertFalse(actions.handleActivityResult(4001, Activity.RESULT_OK, new Intent()));
        assertTrue(work.isEmpty());
        assertEquals(0, refreshCount);
        assertNull(ShadowToast.getTextOfLatestToast());
    }

    private static void idlePress() {
        shadowOf(Looper.getMainLooper()).idleFor(AboutDeferredDialogAction.OPEN_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private void finishWork() {
        work.remove().run();
        shadowOf(Looper.getMainLooper()).idle();
    }
}

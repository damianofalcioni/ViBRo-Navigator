package vibro.navigator.about;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Looper;
import android.widget.Switch;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import android.app.AlertDialog;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import vibro.navigator.R;
import vibro.navigator.android.storage.TestOutputStorageCard;
import vibro.navigator.android.storage.TestOutputMediaDirs;
import vibro.navigator.android.storage.TestOutputDocumentsProvider;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppGpxSettings;
import vibro.navigator.settings.AppOutputStorageSettings;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 28}, shadows = TestOutputMediaDirs.class)
public class AboutOutputStorageSwitchTest {
    @Before
    public void resetSelection() {
        AppOutputStorageSettings.disable(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void switchIsDisabledWithoutSdCard() {
        CardActivity.media = null;
        try (ActivityController<CardActivity> controller = activity()) {
            Switch view = controller.get().findViewById(R.id.aboutExternalStorageSwitch);
            assertFalse(view.isEnabled());
            assertFalse(view.isChecked());
        }
    }

    @Test
    public void enablingSdPromptsEvenWhenBothSavingSwitchesAreOffAndCancelLeavesItOff() {
        Application app = ApplicationProvider.getApplicationContext();
        CardActivity.media = new TestOutputStorageCard(app).media;
        AppGpxSettings.setAutoSaveOnStopEnabled(app, false);
        AppLogger.init(app);
        AppLogger.setLoggingEnabled(app, false);
        try (ActivityController<CardActivity> controller = activity()) {
            Switch view = controller.get().findViewById(R.id.aboutExternalStorageSwitch);
            assertTrue(view.isEnabled());
            view.setChecked(true);
            shadowOf(Looper.getMainLooper()).idleFor(300, TimeUnit.MILLISECONDS);
            ShadowAlertDialog.getLatestAlertDialog().getButton(-1).performClick();
            shadowOf(Looper.getMainLooper()).idle();
            assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE,
                    shadowOf(controller.get()).getNextStartedActivityForResult().intent.getAction());
            controller.get().onActivityResult(AboutOutputStorageSwitch.REQUEST_SD_FOLDER, Activity.RESULT_CANCELED, null);
            assertFalse(view.isChecked());
            assertFalse(AppGpxSettings.isAutoSaveOnStopEnabled(app));
            assertFalse(AppLogger.isLoggingEnabled());
        }
    }

    @Test
    public void grantedAccessAfterRecreationEnablesOnlyStorageSwitch() throws Exception {
        Application app = ApplicationProvider.getApplicationContext();
        CardActivity.media = new TestOutputStorageCard(app).media;
        AppGpxSettings.setAutoSaveOnStopEnabled(app, false);
        AppLogger.init(app);
        AppLogger.setLoggingEnabled(app, false);
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try (ActivityController<CardActivity> controller = activity()) {
            TestOutputDocumentsProvider provider = TestOutputDocumentsProvider.install(app,
                    "com.android.externalstorage.documents", TestOutputStorageCard.ID + ":");
            controller.recreate();
            AboutOutputFolderSettings settings = new AboutOutputFolderSettings(controller.get(), worker);
            settings.configure();
            Intent result = new Intent().setData(provider.tree(TestOutputStorageCard.ID + ":"))
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            assertTrue(settings.handleActivityResult(AboutOutputStorageSwitch.REQUEST_SD_FOLDER, Activity.RESULT_OK, result));
            worker.submit(() -> {}).get(10, TimeUnit.SECONDS);
            shadowOf(Looper.getMainLooper()).idle();
            assertTrue(AppOutputStorageSettings.useSdCard(app));
            assertTrue(((Switch) controller.get().findViewById(R.id.aboutExternalStorageSwitch)).isChecked());
            assertFalse(AppGpxSettings.isAutoSaveOnStopEnabled(app));
            assertFalse(AppLogger.isLoggingEnabled());
            settings.shutdown();
        } finally {
            worker.shutdown();
        }
    }

    @Test
    public void disablingAndReenablingReusesGrantWithoutShowingPicker() throws Exception {
        Application app = ApplicationProvider.getApplicationContext();
        CardActivity.media = new TestOutputStorageCard(app).media;
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try (ActivityController<CardActivity> controller = activity()) {
            CardActivity activity = controller.get();
            TestOutputDocumentsProvider provider = TestOutputDocumentsProvider.install(app,
                    "com.android.externalstorage.documents", TestOutputStorageCard.ID + ":");
            AboutOutputFolderSettings settings = new AboutOutputFolderSettings(activity, worker);
            settings.configure();
            Intent result = new Intent().setData(provider.tree(TestOutputStorageCard.ID + ":"))
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            settings.handleActivityResult(AboutOutputStorageSwitch.REQUEST_SD_FOLDER, Activity.RESULT_OK, result);
            flush(worker);
            Switch view = activity.findViewById(R.id.aboutExternalStorageSwitch);
            view.setChecked(false);
            shadowOf(Looper.getMainLooper()).idleFor(300, TimeUnit.MILLISECONDS);
            flush(worker);
            assertFalse(AppOutputStorageSettings.useSdCard(app));
            AlertDialog previous = ShadowAlertDialog.getLatestAlertDialog();
            view.setChecked(true);
            shadowOf(Looper.getMainLooper()).idleFor(300, TimeUnit.MILLISECONDS);
            flush(worker);
            assertTrue(AppOutputStorageSettings.useSdCard(app));
            assertTrue(view.isChecked());
            assertSame(previous, ShadowAlertDialog.getLatestAlertDialog());
            assertNull(shadowOf(controller.get()).getNextStartedActivityForResult());
            settings.shutdown();
        } finally {
            worker.shutdown();
        }
    }

    private static void flush(ExecutorService worker) throws Exception {
        worker.submit(() -> {}).get(10, TimeUnit.SECONDS);
        shadowOf(Looper.getMainLooper()).idle();
    }

    private static ActivityController<CardActivity> activity() {
        return Robolectric.buildActivity(CardActivity.class,
                new Intent().putExtra(AboutActivity.EXTRA_SCROLL_TO_SETTINGS, true)).setup();
    }

    public static class CardActivity extends AboutActivity {
        static File media;

        @Override
        public File[] getExternalMediaDirs() {
            return media == null ? super.getExternalMediaDirs() : new File[]{TestOutputMediaDirs.primary(this), media};
        }
    }
}

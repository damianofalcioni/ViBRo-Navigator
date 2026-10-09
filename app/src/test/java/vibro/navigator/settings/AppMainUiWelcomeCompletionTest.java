package vibro.navigator.settings;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.rules.TemporaryFolder;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.M)
public class AppMainUiWelcomeCompletionTest {
    @Rule
    public final TemporaryFolder storage = new TemporaryFolder();

    @Test
    public void completionSurvivesRepeatedWritesAndPreferenceReset() {
        Context context = ApplicationProvider.getApplicationContext();
        assertFalse(AppMainUiSettings.isWelcomeCompleted(context));

        AppMainUiSettings.completeWelcome(context);
        AppMainUiSettings.completeWelcome(context);
        context.getSharedPreferences("vibro.navigator.settings", Context.MODE_PRIVATE).edit().clear().commit();

        assertTrue(AppMainUiSettings.isWelcomeCompleted(context));
    }

    @Test
    public void importingBackupCannotCompleteWelcomeOnAnotherInstallation() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        AppMainUiSettings.completeWelcome(context);
        String backup = AppDataBackup.exportJson(context);
        File freshStorage = storage.newFolder();
        Context freshInstall = new ContextWrapper(context) {
            @Override
            public File getNoBackupFilesDir() {
                return freshStorage;
            }
        };

        AppDataBackup.importJson(freshInstall, backup);

        assertFalse(AppMainUiSettings.isWelcomeCompleted(freshInstall));
        assertTrue(AppMainUiSettings.isWelcomeCompleted(context));
    }

    @Test
    public void failedWriteDoesNotMarkWelcomeCompleted() throws Exception {
        File invalidStorage = storage.newFile();
        Context context = new ContextWrapper(ApplicationProvider.getApplicationContext()) {
            @Override
            public File getNoBackupFilesDir() {
                return invalidStorage;
            }
        };

        AppMainUiSettings.completeWelcome(context);

        assertFalse(AppMainUiSettings.isWelcomeCompleted(context));
    }
}
